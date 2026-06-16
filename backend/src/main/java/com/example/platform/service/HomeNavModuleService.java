package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.HomeNavModuleDto;
import com.example.platform.dto.HomeNavModuleUpdateRequest;
import com.example.platform.entity.HomeNavModule;
import com.example.platform.repository.HomeNavModuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HomeNavModuleService {

    private final HomeNavModuleRepository repository;

    public HomeNavModuleService(HomeNavModuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HomeNavModuleDto> listPublic() {
        return repository.findByVisibleTrueOrderBySortOrderAscIdAsc().stream()
                .map(HomeNavModuleDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeNavModuleDto> listAdmin() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(HomeNavModuleDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<HomeNavModule.Code> visibleCodes() {
        return repository.findByVisibleTrueOrderBySortOrderAscIdAsc().stream()
                .map(HomeNavModule::getCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(HomeNavModule.Code.class)));
    }

    @Transactional
    public HomeNavModuleDto update(Long id, HomeNavModuleUpdateRequest req) {
        HomeNavModule module = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("模块不存在"));
        if (req.getVisible() != null) {
            module.setVisible(req.getVisible());
        }
        if (req.getSortOrder() != null) {
            module.setSortOrder(Math.max(0, req.getSortOrder()));
        }
        return HomeNavModuleDto.fromEntity(repository.save(module));
    }
}
