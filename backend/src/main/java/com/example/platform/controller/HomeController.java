package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.HomeDto;
import com.example.platform.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ApiResponse<HomeDto> get() {
        return ApiResponse.ok(homeService.getHome());
    }
}
