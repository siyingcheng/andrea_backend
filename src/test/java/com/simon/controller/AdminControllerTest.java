package com.simon.controller;

import com.simon.model.User;
import com.simon.repository.UserRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    private AdminController adminController;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        adminController = new AdminController(userRepository);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test(description = "List users returns paged result",
            groups = {"unit", "controller"},
            priority = 1)
    public void list_returnsPagedUsers() {
        User u1 = new User().setId(1L).setUsername("a");
        User u2 = new User().setId(2L).setUsername("b");
        PageImpl<User> page = new PageImpl<>(List.of(u1, u2));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> resp = adminController.list(0, 10);
        assertEquals(resp.getStatusCode().value(), 200);
        Object body = resp.getBody();
        assertNotNull(body);
        assertTrue(body instanceof org.springframework.data.domain.Page);
        org.springframework.data.domain.Page<?> p = (org.springframework.data.domain.Page<?>) body;
        assertEquals(p.getTotalElements(), 2);
        assertEquals(p.getContent().size(), 2);
    }

    @Test(description = "Get user by id returns 200 when found",
            groups = {"unit", "controller"},
            priority = 2)
    public void get_whenFound_returns200AndUser() {
        User u = new User().setId(5L).setUsername("user5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        ResponseEntity<?> resp = adminController.get(5L);
        assertEquals(resp.getStatusCode().value(), 200);
        assertSame(resp.getBody(), u);
    }

    @Test(description = "Get user by id returns 404 when not found",
            groups = {"unit", "controller"},
            priority = 3)
    public void get_whenNotFound_returns404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = adminController.get(99L);
        assertEquals(resp.getStatusCode().value(), 404);
        assertNull(resp.getBody());
    }
}

