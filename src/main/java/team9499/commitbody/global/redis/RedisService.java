package team9499.commitbody.global.redis;

import java.time.Duration;

public interface RedisService {

    void setValue(String key, String value);
    void setValues(String key, String value, Duration duration);
    String getValue(String key);
    void deleteValue(String key);

}
