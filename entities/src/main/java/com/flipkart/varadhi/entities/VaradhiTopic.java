package com.flipkart.varadhi.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class VaradhiTopic extends AbstractTopic {


    private final Map<String, InternalTopic> internalTopics;
    private final boolean grouped;

    @JsonIgnore
    private final CapacityPolicy capacityPolicy;

    private VaradhiTopic(
            String name,
            int version,
            boolean grouped,
            CapacityPolicy capacityPolicy,
            Map<String, InternalTopic> internalTopics
    ) {
        super(name, version);
        this.grouped = grouped;
        this.capacityPolicy = capacityPolicy;
        this.internalTopics = null == internalTopics ? new HashMap<>() : internalTopics;
    }

    public static VaradhiTopic of(TopicResource topicResource) {
        CapacityPolicy capacityPolicy = topicResource.getCapacityPolicy();
        if (null == capacityPolicy) {
            capacityPolicy = fetchDefaultCapacityPolicy();
        }
        return new VaradhiTopic(
                buildTopicName(topicResource.getProject(), topicResource.getName()),
                INITIAL_VERSION,
                topicResource.isGrouped(),
                capacityPolicy,
                null
        );
    }

    public static String buildTopicName(String projectName, String topicName) {
        return String.join(NAME_SEPARATOR, projectName, topicName);
    }

    public void addInternalTopic(InternalTopic internalTopic) {
        this.internalTopics.put(internalTopic.getTopicRegion(), internalTopic);
    }

    public InternalTopic getProduceTopicForRegion(String region) {
        return internalTopics.get(region);
    }

    public TopicResource fetchTopicResource() {
        return new TopicResource(
                this.getName().split(NAME_SEPARATOR_REGEX)[1],
                this.getVersion(),
                this.getName().split(NAME_SEPARATOR_REGEX)[0],
                this.isGrouped(),
                this.capacityPolicy
        );
    }

    private static CapacityPolicy fetchDefaultCapacityPolicy() {
        //TODO:: make default capacity config based instead of hard coding.
        return CapacityPolicy.getDefault();
    }
}
