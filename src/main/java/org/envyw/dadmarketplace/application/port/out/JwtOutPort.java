package org.envyw.dadmarketplace.application.port.out;

public interface JwtOutPort {
    String refresh(String refreshToken);
}
