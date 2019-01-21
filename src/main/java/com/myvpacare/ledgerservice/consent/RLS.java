package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.UUID;

/**
 * deals with Row Level Security support at DB level
 */
public class RLS {

    private static Logger log = LogManager.getLogger(RLS.class);
    private final RunTimeSingleton global;
    private final I_DomainAccess domainAccess;

    public RLS(I_DomainAccess domainAccess, RunTimeSingleton global) {
        this.domainAccess = domainAccess;
        this.global = global;
    }

    /**
     * invoked if consent.enabled == true
     * At this stage, a default allow all policy is activated to the table.
     */
    public void enableRLS() throws ServiceManagerException {
        try {
            domainAccess.getContext().query("ALTER TABLE ehr.ehr ENABLE ROW LEVEL SECURITY;").execute();
            domainAccess.getContext().query("ALTER TABLE ehr.composition ENABLE ROW LEVEL SECURITY;").execute();
            log.info("RLS is now activated on tables EHR and COMPOSITION");
        }catch (Exception e){
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't enable RLS:" + e);
        }
    }

    public Boolean hasRLSPolicies() throws ServiceManagerException {
        try {
            //check if that is the last policies removed, if yes re-enabled the default all policy
            Integer count = domainAccess.getContext().fetchOne("select count(*) from pg_policies;").into(Integer.class);

            return count> 0;

        }catch (Exception e){
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't enable RLS:" + e);
        }
    }

    public Boolean cancelAllPolicies(){
        //iterate through the policies and drop them one by one
        Result<Record> records = domainAccess.getContext().fetch("SELECT * FROM pg_policies");

        for (Record record: records){
            String dropPolicy = "DROP POLICY "
                    +record.get("policyname")
                    +" ON "
                    + record.get("schemaname")
                    +"."
                    +record.get("tablename");

            domainAccess.getContext().query(dropPolicy).execute();
        }
        return true;
    }
}
