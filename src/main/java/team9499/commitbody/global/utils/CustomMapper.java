package team9499.commitbody.global.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import team9499.commitbody.global.Exception.ExceptionStatus;
import team9499.commitbody.global.Exception.ExceptionType;
import team9499.commitbody.global.Exception.ServerException;

@AllArgsConstructor
public class CustomMapper<T> {

    private ObjectMapper objectMapper = new ObjectMapper();

    public CustomMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
    }

    public T to(Object ob, Class<T> clazz) {
        String json = "";
        T t = null;
        try {
            json = objectMapper.writeValueAsString(ob);
            t = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new ServerException(ExceptionStatus.SERVER_ERROR, ExceptionType.SERVER_ERROR);
        }

        return t;
    }
}
