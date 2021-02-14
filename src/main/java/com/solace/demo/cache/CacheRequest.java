package com.solace.demo.cache;

import com.google.gson.Gson;

import lombok.Data;

@Data
public class CacheRequest {
    private String topic;
    private String replyTo;

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    static public CacheRequest fromJson(String json){
        Gson gson = new Gson();
        return gson.fromJson(json, CacheRequest.class);
    }
}
