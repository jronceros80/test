package com.smartclide.pipeline_converter.input.jenkins.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder()
@NoArgsConstructor
@AllArgsConstructor
public class Post {	
	//private RunCondition runCondition;
	private List<String> script;
	
	/*public enum RunCondition{
		always, success, failure;
	}*/
	
	private List<String> always;
	private List<String> success;
	private List<String> failure;
	
}
