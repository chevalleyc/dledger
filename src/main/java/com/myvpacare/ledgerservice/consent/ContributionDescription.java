package com.myvpacare.ledgerservice.consent;

import java.util.UUID;

/**
 * make sure that a normalized description is used to store compositions
 */
public class ContributionDescription {

    private final String transactionId; //this is multichain's TxID
    final String prefix = "BC-CONSENT-";

    public ContributionDescription(String transactionId) {
        this.transactionId = transactionId;
    }

    public String stringValue(){
        return prefix+"-"+transactionId;
    }

    public Boolean prefixEquals(String another){
        return prefix.equals(another);
    }
}
