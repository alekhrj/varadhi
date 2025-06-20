package com.flipkart.varadhi.web.routes;


import com.flipkart.varadhi.entities.auth.ResourceAction;
import com.flipkart.varadhi.web.hierarchy.HierarchyFunction;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;


/**
 * Route Definitions describes a route and associated behaviours. The setup code can use appropriate handlers based on
 * this definition.
 */

@Slf4j
@Getter
public class RouteDefinition {
    public static final String DELIMITER = "_";
    private final String methodName;
    private final String apiSubject;

    private final HttpMethod method;
    private final String path;
    private final Set<RouteBehaviour> behaviours;
    private final LinkedHashSet<Handler<RoutingContext>> preHandlers;
    private final Handler<RoutingContext> endReqHandler;
    private final boolean blockingEndHandler;
    private final List<ResourceAction> authorizeOnActions;
    private final Consumer<RoutingContext> bodyParser;
    private final HierarchyFunction hierarchyFunction;
    private final TelemetryType telemetryType;

    public String getName() {
        return String.format("%s%s%s", apiSubject, DELIMITER, methodName);
    }


    RouteDefinition(
        String methodName,
        String apiSubject,
        HttpMethod method,
        String path,
        Set<RouteBehaviour> behaviours,
        LinkedHashSet<Handler<RoutingContext>> preHandlers,
        Handler<RoutingContext> endReqHandler,
        boolean blockingEndHandler,
        Consumer<RoutingContext> bodyParser,
        HierarchyFunction function,
        List<ResourceAction> authorizeOnActions,
        TelemetryType telemetryType
    ) {
        this.methodName = methodName;
        this.apiSubject = apiSubject;
        this.method = method;
        this.path = path;
        this.behaviours = behaviours;
        this.preHandlers = preHandlers;
        this.endReqHandler = endReqHandler;
        this.blockingEndHandler = blockingEndHandler;
        this.bodyParser = bodyParser;
        this.hierarchyFunction = function;
        this.authorizeOnActions = authorizeOnActions;
        this.telemetryType = telemetryType;
    }

    public static Builder get(String name, String handlerName, String path) {
        return new Builder(name, handlerName, HttpMethod.GET, path);
    }

    public static Builder put(String name, String handlerName, String path) {
        return new Builder(name, handlerName, HttpMethod.PUT, path);
    }

    public static Builder post(String name, String handlerName, String path) {
        return new Builder(name, handlerName, HttpMethod.POST, path);
    }

    public static Builder delete(String name, String handlerName, String path) {
        return new Builder(name, handlerName, HttpMethod.DELETE, path);
    }

    public static Builder patch(String name, String handlerName, String path) {
        return new Builder(name, handlerName, HttpMethod.PATCH, path);
    }

    @RequiredArgsConstructor
    public static class Builder {
        private final String methodName;
        private final String apiSubject;
        private final HttpMethod method;
        private final String path;
        private boolean unAuthenticated;
        private boolean hasBody;
        private boolean nonBlocking;
        private boolean metricsEnabled;
        private boolean logsDisabled;
        private boolean tracingDisabled;
        private final LinkedHashSet<Handler<RoutingContext>> preHandlers = new LinkedHashSet<>();
        private Consumer<RoutingContext> bodyParser;

        private LinkedList<ResourceAction> authorizeOnActions = new LinkedList<>();

        public Builder unAuthenticated() {
            this.unAuthenticated = true;
            return this;
        }

        public Builder hasBody() {
            this.hasBody = true;
            return this;
        }

        public Builder bodyParser(Consumer<RoutingContext> bodyParser) {
            this.hasBody = true;
            this.bodyParser = bodyParser;
            return this;
        }

        public Builder nonBlocking() {
            this.nonBlocking = true;
            return this;
        }

        public Builder logsDisabled() {
            this.logsDisabled = true;
            return this;
        }

        public Builder tracingDisabled() {
            this.tracingDisabled = true;
            return this;
        }

        public Builder metricsEnabled() {
            this.metricsEnabled = true;
            return this;
        }

        public Builder authorize(ResourceAction action) {
            this.authorizeOnActions.addLast(action);
            return this;
        }

        public Builder preHandler(Handler<RoutingContext> preHandler) {
            if (null != preHandler) {
                this.preHandlers.add(preHandler);
            }
            return this;
        }

        public RouteDefinition build(HierarchyFunction function, Handler<RoutingContext> reqHandler) {
            Set<RouteBehaviour> behaviours = new LinkedHashSet<>();

            if (!unAuthenticated) {
                behaviours.add(RouteBehaviour.authenticated);
            }
            if (hasBody) {
                behaviours.add(RouteBehaviour.hasBody);
                if (null != bodyParser) {
                    behaviours.add(RouteBehaviour.parseBody);
                }
            }
            behaviours.add(RouteBehaviour.addHierarchy);
            if (metricsEnabled || !logsDisabled || !tracingDisabled) {
                behaviours.add(RouteBehaviour.telemetry);
            }
            if (authorizeOnActions.size() > 0) {
                behaviours.add(RouteBehaviour.authorized);
            }

            return new RouteDefinition(
                methodName,
                apiSubject,
                method,
                path,
                behaviours,
                preHandlers,
                reqHandler,
                !nonBlocking,
                bodyParser,
                function,
                authorizeOnActions,
                new TelemetryType(metricsEnabled, !logsDisabled, !tracingDisabled)
            );
        }
    }
}
