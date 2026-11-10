package com.example.ratelimiter.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class ClientConfig {
    @Id
    private String clientKey;

    private int requestsPerSecond;

    private int burstSize;

    @Enumerated(EnumType.STRING)
    private RateLimitMode mode;

    public String getClientKey(){
        return clientKey;
    }

    public int getRequestsPerSecond(){
        return requestsPerSecond;
    }

    public int getBurstSize(){
        return burstSize;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public void setMode(RateLimitMode rateLimitMode){
        this.mode = rateLimitMode;
    }

    public RateLimitMode getMode(){
        return this.mode;
    }

    public void setRequestsPerSecond(int requestsPerSecond){
        this.requestsPerSecond = requestsPerSecond;
    }

    public void setBurstSize(int burstSize){
        this.burstSize = burstSize;
    }
}
