package com.example.highrestclienttest.service;

import com.basistech.names.parameters.*;
import com.basistech.rni.es.DocScoreFunctionBuilder;
import com.example.highrestclienttest.beans.TestData;
import com.example.highrestclienttest.configs.IndexConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ElasticSearchService {

    private static final String MAPPING_PATH = "/mappings/test_mapping.json";

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private IndexConfiguration indexConfiguration;


    public void index() throws IOException {
        Response response = client.getLowLevelClient().performRequest("HEAD", indexConfiguration.getIndexName());

        if(HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
            String mapping = Streams.copyToString(new InputStreamReader(Streams.class.getResourceAsStream(MAPPING_PATH), Charset.forName("UTF8")));
            HttpEntity entity = new NStringEntity(mapping, ContentType.APPLICATION_JSON);
            Map<String, String> params = new HashMap<>();
            client.getLowLevelClient().performRequest("PUT", indexConfiguration.getIndexName(), params, entity);
        }

    }

    public void createRecord(TestData data) {
        ObjectMapper mapper = new ObjectMapper();
        String dataToIndex;
        try {
            dataToIndex = mapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new ElasticsearchParseException("Invalid input param!");
        }

        IndexRequest request = new IndexRequest(
                indexConfiguration.getIndexName(),
                indexConfiguration.getType())
                .source(dataToIndex, XContentType.JSON);

        try {
            client.index(request);
        } catch (IOException ex) {
            throw new ElasticsearchException("Elasticsearch is unavailable!");
        }
    }

    public String getMapping() throws IOException {
        Map<String, String> params = Collections.singletonMap("pretty", "true");
        return EntityUtils.toString(client.getLowLevelClient().performRequest("GET", indexConfiguration.getIndexName() + "/_mapping", params).getEntity());

    }

    public SearchResponse simpleSearch(String q) throws IOException {

        //Search Elasticsearch using the RNI rescorer
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        DocScoreFunctionBuilder docScorer = new DocScoreFunctionBuilder();

        boolQuery.should(
                QueryBuilders.matchQuery(
                        indexConfiguration.getNameField(),
                        q
                )
        );

        docScorer.queryField(indexConfiguration.getNameField(), q);

        QueryRescorerBuilder queryRescorer = new QueryRescorerBuilder(
                new FunctionScoreQueryBuilder(docScorer)

        );


        queryRescorer = setRNIValuesForRescore(queryRescorer);

        SearchRequest searchRequest = new SearchRequest(indexConfiguration.getIndexName());
        searchRequest.types(indexConfiguration.getType())
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source(new SearchSourceBuilder().query(boolQuery).size(20)
                        .addRescorer(queryRescorer));

        return client.search(searchRequest);

    }

    private QueryRescorerBuilder setRNIValuesForRescore(QueryRescorerBuilder queryRescorerBuilder) {
        return queryRescorerBuilder.setQueryWeight(0.0f).setRescoreQueryWeight(1.0f);
    }

    public SearchResponse advancedSearch(Map<String, String> params) throws IOException {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            boolQuery.must(QueryBuilders.matchQuery(entry.getKey(), entry.getValue()));
        }

        SearchRequest searchRequest = new SearchRequest(indexConfiguration.getIndexName());
        searchRequest.types(indexConfiguration.getType())
                .source(new SearchSourceBuilder().query(boolQuery));
        return client.search(searchRequest);
    }
}
