package com.example.backend.config;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Non-retrying trace hook for outbound {@link org.springframework.web.client.RestTemplate} calls
 * (Overpass). Retries remain in Resilience4j on the service layer.
 */
@Slf4j
public class HttpRequestTraceInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("HTTP {} {}", request.getMethod(), request.getURI());
        }
        return execution.execute(request, body);
    }
}
