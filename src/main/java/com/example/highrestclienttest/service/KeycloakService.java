package com.example.highrestclienttest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

@Service
public class KeycloakService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String getUsernameFromJWT(String jwt) {
        if (jwt == null) {
            return null;
        }

        int start = jwt.indexOf('.') + 1;
        int end = jwt.indexOf('.', start);
        if (start <= 0 || end <= start) {
            throw new IllegalStateException("Invalid jwt: " + jwt);
        }

        try {
            String payloadb64 = jwt.substring(start, end);
            byte[] payloadbytes = Base64.getDecoder().decode(payloadb64);
            JsonNode payload = MAPPER.readTree(payloadbytes);
            JsonNode email = payload.get("email");
            return email.asText();
        } catch (IOException ex) {
            throw new IllegalStateException("Invalid jwt: " + jwt, ex);
        }
    }
}
