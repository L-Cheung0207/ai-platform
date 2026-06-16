package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.HomeNavModuleDto;
import com.example.platform.dto.HomeNavModuleUpdateRequest;
import com.example.platform.service.HomeNavModuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/home/nav-modules")
public class AdminHomeNavModuleController {

    private final HomeNavModuleService homeNavModuleService;

    public AdminHomeNavModuleController(HomeNavModuleService homeNavModuleService) {
        this.homeNavModuleService = homeNavModuleService;
    }

    @GetMapping
    public ApiResponse<List<HomeNavModuleDto>> list() {
        return ApiResponse.ok(homeNavModuleService.listAdmin());
    }

    @PutMapping("/{id}")
    public ApiResponse<HomeNavModuleDto> update(@PathVariable Long id, @RequestBody HomeNavModuleUpdateRequest req) {
        return ApiResponse.ok(homeNavModuleService.update(id, req));
    }
}
