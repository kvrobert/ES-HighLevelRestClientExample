package com.example.highrestclienttest.beans;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@SuppressWarnings("all")
public class TestData {

    private List<String> name;

    private String age;

    private String hobby;

    private String allow_token_parent;

}
