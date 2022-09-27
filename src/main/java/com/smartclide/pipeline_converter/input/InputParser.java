package com.smartclide.pipeline_converter.input;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.smartclide.pipeline_converter.input.gitlab.model.DockerImage;
import com.smartclide.pipeline_converter.input.gitlab.model.Job;
import com.smartclide.pipeline_converter.input.gitlab.model.Pipeline;
import com.smartclide.pipeline_converter.input.jenkins.model.Agent;
import com.smartclide.pipeline_converter.input.jenkins.model.Agent.AgentType;
import com.smartclide.pipeline_converter.input.jenkins.model.Docker;
import com.smartclide.pipeline_converter.input.jenkins.model.Options;
import com.smartclide.pipeline_converter.input.jenkins.model.Post;
import com.smartclide.pipeline_converter.input.jenkins.model.Retry;
import com.smartclide.pipeline_converter.input.jenkins.model.Stage;
import com.smartclide.pipeline_converter.input.jenkins.model.When;

public class InputParser {
	public static final String SUCCESS = "success";

	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
		ObjectMapper mapper2 = new ObjectMapper();
		mapper2.enable(SerializationFeature.INDENT_OUTPUT);
		mapper2.setSerializationInclusion(Include.NON_NULL);
		mapper2.setSerializationInclusion(Include.NON_EMPTY);
		try {
			Pipeline cfg = mapper.readValue(new File("target/classes/test5.yaml"), Pipeline.class);
			System.out.println(mapper2.writeValueAsString(cfg));
//            cfg.getJobs().values().forEach(v -> {System.out.println(v.getClass());});
			mapper.setSerializationInclusion(Include.NON_NULL);
			mapper.setSerializationInclusion(Include.NON_EMPTY);

//			System.out.println(mapper.writeValueAsString(cfg));

			System.out.println("################################################################################");			
			System.out.println(mapper2.writeValueAsString(convert(cfg)));			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static com.smartclide.pipeline_converter.input.jenkins.model.Pipeline convert(Pipeline gitlabPipeline)
			throws JsonProcessingException {						
		var jenkinsPipeline = new com.smartclide.pipeline_converter.input.jenkins.model.Pipeline();
		if (gitlabPipeline != null && (gitlabPipeline.getJobs() != null && !gitlabPipeline.getJobs().isEmpty())) {
			jenkinsPipeline.setAgent(parseAgent(gitlabPipeline));
			jenkinsPipeline.setEnvironment(gitlabPipeline.getVariables());
			jenkinsPipeline.setPost(parsePost(gitlabPipeline));
			jenkinsPipeline.setOptions(parseOptions(gitlabPipeline));
			jenkinsPipeline.setStages(parseStages(gitlabPipeline));						
		}		
		return jenkinsPipeline;
	}
	
	private static List<Stage> parseStages(Pipeline pipeline) {		 		
		var groupedStages = new LinkedHashMap<String, List<Stage>>();		
		List<Stage> stages = new ArrayList<>();		
		pipeline.getJobs().forEach((key, job) -> {					
			Stage stage = parseStage(key, job);
			groupedStages.merge(job.getStage(), Arrays.asList(stage), (current, newVal) -> {				
				return Stream.of(current, newVal)
		                .flatMap(x -> x.stream())
		                .collect(Collectors.toList());				
				}
			);
		  }
		);
		
		groupedStages.forEach((stage,substages) -> {
			if(substages.size() > 1) {
				Stage parent = Stage.builder().name(stage).parallel(substages).build();
				stages.add(parent);
			} else {
				stages.addAll(substages);
			}
		});
		
		return stages;
	}
	
	private static Stage parseStage(String key, Job job) {		
		return Stage.builder()
				.name(key)
				.agent(parseAgentJob(job))				
				.environment(job.getVariables())				
				.when(parseWhen(job))
				.steps(parseSteps(job))
				.post(parsePostJob(key, job))
				.build();
	}

	private static List<String> parseSteps(Job job) {
		final List<String> steps = new ArrayList<>();
		steps.addAll(job.getBeforeScript());
		steps.addAll(job.getScript());				
		return steps;
	}

	private static Agent parseAgent(Pipeline pipeline) {
		var agentBuilder = Agent.builder();		
		if (pipeline.getImage() != null) {
			final Docker docker = parseDocker(pipeline.getImage());
			agentBuilder.docker(docker);
		}
		return agentBuilder.build();
	}

	private static Agent parseAgentJob(Job job) {
		Agent agent = new Agent();
		agent.setAgentType(null);		
		Docker docker = null;
		if (job.getImage() != null) {
			agent.setAgentType(AgentType.OTHER);
			docker = parseDocker(job.getImage());
		}
		if (job.getServices() != null && !job.getServices().isEmpty()) {			
			docker = parseDocker(job.getServices().get(0));
		} 
		if(job.getTags() != null && !job.getTags().isEmpty()) {
			agent.setLabel(job.getTags());
			agent.setAgentType(AgentType.OTHER);		
		}
		if(agent.getDocker()== null && agent.getAgentType()==null) {
			return null;
		}
		agent.setDocker(docker);
		return agent;
	}

	private static Docker parseDocker(DockerImage image) {
		return Docker.builder().image(image.getName()).args(image.getEntryPoint()).build();
	}

	private static Post parsePostJob(String keyParent, Job job) {										
		Post post = new Post();			
		if(job.getArtifacts() != null && job.getArtifacts().getPaths() != null) {							
			post.setAlways(job.getArtifacts().getPaths());						
		}
		
		if(job.getAfterScript() != null && !job.getAfterScript().isEmpty()) {				
			post.setAlways(job.getAfterScript());						
		}
		if(post.getAlways()==null) {
			return null;
		}
		return post;
	}
	


	private static Post parsePost(Pipeline pipeline) {
		Post post = new Post();				
		pipeline.getJobs().forEach((key, job) -> {			
			if(job.getBeforeScript()!= null && !job.getBeforeScript().isEmpty()) {
				post.setTools(job.getBeforeScript());			
			}		
	    });	
		
		if(post.getTools()==null) {
			return null;
		}
		return post;
	}

	private static Options parseOptions(Pipeline pipeline) {
		if (pipeline.get_default() != null) {
			final Retry retry = parseRetry(pipeline.get_default());
			final String timeout = pipeline.get_default().getTimeout();
			if (retry == null && timeout == null) {
				return null;
			}
			return Options.builder().timeout(timeout).retry(retry).build();
		}
		return null;
	}

	private static Retry parseRetry(Job job) {
		if (job.getRetry() != null) {
			return Retry.builder().maxRetries(job.getRetry().getMaxRetries())
					// .when(parseWhen(job.getRetry().getWhen()))
					.build();
		}
		return null;
	}

	public static When parseWhen(Job job) {
		When when = new When();
		List<String> expresions = new ArrayList<>();
		if (job.getOnly() != null) {
			when.setBranch(job.getOnly().getRefs());	
			when.setEnvironmentName(job.getOnly().getVariables());										
		}
		if(job.getRules() != null && !job.getRules().isEmpty()) {
			job.getRules().forEach(rule -> {
				expresions.add(rule.get_if());
			  });
			when.setExpression(expresions);
		}
		if(when.getBranch()==null && when.getEnvironmentName()== null && when.getExpression()== null) {
			return null;
		}
		return when;
	}

}
