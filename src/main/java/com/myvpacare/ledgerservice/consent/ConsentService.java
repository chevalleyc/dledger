package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.handler.PvCompoHandler;
import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.ehr.json.FlatJsonUtil;
import com.ethercis.persistence.DataAccessExceptionMessage;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.exception.DataAccessException;

import java.io.StringReader;
import java.util.Map;
import java.util.UUID;

import static com.ethercis.jooq.pg.tables.Contribution.CONTRIBUTION;

/**
 * simple non REST service to deal with Consent
 */

@Service(id = "ConsentService", system = true)
@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 3, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 5, action = "STOP")})

public class ConsentService extends ServiceDataCluster implements I_ConsentService {

    private RunTimeSingleton global;
    private String ME = "ConsentService";
    private String VERSION = "1.0";
    private String templateId; //template id to use to store consent, pls make sure this template is in sync with supplied json payload
    private String pathwayKey; //path to use to identify the value of the consent state
    private static Logger log = LogManager.getLogger(ConsentService.class);
    private String schema;
    private Boolean useRLS = false;


    @Override
    protected void doInit(RunTimeSingleton runTimeSingleton, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(runTimeSingleton, serviceInfo);
        this.global = runTimeSingleton;
        templateId = global.getProperty().get("consent.template.id", "");
        pathwayKey = global.getProperty().get("consent.template.pathwaykey", "");

        schema = global.getProperty().get("consent.db.schema", "ehr");

        if (Boolean.parseBoolean(global.getProperty().get("consent.enabled", "false"))) {
            new RLS(this.getDataAccess(), global).enableRLS();
            //by default all accesses are granted
            new AllowAllPolicy(schema, getDataAccess(), global).activate();
            useRLS = true;
            log.info("RLS is ACTIVATED");
        }
        //register the service
        global.getServiceRegistry().register(ME + "," + VERSION, this);
        log.info(ME + " Started...");
    }


    /**
     * insert the consent into the CDR
     */

    private UUID insert(UUID ehrId, Map<String, Object> keyValues, UUID committer, UUID system, String description) throws Exception {

        UUID compositionUUID;

        try {
            if (!contributionDescriptionExists(description)) {
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), templateId, null);
                compositionUUID = pvCompoHandler.storeComposition(ehrId,
                        keyValues,
                        committer,
                        system,
                        description);

                if (compositionUUID != null && useRLS) //setup permission accordingly
                    processCode(ehrId, new Pathway(pathwayKey).code(keyValues));
            } //this transaction has already been processed
            else
                compositionUUID = null;
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.RESOURCE, "Could not insert new consent:" + e);
        }

        return compositionUUID;
    }

    @Override
    public UUID insert(UUID ehrId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception {
        Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(jsonConsent.getBytes())));
        return insert(ehrId,
                kvPairs,
                committerId,
                systemId,
                txid);
    }

    @Override
    public UUID insert(String ehrId, String jsonConsent, String txid) throws Exception {
        Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(jsonConsent.getBytes())));
        return insert(UUID.fromString(ehrId),
                kvPairs,
                new ConsentCommitter(this.getDataAccess(), global).id(),
                new ConsentSystem(this.getDataAccess(), global).id(),
                txid);
    }

    @Override
    public UUID insert(String ehrId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception {
        Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(jsonConsent.getBytes())));
        return insert(UUID.fromString(ehrId),
                kvPairs,
                committerId,
                systemId,
                txid);
    }


    private Boolean update(UUID compositionId, Map<String, Object> keyValues, UUID committer, UUID system, String txid) throws Exception {

        Boolean result;

        try {
            if (contributionDescriptionExists(txid)) {
                I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(this.getDataAccess(), compositionId);
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), compositionAccess, templateId, null);
                result = pvCompoHandler.updateComposition(
                        keyValues,
                        committer,
                        system,
                        txid);
                if (result && useRLS)
                    processCode(compositionAccess.getEhrid(), new Pathway(pathwayKey).code(keyValues));
            } else
                result = false;
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.RESOURCE, "Could not update consent:" + e);
        }

        return result;
    }

    @Override
    public Boolean update(UUID compositionId, String jsonConsent, String txid) throws Exception {
        Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(jsonConsent.getBytes())));
        return update(compositionId,
                kvPairs,
                new ConsentCommitter(this.getDataAccess(), global).id(),
                new ConsentSystem(this.getDataAccess(), global).id(), txid);
    }


    @Override
    public Boolean update(UUID compositionId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception {
        Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(jsonConsent.getBytes())));
        return update(compositionId,
                kvPairs,
                committerId,
                systemId,
                txid);
    }

    @Override
    public Map<String, Object> consentFor(UUID ehrId) throws Exception {

        //check if consent exists for this ehr id using AQL
        String aqlString = "select a/uid/value as uid \n" +
                "from EHR e[ehr_id/value='" + ehrId.toString() + "']\n" +
                "contains COMPOSITION a \n" +
                "WHERE a/archetype_details/template_id/value = '" + templateId + "'";

        Map<String, Object> result = I_EntryAccess.queryAqlJson(getDataAccess(), aqlString);
        return result;
    }

    /**
     * logically process a content payload
     * "/content[openEHR-EHR-ACTION.informed_consent.v0]/ism_transition/careflow_step|value": "local::at0013|Planned|",
     *
     * @param ehrId
     * @param jsonConsent
     */
    @Override
    public void process(UUID ehrId, String jsonConsent) throws ServiceManagerException {

        if (!useRLS)
            return;

        //pathway
        processCode(ehrId, new Pathway(pathwayKey).code(jsonConsent));
    }

    public void processCode(UUID ehrId, String pathwayCode) throws ServiceManagerException {

        if (pathwayCode.isEmpty())
            return;

        if (!useRLS)
            return;

        //TODO: grant/revoke access to the EHR depending on pathway value
        //at this stage just grant or revoke access to a particular EHR using Row Level Security
        //applies the policy to any user
        //further improvement will refine policy on a user/role level as long as impersonation is used
        //see: SET ROLE usage in VirtualEhr::ResourceAccessService::RoleControlledSession.java
        try {
            switch (pathwayCode) {
                case "at0013": //planned
                case "at0014": //requested
                case "at0015": //provided
                    //grant access
                    new BlockEhrCompositionPolicy(this.schema, this.getDataAccess(), global, ehrId).disable();
                    break;
                case "at0016": //refused
                case "at0017": //withdrawn
                case "at0018": //cancelled
                case "at0019": //postponed
                    //revoke access
                    new BlockEhrCompositionPolicy(this.schema, this.getDataAccess(), global, ehrId).activate();
                    break;
                case "at0021": //not obtained
                case "at0022": //complete
                    //do nothing?
                    break;
            }
        } catch (DataAccessException e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, "ServiceDataCluster", "Role/user denied access:" + new DataAccessExceptionMessage(e).error());
        }

    }

    /**
     * check if this consent transaction has already been processed
     * Each consent transaction is associated with a contribution (standard openEHR)
     * the contribution contains a description which in this case is the txid coming from
     * the blockchain
     *
     * @param description
     * @return
     */
    @Override
    public Boolean contributionDescriptionExists(String description) {
        return getDataAccess().getContext().fetchExists(CONTRIBUTION, CONTRIBUTION.DESCRIPTION.eq(description));
    }

}
