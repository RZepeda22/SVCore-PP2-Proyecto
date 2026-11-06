package com.svcore.api_demo.models.dtos;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenDTO {

	private String token;
	private String type;
	private Instant expiresAt;
	private String fullName;
	private List<String> roles;
}
