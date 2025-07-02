/**
 * Parallel File Upload/Download System
 * Client-side JavaScript for handling parallel file chunks
 */

document.addEventListener('DOMContentLoaded', function() {
    const dropArea = document.getElementById('drop-area');
    const fileUpload = document.getElementById('file-upload');
    const selectFileBtn = document.getElementById('select-file');
    const uploadStatus = document.getElementById('upload-status');
    const progressBar = uploadStatus.querySelector('.progress-bar');
    const currentFileName = document.getElementById('current-file-name');
    const uploadedChunksEl = document.getElementById('uploaded-chunks');
    const totalChunksEl = document.getElementById('total-chunks');
    const uploadSpeedEl = document.getElementById('upload-speed');

    // Maximum concurrent uploads
    const MAX_CONCURRENT_UPLOADS = navigator.hardwareConcurrency || 4;
    
    // Queue to manage file uploads
    let uploadQueue = [];
    let currentUploads = 0;
    let isUploading = false;

    // Set up event listeners
    selectFileBtn.addEventListener('click', () => fileUpload.click());
    fileUpload.addEventListener('change', handleFiles);
    
    // Drag and drop event listeners
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, preventDefaults, false);
    });
    
    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    
    ['dragenter', 'dragover'].forEach(eventName => {
        dropArea.addEventListener(eventName, highlight, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, unhighlight, false);
    });
    
    function highlight() {
        dropArea.classList.add('bg-light');
    }
    
    function unhighlight() {
        dropArea.classList.remove('bg-light');
    }
    
    dropArea.addEventListener('drop', handleDrop, false);
    
    function handleDrop(e) {
        const dt = e.dataTransfer;
        const files = dt.files;
        handleFiles(files);
    }
    
    function handleFiles(e) {
        const files = e.target?.files || e;
        
        for (let i = 0; i < files.length; i++) {
            uploadQueue.push(files[i]);
        }
        
        if (!isUploading) {
            processQueue();
        }
    }
    
    function processQueue() {
        if (uploadQueue.length === 0) {
            isUploading = false;
            uploadStatus.classList.add('d-none');
            return;
        }
        
        isUploading = true;
        uploadStatus.classList.remove('d-none');
        
        while (currentUploads < MAX_CONCURRENT_UPLOADS && uploadQueue.length > 0) {
            const file = uploadQueue.shift();
            uploadFile(file);
            currentUploads++;
        }
    }
    
    // Delete file event listeners
    document.querySelectorAll('.delete-file').forEach(button => {
        button.addEventListener('click', function() {
            const fileId = this.getAttribute('data-file-id');
            deleteFile(fileId);
        });
    });

    async function initializeUpload(file) {
        try {
            const response = await fetch('/api/files/init', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    'filename': file.name,
                    'contentType': file.type,
                    'fileSize': file.size
                })
            });
            
            if (!response.ok) {
                throw new Error('Failed to initialize upload');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error initializing upload:', error);
            return null;
        }
    }
    
    async function uploadFile(file) {
        const initData = await initializeUpload(file);
        
        if (!initData) {
            currentUploads--;
            processQueue();
            return;
        }
        
        const { fileId, totalChunks, chunkSize } = initData;
        
        currentFileName.textContent = file.name;
        totalChunksEl.textContent = totalChunks;
        uploadedChunksEl.textContent = '0';
        progressBar.style.width = '0%';
        progressBar.textContent = '0%';
        
        const uploadStartTime = Date.now();
        let uploadedBytes = 0;
        let uploadedChunks = 0;
        
        // Create promises for each chunk upload
        const uploadPromises = [];
        
        for (let chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
            const start = chunkNumber * chunkSize;
            const end = Math.min(start + chunkSize, file.size);
            const chunk = file.slice(start, end);
            
            // Create a promise for this chunk upload
            const uploadPromise = uploadChunk(fileId, chunkNumber, chunk)
                .then(result => {
                    uploadedChunks++;
                    uploadedBytes += chunk.size;
                    
                    // Update progress
                    const progress = Math.round((uploadedChunks / totalChunks) * 100);
                    progressBar.style.width = `${progress}%`;
                    progressBar.textContent = `${progress}%`;
                    uploadedChunksEl.textContent = uploadedChunks;
                    
                    // Calculate and display upload speed
                    const uploadTimeInSeconds = (Date.now() - uploadStartTime) / 1000;
                    const speedInKBps = Math.round((uploadedBytes / uploadTimeInSeconds) / 1024);
                    uploadSpeedEl.textContent = `${speedInKBps} KB/s`;
                    
                    return result;
                });
                
            uploadPromises.push(uploadPromise);
        }
        
        // Wait for all chunks to complete
        Promise.all(uploadPromises)
            .then(() => {
                console.log(`File ${file.name} uploaded successfully!`);
                // Reload the page to show the new file in the list
                window.location.reload();
            })
            .catch(error => {
                console.error(`Error uploading file ${file.name}:`, error);
                alert(`Failed to upload ${file.name}. Please try again.`);
            })
            .finally(() => {
                currentUploads--;
                processQueue();
            });
    }
    
    async function uploadChunk(fileId, chunkNumber, chunk) {
        const formData = new FormData();
        formData.append('chunk', chunk);
        
        try {
            const response = await fetch(`/api/files/${fileId}/chunk/${chunkNumber}`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`Failed to upload chunk ${chunkNumber}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error(`Error uploading chunk ${chunkNumber}:`, error);
            throw error;
        }
    }
    
    async function deleteFile(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) {
            return;
        }
        
        try {
            const response = await fetch(`/api/files/${fileId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                // Reload the page to update the file list
                window.location.reload();
            } else {
                alert('Failed to delete file. Please try again.');
            }
        } catch (error) {
            console.error('Error deleting file:', error);
            alert('Failed to delete file. Please try again.');
        }
    }
    
    // Parallel download functionality
    class ParallelDownloader {
        constructor(fileId, chunkSize, totalChunks) {
            this.fileId = fileId;
            this.chunkSize = chunkSize;
            this.totalChunks = totalChunks;
            this.chunks = new Array(totalChunks);
            this.downloadedChunks = 0;
        }
        
        async downloadChunk(chunkNumber) {
            try {
                const response = await fetch(`/api/files/${this.fileId}/chunk/${chunkNumber}`);
                
                if (!response.ok) {
                    throw new Error(`Failed to download chunk ${chunkNumber}`);
                }
                
                const blob = await response.blob();
                this.chunks[chunkNumber] = blob;
                this.downloadedChunks++;
                
                return blob;
            } catch (error) {
                console.error(`Error downloading chunk ${chunkNumber}:`, error);
                throw error;
            }
        }
        
        async downloadFile(fileName) {
            const downloadPromises = [];
            
            for (let i = 0; i < this.totalChunks; i++) {
                downloadPromises.push(this.downloadChunk(i));
            }
            
            try {
                await Promise.all(downloadPromises);
                
                // Combine all chunks into a single blob
                const completeFile = new Blob(this.chunks);
                
                // Create a download link
                const downloadUrl = URL.createObjectURL(completeFile);
                const a = document.createElement('a');
                a.href = downloadUrl;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(downloadUrl);
            } catch (error) {
                console.error('Error downloading file:', error);
                alert('Failed to download file. Please try again.');
            }
        }
    }
});
