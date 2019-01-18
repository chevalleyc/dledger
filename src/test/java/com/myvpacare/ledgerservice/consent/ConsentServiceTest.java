package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_EhrAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.dao.access.support.TestHelper;
import com.ethercis.servicemanager.cluster.ClusterInfo;

import com.myvpacare.ledgerservice.AccessTestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.content.entry.Action;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsentServiceTest extends AccessTestCase {

    I_ConsentService consentService;

    private static Logger log = LogManager.getLogger(ConsentServiceTest.class);

    UUID committer, system;

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "src/test/resources/services.properties"
        });

        consentService = ClusterInfo.getRegisteredService(global, "ConsentService", "1.0", new Object[]{null});
        assertNotNull(consentService);
        setupDomainAccess();
        committer = new ConsentCommitter(testDomainAccess, global).id();
        system = new ConsentSystem(testDomainAccess, global).id();
    }

    @Test
    public void testInsertSimpleAllow() throws Exception {

        String txid =UUID.randomUUID().toString();

        String jsonPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/consentPayload.json")));

        //get a dummy ehr_id
        UUID dummyEhrId = TestHelper.createDummyEhr(testDomainAccess);
        log.info("create dummy ehr:"+dummyEhrId);

        //load the payload into the CDR
        UUID consentCompositionUUID = consentService.insert(dummyEhrId.toString(), jsonPayload, committer, system, txid);

        //retrieve this composition
        I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(testDomainAccess, consentCompositionUUID);
        assertNotNull(compositionAccess);

        //check if the policy is not active (this is a code 13)
        assertFalse(new BlockEhrCompositionPolicy("ehr", testDomainAccess, global, dummyEhrId).policyExists());

        //delete this composition (for test purpose only)
        compositionAccess.delete(committer, system, txid);

        //delete the dummy ehr
        I_EhrAccess.retrieveInstance(testDomainAccess, dummyEhrId).delete(committer, system, txid);

        //wipe out all RLS and policies for next tests
        new RLS(testDomainAccess, global).cancelAllPolicies();
    }

    @Test
    public void update() {
    }

    @Test
    public void testConsentFor() throws Exception {

        String txid ="1234567890";

        String jsonPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/consentPayload.json")));

        //get a dummy ehr_id
        UUID dummyEhrId = TestHelper.createDummyEhr(testDomainAccess);
        log.info("create dummy ehr:"+dummyEhrId);

        //load the payload into the CDR
        UUID consentCompositionUUID = consentService.insert(dummyEhrId.toString(), jsonPayload, committer, system, txid);

        //retrieve the consent for this ehr
        Map<String, Object> map = consentService.consentFor(dummyEhrId);

        assertEquals(consentCompositionUUID, UUID.fromString(((Map<String, Object>)(((List)map.get("resultSet")).get(0))).get("uid").toString().split("::")[0]));

        assertNotNull(map);

        I_CompositionAccess.retrieveInstance(testDomainAccess, consentCompositionUUID).delete(committer, system, txid);
        I_EhrAccess.retrieveInstance(testDomainAccess, dummyEhrId).delete(committer, system, txid);
    }

    @Test
    public void testUpdatePathway() throws Exception {

        String txid ="1234567890";

        String pathwayPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/pathwayPayload_1.json")));
        String jsonPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/consentPayload.json")));

        //get a dummy ehr_id
        UUID dummyEhrId = TestHelper.createDummyEhr(testDomainAccess);
        log.info("create dummy ehr:"+dummyEhrId);

        //load the payload into the CDR
        UUID consentCompositionUUID = consentService.insert(dummyEhrId.toString(), jsonPayload, committer, system, txid);

        Boolean result = consentService.update(consentCompositionUUID, pathwayPayload, committer, system, "Changed pathway");

        assertTrue(result);

        //retrieve the composition and check the pathway
        I_CompositionAccess composition = I_CompositionAccess.retrieveInstance(testDomainAccess, consentCompositionUUID);
        I_EntryAccess entryAccess = composition.getContent().get(0);

        Composition compositionNew = entryAccess.getComposition();

        //check changed pathway code
        String retrievedPathwayCode = ((Action)compositionNew.getContent().get(0)).getIsmTransition().getCareflowStep().getDefiningCode().getCodeString();
        assertEquals("at0015", retrievedPathwayCode);

        I_CompositionAccess.retrieveInstance(testDomainAccess, consentCompositionUUID).delete(committer, system, txid);
        I_EhrAccess.retrieveInstance(testDomainAccess, dummyEhrId).delete(committer, system, txid);
    }
}