package feedpoller.handler;

public interface NewFeedHandler {
    /**
     * @param page
     * @return Next uri for polling
     */
    String receiveNewFeed(String page) throws Exception;
}