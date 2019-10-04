package com.zbychu;

import com.google.gson.Gson;
import com.zbychu.common.LogEntryObject;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MessageGenerator {

    @Test
    public void produce(){
        produceMessages("app.log", 1000000);
    }


    public static void produceMessages(String filename, int number){
        Gson gson = new Gson();
        List<String> states = Arrays.asList("STARTED", "FINISHED");
        ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String hostname = UUID.randomUUID().toString().replace("-","").substring(0,10);
            String id = UUID.randomUUID().toString().replace("-","").substring(0,20);
            Collections.shuffle(states);
            boolean isApplicationEvent = new Random().nextBoolean();
            for (String state : states) {
                LogEntryObject lo = new LogEntryObject();
                lo.setState(state);
                lo.setId(id);
                lo.setTimestamp(new Date().getTime() + (
                                state.equals("FINISHED") ? ThreadLocalRandom.current().nextInt(1, 8)  : 0
                        )
                );
                if(isApplicationEvent){
                    lo.setType("APPLICATION_LOG");
                    lo.setHost(hostname);
                }
                lines.add(gson.toJson(lo));
                if(lines.size() % 100000 == 0) {
                    writeLines(lines, filename);
                    lines = new ArrayList<>();
                }
            }
        }
        writeLines(lines, filename);
    }

    private static void writeLines(ArrayList<String> lines, String filename){
        Collections.shuffle(lines);
        try {
            FileUtils.writeLines(new File(filename), lines, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}