package com.myvpacare.ledgerservice.consent;

import com.a.a.a.E;
import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.UUID;

/**
 * deals with ehr access policy for tables EHR and COMPOSITION
 */

public class BlockEhrCompositionPolicy {

    private final String POLICY_PREFIX = "ehr_block_";

    private final String EHR_BLOCK =
            "CREATE POLICY %s " +
            "ON %s.ehr " +
            "FOR ALL " +
            "USING(ehr.id <> '%s');";

    private final String COMPOSITION_BLOCK =
            "CREATE POLICY %s " +
                    "ON %s.composition " +
                    "FOR ALL " +
                    "USING(composition.ehr_id <> '%s');";

    private final String DROP_EHR_BLOCK = "DROP POLICY %s ON %s.ehr;";
    private final String DROP_COMPOSITION_BLOCK = "DROP POLICY %s ON %s.composition;";

    private final String schema;
    private final I_DomainAccess domainAccess;
    private final RunTimeSingleton global;
    private final UUID ehrId;

    public BlockEhrCompositionPolicy(String schema, I_DomainAccess domainAccess, RunTimeSingleton global, UUID ehrId) {
        this.schema = schema;
        this.domainAccess = domainAccess;
        this.global = global;
        this.ehrId = ehrId;
    }

    public void activate() throws ServiceManagerException {
        try {
            //drop enable all default policy
            new AllowAllPolicy(schema, domainAccess, global).disable();

            domainAccess.getContext().query(formatQuery(EHR_BLOCK)).execute();
            domainAccess.getContext().query(formatQuery(COMPOSITION_BLOCK)).execute();
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't enable policy:" + e);
        }
    }

    public void disable() throws ServiceManagerException {
        try {
            try {
                domainAccess.getContext().query(formatQuery(DROP_EHR_BLOCK)).execute();
            } catch (Exception e){} //ignore, policy didn't exist

            try {
                domainAccess.getContext().query(formatQuery(DROP_COMPOSITION_BLOCK)).execute();
            } catch (Exception e){} //as above

            //check if these were the last policies
            if (!new RLS(domainAccess, global).hasRLSPolicies())
                //reset the enable all default policy
                new AllowAllPolicy(schema, domainAccess, global).activate();


        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't disable policy:" + e);
        }
    }

    public Boolean policyExists(){
        String selectExists = "select exists (select 1 from pg_policies where policyname = '%s')";
        return domainAccess.getContext().fetchOne(String.format(selectExists, policyName())).into(Boolean.class);
    }

    String formatQuery(String query){
        return String.format(query, policyName(), schema, ehrId);
    }

    String policyName(){
        return POLICY_PREFIX+formatEhrId(ehrId);
    }

    String formatEhrId(UUID ehrId){
        return ehrId.toString().replaceAll("-", "_");
    }
}
