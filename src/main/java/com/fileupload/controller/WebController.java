package com.fileupload.controller;

import com.fileupload.model.FileMetadata;
import com.fileupload.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/")
    public String home(Model model) {
        List<FileMetadata> files = fileStorageService.getAllFiles();
        model.addAttribute("files", files);
        return "index";
    }
}
