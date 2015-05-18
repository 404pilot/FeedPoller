package feedpoller.handler;

import feedpoller.domain.EmptyFeedException;

public interface NewFeedHandler {
    /**
     * @param page
     * @return Next uri for polling
     */
    String receiveNewFeed(String page) throws EmptyFeedException;
}