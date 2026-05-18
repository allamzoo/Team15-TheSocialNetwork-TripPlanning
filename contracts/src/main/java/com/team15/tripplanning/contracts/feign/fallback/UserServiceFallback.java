package com.team15.tripplanning.contracts.feign.fallback;

import com.team15.tripplanning.contracts.dto.UserDTO;
import com.team15.tripplanning.contracts.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safe-default fallback for {@link UserServiceClient}.
 *
 * <p>{@link #getUser(Long)} returns {@code null} because user lookup is always
 * a hard validation guard — there is no safe "pretend user" when user-service
 * is unreachable. Callers must null-check and return 503:
 * <pre>
 *   UserDTO user = userServiceClient.getUser(userId);
 *   if (user == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
 *       "user-service unavailable");
 * </pre>
 */
public final class UserServiceFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceFallback.class);

    /** Singleton for use without Spring context. */
    public static final UserServiceFallback SAFE = new UserServiceFallback();

    /**
     * Returns {@code null} — callers must handle this as a hard failure (503).
     * Do NOT return a fake user; that could allow operations on behalf of a
     * non-existent or deactivated account.
     */
    @Override
    public UserDTO getUser(Long userId) {
        log.warn("user-service fallback: getUser({}) — returning null (hard guard)", userId);
        return null;
    }

    @Override
    public UserDTO getUserInternal(Long userId) {
        log.warn("user-service fallback: getUserInternal({}) — returning null (hard guard)", userId);
        return null;
    }
}
