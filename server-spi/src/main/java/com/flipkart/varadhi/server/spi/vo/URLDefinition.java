package com.flipkart.varadhi.server.spi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.util.regex.Pattern;

@Data
public class URLDefinition {
    private String path;
    private List<String> methodList;

    @JsonIgnore
    private Pattern urlPattern;
}
