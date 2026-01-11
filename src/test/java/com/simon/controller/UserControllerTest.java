package com.simon.controller;

import com.simon.model.User;
import com.simon.repository.UserRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    private UserController userController;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        userController = new UserController(userRepository);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test(description = "GET /api/users/me without authentication returns 401",
          groups = {"unit", "controller"},
          priority = 1)
    public void me_whenAuthNull_returns401() {
        ResponseEntity<?> resp = userController.me(null);
        assertEquals(resp.getStatusCode().value(), 401);
        assertEquals(resp.getBody(), "Unauthorized");
    }

    @Test(description = "GET /api/users/me with auth but user not found returns 404",
          groups = {"unit", "controller"},
          priority = 2)
    public void me_whenUserNotFound_returns404() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("123");
        when(userRepository.findById(123L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = userController.me(auth);
        assertEquals(resp.getStatusCode().value(), 404);
        assertNull(resp.getBody());
    }

    @Test(description = "GET /api/users/me with valid auth returns user and 200",
          groups = {"unit", "controller"},
          priority = 3)
    public void me_whenUserFound_returns200AndUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("7");

        User u = new User();
        u.setId(7L);
        u.setUsername("tester");
        u.setEmail("t@example.com");

        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        ResponseEntity<?> resp = userController.me(auth);
        assertEquals(resp.getStatusCode().value(), 200);
        assertSame(resp.getBody(), u);
    }
}
