package com.myvpacare.ledgerservice;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.partyservice.I_PartyIdentifiedService;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.google.gson.GsonBuilder;
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
import static org.junit.Assert.*;

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
    public void testProcessConsentLoad() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        String subjectCodeId = UUID.randomUUID().toString();
        String subjectCodePrefix = "99999-";

        String subjectNameSpace = "uk.nhs.nhs_number";

        String userId = "guest";
        String password = "guest";

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        //create ehr
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        String requestString = "http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace;

        Request request = client.newRequest(requestString);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = request.send();

//
//        Request request = client.newRequest("http://"+hostname+":"+httpPort+"/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
//        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
//        request.method(HttpMethod.GET);
//        response = stopWatchRequestSend(request);

//        log.severe(requestString);
//        log.severe(response.getContentAsString());
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Origin", "http://localhost:1234");
        request.method(HttpMethod.GET);
        response = request.send();

        assertNotNull(response);
        ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        File flatjsonFile = new File("src/test/resources/IDCR - Immunisation summary.v0.flat.json");
//        File flatjsonFile = new File("/Development/Dropbox/eCIS_Development/samples/"+testFile+".json");
        InputStream is = new FileInputStream(flatjsonFile);
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?format=FLAT&templateId=IDCR - Immunisation summary.v0".replaceAll(" ", "%20"));
//        request = client.newRequest("http://" + hostname + ":"+httpPort+"/rest/v1/composition?format=FLAT&templateId=" + testTemplate.replaceAll(" ", "%20"));
        request.header("Content-Type", "application/json");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] jsonContent = new byte[(int) flatjsonFile.length()];
        int i = is.read(jsonContent);
        request.content(new BytesContentProvider(jsonContent), "application/json");
        response = request.send();
        assertNotNull(response);
        String compositionId = decodeBodyResponse(response).get("compositionUid");

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