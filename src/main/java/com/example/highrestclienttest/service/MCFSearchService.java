package com.example.highrestclienttest.service;


import com.basistech.rni.es.DocScoreFunctionBuilder;
import com.example.highrestclienttest.beans.Fq;
import com.example.highrestclienttest.beans.UIFilterQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
               // .should(from); // Nem kell most, mert az RNIis is queryStringgel megy



        String qForRNI = q;
        int relevantName = 0;
        StringBuffer RNiPersons = new StringBuffer();
        List<String> splitedNames = Arrays.asList(q.split(FIELD_NAME + ":"));
        Boolean isUsingRnNI = splitedNames.size() > 1 ? true:false;
        if(isUsingRnNI){
            qForRNI = createRNINames(RNiPersons, splitedNames, relevantName);
        }
        System.out.println("Az RNI query: " + qForRNI);
        docScorer.queryField(FIELD_NAME, qForRNI);

        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );
        queryRescorer = setRNIValuesForRescore(queryRescorer);

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

        if(isUsingRnNI){
            System.out.println("ADDED RECORE");
            searchSourceBuilder.addRescorer(queryRescorer);
        }

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.types(INDEX_TYPE)
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source( searchSourceBuilder
                );


        // TODO.... not works as OBJ... exc because of ENTITY can not be convert to JSON....
        return client.search(searchRequest).toString();
    }



    public Object transparentSearchService(Map<String, String> params) throws IOException {

        final String KEYCLOAK_ACCESS_TOKEN = params.get("KEYCLOAK_ACCESS_TOKEN");
        final String body = params.get("body");

        final String USERNAME_DOMAIN = keycloakService.getUsernameFromJWT(KEYCLOAK_ACCESS_TOKEN);
        final String RNI_FIELD_NAME = "RNI_PERSON";

        System.out.println("JWT..." + params.get("KEYCLOAK_ACCESS_TOKEN"));
        System.out.println("Body..." + params.get("body"));
        System.out.println("User Domain..." + USERNAME_DOMAIN);

        BoolQueryBuilder authorizationFilter =  MCFAuthService.getAuthFilter(USERNAME_DOMAIN);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        ObjectMapper mapper = new ObjectMapper();

        JsonNode requestBodyNode = mapper.readTree(body);

        JsonNode paramsNode =requestBodyNode.path("bodyParams");
        // TODO... custom config set....
        JsonNode configNode =requestBodyNode.path("config");


        /*
         * Build query form q and filter fields
         * */
        // Reading parameters from UI body parameters

        UIFilterQuery uiQueryParams;
        String UiQueryParamsString = paramsNode.toString();
        uiQueryParams = mapper.readValue(UiQueryParamsString, new TypeReference<UIFilterQuery>(){});


        System.out.println("UI QUERY BODY: " + uiQueryParams.q);
        System.out.println("UI QUERY FQ: ");
        uiQueryParams.fq.forEach(fq -> System.out.println(fq.field+" " + fq.operator +" "+ fq.values));
        System.out.println("UI QShort field: " + uiQueryParams.sortField);
        System.out.println("UI QShort order: " + uiQueryParams.sortOrder);
        System.out.println("UI start: " + uiQueryParams.start);

        String q = uiQueryParams.q;
        int from = uiQueryParams.start;
        String sortField = uiQueryParams.sortField;
        String sortOrderingText = !uiQueryParams.sortOrder.equals("") ? uiQueryParams.sortOrder : configNode.path("sortOrderDefault").asText();


        // Reading parameters from frontEnd config
        String index = configNode.path("elasticParams").path("elasticIndex").asText();
        //Todo.. Add to the config file
        String indexType = configNode.path("elasticParams").path("elasticType").asText();
        int size = configNode.path("itemsPerPage").asInt();

        if( from > 0 ){
            from = ( from -1  ) * size;
        }

        if( !configNode.path("highlight").toString().equals("null") ){
            //Todo... Add highlight fileds for the frontend config by elasticParams, and use them here...
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightContentText = new HighlightBuilder.Field("content_text");
        }

        StringBuffer RNiPersons = new StringBuffer();
        List<String> splitedRNINames = Arrays.asList(q.split(RNI_FIELD_NAME + ":"));
        boolean isUsingRnNI = splitedRNINames.size() > 1;
        boolean isUsingSpecificSearch = q.contains(":");
        DocScoreFunctionBuilder docScorer = new DocScoreFunctionBuilder();
        QueryStringQueryBuilder queryString = QueryBuilders.queryStringQuery(q);


        List<String> fields;
        String fieldsString = configNode.path("elasticParams").path("fullTextQueryFields").toString();
        System.out.println("A fieldek:" + fieldsString);
        if(fieldsString.equals("null")){
            throw new IllegalArgumentException("Elastic Params mustn't be null in config file.");
        }
        if(!isUsingSpecificSearch){
            fields = mapper.readValue(fieldsString, List.class);
            fields.forEach( queryString::field );
            System.out.println("USING SPEC SEARCH (:)");
        }
        if(isUsingRnNI){
            queryString.field(RNI_FIELD_NAME);
        }

        System.out.println("==========================PARAMS======================================");
        System.out.println();
        System.out.println("q: " + q);
        System.out.println("from: " + from);
        System.out.println("sortField: " + sortField);
        System.out.println("sortOrdering: " + sortOrderingText);
        System.out.println("size: " + size);
        System.out.println("ES-FulteqtQueryFilds: " + configNode.path("elasticParams").path("fullTextQueryFields"));
        System.out.println("index: " + index);
        System.out.println();
        System.out.println("======================================================================");

        /* Building filter based on aggregation  */
        BoolQueryBuilder fqQueryFilter = new BoolQueryBuilder();
        List<Fq> FQFields;
        String fqArray = paramsNode.path("fq").toString();
        FQFields = mapper.readValue(fqArray, new TypeReference<List<Fq>>(){});


        if( !FQFields.isEmpty() ){

            System.out.println(FQFields.get(0).field);
            FQFields.get(0).values.forEach( System.out::println );

            System.out.println("FQ size: " + FQFields.size());

            FQFields.forEach( fieldName -> {
                if(fieldName.operator.equals("OR")){
                    fieldName.values.forEach( fieldValue -> fqQueryFilter.should(new TermQueryBuilder(fieldName.field,fieldValue)) );
                } else if (fieldName.operator.equals("AND")){
                    fieldName.values.forEach( fieldValue -> fqQueryFilter.must(new TermQueryBuilder(fieldName.field,fieldValue)) );
                }
            });
            System.out.println("Filter Query");
            System.out.println(fqQueryFilter.toString());
        }
        System.out.println("FQ_kint: " + paramsNode.path("fq").toString());

        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(queryString)
                .filter(authorizationFilter)
                .filter(fqQueryFilter)
        );

        searchSourceBuilder.from(from);
        if(!sortField.equals("")){
            if(sortOrderingText.equals("asc")){
                searchSourceBuilder.sort(sortField, SortOrder.ASC);
            }else if(sortOrderingText.equals("desc")){
                searchSourceBuilder.sort(sortField, SortOrder.DESC);
            }
        }
        searchSourceBuilder.size(size);

        /* Building aggregations  */
        List<String> aggregationsFieldList;
        String aggregationsArray;
        aggregationsArray = configNode
                .path("facets")
                .path("facet_options")
                .path("default")
                .path("facet.field")
                .toString();
        if(aggregationsArray.equals("null")){
            throw new IllegalArgumentException("Elastic Params mustn't be null in config file.");
        }
        aggregationsFieldList = mapper.readValue(aggregationsArray, List.class);
        aggregationsFieldList.forEach( aggregationField ->
                searchSourceBuilder.aggregation(
                        AggregationBuilders
                                .terms(aggregationField)
                                .field(aggregationField)
                                .size(100)
                ));


        // RESCORER
        // FOR REGEX IF NEEDED: [(]RNI_PERSON:[a-zA-Zá-űÁ-Ű\s]+[)]
        String qForRNI = q;
        int relevantNamePosition = 0;
        if(isUsingRnNI){
            qForRNI = createRNINames(RNiPersons, splitedRNINames, relevantNamePosition);




        }
        System.out.println("Az RNI query: " + qForRNI);
        docScorer.queryField(RNI_FIELD_NAME, qForRNI);

        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );
        queryRescorer = setRNIValuesForRescore(queryRescorer);

        if(isUsingRnNI){
            System.out.println("ADDED RESCORER");
            searchSourceBuilder.addRescorer(queryRescorer);
        }


        System.out.println("****************  BODY  ***********************************");
        System.out.println("Params....: " + paramsNode);
        System.out.println("Config....: " + configNode);
        System.out.println("Aggregations....: " + "----");
        System.out.println("***********************************************************");

        if(index.equals("")){
            throw new IllegalArgumentException("Elastic index parameter must be set.");
        }
        SearchRequest searchRequest = new SearchRequest(index);
        // Todo...index type from confog file
        searchRequest.types("attachment");
        searchRequest.source(searchSourceBuilder);
        System.out.println("Full Query");
        System.out.println(searchSourceBuilder.toString());

        System.out.println("Full SearchRequest: ");
        System.out.println(searchRequest.toString());

        SearchResponse searchResponse = client.search(searchRequest);

        // return  queryString.toString();
        // Todo.... nem túl elegáns.. egyenlőre nincs rá szebb megoldásom... esetleg custom response építés
        return  searchResponse.toString().replaceAll("sterms#","");


    }

    private String createRNINames(StringBuffer RNiPersons, List<String> splitedRNINames, int relevantNamePosition) {
        String RNINames;
        String noAlphabetAndTagsToDelete = "[^A-Za-zÁ-Űá-ű\\s]|AND|OR|NOT";
        splitedRNINames.forEach(tag ->
                {
                    System.out.println(tag);
                    RNiPersons.append(tag.split(" ")[relevantNamePosition]).append(" ");
                }
        );
        System.out.println("A szükséges nevek: " + RNiPersons);
        RNINames = RNiPersons.toString();
        return RNINames.replaceAll(noAlphabetAndTagsToDelete, "");
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
