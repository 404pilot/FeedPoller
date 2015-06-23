package feedpoller;

import com.sun.jersey.api.client.Client;
import feedpoller.domain.EndpointConfig;
import feedpoller.domain.PollingResult;
import feedpoller.handler.DefaultPollingExceptionHandler;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FeedPoller {
    private static final Logger LOG = LoggerFactory.getLogger(FeedPoller.class);

    private Client client;
    private List<EndpointConfig> endpointConfigs;
    private NewFeedHandler newFeedHandler;
    private long initialDelay;
    private long shutdownTimeout;
    private PollingExceptionHandler pollingExceptionHandler;
    private String contentType;

    private List<PollingTask> tasks;
    private ScheduledExecutorService service;

    private FeedPoller(FeedPollerBuilder builder) {
        this.client = builder.client;
        this.endpointConfigs = builder.endpointConfigs;
        this.newFeedHandler = builder.newFeedHandler;
        this.pollingExceptionHandler = builder.pollingExceptionHandler;
        this.initialDelay = builder.initialDelay;
        this.shutdownTimeout = builder.shutdownTimeout;
        this.contentType = builder.contentType;

        service = Executors.newScheduledThreadPool(endpointConfigs.size());
        tasks = new ArrayList<>();
    }

    public void start() {
        for (EndpointConfig endpointConfig : endpointConfigs) {
            PollingTask task = new PollingTask(client, newFeedHandler, endpointConfig.getKey(), endpointConfig.getStart(), contentType, pollingExceptionHandler);

            service.scheduleAtFixedRate(task, initialDelay, endpointConfig.getPeriodInMilliseconds(), TimeUnit.MILLISECONDS);

            tasks.add(task);
        }
    }

    public List<PollingResult> shutdown() {
        List<PollingResult> results = new ArrayList<>();
        LOG.info("FeedPoller is stated to gracefully shut down all tasks. (timeout {} milliseconds)", shutdownTimeout);
        service.shutdown();

        try {
            if (!service.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                LOG.warn("FeedPoller is forced to shutdown.");
                service.shutdownNow();

                if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOG.warn("FeedPoller is not able to terminate.");
                }
            }
        } catch (InterruptedException e) {
            LOG.error("FeedPoller get interrupted and will be forced to shutdown all tasks.\n", e);
            service.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        } finally {
            for (PollingTask task : tasks) {
                PollingResult result = new PollingResult(task.getKey(), task.getInitialUri(), task.getNextUri());
                results.add(result);
            }
            return results;
        }
    }

    public static class FeedPollerBuilder {
        private Client client;
        private List<EndpointConfig> endpointConfigs;
        private NewFeedHandler newFeedHandler;
        private long initialDelay;
        private long shutdownTimeout = 60000L;
        private PollingExceptionHandler pollingExceptionHandler;
        private String contentType;

        public FeedPollerBuilder withClient(Client client) {
            this.client = client;
            return this;
        }

        public FeedPollerBuilder withEndpointConfigs(List<EndpointConfig> endpointConfigs) {
            this.endpointConfigs = endpointConfigs;
            return this;
        }

        public FeedPollerBuilder withInitialDelay(long initialDelayInMilliseconds) {
            this.initialDelay = initialDelayInMilliseconds;
            return this;
        }

        public FeedPollerBuilder withNewFeedHandler(NewFeedHandler newFeedHandler) {
            this.newFeedHandler = newFeedHandler;
            return this;
        }

        public FeedPollerBuilder withPollingExceptionHandler(PollingExceptionHandler pollingExceptionHandler) {
            this.pollingExceptionHandler = pollingExceptionHandler;
            return this;
        }

        public FeedPollerBuilder withShutdownTimeout(long shutdownTimeoutInMilliseconds) {
            this.shutdownTimeout = shutdownTimeoutInMilliseconds;
            return this;
        }

        public FeedPollerBuilder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public FeedPoller build() {
            if (client == null) {
                client = new Client();
            }

            if (endpointConfigs == null || endpointConfigs.size() < 1) {
                throw new Error("No endpoint found for poller");
            }

            if (newFeedHandler == null) {
                throw new Error("No NewFeedHandler is found for poller");
            }

            if (pollingExceptionHandler == null) {
                pollingExceptionHandler = new DefaultPollingExceptionHandler();
            }

            if (contentType == null) {
                contentType = MediaType.TEXT_PLAIN;
            }

            return new FeedPoller(this);
        }
    }
}