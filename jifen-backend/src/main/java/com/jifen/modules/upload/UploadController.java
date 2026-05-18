package com.jifen.modules.upload;

import com.jifen.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));

    @Value("${upload.dir:/mnt/j/2026/jifen/jifen-backend/uploads}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(400, "文件大小不能超过5MB");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(400, "仅支持 jpg/png/gif 格式的图片");
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            return Result.error("创建上传目录失败");
        }

        // Generate unique filename
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = uploadPath.resolve(newFilename);

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }

        // Return accessible URL - the static resource handler maps /uploads/** to the uploads dir
        String fileUrl = "/api/uploads/" + newFilename;
        return Result.success(fileUrl);
    }
}
