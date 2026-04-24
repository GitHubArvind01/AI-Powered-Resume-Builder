package com.resumeai.auth.client;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.resumeai.auth.dtos.AdminResumeDTO;

@Component
public class AdminResumeClient {

    private final RestTemplate restTemplate;

    public AdminResumeClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AdminResumeDTO> getResumesByUserId(Long userId) {
        try {
            ResponseEntity<List<AdminResumeDTO>> response = restTemplate.exchange(
                    "http://RESUME-SERVICE/api/v1/resumes/user/{userId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AdminResumeDTO>>() {},
                    userId
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }
}
