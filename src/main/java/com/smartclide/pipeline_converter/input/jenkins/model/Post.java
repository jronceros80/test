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
public class Post{
  private List<String> success;
  private List<String> always;
  @Override
  public String toString() {
    final String alwaysFlatten = getAlwaysFlatten();
    final String successFlatten = getSuccessFlatten();
    return getResponse(alwaysFlatten, successFlatten);
  }
  private String getSuccessFlatten() {
    String successFlatten = "";
    if(this.success != null && !this.success.isEmpty()) {
      for (String success: this.success) {
        successFlatten += " " + success;
      }
    }
    return successFlatten;
  }

  private String getAlwaysFlatten() {
    String alwaysFlatten = "";
    if(this.always != null && !this.always.isEmpty()) {
      for (String always: this.always) {
        alwaysFlatten += " " + always + "\n";
      }
    }
    return alwaysFlatten;
  }

  private String getResponse(String alwaysFlatten, String successFlatten){
    String response = "";
    if(this.always != null && !this.always.isEmpty()){
      response += "     always{\n      " + alwaysFlatten + "     }";
    }
    if(this.success != null && !this.success.isEmpty()){
      response += "      success{\n     " + successFlatten + "\n  }";
    }
    return response;
  }
}
