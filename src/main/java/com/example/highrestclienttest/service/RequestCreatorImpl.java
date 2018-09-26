package com.example.highrestclienttest.service;

import com.example.highrestclienttest.beans.EsProxyConfig;
import com.example.highrestclienttest.beans.Fq;
import com.example.highrestclienttest.beans.UIFilterQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;

public class RequestCreatorImpl implements RequestCreator {


   @Override
    public UIFilterQuery uiQueryConfiguration(String requestBody) throws IOException {

       ObjectMapper mapper = new ObjectMapper();
       JsonNode requestBodyNode = mapper.readTree(requestBody);
       JsonNode paramsNode =requestBodyNode.path("bodyParams");

       UIFilterQuery uiQueryParams;
       String UiQueryParamsString = paramsNode.toString();
       uiQueryParams = mapper.readValue(UiQueryParamsString, new TypeReference<UIFilterQuery>(){});

        return uiQueryParams;
    }

    @Override
    public EsProxyConfig ProxyConfigurations(String requestBody) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestBodyNode = mapper.readTree(requestBody);

        JsonNode elasticParams =requestBodyNode.path("config").path("elasticParams");

        EsProxyConfig esProxyConfig;
        String esProxyConfigString = elasticParams.toString();
        esProxyConfig = mapper.readValue(esProxyConfigString, new TypeReference<EsProxyConfig>(){});

        esProxyConfig.setSize( requestBodyNode.path("config").path("itemsPerPage").asInt() );

        return  esProxyConfig;
    }

    @Override
    public SearchSourceBuilder highlightCreator(SearchSourceBuilder searchSourceBuilder, EsProxyConfig config) {
        return null;
    }

    @Override
    public String RNINameCreato(String queryString) {




        return null;
    }

    @Override
    public BoolQueryBuilder filterQueryCreator(List<Fq> fqs) {
        return null;
    }

    @Override
    public BoolQueryBuilder authenticationFilterCreator(String userDomainName) {
        return null;
    }

    @Override
    public SearchSourceBuilder aggregationsCreator(SearchSourceBuilder searchSourceBuilder, JsonNode requestBody) {
        return null;
    }

    @Override
    public SearchRequestBuilder searchRequestCreator(EsProxyConfig config) {
        return null;
    }
}
