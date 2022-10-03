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
public class When {

  List<String> expression;
  List<String> environment;
  List<String> branch;
  List<String> not;

  @Override
  public String toString() {
    final String expressionFlatten = getExpressionFlatten();
    final String envFlatten = getEnvironmentFlatten();
    final String branchFlatten = getBranchFlatten();
    final String notFlatten = getNotFlatten();
    return getResponse(expressionFlatten, envFlatten, branchFlatten, notFlatten);
  }

  private String getExpressionFlatten() {
    String expressionFlatten = "";
    if(this.expression != null && !this.expression.isEmpty()) {
      for (String expression: this.expression) {
        expressionFlatten += " " + expression;
      }
    }
    return expressionFlatten;
  }

  private String getEnvironmentFlatten() {
    String envFlatten = "";
    if(this.environment != null && !this.environment.isEmpty()) {
      for (String env: this.environment) {
        envFlatten += " " + env;
      }
    }
    return envFlatten;
  }

  private String getBranchFlatten() {
    String branchFlatten = "";
    if(this.branch != null && !this.branch.isEmpty()) {
      for (String branch: this.branch) {
        branchFlatten += " " + branch;
      }
    }
    return branchFlatten;
  }

  private String getNotFlatten() {
    String notFlatten = "";
    if(this.not != null && !this.not.isEmpty()) {
      for (String not: this.not) {
        notFlatten += " " + not;
      }
    }
    return notFlatten;
  }

  private String getResponse(String rexpression, String renvironment, String rbranch, String rnot) {
    String response = "{\n";
    if(expression != null && !expression.isEmpty()) {
      response += "         expression{" + rexpression + "} \n";
    }
    if(environment != null && !environment.isEmpty()) {
      response += "     environment" + "  "+ renvironment + "\n";
    }
    if(branch != null && !branch.isEmpty()) {
      response += "      branch" + rbranch + "\n ";
    }
    if(not != null && !not.isEmpty()) {
      response += "     not " + rnot + "\n";
    }
    response +="      }";

    return response;
  }
}
