package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.dto.response.UserInfoResDto;
import org.envyw.dadmarketplace.application.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Mono<UserInfoResDto> getUserInfo() {
        return userService.getCurrentUserInfo();
    }
}
