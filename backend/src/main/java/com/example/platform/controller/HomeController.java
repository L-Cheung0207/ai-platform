package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.HomeDto;
import com.example.platform.dto.HomeNavModuleDto;
import com.example.platform.service.HomeNavModuleService;
import com.example.platform.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;
    private final HomeNavModuleService homeNavModuleService;

    public HomeController(HomeService homeService, HomeNavModuleService homeNavModuleService) {
        this.homeService = homeService;
        this.homeNavModuleService = homeNavModuleService;
    }

    @GetMapping
    public ApiResponse<HomeDto> get() {
        return ApiResponse.ok(homeService.getHome());
    }

    @GetMapping("/nav-modules")
    public ApiResponse<List<HomeNavModuleDto>> navModules() {
        return ApiResponse.ok(homeNavModuleService.listPublic());
    }
}
