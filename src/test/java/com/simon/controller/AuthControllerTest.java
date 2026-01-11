package com.simon.controller;

import com.simon.dto.AuthRequests;
import com.simon.dto.AuthResponses;
import com.simon.model.RefreshToken;
import com.simon.model.User;
import com.simon.service.TokenService;
import com.simon.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthController authController;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        authController = new AuthController(userService, tokenService);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test(description = "Register returns 409 if username exists", groups = {"unit", "controller"}, priority = 1)
    public void register_whenUsernameExists_returnsConflict() {
        AuthRequests.RegisterRequest req = new AuthRequests.RegisterRequest();
        req.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(new User());

        var resp = authController.register(req);
        assertEquals(resp.getStatusCode().value(), 409);
        assertEquals(resp.getBody(), "Username exists");
    }

    @Test(description = "Register returns 201 when new user", groups = {"unit", "controller"}, priority = 2)
    public void register_whenNewUser_returnsCreated() {
        AuthRequests.RegisterRequest req = new AuthRequests.RegisterRequest();
        req.setUsername("bob");
        req.setEmail("b@example.com");
        req.setPassword("password123");
        when(userService.findByUsername("bob")).thenReturn(null);
        when(userService.register(req)).thenReturn(new User().setUsername("bob"));

        var resp = authController.register(req);
        assertEquals(resp.getStatusCode().value(), 201);
    }

    @Test(description = "Login returns 401 when credentials invalid", groups = {"unit", "controller"}, priority = 3)
    public void login_withInvalidCredentials_returns401() {
        AuthRequests.LoginRequest req = new AuthRequests.LoginRequest();
        req.setUsername("noone");
        req.setPassword("bad");
        when(userService.findByUsername("noone")).thenReturn(null);

        var resp = authController.login(req, response);
        assertEquals(resp.getStatusCode().value(), 401);
        assertEquals(resp.getBody(), "Invalid credentials");
    }

    @Test(description = "Login success sets refresh cookie and returns access token", groups = {"unit", "controller"}, priority = 4)
    public void login_withValidCredentials_setsCookieAndReturnsToken() {
        AuthRequests.LoginRequest req = new AuthRequests.LoginRequest();
        req.setUsername("alice");
        req.setPassword("good");

        User u = new User();
        u.setId(10L);
        u.setUsername("alice");

        when(userService.findByUsername("alice")).thenReturn(u);
        when(userService.checkPassword(u, "good")).thenReturn(true);
        when(tokenService.generateAccessToken(u)).thenReturn("access-token-xyz");
        RefreshToken rt = new RefreshToken().setReplacedByToken("refresh-abc");
        when(tokenService.createRefreshToken(u)).thenReturn(rt);

        var resp = authController.login(req, response);
        assertEquals(resp.getStatusCode().value(), 200);
        assertTrue(resp.getBody() instanceof AuthResponses);
        AuthResponses ar = (AuthResponses) resp.getBody();
        assertEquals(ar.getAccessToken(), "access-token-xyz");
        // verify cookie header was added
        verify(response, times(1)).addHeader(eq("Set-Cookie"), contains("refreshToken=refresh-abc"));
    }

    @Test(description = "Refresh returns 401 when no cookie present", groups = {"unit", "controller"}, priority = 5)
    public void refresh_noCookie_returns401() {
        when(request.getCookies()).thenReturn(null);

        var resp = authController.refresh(request);
        assertEquals(resp.getStatusCode().value(), 401);
        assertEquals(resp.getBody(), "No refresh token");
    }

    @Test(description = "Refresh returns 401 when token invalid", groups = {"unit", "controller"}, priority = 6)
    public void refresh_invalidToken_returns401() {
        Cookie c = new Cookie("refreshToken", "tok");
        when(request.getCookies()).thenReturn(new Cookie[]{c});
        when(tokenService.findByTokenHash("tok")).thenReturn(Optional.empty());

        var resp = authController.refresh(request);
        assertEquals(resp.getStatusCode().value(), 401);
        assertEquals(resp.getBody(), "Invalid refresh token");
    }

    @Test(description = "Refresh success returns new access token", groups = {"unit", "controller"}, priority = 7)
    public void refresh_withValidToken_returnsAccessToken() {
        Cookie c = new Cookie("refreshToken", "tok");
        when(request.getCookies()).thenReturn(new Cookie[]{c});

        User u = new User().setId(5L).setUsername("u5");
        RefreshToken rt = new RefreshToken().setUser(u).setRevoked(false).setExpiresAt(LocalDateTime.now().plusDays(1));
        when(tokenService.findByTokenHash("tok")).thenReturn(Optional.of(rt));
        when(tokenService.generateAccessToken(u)).thenReturn("new-access");
        when(tokenService.getAccessExpiresIn()).thenReturn(900L);

        var resp = authController.refresh(request);
        assertEquals(resp.getStatusCode().value(), 200);
        assertTrue(resp.getBody() instanceof AuthResponses);
        AuthResponses ar = (AuthResponses) resp.getBody();
        assertEquals(ar.getAccessToken(), "new-access");
    }

    @Test(description = "Logout revokes token (if present) and clears cookie", groups = {"unit", "controller"}, priority = 8)
    public void logout_revokesAndClearsCookie() {
        Cookie c = new Cookie("refreshToken", "tok");
        when(request.getCookies()).thenReturn(new Cookie[]{c});
        RefreshToken rt = new RefreshToken();
        when(tokenService.findByTokenHash("tok")).thenReturn(Optional.of(rt));

        var resp = authController.logout(request, response);
        assertEquals(resp.getStatusCode().value(), 204);
        verify(tokenService, times(1)).revoke(eq(rt));
        verify(response, times(1)).addHeader(eq("Set-Cookie"), contains("refreshToken="));
        verify(response, times(1)).addHeader(eq("Set-Cookie"), contains("Max-Age=0"));
    }
}

