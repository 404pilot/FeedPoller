package feedpoller;

import feedpoller.domain.EmptyFeedException;
import feedpoller.domain.EndpointConfig;
import feedpoller.domain.PollingResult;
import feedpoller.handler.NewFeedHandler;
import feedpoller.stub.SimpleHandler;
import feedpoller.stub.SimpleServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class FeedPollerIntegrationTest {
    private static final long INITIAL_DELAY = 100L;
    private static final long SHUT_DOWN_TIMEOUT = 2000L;

    private static final String FOO_KEY = "foo";
    private static final long FOO_PERIOD = 200L;
    private static final String FOO_URI_KEY = "http://localhost:9090/foo";

    private static final String BAR_KEY = "bar";
    private static final long BAR_PERIOD = 500L;
    private static final String BAR_URI_KEY = "http://localhost:9090/bar";

    private static final ArrayList<String> URIS = new ArrayList<String>() {{
        add(FOO_URI_KEY);
        add(BAR_URI_KEY);
    }};

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

        for (String uri : URIS) {
            simpleHandler.mapRequestToResponse(uri, uri + "/1", 200);
            for (int i = 1; i <= 100; i++) {
                simpleHandler.mapRequestToResponse(uri + "/" + i, uri + "/" + (i + 1), 200);
            }
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

                if (page.trim().equals("")) {
                    throw new EmptyFeedException();
                }

                return page;
            }
        };

        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(newFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withContentType(MediaType.TEXT_PLAIN)
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

    @Test
    public void feedPoller_retires_withEmptyException() throws Exception {
        String lastUnReadUri = FOO_URI_KEY + "/2";

        simpleHandler.mapRequestToResponse(lastUnReadUri, "", 200);


        try {
            feedPoller.start();
        } catch (Exception e) {
            fail("no exception is thrown");
        }

        TimeUnit.SECONDS.sleep(1);

        List<PollingResult> result = feedPoller.shutdown();

        for (PollingResult pollingResult : result) {
            if (pollingResult.getKey().equals(FOO_KEY)) {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(FOO_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), equalTo(lastUnReadUri));
            } else {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(BAR_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), not(equalTo(BAR_KEY)));
            }
        }
    }

    @Test
    public void feedPoller_retires_withOtherException() throws Exception {
        String lastUnReadUri = FOO_URI_KEY + "/3";

        simpleHandler.mapRequestToResponse(lastUnReadUri, "bad request", 400);


        try {
            feedPoller.start();
        } catch (Exception e) {
            fail("no exception is thrown");
        }

        TimeUnit.SECONDS.sleep(1);

        List<PollingResult> result = feedPoller.shutdown();

        for (PollingResult pollingResult : result) {
            if (pollingResult.getKey().equals(FOO_KEY)) {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(FOO_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), equalTo(lastUnReadUri));
            } else {
                assertThat("first uri", pollingResult.getFirstStartedUri(), equalTo(BAR_URI_KEY));
                assertThat("last unread uri", pollingResult.getLastUnreadUri(), not(equalTo(BAR_KEY)));
            }
        }
    }
}
