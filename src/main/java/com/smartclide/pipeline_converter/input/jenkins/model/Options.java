package com.smartclide.pipeline_converter.input.jenkins.model;

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
public class Options {
  private String timeout;
  private Retry retry;
  @Override
  public String toString() {
    return getResponse();
  }

  private String getResponse() {
    return "Options [timeout=" + timeout + ", retry=" + retry + "]";
  }

}
