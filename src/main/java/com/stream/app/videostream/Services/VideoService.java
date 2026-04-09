package com.stream.app.videostream.Services;

import com.stream.app.videostream.Entities.Video;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

    // Save video
    Video save(Video video, MultipartFile file);

    // Get video by ID
    Video get(String videoId);

    // Get video by title
    Video getByTitle(String title);

    // Get all videos
    List<Video> getAll();

    // Delete video by videoId
    boolean deleteByVideoId(String videoId); // ✅ for deleting video
}
