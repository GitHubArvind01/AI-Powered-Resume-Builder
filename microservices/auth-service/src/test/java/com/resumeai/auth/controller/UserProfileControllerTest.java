package com.resumeai.auth.controller;

import com.resumeai.auth.dtos.CurrentUserResponseDTO;
import com.resumeai.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.resumeai.auth.service.JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    void testGetCurrentUser() throws Exception {
        String email = "test@gmail.com";
        when(userService.getCurrentUser(email)).thenReturn(new CurrentUserResponseDTO());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-User-Email", email))
                .andExpect(status().isOk());

        verify(userService, times(1)).getCurrentUser(email);
    }
}