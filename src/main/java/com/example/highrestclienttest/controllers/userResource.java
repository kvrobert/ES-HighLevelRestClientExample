package com.example.highrestclienttest.controllers;

import com.basistech.rni.es.DocScoreFunctionBuilder;
import com.example.highrestclienttest.beans.Fq;
import com.example.highrestclienttest.service.KeycloakService;
import com.example.highrestclienttest.service.MCFAuthService;
import com.example.highrestclienttest.service.MCFSearchService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest")
public class userResource {

    @Autowired
    private RestHighLevelClient client;
    private int id = 1;

    @Autowired
    private MCFSearchService mcfSearchService;

    @Autowired
    private KeycloakService keycloakService;

    @Autowired
    private MCFAuthService MCFAuthService;



    /**
     * Simple query string search with Faceting, Highlighting, for authentication (u= user@domain.com) or JWT token
     * @param params default ES Query params and "u" for user domain
     * @param KEYCLOAK_ACCESS_TOKEN from header
     * @return respons as String... not works correctly
     * @throws IOException
     */
    @GetMapping(value = "/authtest")
    public Object authTest(@RequestParam Map<String, String> params,
                           @RequestHeader @Nullable final String KEYCLOAK_ACCESS_TOKEN, // MÉG LEHET NULL, de később nem!!!
                           @RequestBody @Nullable final String body
                            ) throws IOException {

        System.out.println("****************  BODY  ***********************************");
        System.out.println("Req body....: " + body) ;
        System.out.println("***********************************************************");

        System.out.println("**************** PARAMS ***********************************");
        params.entrySet().stream().forEach( param -> System.out.println(param.getKey() + " - " + param.getValue()));

        params.put("KEYCLOAK_ACCESS_TOKEN", KEYCLOAK_ACCESS_TOKEN);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object obj = mcfSearchService.simpleSearchTest(params);

        return obj;
        //return new ResponseEntity<>( obj, HttpStatus.OK);
        //return mcfSearchService.simpleSearchTest(params);
    }

    @GetMapping(value = "/rniauth", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String rniauth(@RequestParam(value = "q", defaultValue = "*") String q,
                          @RequestParam(value = "u", defaultValue = "empty") final String USERS) throws IOException {

        // TODO.... not works as OBJ... exc because of ENTITY can not be convert to JSON....
        return mcfSearchService.simpleSearchRNI(q, USERS);
    }

    /** Simple query string search with Faceting, for authentication using KEYCLOAK_ACCESS_TOKEN
     * @param KEYCLOAK_ACCESS_TOKEN
     * @param body
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/_search")
    public Object transparentSearch(
                                @RequestHeader @Nullable final String KEYCLOAK_ACCESS_TOKEN, // MÉG LEHET NULL, de később nem!!!
                                @RequestBody @Nullable final String body
                                ) throws IOException {
        Map<String, String> params = new HashMap<>();
        final String USERNAME_DOMAIN = keycloakService.getUsernameFromJWT(KEYCLOAK_ACCESS_TOKEN);
        final String RNI_FIELD_NAME = "RNI_PERSON";

        System.out.println("JWT..." + KEYCLOAK_ACCESS_TOKEN);
        System.out.println("Body..." + body);
        System.out.println("User Domain..." + USERNAME_DOMAIN);


        //QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(body);
        BoolQueryBuilder authorizationFilter =  MCFAuthService.getAuthFilter(USERNAME_DOMAIN);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        ObjectMapper mapper = new ObjectMapper();

        JsonNode requestBodyNode = mapper.readTree(body);

        JsonNode paramsNode =requestBodyNode.path("bodyParams");
        JsonNode configNode =requestBodyNode.path("config");


    /*
    * Build query form q and filter fields
    * */
        // Reading parameters from UI body parameters
        String q = paramsNode.path("q").asText();
        List<String> fq = Arrays.asList(paramsNode.path("fq").asText());
        int from = paramsNode.path("start").asInt();
        String sortField = paramsNode.path("sortField").asText();
        String sortOrderingText = paramsNode.path("sortOrder").asText(configNode.path("sortOrderDefault").asText());


        // Reading parameters from frontEnd config
        String index = configNode.path("elasticParams").path("elasticIndex").asText();
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
        Boolean isUsingRnNI = splitedRNINames.size() > 1 ? true:false;


        DocScoreFunctionBuilder docScorer = new DocScoreFunctionBuilder();
        QueryStringQueryBuilder queryString = QueryBuilders.queryStringQuery(q);


        List<String> fields;
        String array = configNode.path("elasticParams").path("fullTextQueryFields").toString();
        if(array.equals("null")){
            throw new IllegalArgumentException("Elastic Params mustn't be null in config file.");
        }
        fields = mapper.readValue(array, List.class);
        fields.stream().forEach( fild -> queryString.field(fild) );
        if(isUsingRnNI){
            queryString.field(RNI_FIELD_NAME);
        }

        System.out.println("==========================PARAMS======================================");
        System.out.println();
        System.out.println("q: " + q);
        System.out.println("fq: " + fq);
        System.out.println("from: " + from);
        System.out.println("sortField: " + sortField);
        System.out.println("sortOrdering: " + sortOrderingText);
        System.out.println("size: " + size);
        System.out.println("fields: " + fields);
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
            FQFields.get(0).values.forEach( value -> System.out.println(value));

            System.out.println("FQ size: " + FQFields.size());

            FQFields.stream().forEach( fieldName -> {
                if(fieldName.operator.equals("OR")){
                    fieldName.values.stream().forEach( fieldValue -> fqQueryFilter.should(new TermQueryBuilder(fieldName.field,fieldValue)) );
                } else if (fieldName.operator.equals("AND")){
                    fieldName.values.stream().forEach( fieldValue -> fqQueryFilter.must(new TermQueryBuilder(fieldName.field,fieldValue)) );
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
        String aggregationsArray = configNode
                                    .path("facets")
                                    .path("facet_options")
                                    .path("default")
                                    .path("facet.field")
                                    .toString();
        if(aggregationsArray.equals("null")){
            throw new IllegalArgumentException("Elastic Params mustn't be null in config file.");
        }
        aggregationsFieldList = mapper.readValue(aggregationsArray, List.class);
        aggregationsFieldList.stream().forEach( aggregationField ->
                    searchSourceBuilder.aggregation(
                        AggregationBuilders
                        .terms(aggregationField)
                        .field(aggregationField)
                        .size(100)
                ));


        // RESCORER
        // FOR REGEX IF NEEDED: [(]RNI_PERSON:[a-zA-Zá-űÁ-Ű\s]+[)]
        String qForRNI = q;
        int relevantName = 0;
        if(isUsingRnNI){
            splitedRNINames.stream().forEach(tag ->
                    {
                        System.out.println(tag);
                        RNiPersons.append(tag.split(" ")[relevantName]).append(" ");
                    }

            );
            System.out.println("A szükséges nevek: " + RNiPersons);
            qForRNI = RNiPersons.toString();
        }
        System.out.println("Az RNI query: " + qForRNI);
        docScorer.queryField(RNI_FIELD_NAME, qForRNI);

        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );
        queryRescorer = setRNIValuesForRescore(queryRescorer);

        if(isUsingRnNI){
            System.out.println("ADDED RECORER");
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


        System.out.println("Agggggs:" );
        searchResponse.getAggregations().asList().stream().forEach(agg -> System.out.println(agg.getType()) );

       // return  queryString.toString();
       // Todo.... nem túl elegáns.. egyenlőre nincs rá sebb megoldás... esetleg custom response építés
        return  searchResponse.toString().replaceAll("sterms#","");
    }


    private QueryRescorerBuilder setRNIValuesForRescore(QueryRescorerBuilder queryRescorerBuilder) {
        return queryRescorerBuilder.setQueryWeight(0.0f).setRescoreQueryWeight(1.0f);
    }







    /* *********************************************************************************************************** */
    /* *********************************************************************************************************** */
    /* *********************************************************************************************************** */









    @GetMapping("/insert/{name}/{age}/{hobby}")
    public String insert(@PathVariable final String name,
                         @PathVariable final String age,
                         @PathVariable final String hobby) throws IOException {

        IndexRequest request = new IndexRequest(
                "restindex",
                "doc",
                Integer.toString(id++)
        );

       /* String jsonString = "{" +
                "\"name\":\"Robesz\"," +
                "\"birth_day\":\"1981-05-10\"," +
                "\"hobby\":\"valami izgalmas\"" +
                "}";*/

        System.out.println(name + age + hobby);

        String jsonString = "{" +
                "\"name\":\"" + name + "\"," +
                "\"age\":\"" + age + "\"," +
                "\"hobby\":\"" + hobby + "\"" +
                "}";

        request.source(jsonString, XContentType.JSON);

        IndexResponse indexResponse = client.index(request);

        String index = indexResponse.getIndex();
        String type = indexResponse.getType();
        String id = indexResponse.getId();
        long version = indexResponse.getVersion();

        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            return indexResponse.getResult().toString();

        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            return indexResponse.getResult().toString();

        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            return "Shard count not correct...";
        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                String reason = failure.reason();
                return "Exception...." + reason;
            }
        }

        return "Error";

    }


    @GetMapping("/bulk")
    public String insertBulk() throws IOException {

        String indexName = "restbulk2";

        BulkRequest bulkRequest = new BulkRequest();

        IndexRequest robeszRequest = new IndexRequest(
                indexName,
                "doc",
                "1"
        );

        IndexRequest kareszRequest = new IndexRequest(
                indexName,
                "doc",
                "2"
        );

        IndexRequest adamRequest = new IndexRequest(
                indexName,
                "doc",
                "3"
        );

        IndexRequest zoliRequest = new IndexRequest(
                indexName,
                "doc",
                "4"
        );

        String robesz = "{" +
                "\"name\":\"Robesz\"," +
                "\"age\":\"37\"," +
                "\"hobby\":\"valami izgalmas\"," +
                "\"allow_token_parent\":\"Precognox:S-1-5-21-3014129096-3214889382-4178971525-1157\"" +
                "}";

        String karesz = "{" +
                "\"name\":\"Karesz\"," +
                "\"age\":\"43\"," +
                "\"hobby\":\"valami saját hobby\"," +
                "\"allow_token_parent\":\"Precognox:S-1-5-21-3014129096-3214889382-4178971525-1141\"" +
                "}";

        String adam = "{" +
                "\"name\":\"Ádám\"," +
                "\"age\":\"28\"," +
                "\"hobby\":\"valami saját Ádám hobby\"," +
                "\"allow_token_parent\":[\"Precognox:S-1-5-21-3014129096-3214889382-4178971525-1144\", \"Precognox:S-1-5-21-3014129096-3214889382-4178971525-500\"]" +
                "}";

        String zoli = "{" +
                "\"name\":\"Zoltán\"," +
                "\"age\":\"40\"," +
                "\"hobby\":\"valami  Zoli hobby\"," +
                "\"allow_token_parent\":\"Precognox:S-1-5-21-3014129096-3214889382-4178971525-1145\"" +
                "}";

        robeszRequest.source(robesz, XContentType.JSON);
        kareszRequest.source(karesz, XContentType.JSON);
        adamRequest.source(adam, XContentType.JSON);
        zoliRequest.source(zoli, XContentType.JSON);

        bulkRequest.add(robeszRequest);
        bulkRequest.add(kareszRequest);
        bulkRequest.add(adamRequest);
        bulkRequest.add(zoliRequest);

        BulkResponse bulkResponse = client.bulk(bulkRequest);

        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();

            if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.INDEX
                    || bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE) {
                IndexResponse indexResponse = (IndexResponse) itemResponse;
                return indexResponse.getResult().toString();

            } else if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.UPDATE) {
                UpdateResponse updateResponse = (UpdateResponse) itemResponse;
                return updateResponse.getGetResult().toString();

            } else if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.DELETE) {
                DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
                deleteResponse.getResult().toString();
            }
        }

        if (bulkResponse.hasFailures()) {
            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                if (bulkItemResponse.isFailed()) {
                    BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                    return failure.getMessage();
                }
            }

        }


        return "Error";

    }


    @GetMapping("/search")
    public SearchHits search(@RequestParam(value = "q", defaultValue = "") final String text,
                             @RequestParam(value = "u", defaultValue = "empty") final String token) throws IOException {

        System.out.println("A keresett parameter....: " + text);
        System.out.println("A keresett toke....: " + token);
        QueryBuilder query = QueryBuilders.boolQuery()
               /* .should(
                        QueryBuilders.queryStringQuery(text)
                        .lenient(true)
                        .field("name")
                        .field("hobby")
                )
                .should(
                        QueryBuilders.queryStringQuery("*" + text + "*")
                        .lenient(true)
                        .field("name")
                        .field("hobby")
                )*/
                /*.must(
                        QueryBuilders.termQuery("allow_token_parent", token)

                )*/
                .must(
                        QueryBuilders.boolQuery()
                                .should(
                                        QueryBuilders.queryStringQuery(text)
                                                .lenient(true)
                                                .field("name")
                                                .field("hobby")
                                                .field("content_text")
                                )
                                .should(
                                        QueryBuilders.queryStringQuery("*" + text + "*")
                                                .lenient(true)
                                                .field("name")
                                                .field("hobby")
                                                .field("content_text")
                                )
                );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        SearchRequest searchRequest = new SearchRequest("manifoldcfauth");
        searchRequest.types("attachment");
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest);

        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();

        return searchResponse.getHits();

    }

    @GetMapping("/auth/{index}/{type}")
    public SearchHits auth(@PathVariable final String index,
                           @PathVariable final String type,
                           @RequestParam(value = "q", defaultValue = "") final String text,
                           @RequestParam(value = "u", defaultValue = "empty") final String users) throws IOException {
        System.out.println("A q= " + text);
        System.out.println("A u= " + users);

        ////////////////////////////////////////////////

        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);

        //TODOOO: Request elkapása, összes paraméter kell...RestRequest
        if( users != "" ){
            String[] authenticatedUserNamesAndDomains = users.split(",");
            String[] indices = Strings.splitStringByCommaToArray(index);
        }



        ////////////////////////////////////////////////

        QueryBuilder query = QueryBuilders.boolQuery()

                .must(
                        QueryBuilders.termQuery("allow_token_parent", users)//token)

                )
                .must(
                        QueryBuilders.boolQuery()
                                .should(
                                        QueryBuilders.queryStringQuery(text)
                                                .lenient(true)
                                                .field("name")
                                                .field("hobby")
                                )
                                .should(
                                        QueryBuilders.queryStringQuery("*" + text + "*")
                                                .lenient(true)
                                                .field("name")
                                                .field("hobby")
                                )
                );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);


        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest);

        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();
        return searchResponse.getHits();
    }

}
