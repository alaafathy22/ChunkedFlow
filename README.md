# ChunkedFlow

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-11-red)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.14-green)](https://spring.io/projects/spring-boot)

A high-performance parallel file upload/download system built with Spring Boot that efficiently handles large files through chunked transfers.

## 🚀 Features

- **Chunked File Uploads**: Break large files into smaller chunks for reliable transfers
- **Parallel Processing**: Upload multiple chunks simultaneously for improved performance
- **Resume Capability**: Pause and resume uploads without losing progress
- **Progress Tracking**: Real-time monitoring of upload/download status
- **RESTful API**: Well-documented endpoints for easy integration
- **Web Interface**: User-friendly interface for file operations

## 📋 Requirements

- Java 11 or higher
- Maven 3.6+
- Storage space for uploaded files

## 🛠️ Installation

### Using Docker

```bash
# Build the Docker image
docker build -t chunkedflow .

# Run the container
docker run -p 8080:8080 chunkedflow
```

### Manual Setup

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/ChunkedFlow.git
   cd ChunkedFlow
   ```

2. Build the application
   ```bash
   mvn clean package
   ```

3. Run the application
   ```bash
   java -jar target/parallel-file-uploader-0.0.1-SNAPSHOT.jar
   ```

4. The application will be available at http://localhost:8080

## 🔧 Configuration

The application can be configured through the `application.properties` file:

```properties
# Set the maximum file size (default: 1GB)
spring.servlet.multipart.max-file-size=1GB

# Set the maximum request size (default: 1GB)
spring.servlet.multipart.max-request-size=1GB

# Set chunk size (default: 1MB = 1048576 bytes)
file.chunk-size=1048576
```

## 🔌 API Reference

### Initialize Upload

```
POST /api/files/init
```

Parameters:
- `filename`: The original filename
- `contentType`: MIME type of the file
- `fileSize`: Total size of the file in bytes

Returns:
- `fileId`: Unique identifier for the file
- `totalChunks`: Number of chunks required
- `chunkSize`: Size of each chunk in bytes

### Upload Chunk

```
POST /api/files/{fileId}/chunk/{chunkNumber}
```

Parameters:
- `fileId`: ID of the file
- `chunkNumber`: Index of the chunk (0-based)
- `chunk`: The binary data for this chunk

### Download File

```
GET /api/files/{fileId}/download
```

Parameters:
- `fileId`: ID of the file

### List All Files

```
GET /api/files
```

## 💻 Usage Example

```javascript
// Example using fetch API
async function uploadLargeFile(file) {
  // Initialize upload
  const initResponse = await fetch('/api/files/init', {
    method: 'POST',
    body: new URLSearchParams({
      filename: file.name,
      contentType: file.type,
      fileSize: file.size
    })
  });
  
  const { fileId, totalChunks, chunkSize } = await initResponse.json();
  
  // Upload chunks in parallel
  const uploadPromises = [];
  
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    const chunk = file.slice(start, end);
    
    const formData = new FormData();
    formData.append('chunk', chunk);
    
    uploadPromises.push(
      fetch(`/api/files/${fileId}/chunk/${i}`, {
        method: 'POST',
        body: formData
      })
    );
  }
  
  await Promise.all(uploadPromises);
  console.log('Upload complete!');
}
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot) - The web framework used
- [H2 Database](https://www.h2database.com/) - In-memory database for development
- [Project Lombok](https://projectlombok.org/) - For reducing boilerplate code
