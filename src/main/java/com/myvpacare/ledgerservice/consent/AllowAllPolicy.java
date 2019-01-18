package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public class AllowAllPolicy {

    private final String EHR_ALL = "CREATE POLICY ehr_user_all ON %s.ehr FOR ALL USING (true) WITH CHECK (true);";
    private final String COMPOSITION_ALL = "CREATE POLICY composition_user_all ON %s.composition FOR ALL USING (true) WITH CHECK (true);";
    private final String DROP_EHR_ALL = "DROP POLICY ehr_user_all ON %s.ehr;";
    private final String DROP_COMPOSITION_ALL = "DROP POLICY composition_user_all ON %s.composition;";

    private final String schema;
    private final I_DomainAccess domainAccess;
    private final RunTimeSingleton global;

    public AllowAllPolicy(String schema, I_DomainAccess domainAccess, RunTimeSingleton global) {
        this.schema = schema;
        this.domainAccess = domainAccess;
        this.global = global;
    }

    public void activate() throws ServiceManagerException {
        try {
            domainAccess.getContext().query(formatQuery(EHR_ALL)).execute();
            domainAccess.getContext().query(formatQuery(COMPOSITION_ALL)).execute();
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't enable policy:" + e);
        }
    }

    public void disable() throws ServiceManagerException {
        try {
            domainAccess.getContext().query(formatQuery(DROP_EHR_ALL)).execute();
            domainAccess.getContext().query(formatQuery(DROP_COMPOSITION_ALL)).execute();
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY, "ConsentService", "Couldn't disable policy:" + e);
        }
    }

    private String formatQuery(String query){
        return String.format(query, schema);
    }
}
