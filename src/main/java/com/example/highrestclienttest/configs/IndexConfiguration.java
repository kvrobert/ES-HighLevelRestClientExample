package com.example.highrestclienttest.configs;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@SuppressWarnings("all")
public class IndexConfiguration {

    @Builder.Default
    String indexName = "valami";

    @Builder.Default
    String type = "record";
}
