package com.myvpacare.ledgerservice;

import multichain.command.GrantCommand;
import multichain.command.MultichainException;

import java.util.Properties;

/**
 * grant/revoke permission to connect for a peer
 * DO NOT USE. Use the CLI instead. This is an administrative procedure.
 */
public class McGrant extends McCommand {


    public McGrant(Properties properties) {
        super(properties);
    }

    /**
     * grant a permission connect to a server having initialized a chain with address provided by
     * multichaind chain1@[ip-address]:[port]
     *
     * @return
     */
    public String connect(String address) throws MultichainException {
        try {
            return getGrantCommand().grant(address, GrantCommand.CONNECT);
        } catch (MultichainException e) {
            throw new McException(e.getObject(), e.getMessage());
        }
    }

    public String revoke(String address) throws McException {
        try {
            return getGrantCommand().revoke(address, GrantCommand.CONNECT);
        } catch (MultichainException e) {
            throw new McException(e.getObject(), e.getMessage());
        }
    }
}
