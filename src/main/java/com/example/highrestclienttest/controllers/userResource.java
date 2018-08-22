package com.example.highrestclienttest.controllers;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/rest/users")
public class userResource {

    private RestHighLevelClient client;
    private int id = 1;


    public userResource() {
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200)
                )
        );
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

        BulkRequest bulkRequest = new BulkRequest();

        IndexRequest robeszRequest = new IndexRequest(
                "restbulk",
                "doc",
                "1"
        );

        IndexRequest kareszRequest = new IndexRequest(
                "restbulk",
                "doc",
                "2"
        );

        IndexRequest adamRequest = new IndexRequest(
                "restbulk",
                "doc",
                "3"
        );

        IndexRequest zoliRequest = new IndexRequest(
                "restbulk",
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
                "\"allow_token_parent\":\"Precognox:S-1-5-21-3014129096-3214889382-4178971525-1144\"" +
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


    @PostMapping("/search")
    public SearchResponse search(@RequestBody /*JsonNode*/ String searchRequestObj) throws IOException {

        SearchRequest searchRequest = new SearchRequest("restbulk");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
/*
        QueryBuilder query = QueryBuilders.wrapperQuery(searchRequestObj);

        searchSourceBuilder.query(query);

        searchRequest.source(searchSourceBuilder);
        */

        BoolQueryBuilder bool = new BoolQueryBuilder();
        client.search()
        SearchResponse searchResponse = client.search(searchRequest);

        return searchResponse;
    }
}
