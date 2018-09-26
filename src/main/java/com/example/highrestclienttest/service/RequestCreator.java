package com.example.highrestclienttest.service;

import com.example.highrestclienttest.beans.EsProxyConfig;
import com.example.highrestclienttest.beans.Fq;
import com.example.highrestclienttest.beans.UIFilterQuery;
import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;

public interface RequestCreator {

    EsProxyConfig ProxyConfigurations(String requestBody) throws IOException;

    UIFilterQuery uiQueryConfiguration(String requestBody) throws IOException;

    SearchSourceBuilder highlightCreator(SearchSourceBuilder searchSourceBuilder, EsProxyConfig config);

    String RNINameCreato(String queryString);

    BoolQueryBuilder filterQueryCreator(List<Fq> fqs);

    BoolQueryBuilder authenticationFilterCreator(String userDomainName);

    SearchSourceBuilder aggregationsCreator(SearchSourceBuilder searchSourceBuilder, JsonNode requestBody);

    SearchRequestBuilder searchRequestCreator(EsProxyConfig config);

}
