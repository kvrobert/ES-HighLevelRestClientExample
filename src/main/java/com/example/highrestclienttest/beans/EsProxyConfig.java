package com.example.highrestclienttest.beans;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.NoArgsConstructor;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "elasticIndex",
        "indexType",
        "fullTextQueryFields",
        "elasticAuth",
        "wdAuthUsage",
        "rniUsage",
        "highLightFields"
})
@NoArgsConstructor
public class EsProxyConfig {

    @JsonProperty("elasticIndex")
    private String elasticIndex;
    @JsonProperty("indexType")
    private String indexType;
    @JsonProperty("fullTextQueryFields")
    private List<String> fullTextQueryFields = new ArrayList<String>();
    @JsonProperty("elasticAuth")
    private boolean elasticAuth;
    @JsonProperty("wdAuthUsage")
    private boolean wdAuthUsage;
    @JsonProperty("rniUsage")
    private boolean rniUsage;
    @JsonProperty("highLightFields")
    private List<String> highLightFields = new ArrayList<String>();

    private int size;

    @JsonProperty("elasticIndex")
    public String getElasticIndex() {
        return elasticIndex;
    }

    @JsonProperty("elasticIndex")
    public void setElasticIndex(String elasticIndex) {
        this.elasticIndex = elasticIndex;
    }

    @JsonProperty("indexType")
    public String getIndexType() {
        return indexType;
    }

    @JsonProperty("indexType")
    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    @JsonProperty("fullTextQueryFields")
    public List<String> getFullTextQueryFields() {
        return fullTextQueryFields;
    }

    @JsonProperty("fullTextQueryFields")
    public void setFullTextQueryFields(List<String> fullTextQueryFields) {
        this.fullTextQueryFields = fullTextQueryFields;
    }

    @JsonProperty("elasticAuth")
    public Object getElasticAuth() {
        return elasticAuth;
    }

    @JsonProperty("elasticAuth")
    public void setElasticAuth(boolean elasticAuth) {
        this.elasticAuth = elasticAuth;
    }

    @JsonProperty("wdAuthUsage")
    public boolean getWdAuthUsage() {
        return wdAuthUsage;
    }

    @JsonProperty("wdAuthUsage")
    public void setWdAuthUsage(boolean wdAuthUsage) {
        this.wdAuthUsage = wdAuthUsage;
    }

    @JsonProperty("rniUsage")
    public boolean getRniUsage() {
        return rniUsage;
    }

    @JsonProperty("rniUsage")
    public void setRniUsage(boolean rniUsage) {
        this.rniUsage = rniUsage;
    }

    @JsonProperty("highLightFields")
    public List<String> getHighLightFields() {
        return highLightFields;
    }

    @JsonProperty("highLightFields")
    public void setHighLightFields(List<String> highLightFields) {
        this.highLightFields = highLightFields;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}