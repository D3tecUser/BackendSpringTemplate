package com.d3tec.template.nomeDoSeuProjeto.service.auth;

import com.d3tec.template.nomeDoSeuProjeto.exception.exceptions.RateLimitException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BruteforceProtectionService {
    @Value("${security.bruteforce.enabled:true}")
    private boolean enabled;

    @Value("${security.bruteforce.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.bruteforce.window-seconds:300}")
    private long windowSeconds;

    @Value("${security.bruteforce.lock-seconds:900}")
    private long lockSeconds;

    // Contador de falhas expira automaticamente após a janela
    private Cache<String, Integer> failuresCache;

    // Bloqueio expira automaticamente após lockSeconds
    private Cache<String, Long> lockCache;

    @jakarta.annotation.PostConstruct
    void init() {
        failuresCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(windowSeconds))
                .maximumSize(200_000)
                .build();

        lockCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(lockSeconds))
                .maximumSize(200_000)
                .build();
    }

    public void assertNotBlocked(String key) {
        if (!enabled) return;

        Long lockedUntilEpoch = lockCache.getIfPresent(key);
        if (lockedUntilEpoch == null) return;

        long now = Instant.now().getEpochSecond();

        if (now < lockedUntilEpoch) {
            long retryAfter = Math.max(lockedUntilEpoch - now, 1);
            throw new RateLimitException("Muitas tentativas. Tente novamente em " + retryAfter + "s.");
        }

        lockCache.invalidate(key);
    }

    public void onLoginFailure(String key) {
        if (!enabled) return;

        int current = failuresCache.get(key, k -> 0);
        current++;
        failuresCache.put(key, current);

        if (current >= maxAttempts) {
            long lockedUntil = Instant.now().getEpochSecond() + lockSeconds;
            lockCache.put(key, lockedUntil);
            failuresCache.invalidate(key);
        }
    }

    public void onLoginSuccess(String key) {
        if (!enabled) return;

        failuresCache.invalidate(key);
        lockCache.invalidate(key);
    }
}
