package feedpoller;

import com.sun.jersey.api.client.Client;
import feedpoller.domain.EndpointConfig;
import feedpoller.handler.DefaultPollingExceptionHandler;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FeedPoller.class)
public class FeedPollerTest {
    public static final String FIELD_CLIENT = "client";
    public static final String FILED_ENDPOINT_CONFIGS = "endpointConfigs";
    public static final String FILED_NEW_FEED_HANDLER = "newFeedHandler";
    public static final String FILED_INITIAL_DELAY = "initialDelay";
    public static final String FILED_SHUTDOWN_TIMEOUT = "shutdownTimeout";
    public static final String FILED_POLLING_EXCEPTION_HANDLER = "pollingExceptionHandler";
    public static final String FILED_SERVICE = "service";
    public static final String FILED_TASKS = "tasks";

    private static final long INITIAL_DELAY = 10000L; // don't run tasks in unit test
    private static final long SHUT_DOWN_TIMEOUT = 1L;

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


    FeedPoller feedPoller;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        //PowerMockito.spy(Executors.class);

        endpointConfigs = new ArrayList<EndpointConfig>() {
            {
                add(new EndpointConfig(
                        FOO_KEY,
                        FOO_INITIAL_URI,
                        FOO_PERIOD
                ));
                add(new EndpointConfig(
                        BAR_KEY,
                        BAR_INITIAL_URI,
                        BAR_PERIOD
                ));
            }
        };

        feedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        setPrivateField(feedPoller, FILED_SERVICE, mockedScheduledExecutorService);
    }

    @Test
    public void builder_creates_correctFeedPoller() throws Exception {
        Client client = (Client) getPrivateField(feedPoller, FIELD_CLIENT);

        List<EndpointConfig> endpointConfigs = (List<EndpointConfig>) getPrivateField(feedPoller, FILED_ENDPOINT_CONFIGS);

        NewFeedHandler newFeedHandler = (NewFeedHandler) getPrivateField(feedPoller, FILED_NEW_FEED_HANDLER);

        long initialDelay = (long) getPrivateField(feedPoller, FILED_INITIAL_DELAY);

        long shutdownTimeout = (long) getPrivateField(feedPoller, FILED_SHUTDOWN_TIMEOUT);

        PollingExceptionHandler pollingExceptionHandler = (PollingExceptionHandler) getPrivateField(feedPoller, FILED_POLLING_EXCEPTION_HANDLER);

        assertThat("client:", client, equalTo(mockedClient));
        assertThat("endpointConfigs:", endpointConfigs, equalTo(endpointConfigs));
        assertThat("newFeedHandler:", newFeedHandler, equalTo(mockedNewFeedHandler));
        assertThat("initialDelay:", initialDelay, equalTo(INITIAL_DELAY));
        assertThat("shutdownTimeout:", shutdownTimeout, equalTo(SHUT_DOWN_TIMEOUT));
        assertThat("pollingExceptionHandler:", pollingExceptionHandler, equalTo(mockedPollingExceptionHandler));
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

        Client client = (Client) getPrivateField(feedPoller, FIELD_CLIENT);

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

        PollingExceptionHandler pollingExceptionHandler = (PollingExceptionHandler) getPrivateField(feedPoller, FILED_POLLING_EXCEPTION_HANDLER);

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

        long initialDelay = (long) getPrivateField(feedPoller, FILED_INITIAL_DELAY);

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

        long shutdownTimeout = (long) getPrivateField(feedPoller, FILED_SHUTDOWN_TIMEOUT);

        assertThat("pollingExceptionHandler is default", shutdownTimeout, equalTo(60000L));
    }

    // newScheduledThreadPool size to


    @Test
    public void executors_creates_threadPoll() throws Exception {
        FeedPoller anotherFeedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        ScheduledThreadPoolExecutor service = (ScheduledThreadPoolExecutor) getPrivateField(anotherFeedPoller, FILED_SERVICE);

        int poolSize = service.getCorePoolSize();

        assertThat("initial core poll size is", poolSize, equalTo(endpointConfigs.size()));

    }

    @Test
    public void scheduledThreadPoll_isCreated() throws Exception {
        FeedPoller anotherFeedPoller = new FeedPoller.FeedPollerBuilder()
                .withClient(mockedClient)
                .withEndpointConfigs(endpointConfigs)
                .withNewFeedHandler(mockedNewFeedHandler)
                .withInitialDelay(INITIAL_DELAY)
                .withPollingExceptionHandler(mockedPollingExceptionHandler)
                .withShutdownTimeout(SHUT_DOWN_TIMEOUT)
                .build();

        ScheduledExecutorService service = (ScheduledExecutorService) getPrivateField(anotherFeedPoller, "service");

        assertThat("scheduled", service, instanceOf(ScheduledExecutorService.class));
    }

    @Test
    public void start_createsTasks() throws Exception {
        feedPoller.start();

        List<PollingTask> tasks = (List<PollingTask>) getPrivateField(feedPoller, FILED_TASKS);

        assertThat("size is 2", tasks.size(), equalTo(2));

        ArrayList<EndpointConfig> copy = new ArrayList<EndpointConfig>(this.endpointConfigs);

        for (PollingTask task : tasks) {
            assertThat("client is the same", ((Client) getPrivateField(task, "client")), equalTo(mockedClient));
            assertThat("newFeedHandler is the same", ((NewFeedHandler) getPrivateField(task, "newFeedHandler")), equalTo(mockedNewFeedHandler));
            assertThat("pollingExceptionHandler is the same", ((PollingExceptionHandler) getPrivateField(task, "pollingExceptionHandler")), equalTo(mockedPollingExceptionHandler));

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

    private Object getPrivateField(Object instance, String fieldName) {
        try {
            Field declaredField = instance.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField.get(instance);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setPrivateField(Object instance, String fieldName, Object assignedObj) {
        try {
            Field declaredField = instance.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(instance, assignedObj);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}