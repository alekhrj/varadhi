package com.flipkart.varadhi.web.v1.producer;

import java.util.concurrent.CompletableFuture;

import com.flipkart.varadhi.common.Constants;
import com.flipkart.varadhi.common.Result;
import com.flipkart.varadhi.entities.StdHeaders;
import com.flipkart.varadhi.produce.ProduceResult;
import com.flipkart.varadhi.entities.web.ErrorResponse;
import com.flipkart.varadhi.spi.services.DummyProducer;
import com.flipkart.varadhi.web.WebTestBase;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

public class BodyHandlerTest extends ProduceTestBase {

    HttpRequest<Buffer> request;

    @BeforeEach ()
    public void PreTest() throws InterruptedException {
        super.setUp();
        bodyHandler.setBodyLimit(20);
        route.handler(bodyHandler).handler(ctx -> {
            ctx.put(Constants.ContextKeys.RESOURCE_HIERARCHY, produceHandlers.getHierarchies(ctx, true));
            ctx.next();
        }).handler(produceHandlers::produce);
        setupFailureHandler(route);

        ProduceResult result = ProduceResult.of(messageId, Result.of(new DummyProducer.DummyOffset(10)));
        doReturn(CompletableFuture.completedFuture(result)).when(producerService).produceToTopic(any(), any());
        request = createRequest(HttpMethod.POST, topicPath);
    }

    @AfterEach
    public void PostTest() throws InterruptedException {
        super.tearDown();
    }

    @Test
    public void testProduceWithForBodySize() {
        request.putHeader(StdHeaders.get().msgId(), messageId);
        request.putHeader(StdHeaders.get().msgId(), "host1, host2");
        payload = "0123456789".getBytes();
        String messageIdObtained = sendRequestWithPayload(request, payload, WebTestBase.c(String.class));
        Assertions.assertEquals(messageId, messageIdObtained);

        payload = "0123456789012345678".getBytes();
        messageIdObtained = sendRequestWithPayload(request, payload, WebTestBase.c(String.class));
        Assertions.assertEquals(messageId, messageIdObtained);

        payload = "01234567890123456789".getBytes();
        messageIdObtained = sendRequestWithPayload(request, payload, WebTestBase.c(String.class));
        Assertions.assertEquals(messageId, messageIdObtained);

        payload = "012345678901234567890".getBytes();
        sendRequestAndParseResponse(request, payload, 413, "Entity too large.", WebTestBase.c(ErrorResponse.class));

        payload = "012345678901234567890123456789".getBytes();
        sendRequestAndParseResponse(request, payload, 413, "Entity too large.", WebTestBase.c(ErrorResponse.class));
    }
}
