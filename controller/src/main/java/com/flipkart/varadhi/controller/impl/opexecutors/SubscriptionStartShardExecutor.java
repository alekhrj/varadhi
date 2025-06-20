package com.flipkart.varadhi.controller.impl.opexecutors;

import com.flipkart.varadhi.controller.AssignmentManager;
import com.flipkart.varadhi.controller.OperationMgr;
import com.flipkart.varadhi.core.cluster.consumer.ConsumerApi;
import com.flipkart.varadhi.core.cluster.consumer.ConsumerClientFactory;
import com.flipkart.varadhi.entities.VaradhiSubscription;
import com.flipkart.varadhi.entities.cluster.ShardOperation;
import com.flipkart.varadhi.spi.db.SubscriptionStore;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class SubscriptionStartShardExecutor extends SubscriptionOpExecutor {

    public SubscriptionStartShardExecutor(
        VaradhiSubscription subscription,
        ConsumerClientFactory clientFactory,
        OperationMgr operationMgr,
        AssignmentManager assignmentManager,
        SubscriptionStore metaStore
    ) {
        super(subscription, clientFactory, operationMgr, assignmentManager, metaStore);
    }

    CompletableFuture<Boolean> startShard(ShardOperation startOp, boolean isRetry, ConsumerApi consumer) {
        String subId = startOp.getOpData().getSubscriptionId();
        int shardId = startOp.getOpData().getShardId();
        return consumer.getConsumerState(subId, shardId).thenCompose(state -> {
            // Start can be executed in stopping subscription as well.
            // in general this shouldn't happen as multiple in-progress operations are not allowed.
            if (state.isPresent()) {
                log.info("Subscription:{} Shard:{} already started. Skipping.", subId, shardId);
                return CompletableFuture.completedFuture(false);
            }
            operationMgr.submitShardOp(startOp, isRetry);
            CompletableFuture<Boolean> startFuture = consumer.start((ShardOperation.StartData)startOp.getOpData())
                                                             .thenApply(v -> true);
            log.info("Scheduled shard start({}).", startOp);
            return startFuture;
        }).exceptionally(t -> {
            failShardOperation(startOp, t);
            return true;
        });
    }
}
