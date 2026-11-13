package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.ActionItemStatus;

public class UpdateActionItemStatusRequest {
    private ActionItemStatus status;

    public UpdateActionItemStatusRequest() {
    }

    public UpdateActionItemStatusRequest(ActionItemStatus status) {
        this.status = status;
    }

    public ActionItemStatus getStatus() {
        return status;
    }

    public void setStatus(ActionItemStatus status) {
        this.status = status;
    }
}

