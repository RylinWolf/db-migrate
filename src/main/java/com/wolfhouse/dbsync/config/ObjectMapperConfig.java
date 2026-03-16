package com.wolfhouse.dbsync.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 对象映射器配置
 *
 * @author Rylin Wolf
 */
@Configuration
public class ObjectMapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new JavaTimeModule().addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE)
                                                  .addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE)
                                                  .addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE)
                                                  .addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE)
                                                  .addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE)
                                                  .addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE);

        return JsonMapper.builder()
                         .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                         .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                         .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                         .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                         .addModule(module)
                         .build();
    }
}
