package com.pjs.web.account.dto;

import com.pjs.web.account.entity.Account;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.stream.Collectors;

public class AccountSerializer extends JsonSerializer<Account> {

    @Override
    public void serialize(Account value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("username", value.getUsername());
        gen.writeStringField("joinDate", value.getJoinDate().toString());
        gen.writeStringField("nickname", value.getNickname());
        gen.writeEndObject();
    }
}
