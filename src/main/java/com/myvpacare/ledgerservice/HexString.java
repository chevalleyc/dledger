package com.myvpacare.ledgerservice;

import multichain.object.formatters.HexFormatter;

public class HexString {

    String content;

    public HexString(String content) {
        this.content = content;
    }

    public String encode(){
        return HexFormatter.toHex(content);
    }

    public String decode(){
        return HexFormatter.fromHex(content);
    }
}
