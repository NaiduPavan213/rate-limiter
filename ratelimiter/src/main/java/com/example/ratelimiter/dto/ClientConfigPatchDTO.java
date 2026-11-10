package com.example.ratelimiter.dto;

import com.example.ratelimiter.model.RateLimitMode;

public class ClientConfigPatchDTO {
    private Integer requestsPerSecond;
    private Integer burstSize;
    private RateLimitMode mode;

    public Integer getRequestsPerSecond(){
        return requestsPerSecond;
    }

    public Integer getBurstSize(){
        return burstSize;
    }

    public RateLimitMode getMode(){
        return mode;
    }

    public void setRequestsPerSecond(Integer requestsPerSecond){
        this.requestsPerSecond = requestsPerSecond;
    }

    public void setBurstSize(Integer burstSize){
        this.burstSize = burstSize;
    }

    public void setMode(RateLimitMode mode){
        this.mode = mode;
    }
}
