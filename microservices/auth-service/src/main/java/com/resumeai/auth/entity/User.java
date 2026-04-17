package com.resumeai.auth.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String username;
    private String email;
    private String password;
    private String role;
    private String otpCode;
    private LocalDateTime otpExpiry;
    
    public User(String username, String email, String password, String role) {
    	this.username = username;
    	this.email = email;
    	this.password = password;
    	this.role = role;
    }
}