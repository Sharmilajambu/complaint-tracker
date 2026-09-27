package service;

import model.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory session store — token → user id. No external library. */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, Integer> tokenToUserId = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, user.getId());
        return token;
    }

    public Optional<Integer> getUserId(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return Optional.ofNullable(tokenToUserId.get(token));
    }

    public void invalidate(String token) {
        if (token != null) tokenToUserId.remove(token);
    }
}
