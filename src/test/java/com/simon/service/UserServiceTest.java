package com.simon.service;

import com.simon.dto.AuthRequests;
import com.simon.model.User;
import com.simon.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private AutoCloseable mocks;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        // instantiate service with mocked repository so mocks are used
        userService = new UserService(userRepository, passwordEncoder);
        // Ensure the service uses a matching password encoder; UserService creates its own encoder
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test(description = "Register a new user and persist it to the repository",
            groups = {"unit", "service"},
            priority = 1)
    public void registerNewUser_persistsToRepository() {
        AuthRequests.RegisterRequest req = new AuthRequests.RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(req);

        verify(userRepository, times(1)).save(captor.capture());
        User u = captor.getValue();
        assertEquals(u.getUsername(), "alice");
        assertEquals(u.getEmail(), "alice@example.com");
        assertNotNull(u.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", u.getPasswordHash()));
        assertNotNull(u.getCreatedAt());
        assertNotNull(u.getUpdatedAt());
        assertTrue(u.getIsActive());

        // returned object should be same as saved
        assertEquals(saved.getUsername(), u.getUsername());
    }

    @Test(description = "Find a user by username when the user exists",
            groups = {"unit", "service"},
            priority = 2)
    public void findByUsername_whenExists_returnsUser() {
        User u = new User();
        u.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(u));

        User res = userService.findByUsername("bob");
        assertNotNull(res);
        assertEquals(res.getUsername(), "bob");
    }

    @Test(description = "Return null when finding a username that does not exist",
            groups = {"unit", "service"},
            priority = 3)
    public void findByUsername_whenNotExists_returnsNull() {
        when(userRepository.findByUsername("nofound")).thenReturn(Optional.empty());

        User res = userService.findByUsername("nofound");
        assertNull(res);
    }

    @Test(description = "Verify password checking using BCrypt password encoder",
            groups = {"unit", "service"},
            priority = 4)
    public void checkPassword_withCorrectAndIncorrectPasswords() {
        User u = new User();
        u.setPasswordHash(passwordEncoder.encode("mypw"));

        assertTrue(userService.checkPassword(u, "mypw"));
        assertFalse(userService.checkPassword(u, "wrong"));
    }

    @Test(description = "Update a user's password and ensure updatedAt changes and hash is updated",
            groups = {"unit", "service"},
            priority = 5)
    public void updatePassword_updatesHashAndTimestamp() {
        User u = new User();
        u.setPasswordHash(new BCryptPasswordEncoder(12).encode("old"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updatePassword(u, "newpass");
        assertTrue(passwordEncoder.matches("newpass", updated.getPasswordHash()));
        assertNotNull(updated.getUpdatedAt());
    }
}
