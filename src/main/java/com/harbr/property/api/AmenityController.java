package com.harbr.property.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.property.application.AmenityService;
import com.harbr.property.application.dto.AmenityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(amenityService.listAll()));
    }
}