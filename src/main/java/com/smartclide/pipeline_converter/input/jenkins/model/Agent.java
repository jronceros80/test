package com.smartclide.pipeline_converter.input.jenkins.model;

import java.util.List;
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
public class Agent {

  @Builder.Default
  private AgentType agentType = AgentType.any;
  private List<String> label;
  private Docker docker;

  public enum AgentType {
    any, none, other;
  }

  @Override
  public String toString() {
    final String labelFlatten = getLabelFlatten();
    return getResponse(labelFlatten);
  }

  private String getLabelFlatten() {
    String labelFlatten = "";
    if(this.label != null && !this.label.isEmpty()) {
      for (String label: this.label) {
        labelFlatten += " " + label;
      }
    }
    return labelFlatten;
  }

  private String getResponse(String labelFlatten) {
    String response = "";
    if(label != null && !label.isEmpty()){
      response += "{\n        label " + labelFlatten + "\n       }";
    }else if(docker != null){
      response += "{\n         " + docker + "\n       }";
    }else{
      response += AgentType.any;
    }
    return response;
  }
}

