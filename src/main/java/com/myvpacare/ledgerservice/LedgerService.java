package com.myvpacare.ledgerservice;


import com.ethercis.compositionservice.I_CompositionService;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.AnnotatedMBean;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.myvpacare.ledgerservice.consent.ContributionDescription;
import com.myvpacare.ledgerservice.consent.I_ConsentService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.ethercis.compositionservice.I_CompositionService.COMPOSITION_UID;

@Service(id = "ConsentLedgerService", version = "1.0", system = true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP")})

public class LedgerService extends ServiceDataCluster implements I_LedgerService, LedgerServiceMBean {

    private Logger log = LogManager.getLogger(LedgerService.class);
    private final String ME = "ConsentLedgerService";
    I_ConsentService consentService;

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);

        AnnotatedMBean.RegisterMBean(this.getClass().getCanonicalName(), LedgerServiceMBean.class, this);

        //retrieve consent service
        consentService = getRegisteredService(global, "ConsentService", "1.0");

        if (consentService == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Cannot find a running ConsentService, aborting");
        }

        log.info(getType() + " service started...");
    }


    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/consent", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/consent", responseType = ResponseType.Json)
    })
    //only POST consent (that is: WRITE ONLY)
    //Blockchain TxID is required to feed contribution description
    /**
     * receives a notification of a new transaction by TxID
     * the following parameters are expected:
     * - ehrId
     * - committerId
     * - committerName
     * - description (format: <feeder>::TxID)
     * The payload format is keyvalues ECISFLAT
     * @return
     */
    public Object process(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        //EHR ID must be supplied in the POST parameters (it is the stream key)
        UUID ehrId = UUID.fromString(props.getClientProperty(I_CompositionService.EHR_ID, (String) null));
        UUID committerUuid = auditSetter.getCommitterUuid();
        UUID systemUuid = auditSetter.getSystemUuid();
        String txID = props.getClientProperty(TXID, (String) null);
        if (txID == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "parameter " + TXID + " is required in request");

        String description = new ContributionDescription(txID).stringValue();

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content cannot be empty for a new composition");

        //invoke a consent insert
        if (!consentService.contributionDescriptionExists(description)) {
            UUID compositionId = consentService.insert(ehrId, content, committerUuid, systemUuid, description);

            //create json response
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
            Map<String, Object> retmap = new HashMap<>();
            retmap.put("action", "CREATE");
            retmap.put(COMPOSITION_UID, compositionId);
            Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, compositionId, 1, null));
            retmap.putAll(metaref);
            return retmap;
        } else {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_NO_CONTENT);
            //build the relative part of the link to the existing last version
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("Reason", "TxID has already been processed with description:" + description);
            return retMap;
        }
    }


    @Override
    public String getBuildVersion() {
        return BuildVersion.versionNumber;
    }

    @Override
    public String getBuildId() {
        return BuildVersion.projectId;
    }

    @Override
    public String getBuildDate() {
        return BuildVersion.buildDate;
    }

    @Override
    public String getBuildUser() {
        return BuildVersion.buildUser;
    }

    private String encodeURI(UUID ehrId, UUID compositionId, int version, I_CompositionService.CompositionFormat format) {
        StringBuffer encoded = new StringBuffer();

        if (compositionId != null)
            encoded.append(I_CompositionService.UID + "=" + compositionId);
        if (ehrId != null)
            encoded.append("&" + I_CompositionService.EHR_ID + "=" + ehrId);
        if (format != null)
            encoded.append("&" + I_CompositionService.FORMAT + "=" + format);

        return encoded.toString();
    }

}
