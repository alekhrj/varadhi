package com.flipkart.varadhi.server.spi.authn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flipkart.varadhi.spi.ConfigFile;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties (ignoreUnknown = true)
public class AuthenticationOptions {
    private String handlerProviderClassName;
    private List<String> whitelistedURLs;
    private String authenticatorClassName;

    @ConfigFile
    private String configFile;
}
