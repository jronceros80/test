package com.smartclide.pipeline_converter.input.jenkins.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder()
@NoArgsConstructor
@AllArgsConstructor
public class Stage {

  private String name;
  private Agent agent;
  private Map<String, String> environment;
  private When when;
  private List<String> steps;
  private Post post;
  private List<Stage> stages;
  private List<Stage> parallel;
  private List<String> tags;
  private List<String> dependencies;

  @Override
  public String toString() {
    final String stepsFlatten = getStepsFlatten();
    final String envFlatten = getEnvFlatten();
    final String parallelFlatten = getParallelFlatten();
    return getResponse(stepsFlatten, envFlatten, parallelFlatten);
  }
  private String getParallelFlatten() {
    String parallelFlatten = "";
    if(this.parallel != null && !this.parallel.isEmpty()) {
      for (Stage stage : this.parallel) {
        parallelFlatten += "      " + stage + "\n ";
      }
    }
    return parallelFlatten;
  }

  private String getStepsFlatten() {
    String stepsFlatten = "";
    if(this.steps != null && !this.steps.isEmpty()) {
      for (String step: this.steps) {
        stepsFlatten += "    " + step + "\n     ";
      }
    }
    return stepsFlatten;
  }

  private String getEnvFlatten() {
    String envFlatten = "";
    if(this.environment != null && !this.environment.isEmpty()) {
      for (Iterator<Map.Entry<String, String>> entries = environment.entrySet().iterator(); entries.hasNext(); ) {
        Map.Entry<String, String> entry = entries.next();
        envFlatten += "       " + entry.getKey()+" = "+entry.getValue() + "\n";
      }
    }
    return envFlatten;
  }

  private String getResponse(String stepsFlatten, String envFlatten, String parallelFlatten) {
    String response = "";
    if(name != null) {
      response += "stage('" + name + "'){ \n";
    }
    if(agent != null && agent.getAgentType() !=null && !agent.getAgentType().equals(Agent.AgentType.any)) {
      response += "       agent" + agent + "\n";
    }
    if(environment != null && !environment.isEmpty()) {
      response += "    environment{\n" + envFlatten + "     }\n";
    }
    if(when != null) {
      response += "      when " + when + "\n";
    }
    if(steps != null && !steps.isEmpty()) {
      response += "      steps{\n     " + stepsFlatten + "  }\n";
    }
    if(post != null) {
      response += "    post {\n" + post + "       \n    }\n";
    }
    if(stages != null && !stages.isEmpty()) {
      response += "    stages " + stages + "\n";
    }
    if(parallel != null && !parallel.isEmpty()) {
      response += "      parallel{\n" + parallelFlatten + "}\n ";
    }
    if(tags != null  && !tags.isEmpty()) {
      response += "    tags " + tags + "\n";
    }
    if(dependencies != null  && !dependencies.isEmpty()) {
      response += "    dependencies " + dependencies + "\n";
    }
    response +="     }";
    return response;
  }

}
