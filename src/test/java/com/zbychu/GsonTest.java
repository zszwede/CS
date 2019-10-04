package com.zbychu;

import com.google.gson.JsonParseException;
import com.zbychu.common.LogEntryObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GsonTest {

    @Test
    public void gsonTest() throws IOException {
        String s = "{\"id\":\"B\",\"host\":\"A\",\"timestamp\":123456,\"state\":\"STARTED\",\"type\":\"T\"}";
        Assert.assertNotNull(LogEntryObject.gson.fromJson(s));
    }

    @Test(expected = JsonParseException.class)
    public void gsonLackTimestampTest() throws IOException {
        String s = "{\"id\":\"B\",\"host\":\"A\",\"state\":\"STARTED\",\"type\":\"T\"}";
        LogEntryObject.gson.fromJson(s);
    }

    @Test(expected = JsonParseException.class)
    public void gsonLackIdTest() throws IOException {
        String s = "{\"host\":\"A\",\"timestamp\":123456,\"state\":\"STARTED\",\"type\":\"T\"}";
        LogEntryObject.gson.fromJson(s);
    }

    @Test(expected = JsonParseException.class)
    public void gsonLackStateTest() throws IOException {
        String s = "{\"id\":\"B\",\"host\":\"A\",\"timestamp\":123456,\"type\":\"T\"}";
        LogEntryObject.gson.fromJson(s);
    }
}