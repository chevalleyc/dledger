package com.myvpacare.ledgerservice.consent;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_PartyIdentifiedAccess;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;

import java.util.UUID;

public class ConsentCommitter {

    I_DomainAccess domainAccess;
    String committerName;

    public ConsentCommitter(I_DomainAccess domainAccess, RunTimeSingleton global) {
        this.domainAccess = domainAccess;
        this.committerName = global.getProperty().get("consent.committerName", "CONSENT_COMMITTER");
    }

    public UUID id(){
        return I_PartyIdentifiedAccess.getOrCreateParty(domainAccess, committerName, null);
    }
}
