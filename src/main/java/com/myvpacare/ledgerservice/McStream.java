package com.myvpacare.ledgerservice;

import com.dyngr.Poller;
import com.dyngr.PollerBuilder;
import com.dyngr.core.*;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import multichain.command.MultichainException;
import multichain.object.StreamKeyItem;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * JSON-RPC connector to Multichain
 */
public class McStream extends McCommand {

    String stream;
    private Logger log = LogManager.getLogger(McStream.class);

    public McStream(Properties properties, String stream) {
        super(properties);
        this.stream = stream;
    }

    public McStream(RunTimeSingleton runTimeSingleton, String stream) {
        super(runTimeSingleton);
        this.stream = stream;
    }


    public String publish(List<String> keys, String content) throws McException {
        try {
            String key = String.join(",", keys);
            return getStreamCommand().publish(stream, key, new HexString(content).encode());
        } catch (MultichainException e) {
            throw new McException(e.getObject(), e.getMessage());
        }
    }

    public void subscribe(boolean rescan) throws McException {
        try {
            getStreamCommand().subscribe(stream, rescan);
        } catch (MultichainException e) {
            throw new McException(e.getObject(), e.getMessage());
        }
    }

    public String pollStreamKey(String key) throws ExecutionException, InterruptedException {
        Poller<String> poller = PollerBuilder.<String>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .polling(
                        new AttemptMaker<String>() {
                            @Override
                            public AttemptResult<String> process() throws Exception {
                                //perform a key retrieval in stream
                                try {
                                    String payload = getStreamCommand().getLatestStreamKeyItem(stream, key, false).get("data").toString();
                                    if (payload != null)
                                        return AttemptResults.finishWith(payload);
                                    else
                                        return AttemptResults.justContinue();
                                } catch (Exception e){
                                    log.error("Got error decoding stream key"+key+", error:"+e);
                                }
                                return AttemptResults.finishWith(null);
                            }
                        }
                ).build();

        return poller.start().get();

    }


}
