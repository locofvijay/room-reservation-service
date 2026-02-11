package com.assignment.reservation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    public WebClient.Builder WebClientBuilder(){
        return WebClient.builder();
     }
}
