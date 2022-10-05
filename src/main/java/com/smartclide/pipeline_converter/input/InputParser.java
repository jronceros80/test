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
import com.smartclide.pipeline_converter.input.jenkins.model.Docker;
import com.smartclide.pipeline_converter.input.jenkins.model.Options;
import com.smartclide.pipeline_converter.input.jenkins.model.Post;
import com.smartclide.pipeline_converter.input.jenkins.model.Retry;
import com.smartclide.pipeline_converter.input.jenkins.model.Stage;
import com.smartclide.pipeline_converter.input.jenkins.model.When;

public class InputParser {
  public static final String SUCCESS = "success";
  public static final String ARTIFACT = "archiveArtifacts artifacts:";

  public static void main(String[] args) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
    ObjectMapper mapper2 = new ObjectMapper();
    mapper2.enable(SerializationFeature.INDENT_OUTPUT);
    mapper2.setSerializationInclusion(Include.NON_NULL);
    mapper2.setSerializationInclusion(Include.NON_EMPTY);
    try {
      Pipeline cfg = mapper.readValue(new File("target/classes/test11.yaml"), Pipeline.class);
      System.out.println(mapper2.writeValueAsString(cfg));
      //            cfg.getJobs().values().forEach(v -> {System.out.println(v.getClass());});
      mapper.setSerializationInclusion(Include.NON_NULL);
      mapper.setSerializationInclusion(Include.NON_EMPTY);

      //			System.out.println(mapper.writeValueAsString(cfg));

      System.out.println("################################################################################");
      //System.out.println(mapper2.writeValueAsString(convert(cfg)));
      System.out.println(convert(cfg));

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
        .steps(parseSteps(job))
        .when(parseWhen(job))
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
    Agent agent = new Agent();
    if (pipeline.getImage() != null) {
      final Docker docker = parseDocker(pipeline.getImage());
      agent.setDocker(docker);
    }
    return agent;
  }

  private static Agent parseAgentJob(Job job) {
    Agent agent = new Agent();
    Docker docker = null;
    agent.setAgentType(null);
    if (job.getImage() != null) {
      docker = parseDocker(job.getImage());
      agent.setDocker(docker);
    } else if (job.getTags() != null && !job.getTags().isEmpty()) {
      agent.setLabel(job.getTags());  //solo en caso de q no sea image
    }
    if(agent.getDocker()== null && agent.getAgentType()==null
        && (agent.getLabel() == null || agent.getLabel().isEmpty())) {
      return null;
    }
    return agent;
  }

  private static Docker parseDocker(DockerImage image) {
    return Docker.builder()
            .image(image.getName())
            .args(image.getEntryPoint())
            .build();
  }

  private static Post parsePostJob(String keyParent, Job job) {
    List<String> artifacts = new ArrayList<>();
    List<String> concatenated = new ArrayList<>();
    Post post = new Post();

    if(job.getAfterScript() != null && !job.getAfterScript().isEmpty()) {
      concatenated.addAll(job.getAfterScript());
    }
    if(job.getArtifacts() != null && job.getArtifacts().getPaths() != null
        && !job.getArtifacts().getPaths().isEmpty()) {
      // TODO: revisar tratamiento de artifact
      String artifact = ARTIFACT.concat(job.getArtifacts().getPaths().get(0));
      artifacts.add(artifact);
      concatenated.addAll(artifacts);
    }
    post.setAlways(concatenated);

    if(job.getAllowFailure() != null) {
      post.setFailure(job.getAllowFailure().getAllowFailure());
    }

    if((post.getAlways()==null || post.getAlways().isEmpty()) && (
        post.getSuccess()==null || post.getSuccess().isEmpty()) &&
        post.getFailure()==null) {
      return null;
    }
    return post;
  }
  private static Post parsePost(Pipeline pipeline) {
    Post post = new Post();
    pipeline.getJobs().forEach((key, job) -> {
      //TODO pendiente de revisión, no hay q preguntar por el .post
      if(job.getStage()!= null && job.getStage().equals(".post")) {
        post.setAlways(job.getScript());
      }
    });

    if(post.getAlways()==null) {
      return null;
    }
    return post;
  }

  private static Options parseOptions(Pipeline pipeline) {
    if (pipeline.get_default() != null) {
      final Retry retry = parseRetry(pipeline.get_default());
      final String timeout = pipeline.get_default().getTimeout();

      //TODO: esto es para no mostrar en la respuesta los objetos con todos sus atributos a null
      if (retry == null && timeout == null) {
        return null;
      }
      return Options.builder()
              .timeout(timeout)
              .retry(retry)
              .build();
    }
    return null;
  }

  private static Retry parseRetry(Job job) {
    if (job.getRetry() != null) {
      return Retry.builder()
              .maxRetries(job.getRetry().getMaxRetries())
          .build();
    }
    return null;
  }

  public static When parseWhen(Job job) {
    When when = new When();
    if (job.getOnly() != null) {
      when.setBranch(job.getOnly().getRefs());
      when.setEnvironment(job.getOnly().getVariables());
    }
    if(job.getRules() != null && !job.getRules().isEmpty()) {
      List<String> expr = new ArrayList<>();
      List<String> allOf = new ArrayList<>();
      job.getRules().forEach(rule -> {
        // para q sea una expresion no debe contener el simbolo $
        if(rule.get_if() != null && !rule.get_if().contains("$")){
          expr.add(rule.get_if());
        }
        // las variables de entorno q contienen el $ y estan anidada con el && es porque es un allOf en jenkins
        if(rule.get_if() != null && rule.get_if().contains("&&")&& rule.get_if().contains("$")){
          String[] allOfs = Arrays.stream(rule.get_if().split("&&"))
                            .map(String::trim)
                            .toArray(String[]::new);
          List<String> listAllOf = Stream.of(allOfs).map(temp -> temp.substring(temp.indexOf("$")+1)).collect(Collectors.toList());
          listAllOf.forEach(allOfFound -> allOf.add(allOfFound));
        }
      });
      when.setExpression(expr);
      when.setAllOf(allOf);
    }
    if(job.getExcept()!=null) {
      when.setNot(job.getExcept().getRefs());
    }
    //TODO: esto es para no mostrar en la respuesta los objetos con todos sus atributos a null
    if(when.getBranch()==null
                  && when.getEnvironment()== null
                  && when.getExpression()== null
                  && when.getNot() == null
                  && when.getAllOf()== null) {
      return null;
    }
    return when;
  }

}
