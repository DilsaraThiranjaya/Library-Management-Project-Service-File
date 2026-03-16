package lk.ijse.eca.fileservice.controller;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final Storage storage;

    @Value("${gcp.bucket-id}")
    private String bucketName;

    // ==================== UPLOAD ====================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String generatedFileName = UUID.randomUUID().toString() + extension;

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, generatedFileName)
                    .setContentType(file.getContentType())
                    .setMetadata(Map.of("originalFilename", originalFileName != null ? originalFileName : "unknown"))
                    .build();

            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, generatedFileName);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "filename", generatedFileName,
                    "originalFilename", originalFileName != null ? originalFileName : "unknown",
                    "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    "size", file.getSize(),
                    "publicUrl", publicUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload to GCS: " + e.getMessage()));
        }
    }

    // ==================== LIST ALL FILES ====================
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        try {
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (Blob blob : storage.list(bucketName).iterateAll()) {
                Map<String, Object> fileInfo = new LinkedHashMap<>();
                fileInfo.put("filename", blob.getName());
                fileInfo.put("contentType", blob.getContentType());
                fileInfo.put("size", blob.getSize());
                fileInfo.put("publicUrl", String.format("https://storage.googleapis.com/%s/%s", bucketName, blob.getName()));

                // Get original filename from metadata if available
                Map<String, String> metadata = blob.getMetadata();
                if (metadata != null && metadata.containsKey("originalFilename")) {
                    fileInfo.put("originalFilename", metadata.get("originalFilename"));
                } else {
                    fileInfo.put("originalFilename", blob.getName());
                }

                fileList.add(fileInfo);
            }
            return ResponseEntity.ok(fileList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list files from GCS: " + e.getMessage(), e);
        }
    }

    // ==================== DOWNLOAD / GET SINGLE FILE ====================
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            Blob blob = storage.get(BlobId.of(bucketName, filename));
            if (blob == null || !blob.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = blob.getContent();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    blob.getContentType() != null ? blob.getContentType() : "application/octet-stream"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(content.length);

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== DELETE FILE ====================
    @DeleteMapping("/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            Blob blob = storage.get(BlobId.of(bucketName, filename));
            if (blob == null || !blob.exists()) {
                return ResponseEntity.notFound().build();
            }

            boolean deleted = storage.delete(BlobId.of(bucketName, filename));
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully", "filename", filename));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete file"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
}
