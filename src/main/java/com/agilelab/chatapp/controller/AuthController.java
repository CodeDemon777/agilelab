package com.agilelab.chatapp.controller;

import com.agilelab.chatapp.dto.LoginRequest;
import com.agilelab.chatapp.dto.RegisterRequest;
import com.agilelab.chatapp.dto.UserDto;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/signup")
    public String handleFormSignup(@ModelAttribute("registerRequest") @Valid RegisterRequest request,
                                   BindingResult bindingResult,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "signup";
        }

        try {
            userService.register(request);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "signup";
        }
    }

    @ResponseBody
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> registerApi(@RequestBody @Valid RegisterRequest request) {
        try {
            User user = userService.register(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User registered successfully",
                    "user", new UserDto(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @ResponseBody
    @PostMapping("/api/auth/login")
    public ResponseEntity<?> loginApi(@RequestBody LoginRequest loginRequest,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);

            User user = userService.findByUsername(authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful",
                    "user", new UserDto(user)
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Invalid username or password"
            ));
        }
    }

    @ResponseBody
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logoutApi(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    @ResponseBody
    @GetMapping("/api/auth/current")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.ok(new UserDto(user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
    }
}
