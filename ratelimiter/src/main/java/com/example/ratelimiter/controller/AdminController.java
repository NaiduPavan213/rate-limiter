package com.example.ratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.dto.ClientConfigPatchDTO;
import com.example.ratelimiter.service.ClientConfigService;
import com.example.ratelimiter.service.RateLimitService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private ClientConfigService clientConfigService;
    private RateLimitService rateLimitService;

    public AdminController(ClientConfigService clientConfigService , RateLimitService rateLimitService){
        this.clientConfigService = clientConfigService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/clients")
    public String createClient(@RequestBody ClientConfig config){

        clientConfigService.addClient(config);

        return "Client " + config.getClientKey() + " Configured";
    }

    @GetMapping("/clients/{clientKey}")
    public ClientConfig getClient(@PathVariable String clientKey){
        return clientConfigService.getClient(clientKey);
    }

    @PatchMapping("/clients/{clientKey}")
    public String updateClient(@PathVariable String clientKey , @RequestBody ClientConfigPatchDTO patch){
        rateLimitService.updateClient(clientKey , patch);

        return "Client with " + clientKey + " got updated";
    }
}
