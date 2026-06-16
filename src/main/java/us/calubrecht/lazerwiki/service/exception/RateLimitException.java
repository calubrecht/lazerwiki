package us.calubrecht.lazerwiki.service.exception;

public class RateLimitException extends RuntimeException {
    private final long secondsRemaining;

    public RateLimitException(long secondsRemaining) {
        super("Rate limit exceeded");
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
