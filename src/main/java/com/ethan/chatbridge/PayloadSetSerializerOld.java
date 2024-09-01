package com.ethan.chatbridge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PayloadSetSerializerOld extends JsonSerializer<PayloadSetOld> {

    @Override
    public void serialize(PayloadSetOld payloadSet, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        for (PayloadSetOld.Payload payload : payloadSet.getPayloads()) {
            jsonGenerator.writeObject(payload);
            jsonGenerator.writeRaw('\n');
        }
    }
}
