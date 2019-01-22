package com.myvpacare.ledgerservice;

import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_EhrAccess;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.persistence.I_ResourceService;
import com.ethercis.persistence.RoleControlledSession;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.google.gson.GsonBuilder;
import com.myvpacare.ledgerservice.consent.ConsentCommitter;
import com.myvpacare.ledgerservice.consent.ConsentSystem;
import com.myvpacare.ledgerservice.consent.RLS;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class LedgerServiceTest extends TestServerSimulator{

    protected HttpClient client;

//    private static final String hostname = "188.166.246.78"

    @Before
    public void setUp() throws Exception {
        super.setUp();

        client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(30000); // 30 seconds timeout; if no server reply, the request expires
        client.start();
    }

    @Test
    public void testProcessConsentLoad() throws Exception {

        I_ResourceService resourceService = (I_ResourceService) global.getServiceRegistry().getService("ResourceService,1.0");
        I_DomainAccess testDomainAccess = resourceService.getDomainAccess();

        //do some RLS cleanup
        new RLS(testDomainAccess, global).cancelAllPolicies();

        UUID committer = new ConsentCommitter(testDomainAccess, global).id();
        UUID system = new ConsentSystem(testDomainAccess, global).id();

        String subjectCodeId = UUID.randomUUID().toString();
        String subjectCodePrefix = "99999-";

        String subjectNameSpace = "uk.nhs.nhs_number";

        String userId = "postgres";
        String password = "postgres";

        ContentResponse response;

        //login first!
        String sessionId = connect(userId, password);

        String requestString = "http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace;

        Request request = client.newRequest(requestString);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = request.send();

        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Origin", "http://localhost:1234");
        request.method(HttpMethod.GET);
        response = request.send();

        assertNotNull(response);
        ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        File consentPayload = new File("src/test/resources/consentPayload_revoke.json");
//        File flatjsonFile = new File("/Development/Dropbox/eCIS_Development/samples/"+testFile+".json");
        InputStream is = new FileInputStream(consentPayload);
        request = client.newRequest("http://" + hostname
                + ":" + httpPort
                + "/rest/v1/consent"
                + "?ehrId="+ehrId
                +"&committerId="+committer
                +"&systemId="+system
                +"&txId="+UUID.randomUUID().toString());
//        request = client.newRequest("http://" + hostname + ":"+httpPort+"/rest/v1/composition?format=FLAT&templateId=" + testTemplate.replaceAll(" ", "%20"));
        request.header("Content-Type", "application/json");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] jsonContent = new byte[(int) consentPayload.length()];
        int i = is.read(jsonContent);
        request.content(new BytesContentProvider(jsonContent), "application/json");
        response = request.send();
        assertNotNull(response);
        String compositionId = decodeBodyResponse(response).get("compositionUid");

        //==================== SWITCH ROLE TO LESS PRIVILEGE ===============================================================
        sessionId = connect("ethercis", "ethercis");

        //now retrieving the same composition should fail since the consent revokes accesses to this ehr
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + compositionId + "&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = request.send();
        assertNotNull(response);
        //output the content
        assertTrue(response.getContentAsString().contains("Request did not give any result"));

        //house keeping service
        //do some RLS cleanup
        testDomainAccess = new RoleControlledSession(global, resourceService).setRole("postgres", "postgres");
        new RLS(testDomainAccess, global).cancelAllPolicies();
        I_CompositionAccess.retrieveInstance(testDomainAccess, UUID.fromString(compositionId)).delete(committer, system, "house keeping");
        I_EhrAccess.retrieveInstance(testDomainAccess, ehrId).delete(committer, system, "house keeping");
    }

    public String connect(String userId, String password) throws InterruptedException, ExecutionException, TimeoutException {
        //login first!
        ContentResponse response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        //create ehr
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        return sessionId;
    }


    private Map<String, String> decodeBodyResponse(ContentResponse response) {
//        String body = response.getContentAsString();
        if (response.getStatus() == 200)
            return new GsonBuilder().create().fromJson(response.getContentAsString(), Map.class);
        else {
            Map<String, String> map = new HashMap<>();
            map.put("HTTP Error:" + response.getStatus(), response.getReason());
            return map;
        }
    }
}