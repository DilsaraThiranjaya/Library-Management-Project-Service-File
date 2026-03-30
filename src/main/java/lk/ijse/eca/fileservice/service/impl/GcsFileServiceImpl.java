package lk.ijse.eca.fileservice.service.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lk.ijse.eca.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GcsFileServiceImpl implements FileService {

    private final Storage storage;

    @Value("${gcp.bucket-id}")
    private String bucketName;

    @Value("${file.service.base-url:http://localhost:8080/api/files}")
    private String baseUrl;

    @Override
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {
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

        String publicUrl = String.format("%s/%s", baseUrl, generatedFileName);

        return Map.of(
                "filename", generatedFileName,
                "originalFilename", originalFileName != null ? originalFileName : "unknown",
                "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                "size", file.getSize(),
                "publicUrl", publicUrl
        );
    }

    @Override
    public List<Map<String, Object>> listFiles() throws Exception {
        List<Map<String, Object>> fileList = new ArrayList<>();
        for (Blob blob : storage.list(bucketName).iterateAll()) {
            Map<String, Object> fileInfo = new LinkedHashMap<>();
            fileInfo.put("filename", blob.getName());
            fileInfo.put("contentType", blob.getContentType());
            fileInfo.put("size", blob.getSize());
            fileInfo.put("publicUrl", String.format("%s/%s", baseUrl, blob.getName()));

            // Get original filename from metadata if available
            Map<String, String> metadata = blob.getMetadata();
            if (metadata != null && metadata.containsKey("originalFilename")) {
                fileInfo.put("originalFilename", metadata.get("originalFilename"));
            } else {
                fileInfo.put("originalFilename", blob.getName());
            }

            fileList.add(fileInfo);
        }
        return fileList;
    }

    @Override
    public byte[] downloadFile(String filename) throws Exception {
        Blob blob = storage.get(BlobId.of(bucketName, filename));
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getContent();
    }

    @Override
    public String getContentType(String filename) throws Exception {
        Blob blob = storage.get(BlobId.of(bucketName, filename));
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getContentType();
    }

    @Override
    public boolean deleteFile(String filename) throws Exception {
        Blob blob = storage.get(BlobId.of(bucketName, filename));
        if (blob == null || !blob.exists()) {
            return false;
        }
        return storage.delete(BlobId.of(bucketName, filename));
    }
}
