package com.example.platform.service;

import com.example.platform.dto.HomeNavModuleUpdateRequest;
import com.example.platform.entity.HomeNavModule;
import com.example.platform.repository.HomeNavModuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeNavModuleServiceTest {

    @Autowired HomeNavModuleService service;
    @Autowired HomeNavModuleRepository repository;

    @Test
    void listPublicReturnsOnlyVisibleModules() {
        HomeNavModule forum = repository.findByCode(HomeNavModule.Code.FORUM).orElseThrow();
        forum.setVisible(false);
        repository.save(forum);

        assertThat(service.listPublic())
                .extracting(dto -> dto.getCode())
                .doesNotContain("FORUM");
        assertThat(service.visibleCodes()).doesNotContain(HomeNavModule.Code.FORUM);
    }

    @Test
    void updateCanToggleVisibility() {
        HomeNavModule news = repository.findByCode(HomeNavModule.Code.NEWS).orElseThrow();
        HomeNavModuleUpdateRequest req = new HomeNavModuleUpdateRequest();
        req.setVisible(false);

        var updated = service.update(news.getId(), req);

        assertThat(updated.getVisible()).isFalse();
        assertThat(service.listPublic())
                .extracting(dto -> dto.getCode())
                .doesNotContain("NEWS");
    }
}
