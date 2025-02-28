//Этот файл отвечает за регистрацию и инициализацию бота.
package io.proj1ct.SpringTelegramBot.config;

import io.proj1ct.SpringTelegramBot.service.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class BotInitializer {

    @Autowired
    TelegramBot bot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException{
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try{
            telegramBotsApi.registerBot(bot); //Регистрирует бот в Telegram API, используя объект bot.
        }

        catch (TelegramApiException e){
        //Обрабатывает исключения, если регистрация не удалась.
            log.error("Registration error: " + e.getMessage());
        }
    }

}
