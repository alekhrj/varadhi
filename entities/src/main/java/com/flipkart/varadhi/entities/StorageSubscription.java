package com.flipkart.varadhi.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
@JsonTypeInfo (use = JsonTypeInfo.Id.NAME, property = "@storageType")
public abstract class StorageSubscription<T extends StorageTopic> {

    /**
     * Name of the subscription.
     */
    private final String name;

    /**
     * Partitions to subscribe
     */
    private final TopicPartitions<T> topicPartitions;

    public StorageSubscription(String name, TopicPartitions<T> topicPartitions) {
        this.name = name;
        this.topicPartitions = topicPartitions;
    }

    @JsonIgnore
    public T getStorageTopic() {
        return topicPartitions.getTopic();
    }
}
