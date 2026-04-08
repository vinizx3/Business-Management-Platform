package com.pmei;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PmeiController {
    @GetMapping("/")
    public String home() {
        return "PMEI API is running 🚀";
    }
}
