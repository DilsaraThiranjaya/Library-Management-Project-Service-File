package lk.ijse.eca.fileservice.controller;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final Storage storage;

    @Value("${app.gcs-bucket}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
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
                    .build();
            
            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, generatedFileName);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "filename", generatedFileName,
                    "originalFilename", originalFileName,
                    "publicUrl", publicUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file due to IO issue: " + e.getMessage());
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file to Google Cloud Storage. Ensure Application Default Credentials are set. Error: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {
        try {
            List<String> fileNames = new ArrayList<>();
            for (Blob blob : storage.list(bucketName).iterateAll()) {
                fileNames.add(blob.getName());
            }
            return ResponseEntity.ok(fileNames);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list files from GCS. Ensure Application Default Credentials are set. Error: " + e.getMessage(), e);
        }
    }
}
