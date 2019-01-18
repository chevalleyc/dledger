package com.myvpacare.ledgerservice.consent;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class PathwayTest {

    @Test
    public void testCode() throws IOException {

        String json = new String(Files.readAllBytes(Paths.get("src/test/resources/consentPayload.json")));

        String code = new Pathway("/content[openEHR-EHR-ACTION.informed_consent.v0]/ism_transition/careflow_step|value").code(json);

        assertEquals("at0013", code);

    }
}