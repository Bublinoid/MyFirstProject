package com.example.manicurebot.Telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramBotUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdateScheduler.class);

    private final MyTelegramBotApi botApi;

    @Autowired
    public TelegramBotUpdateScheduler(MyTelegramBotApi botApi) {
        this.botApi = botApi;
    }

    @Scheduled(fixedDelay = 1000)
    public void processUpdates() {
        logger.info("Processing updates...");
        botApi.processUpdates();
    }
}
