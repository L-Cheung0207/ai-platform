package com.example.platform.controller;

import com.example.platform.dto.ForumCategoryDto;
import com.example.platform.dto.ForumPostDetailDto;
import com.example.platform.dto.ForumPostWriteRequest;
import com.example.platform.dto.ForumReplyDto;
import com.example.platform.dto.ForumReplyWriteRequest;
import com.example.platform.entity.ForumCategory;
import com.example.platform.entity.Tag;
import com.example.platform.entity.User;
import com.example.platform.repository.ForumCategoryRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ForumApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ForumCategoryRepository forumCategoryRepository;
    @Autowired TagRepository tagRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        if (!userRepository.existsByUsername("forum-user")) {
            User user = new User();
            user.setUsername("forum-user");
            user.setPasswordHash(passwordEncoder.encode("forum-pass"));
            userRepository.save(user);
        }
        if (!userRepository.existsByUsername("forum-user-2")) {
            User user = new User();
            user.setUsername("forum-user-2");
            user.setPasswordHash(passwordEncoder.encode("forum-pass-2"));
            userRepository.save(user);
        }
        if (!userRepository.existsByUsername("forum-admin")) {
            User user = new User();
            user.setUsername("forum-admin");
            user.setPasswordHash(passwordEncoder.encode("forum-admin-pass"));
            user.setRole(User.Role.ADMIN);
            userRepository.save(user);
        }
        if (forumCategoryRepository.findBySlugIgnoreCase("forum-test").isEmpty()) {
            ForumCategory category = new ForumCategory();
            category.setName("测试交流");
            category.setSlug("forum-test");
            category.setDescription("测试用分类");
            category.setSortOrder(1);
            forumCategoryRepository.save(category);
        }
    }

    @Test
    void anonymousCanReadPublicForumButCannotWrite() throws Exception {
        mockMvc.perform(get("/api/forum/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/forum/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/api/forum/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","content":"y","postType":"DISCUSSION","categoryId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicListSupportsKeywordTagAndHotSort() throws Exception {
        String token = login("forum-user", "forum-pass");
        ForumCategoryDto category = firstCategory();
        Tag tag = createTag("forum-review");

        ForumPostDetailDto hot = createPost(token, "Hot Topic", "关键词命中", "DISCUSSION", category.getId(), List.of(tag.getId()));
        ForumPostDetailDto cold = createPost(token, "Cold Topic", "没有热度", "DISCUSSION", category.getId(), List.of());

        replyToPost(token, hot.getId(), "先补一个回复");
        mockMvc.perform(post("/api/forum/posts/" + hot.getId() + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/forum/posts")
                        .param("keyword", "关键词")
                        .param("categoryId", String.valueOf(category.getId()))
                        .param("tagIds", String.valueOf(tag.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Hot Topic"));

        mockMvc.perform(get("/api/forum/posts")
                        .param("sort", "hot")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("Hot Topic"))
                .andExpect(jsonPath("$.data.items[0].replyCount").value(1))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(1));

        mockMvc.perform(get("/api/forum/tags").param("q", "review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("forum-review"));
        assertThat(cold.getId()).isNotNull();
    }

    @Test
    void authorAndReplyOwnerPermissionsAreEnforced() throws Exception {
        String token = login("forum-user", "forum-pass");
        String otherToken = login("forum-user-2", "forum-pass-2");
        ForumCategoryDto category = firstCategory();

        ForumPostDetailDto post = createPost(token, "Permission Topic", "权限测试", "DISCUSSION", category.getId(), List.of());
        ForumReplyDto reply = replyToPost(token, post.getId(), "作者回复");

        mockMvc.perform(put("/api/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest("Edited", "内容", "DISCUSSION", category.getId(), List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/forum/replies/" + reply.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest("改不了"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest("Edited", "内容", "DISCUSSION", category.getId(), List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Edited"));

        mockMvc.perform(delete("/api/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void mineEndpointsReturnOwnedContent() throws Exception {
        String token = login("forum-user", "forum-pass");
        ForumCategoryDto category = firstCategory();

        ForumPostDetailDto post = createPost(token, "Mine Topic", "我的帖子", "QUESTION", category.getId(), List.of());
        ForumReplyDto reply = replyToPost(token, post.getId(), "我的回复");

        mockMvc.perform(post("/api/forum/posts/" + post.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/forum/mine/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(post.getId().intValue()));

        mockMvc.perform(get("/api/forum/mine/replies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(reply.getId().intValue()));

        mockMvc.perform(get("/api/forum/mine/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(post.getId().intValue()));
    }

    @Test
    void adminCanModeratePostsAndManageCategories() throws Exception {
        String userToken = login("forum-user", "forum-pass");
        String adminToken = login("forum-admin", "forum-admin-pass");
        ForumCategoryDto category = firstCategory();
        ForumPostDetailDto first = createPost(userToken, "Moderation Topic A", "管理测试 A", "DISCUSSION", category.getId(), List.of());
        ForumPostDetailDto post = createPost(userToken, "Moderation Topic B", "管理测试 B", "DISCUSSION", category.getId(), List.of());

        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/pin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/forum/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(post.getId().intValue()));
        assertThat(first.getId()).isNotEqualTo(post.getId());

        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/lock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/forum/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "LOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(post.getId().intValue()));

        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/hide")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/forum/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("HIDDEN"));

        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/show")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/unlock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/forum/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "Moderation Topic B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/forum/posts/" + post.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/forum/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Forum Ops",
                                "slug", "",
                                "description", "Forum ops category",
                                "sortOrder", 88,
                                "enabled", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("forum-ops"));

        mockMvc.perform(post("/api/admin/forum/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Forum Ops 2",
                                "slug", "forum-ops",
                                "description", "duplicate slug",
                                "sortOrder", 89,
                                "enabled", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Slug")));
    }

    @Test
    void lockedPostRejectsEditAndReply() throws Exception {
        String userToken = login("forum-user", "forum-pass");
        String adminToken = login("forum-admin", "forum-admin-pass");
        ForumCategoryDto category = firstCategory();
        ForumPostDetailDto post = createPost(userToken, "Locked Topic", "锁定测试", "QUESTION", category.getId(), List.of());

        mockMvc.perform(post("/api/admin/forum/posts/" + post.getId() + "/lock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/forum/posts/" + post.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest("Locked Edit", "x", "QUESTION", category.getId(), List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("锁定")));

        mockMvc.perform(post("/api/forum/posts/" + post.getId() + "/replies")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest("锁定后回复"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("锁定")));
    }

    @Test
    void authenticatedUserCanCreateReplyLikeFavoriteAndAcceptQuestion() throws Exception {
        String token = login("forum-user", "forum-pass");
        ForumCategoryDto category = firstCategory();

        ForumPostWriteRequest postReq = new ForumPostWriteRequest();
        postReq.setTitle("如何沉淀一个 Java Review Skill？");
        postReq.setContent("请问大家在团队里怎么定义 Review 标准？");
        postReq.setPostType("QUESTION");
        postReq.setCategoryId(category.getId());

        ForumPostDetailDto post = postJson("/api/forum/posts", postReq, token, new TypeReference<>() {});
        assertThat(post.getId()).isNotNull();
        assertThat(post.getPostType()).isEqualTo("QUESTION");

        ForumReplyWriteRequest replyReq = new ForumReplyWriteRequest();
        replyReq.setContent("先从高频缺陷清单和模板校验开始。");
        ForumReplyDto reply = postJson("/api/forum/posts/" + post.getId() + "/replies", replyReq, token, new TypeReference<>() {});
        assertThat(reply.getId()).isNotNull();

        mockMvc.perform(post("/api/forum/posts/" + post.getId() + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/forum/posts/" + post.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/forum/posts/" + post.getId() + "/accept/" + reply.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        ForumPostDetailDto detail = getJson("/api/forum/posts/" + post.getId(), new TypeReference<>() {});
        assertThat(detail.getLikeCount()).isEqualTo(1);
        assertThat(detail.getFavoriteCount()).isEqualTo(1);
        assertThat(detail.getReplyCount()).isEqualTo(1);
        assertThat(detail.getAccepted()).isTrue();
        assertThat(detail.getReplies()).hasSize(1);
    }

    @Test
    void authenticatedUserCanUpdatePostWithEmptyTags() throws Exception {
        String token = login("forum-user", "forum-pass");
        ForumCategoryDto category = firstCategory();

        ForumPostWriteRequest createReq = new ForumPostWriteRequest();
        createReq.setTitle("如何把讨论沉淀成 Skill？");
        createReq.setContent("先记录上下文，再提炼复用步骤。");
        createReq.setPostType("DISCUSSION");
        createReq.setCategoryId(category.getId());

        ForumPostDetailDto post = postJson("/api/forum/posts", createReq, token, new TypeReference<>() {});

        ForumPostWriteRequest updateReq = new ForumPostWriteRequest();
        updateReq.setTitle("如何把论坛的问题整理成可复用 Skill？");
        updateReq.setContent("我想把同事在论坛里的讨论沉淀成 Skill / Rule。");
        updateReq.setPostType("DISCUSSION");
        updateReq.setCategoryId(category.getId());
        updateReq.setTagIds(List.of());

        ForumPostDetailDto updated = putJson("/api/forum/posts/" + post.getId(), updateReq, token, new TypeReference<>() {});

        assertThat(updated.getId()).isEqualTo(post.getId());
        assertThat(updated.getCategoryId()).isEqualTo(category.getId());
        assertThat(updated.getTags()).isEmpty();
    }

    private ForumCategoryDto firstCategory() throws Exception {
        List<ForumCategoryDto> categories = getJson("/api/forum/categories", new TypeReference<>() {});
        return categories.get(0);
    }

    private Tag createTag(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return tagRepository.save(tag);
                });
    }

    private ForumPostWriteRequest postRequest(String title, String content, String postType, Long categoryId, List<Long> tagIds) {
        ForumPostWriteRequest req = new ForumPostWriteRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setPostType(postType);
        req.setCategoryId(categoryId);
        req.setTagIds(tagIds);
        return req;
    }

    private ForumReplyWriteRequest replyRequest(String content) {
        ForumReplyWriteRequest req = new ForumReplyWriteRequest();
        req.setContent(content);
        return req;
    }

    private ForumPostDetailDto createPost(String token, String title, String content, String postType, Long categoryId, List<Long> tagIds) throws Exception {
        ForumPostWriteRequest req = postRequest(title, content, postType, categoryId, tagIds);
        return postJson("/api/forum/posts", req, token, new TypeReference<>() {});
    }

    private ForumReplyDto replyToPost(String token, Long postId, String content) throws Exception {
        ForumReplyWriteRequest req = replyRequest(content);
        return postJson("/api/forum/posts/" + postId + "/replies", req, token, new TypeReference<>() {});
    }

    private String login(String username, String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("token").asText();
    }

    private <T> T getJson(String url, TypeReference<T> type) throws Exception {
        String json = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        return objectMapper.convertValue(root.path("data"), type);
    }

    private <T> T postJson(String url, Object body, String token, TypeReference<T> type) throws Exception {
        String json = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        return objectMapper.convertValue(root.path("data"), type);
    }

    private <T> T putJson(String url, Object body, String token, TypeReference<T> type) throws Exception {
        String json = mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        return objectMapper.convertValue(root.path("data"), type);
    }
}
