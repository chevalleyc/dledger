package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_SystemAccess;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;

import java.util.UUID;

public class ConsentSystem {
    I_DomainAccess domainAccess;

    public ConsentSystem(I_DomainAccess domainAccess, RunTimeSingleton global) {
        this.domainAccess = domainAccess;
    }

    public UUID id() throws Exception {
        //check if system exists
        return I_SystemAccess.createOrRetrieveLocalSystem(domainAccess);
    }
}
