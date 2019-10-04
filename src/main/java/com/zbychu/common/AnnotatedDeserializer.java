package com.zbychu.common;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

class AnnotatedDeserializer<T> implements JsonDeserializer<T>
{
    private final Logger logger = LoggerFactory.getLogger(AnnotatedDeserializer.class);
    public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException
    {
        T pojo = new Gson().fromJson(je, type);
        Field[] fields = pojo.getClass().getDeclaredFields();
        for (Field f : fields)
        {
            if (f.getAnnotation(LogEntryObject.JsonRequired.class) != null)
            {
                try
                {
                    f.setAccessible(true);
                    if (f.get(pojo) == null)
                    {
                        throw new JsonParseException("Missing field in JSON: " + f.getName());
                    }
                }
                catch (IllegalArgumentException ex)
                {
                    logger.error("Exception during JSON deserialization", ex);
                }
                catch (IllegalAccessException ex)
                {
                    logger.error("Exception during JSON deserialization", ex);
                }
            }
        }
        return pojo;
    }
}