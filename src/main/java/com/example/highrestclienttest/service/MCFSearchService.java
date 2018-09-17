package com.example.highrestclienttest.service;


import com.basistech.rni.es.DocScoreFunctionBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MCFSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private MCFAuthService MCFAuthService;

    @Autowired
    private KeycloakService keycloakService;


    public Object simpleSearchTest( Map<String, String> params) throws IOException {

        final String USERNAME_DOMAIN;

        System.out.println("Q=" + params.get("q"));
        System.out.println("USER=" + params.get("u"));
        System.out.println("HEADER=" + params.get("KEYCLOAK_ACCESS_TOKEN"));


        if(params.get("KEYCLOAK_ACCESS_TOKEN") != null ){
            USERNAME_DOMAIN = keycloakService.getUsernameFromJWT(params.get("KEYCLOAK_ACCESS_TOKEN"));
        }else {
            USERNAME_DOMAIN = params.get("u") != null ? params.get("u") : "empty";
        }

        System.out.println(params.get("KEYCLOAK_ACCESS_TOKEN"));
        System.out.println("Domain: " + USERNAME_DOMAIN);

        BoolQueryBuilder authorizationFilter = MCFAuthService.getAuthFilter(USERNAME_DOMAIN);

        String QUERY_STRING = params.get("q") !=null ? params.get("q") : "*";
        QueryStringQueryBuilder from = QueryBuilders.queryStringQuery(QUERY_STRING);

        if(params.containsKey("df"))  from.defaultField(params.get("df"));
        if(params.containsKey("analyzer")) from.analyzer(params.get("analyzer"));
        if(params.containsKey("analyze_wildcard")) from.analyzeWildcard(Boolean.valueOf(params.get("analyze_wildcard")));
        if(params.containsKey("lenient")) from.lenient(Boolean.valueOf(params.get("lenient")));

        if(params.containsKey("default_operator")){
            if(params.get("default_operator").equals("OR")){
                from.defaultOperator(Operator.OR);
            }else if( params.get("default_operator").equals("AND")){
                from.defaultOperator(Operator.AND);
            }else{
                throw new IllegalArgumentException("Unsupported defaultOperator [" + params.get("default_operator") + "], can either be [OR] or [AND]");
            }
        }

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightPerson = new HighlightBuilder.Field("content_text");

        highlightBuilder.field(highlightPerson);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(from)
                .must(authorizationFilter)
        ).highlighter(highlightBuilder);

        if(params.containsKey("size")) {
            searchSourceBuilder.size(Integer.parseInt(params.get("size")));
        }

        if(params.containsKey("sort")){
            String var23 = params.get("sort");
            int suggestText;
            String indexName;
            String[] var26;

            String[] var24 = Strings.splitStringByCommaToArray(var23);
            var26 = var24;
            int var27 = var24.length;

            for(suggestText = 0; suggestText < var27; ++suggestText) {
                String suggestSize = var26[suggestText];
                int suggestMode = suggestSize.lastIndexOf(":");
                if(suggestMode != -1) {
                    String divisor = suggestSize.substring(0, suggestMode);
                    indexName = suggestSize.substring(suggestMode + 1);
                    if("asc".equals(indexName)) {
                        searchSourceBuilder.sort(divisor, SortOrder.ASC);
                    } else if("desc".equals(indexName)) {
                        searchSourceBuilder.sort(divisor, SortOrder.DESC);
                    }
                } else {
                    searchSourceBuilder.sort(suggestSize);
                }
            }
        }

        if(params.containsKey("explain")){
            searchSourceBuilder.explain(Boolean.parseBoolean(params.get("explain")));
        }

        if(params.containsKey("from")){
            searchSourceBuilder.from(Integer.parseInt(params.get("from")));
        }



         /* NOT USED PARAMETERS by MCF
         * source, version, timeout, terminate_after, fields, track_scores, indices_boost, stats, suggest_field
         * suggest_mode
         */

        SearchRequest searchRequest = new SearchRequest("testrni");
        searchRequest.types("attachment");
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest);

        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();

        //return searchResponse.toString();
        return searchResponse;
    }

    /**
     * @param q Query string
     * @param USERS WD user domain
     * @return Searchresult as string..yet
     * @throws IOException
     */
    public String simpleSearchRNI(String q, String USERS) throws IOException {

        final String FIELD_NAME = "RNI_PERSON"; // Copy field
        final String INDEX_NAME = "testrni";
        final String INDEX_TYPE = "attachment";

        QueryStringQueryBuilder from = QueryBuilders.queryStringQuery(q);

        System.out.println("Params: " + q + " - " + USERS);

        BoolQueryBuilder authorizationFilter = MCFAuthService.getAuthFilter(USERS);

        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        DocScoreFunctionBuilder docScorer = new DocScoreFunctionBuilder();

        boolQuery
                .must( // Muszáj, hogy MUST legyen az összetett keresési feltétel miatt....
                        QueryBuilders.queryStringQuery(q)
                        /*QueryBuilders.matchQuery(
                                FIELD_NAME,
                                q
                        )*/
                )
                .filter(authorizationFilter); // így nem zavarja a SCORE-t!!!
               // .should(from);

        String patternUsingByRNI = "(?:RNI_PERSON:)(.*?)\\s(.*)";
        Pattern pattern = Pattern.compile(patternUsingByRNI);
        Matcher matcher = pattern.matcher(q);
        String qForRNI = q;
        Boolean isRNI_PersonExist = matcher.find();
        if(isRNI_PersonExist){
            System.out.println("MATTCCCCH " + matcher.group(1));
            qForRNI = matcher.group(1);
        }

        System.out.println("Az RNI query: " + qForRNI);
        docScorer.queryField(FIELD_NAME, qForRNI);

        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );
        queryRescorer = setRNIValuesForRescore(queryRescorer);





     /*   docScorer.queryField(FIELD_NAME, q);
        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );

        queryRescorer = setRNIValuesForRescore(queryRescorer);
*/
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightContent = new HighlightBuilder.Field("content_text");
        HighlightBuilder.Field highlightPerson = new HighlightBuilder.Field("ENTITY:PERSON");

        highlightBuilder.field(highlightPerson);
        highlightBuilder.field(highlightContent);


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .query(boolQuery).size(22) //20
                //.addRescorer(queryRescorer)
                .aggregation(AggregationSearch.Person)
                .aggregation(AggregationSearch.Nationality)
                .aggregation(AggregationSearch.Location)
                .aggregation(AggregationSearch.Phone)
                .aggregation(AggregationSearch.Organization)
                .aggregation(AggregationSearch.Product)
                .aggregation(AggregationSearch.Title)
                .aggregation(AggregationSearch.URL)

                .highlighter(highlightBuilder);

        if(isRNI_PersonExist){
            System.out.println("ADDED RECORE");
            searchSourceBuilder.addRescorer(queryRescorer);
        }

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.types(INDEX_TYPE)
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source( searchSourceBuilder

                        /*new SearchSourceBuilder()
                        .query(boolQuery).size(20) //20
                        .addRescorer(queryRescorer)

                        .aggregation(AggregationSearch.Person)
                        .aggregation(AggregationSearch.Nationality)
                        .aggregation(AggregationSearch.Location)
                        .aggregation(AggregationSearch.Phone)
                        .aggregation(AggregationSearch.Organization)
                        .aggregation(AggregationSearch.Product)
                        .aggregation(AggregationSearch.Title)
                        .aggregation(AggregationSearch.URL)

                        .highlighter(highlightBuilder)
                        //.explain(true)*/
                );


        // TODO.... not works as OBJ... exc because of ENTITY can not be convert to JSON....
        return client.search(searchRequest).toString();
    }

    /**They need for RNI as default values for rescoring....if everything works...Try to set it...
     *
     * @param queryRescorerBuilder new type for RNI rescorer
     * @return setting for RNI
     */

    private QueryRescorerBuilder setRNIValuesForRescore(QueryRescorerBuilder queryRescorerBuilder) {
        return queryRescorerBuilder.setQueryWeight(0.0f).setRescoreQueryWeight(1.0f);
    }

    /**
     * Aggregations for search
     */
    private static class AggregationSearch{

        static AggregationBuilder Person = AggregationBuilders
                .terms("Persons")
                .field("ENTITY:PERSON.keyword");

        static AggregationBuilder Nationality = AggregationBuilders
                .terms("Nationality")
                .field("ENTITY:NATIONALITY.keyword");


        static AggregationBuilder Location = AggregationBuilders
                .terms("Locations")
                .field("ENTITY:LOCATION.keyword");

        static AggregationBuilder Phone = AggregationBuilders
                .terms("Phones")
                .field("ENTITY:IDENTIFIER:PHONE_NUMBER.keyword");

        static AggregationBuilder URL = AggregationBuilders
                .terms("URLs")
                .field("ENTITY:IDENTIFIER:URL.keyword");

        static AggregationBuilder Organization = AggregationBuilders
                .terms("Organizations")
                .field("ENTITY:ORGANIZATION.keyword");

        static AggregationBuilder Product = AggregationBuilders
                .terms("Products")
                .field("ENTITY:PRODUCT.keyword");

        static AggregationBuilder Title = AggregationBuilders
                .terms("Titles")
                .field("ENTITY:TITLE.keyword");
    }

}
