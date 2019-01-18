package com.myvpacare.ledgerservice;

import multichain.command.MultichainException;

public class McException extends MultichainException {

    public McException(String excep_object, String excep_reason) {
        super(excep_object, excep_reason);
    }
}
