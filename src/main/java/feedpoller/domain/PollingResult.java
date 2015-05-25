package feedpoller.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PollingResult {
    private String key;
    private String firstStartedUri;
    private String lastUnreadUri;

    // equals and hashcode
}