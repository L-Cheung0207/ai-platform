package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.ForumCategoryWriteRequest;
import com.example.platform.entity.ForumCategory;
import com.example.platform.repository.ForumCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumCategoryServiceTest {

    @Mock
    private ForumCategoryRepository categoryRepository;

    private ForumCategoryService service;

    @BeforeEach
    void setUp() {
        service = new ForumCategoryService(categoryRepository);
    }

    @Test
    void createAutoSlugifiesAndTrimsFields() {
        when(categoryRepository.findBySlugIgnoreCase("forum-ops")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(ForumCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForumCategoryWriteRequest req = new ForumCategoryWriteRequest();
        req.setName("  Forum Ops  ");
        req.setSlug("  ");
        req.setDescription("  ops category  ");
        req.setSortOrder(9);
        req.setEnabled(true);

        var dto = service.create(req);

        ArgumentCaptor<ForumCategory> captor = ArgumentCaptor.forClass(ForumCategory.class);
        verify(categoryRepository).save(captor.capture());
        ForumCategory saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Forum Ops");
        assertThat(saved.getSlug()).isEqualTo("forum-ops");
        assertThat(saved.getDescription()).isEqualTo("ops category");
        assertThat(saved.getSortOrder()).isEqualTo(9);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(dto.getSlug()).isEqualTo("forum-ops");
    }

    @Test
    void updateRejectsDuplicateSlugFromAnotherCategory() {
        ForumCategory category = category(10L, "Old", "old");
        ForumCategory existing = category(20L, "Other", "forum-ops");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.findBySlugIgnoreCase("forum-ops")).thenReturn(Optional.of(existing));

        ForumCategoryWriteRequest req = new ForumCategoryWriteRequest();
        req.setName("Forum Ops");
        req.setSlug("forum-ops");
        req.setSortOrder(1);

        assertThatThrownBy(() -> service.update(10L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slug");
    }

    @Test
    void deleteDisablesCategory() {
        ForumCategory category = category(10L, "Ops", "forum-ops");
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(ForumCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(10L);

        assertThat(category.getEnabled()).isFalse();
        verify(categoryRepository).save(category);
    }

    @Test
    void getVisibleRejectsDisabledCategory() {
        ForumCategory category = category(10L, "Ops", "forum-ops");
        category.setEnabled(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.getVisible(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("论坛分类不存在");
    }

    private ForumCategory category(Long id, String name, String slug) {
        ForumCategory category = new ForumCategory();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setSortOrder(1);
        category.setEnabled(true);
        return category;
    }
}
