package com.smartclide.pipeline_converter.input.jenkins.model;

import java.util.ArrayList;
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
  List<String> allOf;
  List<String> not;

  @Override
  public String toString() {
    final String expressionFlatten = getExpressionFlatten();
    final String envFlatten = getEnvironmentFlatten();
    final String branchFlatten = getBranchFlatten();
    final String notFlatten = getNotFlatten();
    final String allOfFlatten = getAllOfFlatten();
    return getResponse(expressionFlatten, envFlatten, branchFlatten, notFlatten, allOfFlatten);
  }

  private String getExpressionFlatten() {
    String expressionFlatten = "";
    if(this.expression != null && !this.expression.isEmpty()) {
      for (String expression: this.expression) {
        expressionFlatten += " " + expression ;
      }
    }
    return expressionFlatten;
  }

  private String getEnvironmentFlatten() {
    String envFlatten = "";
    if(this.environment != null && !this.environment.isEmpty()) {
      for (String env: this.environment) {
        envFlatten += "      " + env + "    \n    ";
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

  private String getAllOfFlatten() {
    String response ="";
    if(this.allOf != null && !this.allOf.isEmpty()) {
      for (String allOf: this.allOf) {
        String envName = allOf.substring(0,allOf.lastIndexOf("=="));
        String enValue = allOf.substring(allOf.lastIndexOf("==")+2);
        String env = "        environment name: " + envName + ", environment value: " + enValue + "\n";
        response += "  " + env;
      }
    }
    return response;
  }

  private String getResponse(String expressionFlatten, String envFlatten,
                             String branchFlatten, String notFlatten, String allOfs) {
    String response = "{\n";
    if(expression != null && !expression.isEmpty()) {
      response += "         expression{" + expressionFlatten + "} \n";
    }
    if(environment != null && !environment.isEmpty()) {
      response += "        environment{\n" + "    "+ envFlatten + "    }\n";
    }
    if(branch != null && !branch.isEmpty()) {
      response += "      branch" + branchFlatten + "\n ";
    }
    if(not != null && !not.isEmpty()) {
      response += "     not " + notFlatten + "\n";
    }
    if(allOf != null && !allOf.isEmpty()) {
      response += "         allOf{ \n " + allOfs + "    }\n";
    }
    response +="      }";

    return response;
  }
}
