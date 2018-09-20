package com.example.highrestclienttest.beans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "field",
        "operator",
        "values"
})
public class Fq {

    @JsonProperty("field")
    public String field;
    @JsonProperty("operator")
    public String operator;
    @JsonProperty("values")
    public List<String> values = null;

    /**
     * No args constructor for use in serialization
     *
     */
    public Fq() {
    }

    /**
     *
     * @param field
     * @param values
     * @param operator
     */
    public Fq(String field, String operator, List<String> values) {
        super();
        this.field = field;
        this.operator = operator;
        this.values = values;
    }

}