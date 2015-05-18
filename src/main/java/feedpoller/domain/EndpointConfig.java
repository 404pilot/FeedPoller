package feedpoller.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class EndpointConfig {
    private final String key;
    private final String start;
    private final long periodInMilliseconds;
}