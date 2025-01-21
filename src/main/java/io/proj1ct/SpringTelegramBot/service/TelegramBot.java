package io.proj1ct.SpringTelegramBot.service;

import io.proj1ct.SpringTelegramBot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

// Помечаем класс аннотацией @Component для его автоматического создания Spring'ом
@Component
public class TelegramBot extends TelegramLongPollingBot {

    // Поле для хранения конфигурации бота (имя и токен)
    final BotConfig config;

    // Конструктор класса TelegramBot, принимающий объект конфигурации
    public TelegramBot(BotConfig config) {
        this.config = config;
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
                default:
                    // Если команда не распознана, отправляем сообщение об ошибке
                    sendMessage(chatId, "Sorry, not command!");
            }
        }
    }

    // Метод обработки команды "/start"
    private void startCommandReceived(long chatId, String name) {

        // Создаем приветственное сообщение с использованием имени пользователя
        String answer = "Hi, " + name + " nice to meet you!";

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
        }
    }
}
