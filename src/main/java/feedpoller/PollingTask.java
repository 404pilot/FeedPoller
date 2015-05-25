package feedpoller;

import com.sun.jersey.api.client.Client;
import feedpoller.domain.EmptyFeedException;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PollingTask.class);

    private Client client;
    private NewFeedHandler newFeedHandler;
    private String key;
    private String InitialUri;
    private PollingExceptionHandler pollingExceptionHandler;

    private String nextUri;

    public PollingTask(Client client, NewFeedHandler newFeedHandler, String key, String InitialUri, PollingExceptionHandler pollingExceptionHandler) {
        this.client = client;
        this.newFeedHandler = newFeedHandler;
        this.key = key;
        this.InitialUri = InitialUri;
        this.pollingExceptionHandler = pollingExceptionHandler;

        this.nextUri = InitialUri;
    }

    private void poll() {
        LOG.info("Poller [{}] is reading feed from {}.", key, nextUri);

        String page = client.resource(nextUri).get(String.class);

        try {
            nextUri = newFeedHandler.receiveNewFeed(page);
        } catch (EmptyFeedException e) {
            LOG.warn("Poller [{}] receives a empty feed from {}. The feed will be read again next time.", key, nextUri);
        }
    }

    public void run() {
        try {
            poll();
        } catch (Exception e) {
            LOG.error("Poller [{}] get unexpected exception. The feed {} will be read again next time.", key, nextUri, e);
            pollingExceptionHandler.handle(e);
        }
    }

    public String getKey() {
        return this.key;
    }

    public String getInitialUri() {
        return this.InitialUri;
    }

    public String getNextUri() {
        return this.nextUri;
    }
}