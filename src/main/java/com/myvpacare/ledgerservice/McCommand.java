package com.myvpacare.ledgerservice;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import multichain.command.MultiChainCommand;

import java.util.Properties;

public abstract class McCommand extends MultiChainCommand {

    public McCommand(Properties properties){
        super(
                (String) properties.getOrDefault("multichain.host", "localhost"),
                (String) properties.getOrDefault("multichain.port", "6824"),
                (String) properties.getOrDefault("multichain.user", ""),
                (String) properties.getOrDefault("multichain.password", ""),
                new McRuntimeParameters(properties).instance());
    }

    public McCommand(RunTimeSingleton runTimeSingleton){
        super(
                (String) runTimeSingleton.getProperty().getProperties().getOrDefault("multichain.host", "localhost"),
                (String) runTimeSingleton.getProperty().getProperties().getOrDefault("multichain.port", "6824"),
                (String) runTimeSingleton.getProperty().getProperties().getOrDefault("multichain.user", ""),
                (String) runTimeSingleton.getProperty().getProperties().getOrDefault("multichain.password", ""),
                new McRuntimeParameters(runTimeSingleton.getProperty().getProperties()).instance());
    }
}
