package com.example.highrestclienttest;

import com.example.highrestclienttest.beans.TestData;
import com.example.highrestclienttest.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ElasticSearchService elasticSearchService;


    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }

    private void initializeData() throws IOException {
        TestData robesz = TestData.builder()
                .name(Arrays.asList("Robesz", "Gabi"))
                .age("25")
                .hobby("developing")
                .allow_token_parent("Precognox:S-1-5-21-3014129096-3214889382-4178971525-1157")
                .build();

        elasticSearchService.index();
        elasticSearchService.createRecord(robesz);
    }


}
