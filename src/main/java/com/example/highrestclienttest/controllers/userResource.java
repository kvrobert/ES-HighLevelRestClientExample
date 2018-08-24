package com.example.highrestclienttest.controllers;

import com.example.highrestclienttest.service.MCFAuthorizer;
import com.example.highrestclienttest.service.MCFConfigurationParameters;
import com.example.highrestclienttest.service.MFCAuthTestService;
import org.apache.http.HttpHost;
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
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/rest/users")
public class userResource {

    private RestHighLevelClient client;
    private int id = 1;
    private final MCFAuthorizer authorizer;

    @Autowired
    private  MFCAuthTestService mfcAuthTestService;


    public userResource() {
        this.client = new RestHighLevelClient(
                            RestClient.builder(
                                    new HttpHost("localhost", 9200)
                            )
                        );
        final MCFConfigurationParameters conf = new MCFConfigurationParameters();
        this.authorizer = new MCFAuthorizer(conf);
    }

    @GetMapping("/insert/{name}/{age}/{hobby}")
    public String insert(@PathVariable final String name, @PathVariable final String age, @PathVariable final String hobby) throws IOException {

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
                .must(
                        QueryBuilders.termQuery("allow_token_parent", token)

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
        SearchRequest searchRequest = new SearchRequest("restbulk2");
        searchRequest.types("doc");
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


    @GetMapping("/authtest")
    public SearchHits authTest(@RequestParam(value = "q", defaultValue = "") final String text,
                               @RequestParam(value = "u", defaultValue = "empty") final String users) throws IOException {

        final String USERNAME_DOMAIN = users;

        List<String> tokens = mfcAuthTestService.getAllowsTokens(USERNAME_DOMAIN);

        BoolQueryBuilder authorizationFilter = new BoolQueryBuilder();


        for( String token : tokens ){
            authorizationFilter.should(
                    QueryBuilders.termQuery("allow_token_parent", token)

            );
        }

        QueryBuilder querySearch = QueryBuilders.boolQuery()
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
                )
                .must(authorizationFilter);


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(querySearch);
        SearchRequest searchRequest = new SearchRequest("restbulk2");
        searchRequest.types("doc");
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest);

        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();

        return searchResponse.getHits();

    }

}
