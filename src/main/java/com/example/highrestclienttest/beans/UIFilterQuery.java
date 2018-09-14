package com.example.highrestclienttest.beans;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "q",
        "fq",
        "start",
        "sortField",
        "sortOrder",
        "config"
})
public class UIFilterQuery {

    @JsonProperty("q")
    public String q;
    @JsonProperty("fq")
    public List<Fq> fq = null;
    @JsonProperty("start")
    public String start;
    @JsonProperty("sortField")
    public String sortField;
    @JsonProperty("sortOrder")
    public String sortOrder;
    @JsonProperty("config")
    public String config;

    /**
     * No args constructor for use in serialization
     *
     */
    public UIFilterQuery() {
    }

    /**
     *
     * @param sortField
     * @param start
     * @param sortOrder
     * @param q
     * @param config
     * @param fq
     */
    public UIFilterQuery(String q, List<Fq> fq, String start, String sortField, String sortOrder, String config) {
        super();
        this.q = q;
        this.fq = fq;
        this.start = start;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
        this.config = config;
    }

}