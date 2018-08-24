package com.example.highrestclienttest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class MFCAuthTestService {

    private static final Logger LOG = LoggerFactory.getLogger(MFCAuthTestService.class);
    private String BASE_URL = "http://localhost:8345/mcf-authority-service";



    public List<String> getAllowsTokens(String domain) throws IOException {

        StringBuilder urlBuffer = new StringBuilder(BASE_URL);
        urlBuffer.append("/UserACLs");

        // ha az érkezett domain nev > 0, akkor hibás legyen, csak 1 userrel dolgozunk

        urlBuffer.append("?");
        urlBuffer.append("username=");
        // ha a domain nem szabványos email cím...hiba
        urlBuffer.append(domain);


        URL authURL = new URL(urlBuffer.toString());
        HttpURLConnection connection = (HttpURLConnection) authURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("content-Type", "text/html");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        //        connection.setInstanceFollowRedirects(false);

        List<String> tokenList = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            if (connection.getResponseCode() == 200) {
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    if (line.startsWith("TOKEN:")) {
                        tokenList.add(line.substring("TOKEN:".length()));
                    } else {
                        LOG.info("Authory response..." + line);
                    }
                }
            } else {
                LOG.warn("Authentication error.");
                throw new MCFAuthorizerException("Authentication error.");
            }


        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw new MCFAuthorizerException(ex.getMessage());
        }
        return tokenList;
    }


}
