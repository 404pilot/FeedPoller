package feedpoller;

import com.sun.jersey.api.client.Client;
import feedpoller.domain.EmptyFeedException;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PollingTask.class);

    private Client client;
    private NewFeedHandler newFeedHandler;
    private PollingExceptionHandler pollingExceptionHandler;
    private String acceptType;

    @Getter
    private String key;
    @Getter
    private String InitialUri;
    @Getter
    private String nextUri;

    public PollingTask(Client client, NewFeedHandler newFeedHandler, String key, String InitialUri, String acceptType, PollingExceptionHandler pollingExceptionHandler) {
        this.client = client;
        this.newFeedHandler = newFeedHandler;
        this.key = key;
        this.InitialUri = InitialUri;
        this.acceptType = acceptType;
        this.pollingExceptionHandler = pollingExceptionHandler;

        this.nextUri = InitialUri;
    }

    private void poll() throws Exception {
        LOG.info("Poller [{}] is reading feeds from {}.", key, nextUri);

        String page = client.resource(nextUri).accept(acceptType).get(String.class);

        try {
            nextUri = newFeedHandler.receiveNewFeed(page);
        } catch (EmptyFeedException e) {
            LOG.warn("Poller [{}] receives an empty feed from {}. The feed will be read again next time.", key, nextUri);
        }
    }

    @Override
    public void run() {
        try {
            poll();
        } catch (Exception e) {
            LOG.error("Poller [{}] gets an unexpected exception. The feed {} will be read again next time.", key, nextUri, e);
            pollingExceptionHandler.handle(e);
        }
    }
}