package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.service.exception.RateLimitException;

@Service
public class EmailRateLimitService {

    private static final String RATE_LIMIT_CACHE = "email-rate-limit";

    private final long userEmailMillis;
    private final long forgotPasswordMillis;

    @Autowired
    private CacheManager cacheManager;

    public EmailRateLimitService(
            @Value("${lazerwiki.email.user_email.throttle_minutes:5}") long userEmailMinutes,
            @Value("${lazerwiki.email.forgot_password.throttle_minutes:15}") long forgotPasswordMinutes) {
        this.userEmailMillis = userEmailMinutes * 60 * 1000;
        this.forgotPasswordMillis = forgotPasswordMinutes * 60 * 1000;
    }

    public void checkSetEmailRateLimit(String userName) {
        checkAndRecordLimit("user-email:" + userName, userEmailMillis,
                "Email change requested too recently. Please try again in %d seconds.");
    }

    public void checkPasswordResetRateLimit(String email) {
        checkAndRecordLimit("forgot-password:" + email, forgotPasswordMillis,
                "Too many password reset requests. Please try again in %d seconds.");
    }

    private void checkAndRecordLimit(String cacheKey, long throttleMillis, String messageTemplate) {
        Cache cache = cacheManager.getCache(RATE_LIMIT_CACHE);
        Long lastAttemptTime = cache.get(cacheKey, Long.class);

        if (lastAttemptTime != null) {
            long timeSinceLastAttempt = System.currentTimeMillis() - lastAttemptTime;
            if (timeSinceLastAttempt < throttleMillis) {
                long remainingSeconds = (throttleMillis - timeSinceLastAttempt + 999) / 1000;
                throw new RateLimitException(remainingSeconds);
            }
        }

        cache.put(cacheKey, System.currentTimeMillis());
    }
}
