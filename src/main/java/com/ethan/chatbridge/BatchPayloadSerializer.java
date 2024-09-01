package com.ethan.chatbridge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class BatchPayloadSerializer extends JsonSerializer<BatchPayload> {
    @Override
    public void serialize(BatchPayload batchPayload, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        for (BatchPayload.RequestPayload requestPayload : batchPayload.getRequests()) {
            jsonGenerator.writeObject(requestPayload);
            jsonGenerator.writeRaw('\n');
        }
    }
}
