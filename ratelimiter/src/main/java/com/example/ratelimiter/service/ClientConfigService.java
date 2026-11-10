package com.example.ratelimiter.service;

import org.springframework.stereotype.Service;

import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.dto.ClientConfigPatchDTO;
import com.example.ratelimiter.repository.ClientConfigRepository;

@Service
public class ClientConfigService {

    private final ClientConfigRepository clientConfigRepository;

    public ClientConfigService(ClientConfigRepository clientConfigRepository){
        this.clientConfigRepository = clientConfigRepository;
    }

    public void addClient(ClientConfig config){
        validateClient(config);
        clientConfigRepository.save(config);
    }

    public ClientConfig getClient(String clientKey){
        return clientConfigRepository.findById(clientKey).orElse(null);
    }

    public void updateClientConfig(String clientKey , ClientConfigPatchDTO patch){
        ClientConfig client = clientConfigRepository.findById(clientKey).orElse(null);

        if(client == null){
            throw new RuntimeException("the client doesn't exist");
        }

        if(patch.getRequestsPerSecond() != null){
            client.setRequestsPerSecond(patch.getRequestsPerSecond());
        }

        if(patch.getBurstSize() != null){
            client.setBurstSize(patch.getBurstSize());
        }

        if(patch.getMode() != null){
            client.setMode(patch.getMode());
        }

        validateClient(client);

        clientConfigRepository.save(client);
    }

    private void validateClient(ClientConfig client){
        if(client.getBurstSize() <= 0 || client.getRequestsPerSecond() <= 0){
            throw new IllegalArgumentException("Invalid client config");
        }
    }

    public void validateProposedConfig(ClientConfig proposedConfig) {
        validateClient(proposedConfig);
    }
}
