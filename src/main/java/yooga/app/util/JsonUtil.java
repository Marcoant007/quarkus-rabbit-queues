package yooga.app.util;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class JsonUtil {

    @Inject
    ObjectMapper objectMapper;

    public <T> T toObject(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String fromObject(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
