package com.example.ratelimiter.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiter.dto.RateLimitRequest;
import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.service.RateLimitService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/rate-limit")
public class RateLimiterController {

    private final RateLimitService rateLimitService;

    public RateLimiterController(RateLimitService rateLimitService){
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@RequestBody RateLimitRequest request){
        RateLimitResponse result = rateLimitService.getRateLimit(request.getClientKey());

        HttpHeaders headers = new HttpHeaders();

        headers.set("X-RateLimit-Limit" , String.valueOf(result.getLimit()));
        headers.set("X-RateLimit-Remaining" , String.valueOf(result.getCurrentTokens()));
        headers.set("X-RateLimit-Reset" , String.valueOf(result.getResetAfterSeconds()));

        HttpStatus status = result.isAllowed()
                ? HttpStatus.OK
                : HttpStatus.TOO_MANY_REQUESTS;

        return new ResponseEntity<>(result , headers , status);
    }

}
