package com.agilelab.chatapp.controller;

import com.agilelab.chatapp.dto.ChatRoomDto;
import com.agilelab.chatapp.dto.UserDto;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.service.ChatService;
import com.agilelab.chatapp.service.NotificationService;
import com.agilelab.chatapp.service.UserService;
import com.agilelab.chatapp.service.ai.AiService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ViewController {

    private final UserService userService;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final AiService aiService;

    public ViewController(UserService userService, ChatService chatService,
                          NotificationService notificationService, AiService aiService) {
        this.userService = userService;
        this.chatService = chatService;
        this.notificationService = notificationService;
        this.aiService = aiService;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        try {
            return userService.findByUsername(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }

    private void addCommonAttributes(Model model, User currentUser) {
        if (currentUser != null) {
            model.addAttribute("currentUser", new UserDto(currentUser));
            model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getUsername()));
        }
    }

    @GetMapping("/")
    public String landing(Model model) {
        User user = getAuthenticatedUser();
        addCommonAttributes(model, user);
        return "landing";
    }

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout) {
        User user = getAuthenticatedUser();
        if (user != null) {
            return "redirect:/chats";
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been successfully logged out.");
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        User user = getAuthenticatedUser();
        if (user != null) {
            return "redirect:/chats";
        }
        model.addAttribute("registerRequest", new com.agilelab.chatapp.dto.RegisterRequest());
        return "signup";
    }

    @GetMapping("/chats")
    public String chats(Model model, @RequestParam(value = "room", required = false) Long roomId) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        List<ChatRoomDto> rooms = chatService.getUserChatRooms(user.getUsername());
        model.addAttribute("rooms", rooms);
        model.addAttribute("activeRoomId", roomId != null ? roomId : (!rooms.isEmpty() ? rooms.get(0).getId() : null));

        return "chats";
    }

    @GetMapping("/call/{roomId}")
    public String call(Model model,
                       @PathVariable("roomId") String roomId,
                       @RequestParam(value = "target", required = false) String target,
                       @RequestParam(value = "type", required = false, defaultValue = "VIDEO") String callType) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        model.addAttribute("roomId", roomId);
        model.addAttribute("targetUsername", target);
        model.addAttribute("callType", callType.toUpperCase());

        return "call";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        model.addAttribute("notifications", notificationService.getUserNotifications(user.getUsername()));

        return "notifications";
    }

    @GetMapping("/profile")
    public String currentProfile(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        model.addAttribute("profileUser", new UserDto(user));
        model.addAttribute("isSelf", true);

        return "profile";
    }

    @GetMapping("/profile/{username}")
    public String userProfile(@PathVariable("username") String username, Model model) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        try {
            User target = userService.findByUsername(username);
            model.addAttribute("profileUser", new UserDto(target));
            model.addAttribute("isSelf", user.getUsername().equalsIgnoreCase(username));
        } catch (Exception e) {
            return "redirect:/chats";
        }

        return "profile";
    }

    @GetMapping("/ai-workspace")
    public String aiWorkspace(Model model, @RequestParam(value = "room", required = false) Long roomId) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        addCommonAttributes(model, user);
        List<ChatRoomDto> rooms = chatService.getUserChatRooms(user.getUsername());
        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("insights", aiService.getWorkspaceInsights());
        model.addAttribute("actionItems", aiService.getActionItems(roomId, null, null));
        model.addAttribute("meetingSummaries", aiService.getAllMeetingSummaries());

        return "ai-workspace";
    }
}

