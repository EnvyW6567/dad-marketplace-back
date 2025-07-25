package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
@Slf4j
@RequiredArgsConstructor
public class DebugController {

    @GetMapping("/protected")
    public void protectedEndPoint() {
        log.info("if authentication token included. this message is normal. if not, abnormal");
    }
}
