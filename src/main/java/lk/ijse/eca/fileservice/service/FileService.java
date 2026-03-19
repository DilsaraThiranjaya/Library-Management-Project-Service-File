package lk.ijse.eca.fileservice.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FileService {
    Map<String, Object> uploadFile(MultipartFile file) throws IOException;
    List<Map<String, Object>> listFiles() throws Exception;
    byte[] downloadFile(String filename) throws Exception;
    String getContentType(String filename) throws Exception;
    boolean deleteFile(String filename) throws Exception;
}
