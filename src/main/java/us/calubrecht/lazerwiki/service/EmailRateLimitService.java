package us.calubrecht.lazerwiki.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.service.exception.RateLimitException;

import java.util.concurrent.TimeUnit;

@Service
public class EmailRateLimitService {

    private final long userEmailMinutes;
    private final long forgotPasswordMinutes;

    private Cache<String, Long> setEmailCache;
    private Cache<String, Long> resetPasswordCache;

    public EmailRateLimitService(
            @Value("${lazerwiki.email.user_email.throttle_minutes:5}") long userEmailMinutes,
            @Value("${lazerwiki.email.forgot_password.throttle_minutes:15}") long forgotPasswordMinutes) {
        this.userEmailMinutes = userEmailMinutes;
        this.forgotPasswordMinutes = forgotPasswordMinutes;
    }

    @PostConstruct
    void initCaches() {
        setEmailCache = Caffeine.newBuilder().expireAfterWrite(userEmailMinutes, TimeUnit.MINUTES).build();
        resetPasswordCache = Caffeine.newBuilder().expireAfterWrite(forgotPasswordMinutes, TimeUnit.MINUTES).build();
    }

    public void checkSetEmailRateLimit(String userName) {
        checkAndRecordLimit(setEmailCache, userName, userEmailMinutes * 60 * 1000);
    }

    public void checkPasswordResetRateLimit(String email) {
        checkAndRecordLimit(resetPasswordCache, email, forgotPasswordMinutes * 60 * 1000);
    }

    private void checkAndRecordLimit(Cache<String, Long> cache, String key, long throttleMillis) {
        long expiresAt = System.currentTimeMillis() + throttleMillis;
        Long existing = cache.asMap().putIfAbsent(key, expiresAt);
        if (existing != null) {
            long remainingSeconds = (existing - System.currentTimeMillis() + 999) / 1000;
            throw new RateLimitException(Math.max(1, remainingSeconds));
        }
    }
}
