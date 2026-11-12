package com.agilelab.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupRequest {
    @NotBlank(message = "Group name is required")
    private String name;

    private List<String> memberUsernames = new ArrayList<>();

    public CreateGroupRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getMemberUsernames() { return memberUsernames; }
    public void setMemberUsernames(List<String> memberUsernames) { this.memberUsernames = memberUsernames; }
}
