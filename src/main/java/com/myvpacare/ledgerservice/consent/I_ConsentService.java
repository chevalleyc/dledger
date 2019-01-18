package com.myvpacare.ledgerservice.consent;

import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.Map;
import java.util.UUID;

public interface I_ConsentService {
    UUID insert(String ehrId, String jsonConsent, String txid) throws Exception;

    UUID insert(String ehrId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception;

    UUID insert(UUID ehrId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception;

    Boolean update(UUID compositionId, String jsonConsent, String txid) throws Exception;

    Boolean update(UUID compositionId, String jsonConsent, UUID committerId, UUID systemId, String txid) throws Exception;

    Map<String, Object> consentFor(UUID ehrId) throws Exception;

    void process(UUID ehrId, String jsonConsent) throws ServiceManagerException;

    Boolean contributionDescriptionExists(String description);
}
