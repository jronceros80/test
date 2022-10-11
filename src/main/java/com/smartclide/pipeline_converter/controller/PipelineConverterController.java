package com.smartclide.pipeline_converter.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.smartclide.pipeline_converter.service.PipelineDescriptorConversionService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineConverterController {
		
	private final PipelineDescriptorConversionService conversionService;
	
	@CrossOrigin("*")
	@PostMapping("/gitlab")
    public ResponseEntity<Resource> uploadFile(
			@RequestParam("file") MultipartFile file) {
		try {
			Resource converted = conversionService.convertFileToGitLab(file.getInputStream());
			return ResponseEntity.ok()
			        .contentType(MediaType.parseMediaType("application/octet-stream"))
			        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + converted.getFilename() + "\"")
			        .body(converted);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "There was a problem while converting: ", e);
		}
	}

	@CrossOrigin("*")
	@PostMapping("/jenkins")
	public ResponseEntity<Resource> parseFileToJenkins(
			@RequestParam("file") MultipartFile file) {
		try {
			Resource converted = conversionService.convertFileToJenkins(file);
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("application/octet-stream"))
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + converted.getFilename() + "\"")
					.body(converted);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "There was a problem while converting: ", e);
		}
	}
}
