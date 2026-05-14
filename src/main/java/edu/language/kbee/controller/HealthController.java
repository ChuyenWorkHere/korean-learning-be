package edu.language.kbee.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/loaderio-3ce7a2dd8a033d5208bcc83a550bbae2/")
    public String verifyLoader() {
        return "loaderio-3ce7a2dd8a033d5208bcc83a550bbae2";
    }
}
