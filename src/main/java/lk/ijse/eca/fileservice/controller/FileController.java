package lk.ijse.eca.fileservice.controller;

import lk.ijse.eca.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ==================== UPLOAD ====================
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            Map<String, Object> result = fileService.uploadFile(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
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
            return ResponseEntity.ok(fileService.listFiles());
        } catch (Exception e) {
            throw new RuntimeException("Failed to list files from GCS: " + e.getMessage(), e);
        }
    }

    // ==================== DOWNLOAD / GET SINGLE FILE ====================
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            byte[] content = fileService.downloadFile(filename);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = fileService.getContentType(filename);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    contentType != null ? contentType : "application/octet-stream"));
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
            boolean deleted = fileService.deleteFile(filename);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully", "filename", filename));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Failed to delete file, or file not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
}
