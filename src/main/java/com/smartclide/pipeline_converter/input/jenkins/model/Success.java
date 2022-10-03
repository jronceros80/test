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
public class Success {
  private List<String> archiveArtifacts;
  @Override
  public String toString() {
    final String artsFlatten = getArtifactFlatten();
    return getResponse(artsFlatten);
  }

  private String getArtifactFlatten() {
    String artsFlatten = "";
    if(this.archiveArtifacts != null &&  !this.archiveArtifacts.isEmpty()) {
      for (String artifact: this.archiveArtifacts) {
        artsFlatten += " " + artifact;
      }
    }
    return artsFlatten;
  }

  private String getResponse(String artsFlatten) {
    String response = "";
    //TODO pendiente de revisión
    if(archiveArtifacts != null && !archiveArtifacts.isEmpty()){
      response += "archiveArtifacts artifacts: " + artsFlatten;
    }
    return response;
  }
}
