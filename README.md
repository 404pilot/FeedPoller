# FeedPoller
[![Build Status](https://travis-ci.org/404pilot/FeedPoller.svg?branch=master)](https://travis-ci.org/404pilot/FeedPoller)

a simple util to poll resources

## how-to
``` java
// endpoint
List<EndpointConfig> endpointConfigs = new ArrayList<EndpointConfig>() {{
    add(new EndpointConfig("google", "https://www.google.com", 5000L));
    add(new EndpointConfig("google", "https://www.amazon.com", 5000L));
}};

// newFeedHandler
NewFeedHandler newFeedHandler = new NewFeedHandler() {
    public String receiveNewFeed(String page) throws EmptyFeedException {
        return "next uri";
    }
};

// client
Client client = Client.create();

// feedPoller
FeedPoller feedPoller = new FeedPoller.FeedPollerBuilder()
        .withClient(client)
        .withEndpointConfigs(endpointConfigs)
        .withNewFeedHandler(newFeedHandler)
        .withInitialDelay(1000)
        .withShutdownTimeout(10000)
        .build();

feedPoller.start();

// run

List<PollingResult> result = feedPoller.shutdown();
```