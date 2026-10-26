
package com.avega.tax_gap_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotencyJacksonConfig {

    @Bean
    public ObjectMapper idempotencyObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
