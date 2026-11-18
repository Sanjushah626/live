package com.myshareapp.service;

import com.myshareapp.model.News;
import com.myshareapp.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsService {
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final NewsRepository newsRepo;

    @Value("${app.upload.news-dir:uploads/news}")
    private String uploadDir;

    public NewsService(NewsRepository newsRepo) {
        this.newsRepo = newsRepo;
    }

    public News createNews(Integer userId, String title, String description, MultipartFile file) throws IOException {
        News n = new News();
        n.setUserId(userId);
        n.setTitle(title.trim());
        n.setDescription(description.trim());
        if (file != null && !file.isEmpty()) {
            n.setImage(storeFile(file));
        }
        return newsRepo.save(n);
    }

    public News updateNews(Integer id, String title, String description, MultipartFile file) throws IOException {
        News n = newsRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("News not found."));
        n.setTitle(title.trim());
        n.setDescription(description.trim());
        if (file != null && !file.isEmpty()) {
            if (n.getImage() != null) deleteFile(n.getImage());
            n.setImage(storeFile(file));
        }
        return newsRepo.save(n);
    }

    public void deleteNews(Integer id) {
        newsRepo.findById(id).ifPresent(n -> {
            if (n.getImage() != null) deleteFile(n.getImage());
            newsRepo.delete(n);
        });
    }

    private String storeFile(MultipartFile file) throws IOException {
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) throw new IllegalArgumentException("Only JPG, PNG, GIF and WEBP images are allowed.");
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = "news_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "." + ext;
        Files.copy(file.getInputStream(), dir.resolve(filename));
        return filename;
    }

    private void deleteFile(String filename) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename));
        } catch (IOException ignored) {
        }
    }
}
