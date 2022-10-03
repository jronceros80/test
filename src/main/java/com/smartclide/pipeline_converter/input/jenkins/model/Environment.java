package com.smartclide.pipeline_converter.input.jenkins.model;

import java.util.Iterator;
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
public class Environment {
  private Map<String, String> variables;

  @Override
  public String toString() {
    final String variablesFlatten = getVariablesFlatten();
    return getResponse(variablesFlatten);
  }

  private String getVariablesFlatten() {
    String variablesFlatten = "";
    if(this.variables != null && !this.variables.isEmpty()) {
      for (Iterator<Map.Entry<String, String>> entries = variables.entrySet().iterator(); entries.hasNext(); ) {
        Map.Entry<String, String> entry = entries.next();
        variablesFlatten += " " + entry.getKey()+" = "+entry.getValue();
      }
    }
    return variablesFlatten;
  }

  private String getResponse(String variablesFlatten) {
    if(variablesFlatten != null) {
      return "environment {\n" + variablesFlatten + "\n}";
    }
    return null;
  }
}
