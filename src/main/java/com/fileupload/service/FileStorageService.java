package com.fileupload.service;

import com.fileupload.exception.FileStorageException;
import com.fileupload.model.FileChunk;
import com.fileupload.model.FileMetadata;
import com.fileupload.repository.FileChunkRepository;
import com.fileupload.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Value("${file.chunk-size:1048576}") // Default 1MB
    private int chunkSize;
    
    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    
    @Autowired
    private FileChunkRepository fileChunkRepository;
    
    private ExecutorService executorService;
    
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path chunksPath = Paths.get(uploadDir + "/chunks");
            if (!Files.exists(chunksPath)) {
                Files.createDirectories(chunksPath);
            }
            
            // Initialize thread pool with number of available processors
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directories", e);
        }
    }
    
    public FileMetadata initializeUpload(String filename, String contentType, long fileSize) {
        try {
            // Calculate total chunks
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
            
            // Create unique filename to prevent collisions
            String uniqueFilename = UUID.randomUUID() + "_" + filename;
            
            // Create file metadata
            FileMetadata metadata = new FileMetadata();
            metadata.setFilename(uniqueFilename);
            metadata.setOriginalFilename(filename);
            metadata.setContentType(contentType);
            metadata.setSize(fileSize);
            metadata.setTotalChunks(totalChunks);
            metadata.setFilePath(uploadDir + "/" + uniqueFilename);
            
            FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
            
            // Initialize file chunks
            for (int i = 0; i < totalChunks; i++) {
                FileChunk chunk = new FileChunk();
                chunk.setFile(savedMetadata);
                chunk.setChunkNumber(i);
                chunk.setChunkPath(uploadDir + "/chunks/" + savedMetadata.getId() + "_" + i);
                chunk.setUploaded(false);
                fileChunkRepository.save(chunk);
            }
            
            return savedMetadata;
        } catch (Exception e) {
            throw new FileStorageException("Failed to initialize upload for " + filename, e);
        }
    }
    
    public void uploadChunk(Long fileId, int chunkNumber, MultipartFile chunkData) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileStorageException("File not found with id " + fileId));
                
            FileChunk chunk = fileChunkRepository.findByFileAndChunkNumber(metadata, chunkNumber);
            if (chunk == null) {
                throw new FileStorageException("Chunk not found: " + chunkNumber + " for file " + fileId);
            }
            
            // Save chunk to disk
            Files.write(Paths.get(chunk.getChunkPath()), chunkData.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
            // Update chunk status
            chunk.setSize(chunkData.getSize());
            chunk.setUploaded(true);
            fileChunkRepository.save(chunk);
            
            // Check if all chunks are uploaded
            long uploadedChunks = fileChunkRepository.countByFileAndUploaded(metadata, true);
            metadata.setUploadedChunks((int) uploadedChunks);
            
            if (uploadedChunks == metadata.getTotalChunks()) {
                // All chunks uploaded, start async merge
                metadata.setCompleted(true);
                CompletableFuture.runAsync(() -> mergeChunks(metadata), executorService);
            }
            
            fileMetadataRepository.save(metadata);
            
        } catch (IOException e) {
            throw new FileStorageException("Could not store chunk " + chunkNumber, e);
        }
    }
    
    public void mergeChunks(FileMetadata metadata) {
        try {
            List<FileChunk> chunks = fileChunkRepository.findByFileOrderByChunkNumber(metadata);
            Path outputPath = Paths.get(metadata.getFilePath());
            
            try (OutputStream outputStream = Files.newOutputStream(outputPath, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                
                for (FileChunk chunk : chunks) {
                    File chunkFile = new File(chunk.getChunkPath());
                    try (InputStream inputStream = new FileInputStream(chunkFile)) {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    // Optionally delete chunk file after merging
                    // chunkFile.delete();
                }
            }
            
        } catch (IOException e) {
            throw new FileStorageException("Failed to merge chunks for file " + metadata.getId(), e);
        }
    }
    
    public byte[] downloadChunk(Long fileId, int chunkNumber, int chunkSize) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileStorageException("File not found with id " + fileId));
                
            if (!metadata.isCompleted()) {
                throw new FileStorageException("File is not ready for download");
            }
            
            File file = new File(metadata.getFilePath());
            if (!file.exists()) {
                throw new FileStorageException("File not found on disk");
            }
            
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                long startPosition = (long) chunkNumber * chunkSize;
                
                // Make sure we don't read beyond the file size
                int actualChunkSize = (int) Math.min(chunkSize, file.length() - startPosition);
                if (actualChunkSize <= 0) {
                    return new byte[0]; // End of file reached
                }
                
                byte[] buffer = new byte[actualChunkSize];
                randomAccessFile.seek(startPosition);
                randomAccessFile.readFully(buffer);
                
                return buffer;
            }
            
        } catch (IOException e) {
            throw new FileStorageException("Could not download chunk " + chunkNumber, e);
        }
    }
    
    public FileMetadata getFileMetadata(Long fileId) {
        return fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new FileStorageException("File not found with id " + fileId));
    }
    
    public List<FileMetadata> getAllFiles() {
        return fileMetadataRepository.findAll();
    }
    
    public void deleteFile(Long fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new FileStorageException("File not found with id " + fileId));
            
        // Delete file chunks
        List<FileChunk> chunks = fileChunkRepository.findByFileOrderByChunkNumber(metadata);
        for (FileChunk chunk : chunks) {
            try {
                Files.deleteIfExists(Paths.get(chunk.getChunkPath()));
            } catch (IOException e) {
                // Log but continue with deletion
                System.err.println("Failed to delete chunk file: " + chunk.getChunkPath());
            }
            fileChunkRepository.delete(chunk);
        }
        
        // Delete the actual file
        try {
            Files.deleteIfExists(Paths.get(metadata.getFilePath()));
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + metadata.getFilePath());
        }
        
        // Delete metadata
        fileMetadataRepository.delete(metadata);
    }
}
