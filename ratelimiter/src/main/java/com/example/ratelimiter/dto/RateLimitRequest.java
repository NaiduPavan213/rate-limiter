package com.example.ratelimiter.dto;

public class RateLimitRequest {
    private String clientKey;

    public String getClientKey(){
        return clientKey;
    }

    public void setClientKey(String key){
        this.clientKey = key;
    }
}
