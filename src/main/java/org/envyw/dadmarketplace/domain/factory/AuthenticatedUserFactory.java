package org.envyw.dadmarketplace.domain.factory;

import org.envyw.dadmarketplace.domain.AuthenticatedUser;

import java.util.Set;

public class AuthenticatedUserFactory {
    public static AuthenticatedUser anonymous() {
        return new AuthenticatedUser(null, Set.of(), false);
    }

    public static AuthenticatedUser authenticated(Long userId, Set<String> authorities) {
        return new AuthenticatedUser(userId, authorities, true);
    }
}
