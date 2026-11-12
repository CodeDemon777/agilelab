package com.agilelab.chatapp.service;

import com.agilelab.chatapp.dto.ProfileUpdateRequest;
import com.agilelab.chatapp.dto.RegisterRequest;
import com.agilelab.chatapp.dto.UserDto;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    public User register(RegisterRequest req) {
        if (userRepository.existsByUsernameIgnoreCase(req.getUsername())) {
            throw new IllegalArgumentException("Username '" + req.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new IllegalArgumentException("Email '" + req.getEmail() + "' is already in use.");
        }

        User user = new User(
                req.getUsername().trim(),
                req.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(req.getPassword()),
                req.getFullName() != null && !req.getFullName().isBlank() ? req.getFullName().trim() : req.getUsername()
        );
        user.setStatusQuote("Living the dream");
        user.setAvatarUrl(generateDefaultAvatar(user.getUsername()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public User updateProfile(String username, ProfileUpdateRequest req) {
        User user = findByUsername(username);

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new IllegalArgumentException("Email '" + newEmail + "' is already used by another account.");
            }
            user.setEmail(newEmail);
        }
        if (req.getStatusQuote() != null) {
            user.setStatusQuote(req.getStatusQuote().trim());
        }
        if (req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }

        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password does not match.");
            }
            if (req.getNewPassword().length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters.");
            }
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }

        return userRepository.save(user);
    }

    public void setOnlineStatus(String username, boolean online) {
        userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
            user.setOnline(online);
            if (!online) {
                user.setLastSeen(LocalDateTime.now());
            }
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query, String excludeUsername) {
        if (query == null || query.isBlank()) {
            return getAllOtherUsers(excludeUsername);
        }
        return userRepository.searchUsers(query.trim(), excludeUsername).stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllOtherUsers(String excludeUsername) {
        return userRepository.findByUsernameNot(excludeUsername).stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    public static String generateDefaultAvatar(String username) {
        String name = username != null ? username : "User";
        return "https://api.dicebear.com/7.x/bottts/svg?seed=" + name;
    }
}
