package com.myvpacare.ledgerservice;

import com.myvpacare.ledgerservice.consent.ConsentServiceTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class McStreamTest extends AccessTestCase {

    private static Logger log = LogManager.getLogger(McStreamTest.class);

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "src/test/resources/services.properties"
        });
    }


    /**
     * this test assumes a running local multichain with a stream 'stream1' and a published key 'key4'
     */
    public void testSubscribeGetKey() throws McException, ExecutionException, InterruptedException {

        McStream mcStream = new McStream(global, "stream1");

        mcStream.subscribe(true);

        mcStream.pollStreamKey("key3");
    }
}