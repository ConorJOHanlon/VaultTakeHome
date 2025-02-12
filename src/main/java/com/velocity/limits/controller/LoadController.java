package com.velocity.limits.controller;

import com.velocity.limits.model.LoadRequest;
import com.velocity.limits.model.LoadResponse;
import com.velocity.limits.service.LoadLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loads")
@RequiredArgsConstructor
public class LoadController {
    private final LoadLimitService loadLimitService;

    @PostMapping
    public ResponseEntity<LoadResponse> processLoad(@RequestBody LoadRequest request) {
        LoadResponse response = loadLimitService.processLoad(request);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
} 