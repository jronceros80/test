package com.smartclide.pipeline_converter.input;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
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
import com.smartclide.pipeline_converter.input.gitlab.model.RunConditions;
import com.smartclide.pipeline_converter.input.jenkins.model.Agent;
import com.smartclide.pipeline_converter.input.jenkins.model.Docker;
import com.smartclide.pipeline_converter.input.jenkins.model.Environment;
import com.smartclide.pipeline_converter.input.jenkins.model.Options;
import com.smartclide.pipeline_converter.input.jenkins.model.Post;
import com.smartclide.pipeline_converter.input.jenkins.model.Retry;
import com.smartclide.pipeline_converter.input.jenkins.model.Stage;
import com.smartclide.pipeline_converter.input.jenkins.model.Step;
import com.smartclide.pipeline_converter.input.jenkins.model.Success;
import com.smartclide.pipeline_converter.input.jenkins.model.When;

import ch.qos.logback.core.net.SyslogOutputStream;

public class InputParser {
	public static final String SUCCESS = "success";

	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
		ObjectMapper mapper2 = new ObjectMapper();
		mapper2.enable(SerializationFeature.INDENT_OUTPUT);
		try {
			Pipeline cfg = mapper.readValue(new File("target/classes/test3.yaml"), Pipeline.class);
			System.out.println(mapper2.writeValueAsString(cfg));
//            cfg.getJobs().values().forEach(v -> {System.out.println(v.getClass());});
			mapper.setSerializationInclusion(Include.NON_NULL);
			mapper.setSerializationInclusion(Include.NON_EMPTY);

//			System.out.println(mapper.writeValueAsString(cfg));

			System.out.println("################################################################################");			
			//System.out.println(mapper2.writeValueAsString(convert(cfg)));
			convert(cfg);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static com.smartclide.pipeline_converter.input.jenkins.model.Pipeline convert(Pipeline gitlabPipeline)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		
		// Recuperamos todos los jobs que se crearon en git a partir del post y q en jenkins deberia incluirse en el stage
		Map<String, List<Job>> always = getJobPost(gitlabPipeline, "always");
		Map<String, List<Job>> success = getJobPost(gitlabPipeline, "success");
		Map<String, List<Job>> failure = getJobPost(gitlabPipeline, "failure");
		
		var jenkinsPipeline = new com.smartclide.pipeline_converter.input.jenkins.model.Pipeline();
		if (gitlabPipeline != null && (gitlabPipeline.getJobs() != null && !gitlabPipeline.getJobs().isEmpty())) {
			jenkinsPipeline.setAgent(parseAgent(gitlabPipeline));
			jenkinsPipeline.setEnvironment(gitlabPipeline.getVariables());
			jenkinsPipeline.setPost(parsePost(gitlabPipeline));
			jenkinsPipeline.setOptions(parseOptions(gitlabPipeline));
			jenkinsPipeline.setStages(parseStages(gitlabPipeline, always, success, failure));						
		}
		System.out.println(mapper.writeValueAsString(jenkinsPipeline));
		return jenkinsPipeline;
	}

	@SuppressWarnings("unchecked")
	public static List<Stage> parseStages(Pipeline pipeline, Map<String, List<Job>> always, 
								Map<String, List<Job>> success, Map<String, List<Job>> failure) {
						
		filterJobs(pipeline);
					
		//obtenemos un mapa solo para los jobs paralelos
		Map<String, List<Job>> parallelJobs = getJobsParallel(pipeline);						
		List<Job> listJobParallel = parallelJobs.values().stream()
		          .flatMap(x -> x.stream())
		          .collect(Collectors.toList());
		
		//obtenemos un mapa solo con jobs normales(sin los paralelos)
		Map<String, Job> normalJobs = pipeline.getJobs();			
		 Iterator<String> iterator = pipeline.getJobs().keySet().iterator();
		 while(iterator.hasNext()){ 
			 String job = iterator.next(); 
			 listJobParallel.forEach(jobParallel -> {
					if(jobParallel.getName().contains(job)) {					
						 iterator.remove(); 		
					}
			 });			
		 }
		 
		 //Concatenamos ambos mapas(jobs paralelos y normales) 		 
		 Map<String, Object> totalJobs = new HashMap<String, Object>(normalJobs);
		 parallelJobs.forEach(totalJobs::putIfAbsent);
		 
		 List<Stage> stages = new ArrayList<>();			 
		 totalJobs.forEach((key, obj) -> {					
			 if(obj instanceof Job) {
				 List<Stage> substages= new ArrayList<>();				 
				 Stage stage = parseStage(key, (Job)obj, always, success, failure,"normal");
				 substages.add(stage);
				 stages.addAll(substages);
			 }else if(obj instanceof Collection) {	
				 List<Stage> substages= new ArrayList<>();
				 List<Job> jobs = (List<Job>)obj;
				 jobs.forEach(job -> {
					 Stage stage = parseStage(key, job, always, success, failure, "parallel");
					 substages.add(stage);				 
				 });				 				 				
				 Stage parent = Stage.builder().name(key).parallel(substages).build();		
				 stages.add(parent);
			 }			 					
		 });
		 		
		/*var groupedStages = new LinkedHashMap<String, List<Stage>>();		
		List<Stage> stages = new ArrayList<>();		
		pipeline.getJobs().forEach((key, job) -> {					
			Stage stage = parseStage(key, job, always, success, failure);
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
		});*/
		
		return stages;
	}
	
	public static boolean isRemove(List<Job> jobs, String key) {
		return true;
	}
	
	public static Map<String, List<Job>> getJobsParallel(Pipeline pipeline) {	
		Map<String, List<Job>> mapJobs = new Hashtable<>();
		List<Job> jobs = new ArrayList<>();
		pipeline.getJobs().forEach((key, job) -> {			
				if(job.getExtends()!= null && !job.getExtends().isEmpty()) {
					String newKey = job.getExtends().get(0).substring(job.getExtends().get(0).indexOf(".")+1);
					job.setName(key);
					jobs.add(job);
					mapJobs.put(newKey,jobs);
				}									
	    });
		return mapJobs;
	}
	
	// Descartamos los siguientes jobs para poder crear bien la estructuta en jenkis:
	// Post a nivel de pipeline(post_pipeline)
	// Job padre paralelo(tiene su stage= null y a su nombre se le añade un .)
	// Post a nivel job(success, always, failure)
	private static void filterJobs(Pipeline pipeline) {
		pipeline.getJobs().entrySet().removeIf(
										j -> j.getKey().contains("post_pipeline") 							
										|| (j.getKey().contains(".") && j.getValue().getStage() == null)	
										|| j.getKey().contains("success")									
										|| j.getKey().contains("always")
										|| j.getKey().contains("failure")
									  );
	}

	
	public static Stage parseStage(String key, Job job, Map<String, List<Job>> always, 
			Map<String, List<Job>> success, Map<String, List<Job>> failure, String jobType) {
		String name = jobType.equals("normal")?job.getStage(): job.getName();
		return Stage.builder()
				.name(name)
				.agent(parseAgentJob(job))				
				.environment(job.getVariables())				
				.when(parseWhen(job))
				.steps(parseSteps(job))
				.post(parsePostJob(key, job, always, success, failure))
				.build();
	}

	public static List<String> parseSteps(Job job) {
		final List<String> steps = new ArrayList<>();
		steps.addAll(job.getBeforeScript());
		steps.addAll(job.getScript());
		return steps;
	}

	public static Agent parseAgent(Pipeline pipeline) {
		var agentBuilder = Agent.builder();		
		if (pipeline.getImage() != null) {
			final Docker docker = parseDocker(pipeline.getImage());
			agentBuilder.docker(docker);
		}
		return agentBuilder.build();
	}

	public static Agent parseServiceJob(Job job) {
		if (job.getServices() != null && !job.getServices().isEmpty()) {
			final Docker docker = parseDocker(job.getServices().get(0));
			return Agent.builder().docker(docker).build();
		}
		return null;
	}

	public static Agent parseAgentJob(Job job) {
		Agent agent = new Agent();
		Docker docker = null;
		if (job.getImage() != null) {
			docker = parseDocker(job.getImage());
		} else if (job.getServices() != null && !job.getServices().isEmpty()) {
			docker = parseDocker(job.getServices().get(0));
		} else {
			return null;
		}
		agent.setDocker(docker);
		return agent;
	}

	public static Docker parseDocker(DockerImage image) {
		return Docker.builder().image(image.getName()).args(image.getEntryPoint()).build();
	}

	public static Post parsePostJob(String keyParent, Job jobParent, Map<String, List<Job>> always, 
										Map<String, List<Job>> success, Map<String, List<Job>> failure) {				
		Post post = new Post();
		setAlwaysToPost(keyParent, always, post);			
		setSuccessToPost(keyParent, success, post);		
		setFailureToPost(keyParent, failure, post);
		return post;
	}

	private static void setFailureToPost(String keyParent, Map<String, List<Job>> failure, Post post) {
		failure.forEach((key, job) -> {	
			String keyParentJob = key.substring(0, key.lastIndexOf(":"));			
			if(keyParentJob.equals(keyParent)) {
				post.setFailure(job.get(0).getScript());			
			}
	    });
	}

	private static void setSuccessToPost(String keyParent, Map<String, List<Job>> success, Post post) {
		success.forEach((key, job) -> {	
			String keyParentJob = key.substring(0, key.lastIndexOf(":"));			
			if(keyParentJob.equals(keyParent)) {
				post.setSuccess(job.get(0).getScript());			
			}
	    });
	}

	private static void setAlwaysToPost(String keyParent, Map<String, List<Job>> always, Post post) {
		always.forEach((key, job) -> {	
			String keyParentJob = key.substring(0, key.lastIndexOf(":"));			
			if(keyParentJob.equals(keyParent)) {
				post.setAlways(job.get(0).getScript());			
			}
	    });
	}
		
	public static Map<String, List<Job>> getJobPost(Pipeline pipeline, String condition) {	
		Map<String, List<Job>> mapJobs = new Hashtable<>();
		List<Job> jobs = new ArrayList<>();
		pipeline.getJobs().forEach((key, job) -> {	
			if(!key.contains("post_pipeline")) {
				if(key.contains(condition)) {							
					jobs.add(job);
					mapJobs.put(key,jobs);
				}				
			}
	    });
		return mapJobs;
	}		

	public static Post parsePost(Pipeline pipeline) {
		Post post = new Post();				
		pipeline.getJobs().forEach((key, job) -> {			
			if(key.contains("post_pipeline")) {						
				if(job.getWhen().equals(RunConditions.always)) {
					post.setAlways(job.getScript());
				}
				if(job.getWhen().equals(RunConditions.on_success)) {
					post.setSuccess(job.getScript());
				}
				if(job.getWhen().equals(RunConditions.on_failure)) {
					post.setFailure(job.getScript());
				}					
			}	
	    });
		return post;
	}
//
//	public static Environment parseEnvironmentJob(Job job) {
//		if (job.getVariables() != null && !job.getVariables().isEmpty()) {
//			return Environment.builder().variables(job.getVariables()).build();
//		}
//		return null;
//	}
//
//	public static Environment parseEnvironment(Pipeline pipeline) {
//		if (pipeline.getVariables() != null && !pipeline.getVariables().isEmpty()) {
//			return Environment.builder().variables(pipeline.getVariables()).build();
//		}
//		return null;
//	}

	public static Options parseOptions(Pipeline pipeline) {
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

	public static Retry parseRetry(Job job) {
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
		return when;
	}

}
