package feedpoller;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import feedpoller.domain.EmptyFeedException;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PollingTaskTest {
    private static final String KEY = "MOCKED KEY";
    private static final String INITIAL_URI = "MOCKED INITIAL URI";
    private static final String PAGE = "MOCKED PAGE";
    private static final String NEXT_URI = "MOCKED NEXT URI";

    @Mock
    private Client mockedClient;

    @Mock
    private WebResource mockedWebResource;

    @Mock
    private NewFeedHandler mockedNewFeedHandler;

    @Mock
    private PollingExceptionHandler mockedPollingExceptionHandler;

    @Mock
    private EmptyFeedException mockedEmptyFeedException;

    @Mock
    private RuntimeException mockedRuntimeException;


    private PollingTask task;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        task = new PollingTask(mockedClient, mockedNewFeedHandler, KEY, INITIAL_URI, mockedPollingExceptionHandler);

        when(mockedClient.resource(INITIAL_URI)).thenReturn(mockedWebResource);
        when(mockedWebResource.get(String.class)).thenReturn(PAGE);
        when(mockedNewFeedHandler.receiveNewFeed(PAGE)).thenReturn(NEXT_URI);
    }

    @Test
    public void task_createsWebResource() throws Exception {
        task.run();

        verify(mockedClient).resource(INITIAL_URI);
    }

    @Test
    public void task_pollsWebResource() throws Exception {
        task.run();

        verify(mockedWebResource).get(String.class);
    }

    @Test
    public void newFeedHandler_processNewFeed() throws Exception {
        task.run();

        verify(mockedNewFeedHandler).receiveNewFeed(PAGE);
    }

    @Test
    public void task_setsNextUriToItself() throws Exception {
        task.run();

        assertThat("uri is set back to itself", task.getNextUri(), equalTo(NEXT_URI));
    }

    @Test
    public void task_catchesEmptyFeedException_andDoNotThrowIt() throws Exception {
        doThrow(mockedEmptyFeedException).when(mockedNewFeedHandler).receiveNewFeed(PAGE);

        try {
            task.run();
        } catch (Exception e) {
            fail("no exception is thrown");
        }
    }

    @Test
    public void nextUri_isNotChanged_ifEmptyFeedExceptionIsThrown() throws Exception {
        doThrow(mockedEmptyFeedException).when(mockedNewFeedHandler).receiveNewFeed(PAGE);

        task.run();

        assertThat("uri is not changed", task.getNextUri(), equalTo(INITIAL_URI));
    }

    @Test
    public void task_catchException_andDoNotThrowIt() throws Exception {
        doThrow(mockedRuntimeException).when(mockedWebResource).get(String.class);

        try {
            task.run();
        } catch (Exception e) {
            fail("no exception is thrown");
        }
    }

    @Test
    public void nextUri_isNotChanged_ifExceptionIsThrownDuringPolling() throws Exception {
        doThrow(mockedRuntimeException).when(mockedWebResource).get(String.class);

        task.run();

        assertThat("uri is not changed", task.getNextUri(), equalTo(INITIAL_URI));
    }

    @Test
    public void pollingExceptionHandler_processException_ifExceptionIsThrownDuringPolling() throws Exception {
        doThrow(mockedRuntimeException).when(mockedWebResource).get(String.class);

        task.run();

        verify(mockedPollingExceptionHandler).handle(mockedRuntimeException);
    }
}