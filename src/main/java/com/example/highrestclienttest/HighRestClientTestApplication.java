package com.example.highrestclienttest;

import com.example.highrestclienttest.configs.IndexConfiguration;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HighRestClientTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighRestClientTestApplication.class, args);

    }

    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200)
                )
        );
    }

    @Bean
    public IndexConfiguration jobIndexConfiguration() {
        return IndexConfiguration.builder().build();
    }

}
