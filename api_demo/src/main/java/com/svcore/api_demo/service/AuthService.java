package com.svcore.api_demo.service;

import com.svcore.api_demo.models.dtos.LoginDTO;
import com.svcore.api_demo.models.dtos.TokenDTO;

public interface AuthService {

	TokenDTO login(LoginDTO info);
}
