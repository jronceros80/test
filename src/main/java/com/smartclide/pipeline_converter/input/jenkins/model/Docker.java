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
public class Docker {
  private String image;
  private List<String> args;
  @Override
  public String toString() {
    final String argsFlatten = getArgsFlatten();
    return getResponse(argsFlatten);
  }

  private String getArgsFlatten() {
    String argsFlatten = "";
    if(this.args != null && !this.args.isEmpty()) {
      for (String arg: this.args) {
        argsFlatten += " " + arg + "\n ";
      }
    }
    return argsFlatten;
  }

  private String getResponse(String argsFlatten) {
    String response = "docker{\n";
    if(image!= null && !image.isEmpty()){
      response += "            image " + image + "\n";
    }
    if(args!= null && !args.isEmpty()){
      response += "       args" + args + "\n";
    }
    response += "         }";

    return response;
  }
}
