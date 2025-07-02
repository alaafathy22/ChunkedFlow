package com.fileupload.controller;

import com.fileupload.model.FileMetadata;
import com.fileupload.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;
    
    @Value("${file.chunk-size:1048576}")
    private int chunkSize;
    
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initializeUpload(
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType,
            @RequestParam("fileSize") long fileSize) {
        
        FileMetadata metadata = fileStorageService.initializeUpload(filename, contentType, fileSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", metadata.getId());
        response.put("totalChunks", metadata.getTotalChunks());
        response.put("chunkSize", chunkSize);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{fileId}/chunk/{chunkNumber}")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @PathVariable Long fileId,
            @PathVariable int chunkNumber,
            @RequestParam("chunk") MultipartFile chunk) {
        
        fileStorageService.uploadChunk(fileId, chunkNumber, chunk);
        
        FileMetadata metadata = fileStorageService.getFileMetadata(fileId);
        Map<String, Object> response = new HashMap<>();
        response.put("chunkNumber", chunkNumber);
        response.put("uploaded", true);
        response.put("completed", metadata.isCompleted());
        response.put("uploadedChunks", metadata.getUploadedChunks());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<FileMetadata>> getAllFiles() {
        List<FileMetadata> files = fileStorageService.getAllFiles();
        return ResponseEntity.ok(files);
    }
    
    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable Long fileId) {
        FileMetadata metadata = fileStorageService.getFileMetadata(fileId);
        return ResponseEntity.ok(metadata);
    }
    
    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        FileMetadata metadata = fileStorageService.getFileMetadata(fileId);
        
        if (!metadata.isCompleted()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Get the first chunk to start the download
        byte[] data = fileStorageService.downloadChunk(fileId, 0, chunkSize);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
        headers.setContentDispositionFormData("attachment", metadata.getOriginalFilename());
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }
    
    @GetMapping("/{fileId}/chunk/{chunkNumber}")
    public ResponseEntity<byte[]> downloadChunk(
            @PathVariable Long fileId,
            @PathVariable int chunkNumber) {
        
        FileMetadata metadata = fileStorageService.getFileMetadata(fileId);
        
        if (!metadata.isCompleted()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        byte[] data = fileStorageService.downloadChunk(fileId, chunkNumber, chunkSize);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }
    
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long fileId) {
        fileStorageService.deleteFile(fileId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", fileId);
        response.put("deleted", true);
        
        return ResponseEntity.ok(response);
    }
}
