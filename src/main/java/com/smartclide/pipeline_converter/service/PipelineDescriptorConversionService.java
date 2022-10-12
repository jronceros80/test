package com.smartclide.pipeline_converter.service;

import java.io.*;

import com.smartclide.pipeline_converter.output.JenkinsCIOutputConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.smartclide.pipeline_converter.input.gitlab.model.Pipeline;
import com.smartclide.pipeline_converter.input.jenkins.JenkinsfileReader;
import com.smartclide.pipeline_converter.input.jenkins.Node;
import com.smartclide.pipeline_converter.output.GitlabCIOutputConverter;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class PipelineDescriptorConversionService {

	private final JenkinsfileReader reader;
	private final GitlabCIOutputConverter converterGitLab;
	private final JenkinsCIOutputConverter converterJenkins;
	
	ObjectMapper mapper;

	public Resource convertFileToGitLab(MultipartFile file){                       
    	mapper = new ObjectMapper(new YAMLFactory()
        		.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        		.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));        
        mapper.disable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature());
        mapper.setSerializationInclusion(Include.NON_NULL)
        	.setSerializationInclusion(Include.NON_EMPTY)
        	.setSerializationInclusion(Include.NON_DEFAULT);                     
        try {        	
        	Node node = reader.read(file.getInputStream());
    		Pipeline gitlabPipeline = converterGitLab.convert(node);
    		if(gitlabPipeline != null) {
    			String converted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(gitlabPipeline);
        		return new ByteArrayResource(converted.getBytes());
    		}        		
		} catch (Exception e) {
			log.debug("Formated file Jenkins incorrect [{}]", file.getOriginalFilename());
		}	                    
        return null;
	}

	public Resource convertFileToJenkins(MultipartFile multipartFile) {
		mapper = new ObjectMapper(new YAMLFactory()
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(Include.NON_NULL)
				.setSerializationInclusion(Include.NON_EMPTY)
				.setSerializationInclusion(Include.NON_DEFAULT);
		try {
			File file = convertToFile(multipartFile);
			Pipeline gitlabPipeline = mapper.readValue(file, Pipeline.class);
			com.smartclide.pipeline_converter.input.jenkins.model.Pipeline jenkinsPipeline = converterJenkins.convert(gitlabPipeline);		
			return new ByteArrayResource(jenkinsPipeline.toString().getBytes());
		} catch (Exception e) {			
			log.debug("Formated file GitLab incorrect [{}]", multipartFile.getOriginalFilename());
		} 
		return null;
	}
	
	public File convertToFile(MultipartFile file) throws IOException {
		File convFile = new File(file.getOriginalFilename());
		convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}
	
	
}
