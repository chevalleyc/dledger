package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_EhrAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.dao.access.jooq.CompositionAccess;
import com.ethercis.dao.access.support.TestHelper;
import com.ethercis.ehr.building.util.CompositionAttributesHelper;
import com.ethercis.ehr.building.util.ContextHelper;
import com.ethercis.persistence.I_ResourceService;
import com.ethercis.persistence.RoleControlledSession;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.myvpacare.ledgerservice.AccessTestCase;
import com.myvpacare.ledgerservice.DummyComposition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.content.entry.Action;
import org.openehr.rm.datatypes.text.CodePhrase;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsentServiceTest extends AccessTestCase {

    I_ConsentService consentService;
    I_ResourceService resourceService;

    private static Logger log = LogManager.getLogger(ConsentServiceTest.class);

    UUID committer, system;

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "src/test/resources/services.properties"
        });

        consentService = ClusterInfo.getRegisteredService(global, "ConsentService", "1.0", new Object[]{null});
        assertNotNull(consentService);
        resourceService = ClusterInfo.getRegisteredService(global, "ResourceService", "1.0", new Object[]{null});
        assertNotNull(resourceService);
        setupDomainAccess();
        committer = new ConsentCommitter(testDomainAccess, global).id();
        system = new ConsentSystem(testDomainAccess, global).id();

        //wipe out all RLS and policies for next tests
        new RLS(testDomainAccess, global).cancelAllPolicies();
        new AllowAllPolicy("ehr", testDomainAccess, global).activate();
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
    public void testInsertSimpleRevoke() throws Exception {

        I_DomainAccess sessionDomainAccess = new RoleControlledSession(global, resourceService).setRole("postgres", "");
        String txid = UUID.randomUUID().toString();

        String jsonPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/consentPayload_revoke.json")));

        //get a dummy ehr_id
        UUID dummyEhrId = TestHelper.createDummyEhr(sessionDomainAccess);
        log.info("create dummy ehr:" + dummyEhrId);

        Composition dummyComposition = new DummyComposition(sessionDomainAccess.getKnowledgeManager()).createDummyCompositionWithParameters(
                "VPA body height.v0",
                CompositionAttributesHelper.createComposer("ludwig", "NHS-UK", "999999-9991"),
                new CodePhrase("ISO_639-1", "en"),
                new CodePhrase("IANA_character-sets", "UTF-8"),
                new CodePhrase("ISO_3166-1", "GB"),
                ContextHelper.createDummyContext());

        I_CompositionAccess composition = new CompositionAccess(sessionDomainAccess.getContext(),
                sessionDomainAccess.getKnowledgeManager(),
                sessionDomainAccess.getIntrospectCache(),
                dummyComposition, null, dummyEhrId);

        UUID compositionId = composition.commit(committer, system, "test");

        //load the payload into the CDR
        UUID consentCompositionUUID = consentService.insert(dummyEhrId.toString(), jsonPayload, committer, system, txid);

        //retrieve this composition
        I_CompositionAccess compositionConsent = I_CompositionAccess.retrieveInstance(sessionDomainAccess, consentCompositionUUID);
        assertNotNull(compositionConsent);

        //check if the policy is active (this is a code 17 meaning access will be denied from now on)
        assertTrue(new BlockEhrCompositionPolicy("ehr", sessionDomainAccess, global, dummyEhrId).policyExists());

        //check that access to this EHR is now blocked
        sessionDomainAccess = new RoleControlledSession(global, resourceService).setRole("ethercis", "ethercis");

        assertNull(I_CompositionAccess.retrieveInstance(sessionDomainAccess, compositionId));
        assertNull(I_EhrAccess.retrieveInstance(sessionDomainAccess, dummyEhrId));

        sessionDomainAccess = new RoleControlledSession(global, resourceService).setRole("postgres", "postgres");

        assertNotNull(I_CompositionAccess.retrieveInstance(sessionDomainAccess, compositionId));
        assertNotNull(I_EhrAccess.retrieveInstance(sessionDomainAccess, dummyEhrId));

        //wipe out all RLS and policies for next tests
        new RLS(sessionDomainAccess, global).cancelAllPolicies();

        //delete this composition (for test purpose only)
        compositionConsent.delete(committer, system, txid);

        //delete the dummy ehr
        I_EhrAccess.retrieveInstance(sessionDomainAccess, dummyEhrId).delete(committer, system, txid);
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