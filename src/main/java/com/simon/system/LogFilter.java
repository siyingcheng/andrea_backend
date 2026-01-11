package com.simon.system;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@Slf4j
public class LogFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 0);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        try {
            String uri = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            String method = request.getMethod();
            String params = request.getParameterMap().isEmpty() ? "{}" : objectMapper.writeValueAsString(request.getParameterMap());

            String payload = getPayload(request.getContentAsByteArray(), request.getCharacterEncoding(), request.getContentType());

            log.info("Incoming Request: method={}, uri={}, params={}, body={}", method, uri, params, payload);
        } catch (Exception e) {
            log.warn("Failed to log request", e);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        try {
            int status = response.getStatus();
            String contentType = response.getContentType();
            String payload = getPayload(response.getContentAsByteArray(), response.getCharacterEncoding(), contentType);

            log.info("Outgoing Response: status={}, contentType={}, body={}", status, contentType, payload);
        } catch (Exception e) {
            log.warn("Failed to log response", e);
        }
    }

    private String getPayload(byte[] content, String charsetName, String contentType) {
        if (content == null || content.length == 0) return "";
        Charset charset = (charsetName == null) ? Charset.defaultCharset() : Charset.forName(charsetName);
        String raw = new String(content, charset);

        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try {
                Object json = objectMapper.readValue(raw, Object.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (Exception ignored) {
                // fall through to return raw if parsing fails
            }
        }
        return raw;
    }
}