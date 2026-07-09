package org.example.mydailymemoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class MyDailyMemoAppApplication {

    public static void main(String[] args) throws Exception {
        //鍵がなければ生成して.vapid-keys.propertiesに書き込む
        var keys = VapidKeys.loadOrGenerate(Path.of("vapid-keys.properties"));
        System.setProperty("vapid.public.key", keys.publicKey());
        System.setProperty("vapid.private.key", keys.privateKey());
        SpringApplication.run(MyDailyMemoAppApplication.class, args);
    }

}
