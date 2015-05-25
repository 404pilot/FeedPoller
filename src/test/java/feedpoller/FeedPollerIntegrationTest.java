package feedpoller;

import feedpoller.domain.EmptyFeedException;
import feedpoller.domain.EndpointConfig;
import feedpoller.domain.PollingResult;
import feedpoller.handler.NewFeedHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeedPollerIntegrationTest {
    private static final long INITIAL_DELAY = 100L;
    private static final long SHUT_DOWN_TIMEOUT = 2000L;

    private static final String FOO_KEY = "foo";
    private static final long FOO_PERIOD = 200L;

    private static final String BAR_KEY = "bar";
    private static final long BAR_PERIOD = 500L;

    private static final String FOO_URI_KEY = "http://localhost:9090/foo";
    private static final String BAR_URI_KEY = "http://localhost:9090/bar";

    List<EndpointConfig> endpointConfigs;

    FeedPoller feedPoller;

    SimpleServer simpleServer;
    SimpleHandler simpleHandler;

    @Before
    public void setUp() throws Exception {
        initServer();

        initFeedPoller();
    }

    @After
    public void tearDown() throws Exception {
        simpleServer.stop();
    }

    private void initServer() throws Exception {
        simpleHandler = new SimpleHandler();

        simpleHandler.mapRequestToResponse(FOO_URI_KEY, FOO_URI_KEY + "/1", 200);
        simpleHandler.mapRequestToResponse(BAR_URI_KEY, BAR_URI_KEY + "/1", 200);

        for (int i = 1; i <= 100; i++) {
            simpleHandler.mapRequestToResponse(
                    FOO_URI_KEY + "/" + String.valueOf(i), FOO_URI_KEY + "/" + String.valueOf(i + 1), 200);
            simpleHandler.mapRequestToResponse(
                    BAR_URI_KEY + "/" + String.valueOf(i), BAR_URI_KEY + "/" + String.valueOf(i + 1), 200);
        }

        simpleServer = new SimpleServer(simpleHandler, 9090);

        simpleServer.start();
    }

    private void initFeedPoller() {
        endpointConfigs = new ArrayList<EndpointConfig>() {{
            add(new EndpointConfig(FOO_KEY, FOO_URI_KEY, FOO_PERIOD));
            add(new EndpointConfig(BAR_KEY, BAR_URI_KEY, BAR_PERIOD));
        }};

        NewFeedHandler newFeedHandler = new NewFeedHandler() {
            @Override
            public String receiveNewFeed(String page) throws EmptyFeedException {
                // page is mocked to uri
                System.out.printf("new feed handler receive: %s\n", page);

                return page;
            }
        };

        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(newFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();
    }

    @Test
    public void FeedPoller_works() throws Exception {
        feedPoller.start();

        TimeUnit.SECONDS.sleep(1);

        List<PollingResult> result = feedPoller.shutdown();

        assertThat("size is correct", result.size(), equalTo(endpointConfigs.size()));

        for (PollingResult pollingResult : result) {
            if (pollingResult.getKey().equals(FOO_KEY)) {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(FOO_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), not(equalTo(FOO_KEY)));
            } else {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(BAR_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), not(equalTo(BAR_KEY)));
            }
        }
    }
}
