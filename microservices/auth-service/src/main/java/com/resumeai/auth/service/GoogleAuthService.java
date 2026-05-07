package com.resumeai.auth.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.UnauthorizedException;
import com.resumeai.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class GoogleAuthService {	
	
	private final CustomUserDetailsService customUserDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final RestTemplate restTemplate;
	private final JwtService jwtService;


	@Value("${spring.security.oauth2.client.registration.google.client-id}") 
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String clientSecret;
	
	@Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;
	
	public AuthResponse handleGoogleAuth(String code) {
		try {
			// 1. Exchange auth code for tokens
			String tokenEndpoint = "https://oauth2.googleapis.com/token";
			
			/*
			 * This params have application Owner - google credentials [When user hit login with google then this params and header go to authorization server to get access token]
			 */
			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("code", code);
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);
			params.add("redirect_uri", redirectUri);
			params.add("grant_type", "authorization_code");
	
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
			
			/*
			 * Here server hit the post request to google end point to get access token- 
			 */
			ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
			
			/*
			 * Here - we get the access token - with this access token server can get details of user
			 */
			String idToken = (String)tokenResponse.getBody().get("id_token");
			
			/*
			 * Here - this userInfor URL- help to get the user details - in this URL - we add the access token 
			 */
			String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
			
			/*
			 * Here - server again hit the get request to google server, but now they hit with access token, so now we get the entity details of user
			 */
			ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);
			
			/*
			 * Every things well here - means status code 200 - means token correct, userInfoUrl correct, and we got userResponse 
			 */
			if(userInfoResponse.getStatusCode()==HttpStatus.OK) {
				Map<String,Object> userInfo = userInfoResponse.getBody();
				String email = (String)userInfo.get("email");
				
				UserDetails userDetails = null;
				
				try {
					userDetails = customUserDetailsService.loadUserByEmail(email);
				}
				/*
				 * If userDetails null means- new User
				 */
				catch(Exception e) {
					/*
					 * here we create new user and save to db
					 */
					User user = new User();
					user.setEmail(email);
					user.setFullName(email);
					user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
					user.setRole("USER");
					user.setSubscriptionPlan("FREE");
					user.setPremiumActive(false);
					user.setSubscriptionStatus("FREE");
					user.setPaymentStatus("UNPAID");
					user.setActive(true);
					userRepository.save(user);
				}
				User userDB = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));
				String token = jwtService.generateToken(userDB);
			return new AuthResponse(token, "Google login success", null);
		}
		throw new UnauthorizedException("Unauthorize authentication exception.");
	}
	catch(Exception e) {
		log.error("Exception occur during google authentication");
		throw new UnauthorizedException("Exception occur during google authentication");
		}
	}
}
