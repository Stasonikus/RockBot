package io.proj1ct.SpringTelegramBot.service;

import io.proj1ct.SpringTelegramBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
// Помечаем класс аннотацией @Component для его автоматического создания Spring'ом
@Component
public class TelegramBot extends TelegramLongPollingBot {

    // Поле для хранения конфигурации бота (имя и токен)
    final BotConfig config;

    static final String HELP_TEXT = "Привет! Вот как ты можешь использовать этого бота:\n" +
            "\n" +
            "/start — Получить приветственное сообщение и начать работу с ботом.\n" +
            "/mydata — Узнать, какие данные о тебе хранятся.\n" +
            "/deletedata — Удалить свои данные из системы.\n" +
            "/settings — Настроить свои предпочтения (уведомления, напоминания и т. д.).\n" +
            "/help — Получить это сообщение с описанием возможностей бота.\n" +
            "Если у тебя есть вопросы или нужна помощь, напиши в ответ на это сообщение! \uD83D\uDE0A \n\n" +
            "P.S: Пока из рабочих команд только /help и /start";

    // Конструктор класса TelegramBot, принимающий объект конфигурации
    public TelegramBot(BotConfig config) {
        this.config = config;


        //Список команд для бота
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        //listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        //listofCommands.add(new BotCommand("/deletedata", "delete your data"));
        listofCommands.add(new BotCommand("/help" , "info how to use this bot"));
        //listofCommands.add(new BotCommand("/settings", "set your preferences"));

        try{
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    // Возвращаем имя бота из конфигурации
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    // Возвращаем токен бота из конфигурации
    @Override
    public String getBotToken() {
        return config.getToken();
    }

    // Обработка входящих сообщений
    @Override
    public void onUpdateReceived(Update update) {

        // Проверяем, что обновление содержит сообщение и что это текст
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText(); // Текст сообщения
            long chatId = update.getMessage().getChatId(); // ID чата, откуда пришло сообщение

            // Обрабатываем команды по тексту сообщения
            switch (messageText) {
                case "/start":
                    // Если команда "/start", вызываем метод обработки команды
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                default:
                    // Если команда не распознана, отправляем сообщение об ошибке
                    sendMessage(chatId, "Sorry, not command!");
            }
        }
    }

    // Метод обработки команды "/start"
    private void startCommandReceived(long chatId, String name) {

        // Создаем приветственное сообщение с использованием имени пользователя
        String answer = "Шалом, " + name + " мир и благодать вам!";
        log.info("Replied to user " + name);

        // Отправляем сообщение в чат
        sendMessage(chatId, answer);
    }

    // Метод для отправки сообщений в указанный чат
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage(); // Создаем объект для отправки сообщения
        message.setChatId(String.valueOf(chatId)); // Указываем ID чата
        message.setText(textToSend); // Указываем текст сообщения

        try {
            execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            // Обработка ошибок при отправке
            log.error("Error occured: " + e.getMessage());
        }
    }
}
