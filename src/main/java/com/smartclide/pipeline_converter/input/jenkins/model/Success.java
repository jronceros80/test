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
public class Success {
	private List<String> scripts;
}
