package com.zbychu.common;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class LogEntryObject {

    private static final Logger logger = LoggerFactory.getLogger(LogEntryObject.class);
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface JsonRequired
    {
    }

    @JsonRequired private String id;
    @JsonRequired private Long timestamp;
    private String host;
    @JsonRequired private String state;
    private String type;
    public static TypeAdapter<LogEntryObject> gson = new GsonBuilder().registerTypeAdapter(LogEntryObject.class, new AnnotatedDeserializer<LogEntryObject>()).create().getAdapter(LogEntryObject.class);


    public void setId(String id) {
        this.id = id;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public static LogEntryObject fromString(String s){
        try {
            return gson.fromJson(s);
        } catch (JsonParseException e) {
            logger.error(String.format("Unable to parse JSON %s", s), e);
        } catch (IOException e) {
            logger.error(String.format("Error during processing JSON %s", s), e);
        }
        return null;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}