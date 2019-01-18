package com.myvpacare.ledgerservice.consent;

import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.myvpacare.ledgerservice.AccessTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RLSTest extends AccessTestCase {

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "src/test/resources/services.properties"
        });

        setupDomainAccess();
    }

    @Test
    public void testCancelAllPolicies() {
        assertTrue(new RLS(testDomainAccess, global).cancelAllPolicies());
    }
}