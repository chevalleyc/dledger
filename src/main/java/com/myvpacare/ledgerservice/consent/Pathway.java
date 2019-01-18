package com.myvpacare.ledgerservice.consent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;

public class Pathway {

    private final String pathwayKey;

    public Pathway(String pathwayKey) {
        this.pathwayKey = pathwayKey;
    }

    public String code(String json){
        Gson gson = new GsonBuilder().create();
        Map<String, Object> map = gson.fromJson(json, Map.class);

        return code(map);
    }

    public String code(Map<String, Object> map){
        String code = (String)map.get(pathwayKey);
        if (code != null)
            code = code.split("::")[1].split("\\|")[0];
        return code;
    }
}
