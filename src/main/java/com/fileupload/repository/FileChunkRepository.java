package com.fileupload.repository;

import com.fileupload.model.FileChunk;
import com.fileupload.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, Long> {
    List<FileChunk> findByFileOrderByChunkNumber(FileMetadata file);
    FileChunk findByFileAndChunkNumber(FileMetadata file, int chunkNumber);
    long countByFileAndUploaded(FileMetadata file, boolean uploaded);
}
