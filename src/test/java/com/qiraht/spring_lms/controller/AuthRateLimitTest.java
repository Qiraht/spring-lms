package com.qiraht.spring_lms.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qiraht.spring_lms.AbstractContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AuthRateLimitTest extends AbstractContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsTooManyRequestsOnceLimitExceeded() throws Exception {
        String body = "{\"email\":\"x@y.z\",\"password\":\"whatever\"}";

        // given/when: within the default limit (3) the request is processed (not rate limited)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();
        }

        // then: the 4th request exceeds the configured rate limit and is rejected with 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
