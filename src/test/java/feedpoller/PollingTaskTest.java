package feedpoller;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import feedpoller.domain.EmptyFeedException;
import feedpoller.handler.NewFeedHandler;
import feedpoller.handler.PollingExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.MediaType;

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
    private static final String ACCEPT = MediaType.TEXT_PLAIN;

    @Mock
    private Client mockedClient;

    @Mock
    private WebResource mockedWebResource;

    @Mock
    private WebResource.Builder mockedBuilder;

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

        task = new PollingTask(mockedClient, mockedNewFeedHandler, KEY, INITIAL_URI, ACCEPT, mockedPollingExceptionHandler);

        when(mockedClient.resource(INITIAL_URI)).thenReturn(mockedWebResource);
        when(mockedWebResource.accept(anyString())).thenReturn(mockedBuilder);
        when(mockedBuilder.get(String.class)).thenReturn(PAGE);
        when(mockedNewFeedHandler.receiveNewFeed(PAGE)).thenReturn(NEXT_URI);
    }

    @Test
    public void task_createsWebResource() throws Exception {
        task.run();

        verify(mockedClient).resource(INITIAL_URI);
    }

    @Test
    public void task_setAcceptType() throws Exception {
        task.run();

        verify(mockedWebResource).accept(ACCEPT);
    }

    @Test
    public void task_pollsWebResource() throws Exception {
        task.run();

        verify(mockedBuilder).get(String.class);
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
        doThrow(mockedRuntimeException).when(mockedBuilder).get(String.class);

        task.run();

        assertThat("uri is not changed", task.getNextUri(), equalTo(INITIAL_URI));
    }

    @Test
    public void pollingExceptionHandler_processException_ifExceptionIsThrownDuringPolling() throws Exception {
        doThrow(mockedRuntimeException).when(mockedBuilder).get(String.class);

        task.run();

        verify(mockedPollingExceptionHandler).handle(mockedRuntimeException);
    }
}