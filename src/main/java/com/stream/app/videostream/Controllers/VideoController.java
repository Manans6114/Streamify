package com.stream.app.videostream.Controllers;

import com.stream.app.videostream.Entities.Video;
import com.stream.app.videostream.Services.VideoService;
import com.stream.app.videostream.playload.CustomMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling video-related operations
 */
@RestController
@RequestMapping("api/v1/videos")
@CrossOrigin(origins = "http://localhost:5173") // ✅ CORS fixed for React
public class VideoController {

    private final VideoService videoservice;

    public VideoController(VideoService videoService) {
        this.videoservice = videoService;
    }

    /**
     * Upload a new video with metadata
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file") MultipartFile videoFile,
            @RequestParam("title") String title,
            @RequestParam("description") String description
    ) {
        // 🪵 Logging the incoming request data
        System.out.println("📥 Incoming POST /api/v1/videos request");
        System.out.println("Video title: " + title);
        System.out.println("Video description: " + description);
        System.out.println("Video file name: " + videoFile.getOriginalFilename());

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        Video savedVideo = videoservice.save(video, videoFile);

        if (savedVideo != null) {
            return ResponseEntity.status(HttpStatus.OK).body(savedVideo);
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage.builder()
                            .message("Something went wrong. Video not uploaded.")
                            .success(false)
                            .build());
        }
    }

    /**
     * Get all videos
     */
    @GetMapping
    public List<Video> getAll() {
        return videoservice.getAll();
    }

    /**
     * Delete a video by its ID
     */
    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> delete(@PathVariable String videoId) {
        boolean deleted = videoservice.deleteByVideoId(videoId);

        if (deleted) {
            return ResponseEntity.ok(
                    CustomMessage.builder()
                            .message("Video deleted successfully.")
                            .success(true)
                            .build()
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomMessage.builder()
                            .message("Video not found or could not be deleted.")
                            .success(false)
                            .build());
        }
    }

    /**
     * Stream a complete video file
     */
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId) {
        Video video = videoservice.get(videoId);
        String contentType = video.getContentType();
        String filePath = video.getFilePath();
        Resource resource = new FileSystemResource(filePath);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Accept-Ranges", "bytes") // Added for browser compatibility
                .body(resource);
    }

    /**
     * Get JSON metadata about video range instead of streaming actual bytes
     * Useful for debugging and testing range requests
     */
    @GetMapping(value = "/stream/range/{videoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> streamVideoRange(
            @PathVariable String videoId,
            @RequestHeader(value = "Range", required = false) String range
    ) {
        // Simulate Hibernate query log
        System.out.println("Hibernate: select v1_0.video_id,v1_0.content_type,v1_0.description,v1_0.file_path,v1_0.title from yt_videos v1_0 where v1_0.video_id = " + videoId);

        // Retrieve the Video entity
        Video video = videoservice.get(videoId);
        Path path = Paths.get(video.getFilePath());

        // Determine the content type
        String contentType = video.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        long fileLength = path.toFile().length();

        long rangeStart = 0;
        long rangeEnd = fileLength - 1;

        if (range != null) {
            String[] parts = range.replace("bytes=", "").split("-");
            try {
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    rangeStart = Long.parseLong(parts[0]);
                }
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    rangeEnd = Long.parseLong(parts[1]);
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .body(Map.of("error", "Invalid Range header"));
            }
            if (rangeStart > rangeEnd || rangeStart >= fileLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .body(Map.of("error", "Range not satisfiable"));
            }
            if (rangeEnd >= fileLength) {
                rangeEnd = fileLength - 1;
            }
        }

        long contentLength = rangeEnd - rangeStart + 1;

        // Log info for debugging
        System.out.println("range start : " + rangeStart);
        System.out.println("range end : " + rangeEnd);
        System.out.println("read(number of bytes) : " + contentLength);
        System.out.println("bytes=" + rangeStart + "-" + rangeEnd);


        // Return all video fields in JSON response
        return ResponseEntity.ok(Map.of(
                "videoId", video.getVideoId(),
                "title", video.getTitle(),
                "description", video.getDescription(),
                "filePath", video.getFilePath(),
                "contentType", contentType,
                "fileLength", fileLength,
                "rangeStart", rangeStart,
                "rangeEnd", rangeEnd,
                "contentLength", contentLength,
                "message", "This endpoint returns all video fields and range info."
        ));
    }

}
