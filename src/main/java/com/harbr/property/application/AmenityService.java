package com.harbr.property.application;

import com.harbr.property.application.dto.AmenityResponse;
import com.harbr.property.infrastructure.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;

    @Transactional(readOnly = true)
    public List<AmenityResponse> listAll() {
        return amenityRepository.findAll().stream()
                .map(a -> new AmenityResponse(a.getId(), a.getName(), a.getCategory().name(), a.getIconKey()))
                .toList();
    }
}