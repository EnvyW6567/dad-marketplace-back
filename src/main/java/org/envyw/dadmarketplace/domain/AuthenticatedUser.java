package org.envyw.dadmarketplace.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
@Getter
public class AuthenticatedUser {
    private Long userId;
    private Set<String> authorities;
    private boolean isAuthenticated;

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

}
