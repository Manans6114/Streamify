package com.stream.app.videostream.Services.Imple;

import com.stream.app.videostream.Entities.Video;
import com.stream.app.videostream.Repository.VideoRepo;
import com.stream.app.videostream.Services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class VideoServiceImple implements VideoService {

    @Value("${files.video}")
    String DIR;

    private final VideoRepo videoRepo;

    public VideoServiceImple(VideoRepo videoRepo) {
        this.videoRepo = videoRepo;
    }

    @PostConstruct
    public void init() {
        File file = new File(DIR);
        if (!file.exists()) {
            file.mkdir();
            System.out.println("Folder created");
        } else {
            System.out.println("Folder already exists");
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            String cleanFileName = StringUtils.cleanPath(filename);
            String cleanFolder = StringUtils.cleanPath(DIR);
            Path path = Paths.get(cleanFolder, cleanFileName);

            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            video.setContentType(contentType);
            video.setFilePath(path.toString());

            return videoRepo.save(video);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Video get(String videoId) {
        return videoRepo.findById(videoId).orElseThrow(() -> new RuntimeException("VIDEO NOT FOUND"));
    }

    @Override
    public Video getByTitle(String title) {
        return null; // You can implement this as needed
    }

    @Override
    public List<Video> getAll() {
        return videoRepo.findAll();
    }

    @Override
    public boolean deleteByVideoId(String videoId) {
        Optional<Video> optionalVideo = videoRepo.findById(videoId);
        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();

            try {
                Path path = Paths.get(video.getFilePath());
                Files.deleteIfExists(path);
                videoRepo.delete(video);
//                return true;
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            }

        } else {
            return false;
        }
    }
}
