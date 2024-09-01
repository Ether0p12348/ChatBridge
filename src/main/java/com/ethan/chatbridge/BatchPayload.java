package com.ethan.chatbridge;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BatchPayload {
    @JsonProperty("custom_id")
    private String id;
    @JsonProperty("method")
    private String method = "POST";
}
