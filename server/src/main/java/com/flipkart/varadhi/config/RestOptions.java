package com.flipkart.varadhi.config;

import com.flipkart.varadhi.common.Constants;
import com.flipkart.varadhi.entities.TopicCapacityPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static com.flipkart.varadhi.common.Constants.RestDefaults.*;

@Data
public class RestOptions {
    @NotBlank
    private String deployedRegion;
    @NotNull
    private String projectCacheBuilderSpec = "expireAfterWrite=3600s";
    private TopicCapacityPolicy defaultTopicCapacity = Constants.DEFAULT_TOPIC_CAPACITY;
    private boolean traceRequestEnabled = true;
    private int payloadSizeMax = PAYLOAD_SIZE_MAX;

    private int unsidelineApiMsgCountMax = 1000;
    private int unsidelineApiGroupCountMax = 100;
    private int getMessagesApiMessagesLimitMax = 1000;
    private int unsidelineApiMsgCountDefault = 100;
    private int unsidelineApiGroupCountDefault = 20;
    private int getMessagesApiMessagesLimitDefault = 100;


    // TODO: These dont look related to rest. Looks related to lean deployment.
    private String defaultOrg = DEFAULT_ORG;
    private String defaultTeam = DEFAULT_TEAM;
    private String defaultProject = DEFAULT_PROJECT;
}
