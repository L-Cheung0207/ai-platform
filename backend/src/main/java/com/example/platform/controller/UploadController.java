package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 管理端上传接口，用于 AI 知识库富文本中的图片等。
 * 上传文件保存至 app.upload.dir/articles/yyyyMMdd/ 下，返回可访问的 URL。
 */
@RestController
@RequestMapping("/api/admin")
public class UploadController {

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.dir:./upload}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(400, "请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return ApiResponse.fail(400, "仅支持图片：JPEG、PNG、GIF、WebP");
        }
        if (file.getSize() > MAX_SIZE) {
            return ApiResponse.fail(400, "文件大小不能超过 5MB");
        }
        String ext = getExtension(file.getOriginalFilename(), contentType);
        String subdir = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, "articles", subdir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.write(target, file.getBytes());
            String url = baseUrl + "/articles/" + subdir + "/" + filename;
            return ApiResponse.ok(new UploadResult(url));
        } catch (Exception e) {
            return ApiResponse.fail(500, "上传失败：" + e.getMessage());
        }
    }

    private static String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            int i = originalFilename.lastIndexOf('.');
            return originalFilename.substring(i).toLowerCase();
        }
        if ("image/png".equals(contentType)) return ".png";
        if ("image/gif".equals(contentType)) return ".gif";
        if ("image/webp".equals(contentType)) return ".webp";
        return ".jpg";
    }

    public record UploadResult(String url) {}
}
