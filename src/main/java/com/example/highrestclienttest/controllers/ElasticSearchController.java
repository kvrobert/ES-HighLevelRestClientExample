package com.example.highrestclienttest.controllers;

import com.example.highrestclienttest.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/rs/search")
public class ElasticSearchController {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @GetMapping(value = "/mapping", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getMapping() throws IOException {
        return new ResponseEntity<>(elasticSearchService.getMapping(), HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    private ResponseEntity advancedSearch(@RequestBody Map<String, String> params) throws IOException {
        return new ResponseEntity<>(elasticSearchService.advancedSearch(params).getHits(), HttpStatus.OK);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    private ResponseEntity simpleSearch(@RequestParam(value = "q") String q) throws IOException {
        return new ResponseEntity<>(elasticSearchService.simpleSearch(q).getHits(), HttpStatus.OK);
    }

}
