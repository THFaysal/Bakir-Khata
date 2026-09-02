package com.example.bakir_khata.security.tab;

import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TabAuthenticationStore {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;

    public TabAuthenticationStore(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String create(String httpSessionId, Authentication authentication) {
        cleanup();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(token, new Entry(httpSessionId, authentication, Instant.now()));
        return token;
    }

    public Optional<Authentication> resolve(String token, String httpSessionId) {
        if (token == null || token.isBlank() || httpSessionId == null) return Optional.empty();
        Entry entry = entries.get(token);
        if (entry == null || !entry.httpSessionId().equals(httpSessionId)) return Optional.empty();

        Authentication authentication = entry.authentication();
        if (authentication.getPrincipal() instanceof User storedUser && storedUser.getId() != null) {
            Optional<User> freshUser = userRepository.findById(storedUser.getId());
            if (freshUser.isEmpty() || !freshUser.get().isEnabled() || !freshUser.get().isAccountNonLocked()) {
                entries.remove(token);
                return Optional.empty();
            }
            User user = freshUser.get();
            authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        }

        entries.put(token, new Entry(entry.httpSessionId(), authentication, Instant.now()));
        return Optional.of(authentication);
    }

    public void remove(String token, String httpSessionId) {
        if (token == null || httpSessionId == null) return;
        Entry entry = entries.get(token);
        if (entry != null && entry.httpSessionId().equals(httpSessionId)) entries.remove(token);
    }

    /**
     * Immediately revoke every tab-scoped login for a user after a role or account-status
     * change. This prevents a previously authenticated tab from continuing with stale
     * authorities until its normal expiry time.
     */
    public void removeForUser(Long userId) {
        if (userId == null) return;
        entries.entrySet().removeIf(e -> {
            Object principal = e.getValue().authentication().getPrincipal();
            return principal instanceof User user && userId.equals(user.getId());
        });
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(12, ChronoUnit.HOURS);
        entries.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
    }

    private record Entry(String httpSessionId, Authentication authentication, Instant lastAccess) {}
}
