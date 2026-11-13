package com.agilelab.chatapp.dto;

import java.util.ArrayList;
import java.util.List;

public class AiAskResponse {
    private String answer;
    private String query;
    private List<CitationDto> citations = new ArrayList<>();
    private String formattedTimestamp;

    public AiAskResponse() {
    }

    public AiAskResponse(String answer, String query, List<CitationDto> citations, String formattedTimestamp) {
        this.answer = answer;
        this.query = query;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.formattedTimestamp = formattedTimestamp;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<CitationDto> getCitations() {
        return citations;
    }

    public void setCitations(List<CitationDto> citations) {
        this.citations = citations;
    }

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }
}

