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
  List<String> allOf;
  List<String> notAnyOf;

  @Override
  public String toString() {
    final String expressionFlatten = getExpressionFlatten();
    final String envFlatten = getEnvironmentFlatten();
    final String branchFlatten = getBranchFlatten();
    final String notFlatten = getNotAnyOfFlatten();
    final String allOfFlatten = getAllOfFlatten();
    return getResponse(expressionFlatten, envFlatten, branchFlatten, notFlatten, allOfFlatten);
  }

  private String getExpressionFlatten() {
    String expressionFlatten = "";
    if(this.expression != null && !this.expression.isEmpty()) {
      for (String expression: this.expression) {
        expressionFlatten += "          " + expression ;
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
        branchFlatten += branch;
      }
    }
    return branchFlatten;
  }

  private String getNotAnyOfFlatten() {
    String notAnyOfFlatten = "";
    String concatenated = "";
    if(this.notAnyOf != null && !this.notAnyOf.isEmpty()) {
      for (String nAnyOf: this.notAnyOf) {
        if(!nAnyOf.contains("CI_COMMIT_BRANCH")){
          String envName = substractInitial(nAnyOf).trim();
          String enValue = substractEnd(nAnyOf).trim();
          concatenated = "        environment name: '" + envName + "', environment value: '" + enValue + "'\n";
        }else{
          String branch = substractEnd(nAnyOf);
          concatenated = "        branch: " + branch + "\n";
        }
        notAnyOfFlatten += " " + concatenated;
      }
    }
    return notAnyOfFlatten;
  }
  private String getAllOfFlatten() {
    String response ="";
    if(this.allOf != null && !this.allOf.isEmpty()) {
      for (String allOf: this.allOf) {
        String envName = substractInitial(allOf);
        String enValue = substractEnd(allOf);
        String env = "       environment name: '" + envName + "', environment value: '" + enValue + "'\n";
        response += "  " + env;
      }
    }
    return response;
  }
  private static String substractInitial(String text) {
    return text.substring(0, text.lastIndexOf("=="));
  }
  private static String substractEnd(String text) {
    return  text.substring(text.lastIndexOf("==")+2);
  }

  private String getResponse(String expressionFlatten, String envFlatten,
                             String branchFlatten, String notFlatten, String allOfs) {
    String response = "{\n";
    if(expression != null && !expression.isEmpty()) {
      response += "        expression{\n" + expressionFlatten + "\n        } \n";
    }
    if(environment != null && !environment.isEmpty()) {
      response += "        environment{\n" + "    "+ envFlatten + "    }\n";
    }
    if(branch != null && !branch.isEmpty()) {
      response += "        branch '" + branchFlatten + "'\n ";
    }
    if(notAnyOf != null && !notAnyOf.isEmpty()) {
      response += "        not{\n" + notFlatten + "        }\n";
    }
    if(allOf != null && !allOf.isEmpty()) {
      response += "         allOf{ \n " + allOfs + "    }\n";
    }
    response +="     }";

    return response;
  }
}
