package feedpoller;

import com.sun.jersey.api.client.Client;
import feedpoller.domain.EndpointConfig;
import feedpoller.domain.PollingResult;
import feedpoller.handler.DefaultPollingExceptionHandler;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FeedPoller.class)
public class FeedPollerTest {
    public static final String FIELD_CLIENT = "client";
    public static final String FIELD_ENDPOINT_CONFIGS = "endpointConfigs";
    public static final String FIELD_NEW_FEED_HANDLER = "newFeedHandler";
    public static final String FIELD_INITIAL_DELAY = "initialDelay";
    public static final String FIELD_SHUTDOWN_TIMEOUT = "shutdownTimeout";
    public static final String FIELD_POLLING_EXCEPTION_HANDLER = "pollingExceptionHandler";
    public static final String FIELD_ACCEPT_TYPE = "acceptType";
    public static final String FILED_TASKS = "tasks";

    private static final long INITIAL_DELAY = 10000L; // don't run tasks in unit test
    private static final long SHUT_DOWN_TIMEOUT = 1L;
    private static final String ACCEPT_TYPE = MediaType.APPLICATION_JSON;


    private static final String FOO_KEY = "foo";
    private static final String FOO_INITIAL_URI = "http://www.foo.com/1";
    private static final long FOO_PERIOD = 1000L;

    private static final String BAR_KEY = "bar";
    private static final String BAR_INITIAL_URI = "http://www.bar.comm/2";
    private static final long BAR_PERIOD = 2000L;

    @Mock
    Client mockedClient;

    @Mock
    NewFeedHandler mockedNewFeedHandler;

    @Mock
    PollingExceptionHandler mockedPollingExceptionHandler;

    @Mock
    ScheduledExecutorService mockedScheduledExecutorService;

    List<EndpointConfig> endpointConfigs;

    FeedPoller.FeedPollerBuilder builder;

    FeedPoller feedPoller;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mockStatic(Executors.class);
        when(Executors.newScheduledThreadPool(anyInt())).thenReturn(mockedScheduledExecutorService);

        endpointConfigs = new ArrayList<EndpointConfig>() {{
            add(new EndpointConfig(FOO_KEY, FOO_INITIAL_URI, FOO_PERIOD));
            add(new EndpointConfig(BAR_KEY, BAR_INITIAL_URI, BAR_PERIOD));
        }};

        builder = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withAcceptType(ACCEPT_TYPE)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT);

        feedPoller = builder.build();
    }

    // ************************************************************************
    // ****************************** Builder *********************************
    // ************************************************************************
    @Test
    public void builder_creates_correctFeedPoller() throws Exception {
        Client client = (Client) getInternalState(feedPoller, FIELD_CLIENT);

        List<EndpointConfig> endpointConfigs = (List<EndpointConfig>) getInternalState(feedPoller, FIELD_ENDPOINT_CONFIGS);
        NewFeedHandler newFeedHandler = (NewFeedHandler) getInternalState(feedPoller, FIELD_NEW_FEED_HANDLER);
        long initialDelay = (long) getInternalState(feedPoller, FIELD_INITIAL_DELAY);
        long shutdownTimeout = (long) getInternalState(feedPoller, FIELD_SHUTDOWN_TIMEOUT);
        PollingExceptionHandler pollingExceptionHandler = (PollingExceptionHandler) getInternalState(feedPoller, FIELD_POLLING_EXCEPTION_HANDLER);
        String acceptType = (String) getInternalState(feedPoller, FIELD_ACCEPT_TYPE);

        assertThat("client:", client, equalTo(mockedClient));
        assertThat("endpointConfigs:", endpointConfigs, equalTo(endpointConfigs));
        assertThat("newFeedHandler:", newFeedHandler, equalTo(mockedNewFeedHandler));
        assertThat("initialDelay:", initialDelay, equalTo(INITIAL_DELAY));
        assertThat("shutdownTimeout:", shutdownTimeout, equalTo(SHUT_DOWN_TIMEOUT));
        assertThat("pollingExceptionHandler:", pollingExceptionHandler, equalTo(mockedPollingExceptionHandler));
        assertThat("acceptType:", acceptType, equalTo(ACCEPT_TYPE));
    }

    @Test
    public void builder_useDefaultJerseyClient_ifClientIsNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        Client client = (Client) getInternalState(feedPoller, FIELD_CLIENT);

        assertThat("client is default", client, instanceOf(Client.class));
    }

    @Test(expected = Error.class)
    public void builder_throwsError_ifEndpointConfigsAreNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

    }

    @Test(expected = Error.class)
    public void builder_throwsError_ifNewFeedHandlerAreNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();
    }

    @Test
    public void builder_useDefaultPollingExceptionHandler_ifPollingExceptionHandlerIsNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        PollingExceptionHandler pollingExceptionHandler = (PollingExceptionHandler) getInternalState(feedPoller, FIELD_POLLING_EXCEPTION_HANDLER);

        assertThat("pollingExceptionHandler is default", pollingExceptionHandler, instanceOf(DefaultPollingExceptionHandler.class));
    }

    @Test
    public void builder_useDefaultInitialDelay_ifInitialDelayIsNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        long initialDelay = (long) getInternalState(feedPoller, FIELD_INITIAL_DELAY);

        assertThat("pollingExceptionHandler is default", initialDelay, equalTo(0L));
    }

    @Test
    public void builder_useDefaultShutdownTimeout_ifShutdownTimeoutIsNotAssigned() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withInitialDelay(INITIAL_DELAY)
                .build();

        long shutdownTimeout = (long) getInternalState(feedPoller, FIELD_SHUTDOWN_TIMEOUT);

        assertThat("pollingExceptionHandler is default", shutdownTimeout, equalTo(60000L));
    }

    @Test
    public void builder_useTextAcceptType_ifAcceptTypeIsNotSpecified() throws Exception {
        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();


        String AcceptType = (String) getInternalState(feedPoller, FIELD_ACCEPT_TYPE);

        assertThat("AcceptType is default", AcceptType, equalTo(MediaType.TEXT_PLAIN));
    }

    // ************************************************************************
    // ****************************** FeedPoller ******************************
    // ************************************************************************

    @Test
    public void executors_creates_scheduledThreadPoll() throws Exception {
        verifyStatic(times(1));
        Executors.newScheduledThreadPool(endpointConfigs.size());
    }

    @Test
    public void start_createsTasks() throws Exception {
        feedPoller.start();

        List<PollingTask> tasks = (List<PollingTask>) getInternalState(feedPoller, FILED_TASKS);

        assertThat("size is 2", tasks.size(), equalTo(2));

        ArrayList<EndpointConfig> copy = new ArrayList<EndpointConfig>(this.endpointConfigs);

        for (PollingTask task : tasks) {
            assertThat("client is the same", ((Client) getInternalState(task, "client")), equalTo(mockedClient));
            assertThat("newFeedHandler is the same", ((NewFeedHandler) getInternalState(task, "newFeedHandler")), equalTo(mockedNewFeedHandler));
            assertThat("pollingExceptionHandler is the same", ((PollingExceptionHandler) getInternalState(task, "pollingExceptionHandler")), equalTo(mockedPollingExceptionHandler));
            assertThat("AcceptType is expected", ((String) getInternalState(task, FIELD_ACCEPT_TYPE)), equalTo(ACCEPT_TYPE));

            if (task.getKey().equals(FOO_KEY)) {
                assertThat("initial uri is the same", task.getInitialUri(), equalTo(FOO_INITIAL_URI));

            } else if (task.getKey().equals(BAR_KEY)) {
                assertThat("initial uri is the same", task.getInitialUri(), equalTo(BAR_INITIAL_URI));
            } else {
                fail("impossible to reach here");
            }
        }
    }

    @Test
    public void start_serviceExecutesTasks() throws Exception {
        feedPoller.start();

        verify(mockedScheduledExecutorService, times(1)).scheduleAtFixedRate(any(PollingTask.class), eq(INITIAL_DELAY), eq(FOO_PERIOD), eq(TimeUnit.MILLISECONDS));
        verify(mockedScheduledExecutorService, times(1)).scheduleAtFixedRate(any(PollingTask.class), eq(INITIAL_DELAY), eq(BAR_PERIOD), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shutdown_callsShutdown() throws Exception {
        feedPoller.shutdown();

        verify(mockedScheduledExecutorService, times(1)).shutdown();
    }

    @Test
    public void shutdown_awaitTermination() throws Exception {
        feedPoller.shutdown();

        verify(mockedScheduledExecutorService).awaitTermination(SHUT_DOWN_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shutdown_forceSystemToShutdown_ifTimeIsExceeded() throws Exception {
        given(mockedScheduledExecutorService.awaitTermination(SHUT_DOWN_TIMEOUT, TimeUnit.MILLISECONDS)).willReturn(false);

        feedPoller.shutdown();

        then(mockedScheduledExecutorService).should(times(1)).shutdownNow();
    }

    @Test
    public void shutdown_forceSystemToShutdown_awaitTermination() throws Exception {
        given(mockedScheduledExecutorService.awaitTermination(SHUT_DOWN_TIMEOUT, TimeUnit.MILLISECONDS)).willReturn(false);

        feedPoller.shutdown();

        then(mockedScheduledExecutorService).should(times(1)).awaitTermination(60, TimeUnit.SECONDS);
    }

    @Test
    public void shutdown_forceSystemToShutdown_ifInterruptedExceptionIsThrown() throws Exception {
        given(mockedScheduledExecutorService.awaitTermination(SHUT_DOWN_TIMEOUT, TimeUnit.MILLISECONDS)).willThrow(new InterruptedException());

        feedPoller.shutdown();

        then(mockedScheduledExecutorService).should(times(1)).shutdownNow();
    }

    @Test
    public void shutdown_preserveInterruptStatus_ifInterruptedExceptionIsThrown() throws Exception {
        mockStatic(Thread.class);
        Thread mockedThread = mock(Thread.class);

        given(mockedScheduledExecutorService.awaitTermination(SHUT_DOWN_TIMEOUT, TimeUnit.MILLISECONDS)).willThrow(new InterruptedException());
        given(Thread.currentThread()).willReturn(mockedThread);

        feedPoller.shutdown();

        verifyStatic(times(1));
        Thread.currentThread();

        then(mockedThread).should(times(1)).interrupt();
    }

    @Test
    public void shutdown_returnsPollingResult() throws Exception {
        String nextUri = "next uri";
        feedPoller.start();

        List<PollingTask> tasks = (List<PollingTask>) getInternalState(feedPoller, FILED_TASKS);
        for (PollingTask task : tasks) {
            setInternalState(task, "nextUri", nextUri);
        }

        List<PollingResult> results = feedPoller.shutdown();

        assertThat("size: ", results.size(), equalTo(endpointConfigs.size()));

        for (PollingResult result : results) {
            if (result.getKey().equals(FOO_KEY)) {
                assertThat("initial uri is the same", result.getFirstStartedUri(), equalTo(FOO_INITIAL_URI));

            } else if (result.getKey().equals(BAR_KEY)) {
                assertThat("initial uri is the same", result.getFirstStartedUri(), equalTo(BAR_INITIAL_URI));
            } else {
                fail("impossible to reach here");
            }
            assertThat("last unread uri:", result.getLastUnreadUri(), equalTo(nextUri));
        }
    }
}