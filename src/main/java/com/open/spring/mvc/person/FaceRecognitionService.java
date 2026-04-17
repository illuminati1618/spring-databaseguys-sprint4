package com.open.spring.mvc.person;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class FaceRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Identifies a person from a query image against candidates.
     * Proxies the request to the Flask backend's /api/face/identify endpoint.
     */
    public Map<String, Object> identify(String queryImage, Double threshold) {
        // Automatically extract JWT token from the current request context if available
        String jwtToken = extractJwtFromContext();
        return identify(queryImage, threshold, jwtToken);
    }

    /**
     * Identifies a person from a query image against candidates.
     * Proxies the request to the Flask backend's /api/face/identify endpoint.
     */
    public Map<String, Object> identify(String queryImage, Double threshold, String jwtToken) {
        String flaskUrl = "http://127.0.0.1:8587/api/face/identify";
        
        // Environment detection: Use request host if available to decide between local and production
        HttpServletRequest currentRequest = getCurrentRequest();
        if (currentRequest != null) {
            String host = currentRequest.getServerName();
            if (host != null && (host.contains("opencodingsociety.com") || host.contains("open-coding-society.github.io"))) {
                flaskUrl = "https://flask.opencodingsociety.com/api/face/identify";
            }
        }
        
        // Secondary environment detection: Fallback to OS-based or system property if request context is missing
        if (flaskUrl.contains("127.0.0.1") && System.getProperty("os.name").toLowerCase().contains("linux")) {
             // In some deployments, even if host is unknown, if it's linux it's likely prod
             // flaskUrl = "https://flask.opencodingsociety.com/api/face/identify";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // If jwtToken was not passed but we can find it in context, use it
        if (jwtToken == null) {
            jwtToken = extractJwtFromContext();
        }

        if (jwtToken != null && !jwtToken.isEmpty()) {
            headers.add("Cookie", "jwt_java_spring=" + jwtToken);
            // Also add as Authorization header for flexibility
            headers.add("Authorization", "Bearer " + jwtToken);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("image", queryImage);
        if (threshold != null) {
            payload.put("threshold", threshold);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            logger.info("Proxying IDENTIFY request to Face Recognition engine at: {}, token present: {}", flaskUrl, jwtToken != null);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(flaskUrl, requestEntity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error communicating with Pirna-flask Face Recognition Service at " + flaskUrl, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("match", false);
            errorResult.put("message", "Error communicating with the Face Recognition engine");
            return errorResult;
        }
    }

    private String extractJwtFromContext() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            // Check Cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt_java_spring".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            // Check Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return (attrs != null) ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
