package com.myvpacare.ledgerservice;

import multichain.command.RuntimeParameters;

import java.util.Properties;

public class McRuntimeParameters {

    RuntimeParameters runtimeParameters;

    public McRuntimeParameters(Properties properties) {

        runtimeParameters = new RuntimeParameters(
                (String) properties.getOrDefault("multichain.runtime.datadir", ""),
                (String) properties.getOrDefault("multichain.runtime.rpcport", "")
        );
    }

    public RuntimeParameters instance(){
        return runtimeParameters;
    }
}
