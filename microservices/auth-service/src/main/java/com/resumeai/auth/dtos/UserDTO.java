package com.resumeai.auth.dtos;

import com.resumeai.auth.entity.User;

public class UserDTO {
	public User toUser(RegisterRequest registerRequest){
		return new User(registerRequest.getUsername(), registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getRole());
	}
}
