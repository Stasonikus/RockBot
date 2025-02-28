package io.proj1ct.SpringTelegramBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj1ct.SpringTelegramBot.config.BotConfig;
import io.proj1ct.SpringTelegramBot.model.User;
import io.proj1ct.SpringTelegramBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    static final String HELP_TEXT = "Привет! Вот как ты можешь использовать этого бота:\n" +
            "\n" +
            "/start — Начать работу с ботом.\n" +
            "/admin — Получить права админа. \n" +
            "/mydata — Узнать, какие данные о тебе хранятся.\n" +
            "/deletedata — Удалить свои данные из системы.\n" +
            "/settings — Настроить свои предпочтения (уведомления, напоминания и т. д.).\n" +
            "/help — Получить это сообщение с описанием возможностей бота.\n" +
            "Если у тебя есть вопросы, напиши в ответ на это сообщение! \uD83D\uDE0A";

    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/help", "Информация по работе с ботом"));

        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка установки списка команд бота: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            if (!messageText.isEmpty()) {
                switch (messageText) {
                    case "/start":                                  //комада для начала работы тг бота
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        break;
                    case "/admin":
                        grantAdminAccess(update.getMessage());
                        break;
                    default:
                        sendMessage(chatId, "Извините, такой команды нет!");
                }
            } else {
                log.warn("Пользователь {} отправил пустое сообщение", chatId);
            }
        }
    }

    private void sendHelpMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(HELP_TEXT);

        // Кнопка для входа в админ-режим
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Войти как администратор");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки /help: {}", e.getMessage());
        }
    }


    //Метот базовой регистрации юзера без админ возможностей
    private void registerUser(Message msg) {
        long chatId = msg.getChatId();

        if (userRepository.findById(chatId).isEmpty()) {
            User user = new User();
            user.setChatId(chatId);
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            user.setAdmin(false); // Всегда НЕ АДМИН при регистрации

            userRepository.save(user);
            log.info("Пользователь зарегистрирован: {}", user);
        }
    }

    private void grantAdminAccess(Message msg) {
        long chatId = msg.getChatId();

        userRepository.findById(chatId).ifPresent(user -> {
            user.setFirstName(msg.getChat().getFirstName());
            user.setLastName(msg.getChat().getLastName());
            user.setUserName(msg.getChat().getUserName());
            user.setAdmin(true);
            userRepository.save(user);

            sendMessage(chatId, "Вы получили права администратора!");
            log.info("Пользователь {} теперь администратор", chatId);
        });
    }




    // Метод для проверки, является ли пользователь администратором
    private boolean checkIfAdmin(long chatId) {
        List<Long> adminIds = List.of(610615962L, 1133733294L); // Список ID админов
        return adminIds.contains(chatId);
    }


    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Шалом, " + name + " мир и благодать вам!" + ":blush:");
        //String answer = "Шалом, " + name + " мир и благодать вам!"; //это старный способ, без подклбчения эмодзи
        log.info("Ответ пользователю {} отправлен", name);
        sendMessage(chatId, answer);
    }


    //Система для безопасной отправки сообщения, сделано для того что бы бот в случае превышения лемита в 4096 символов разделил длинное сообщение
    //А если ошибка вернуться еще раз, то тогда просто подождать время лимита отправки сообщения
    private void sendMessage(long chatId, String textToSend) {
        // 1. Создаём разметку клавиатуры
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        // Создаём первую строку кнопок
        KeyboardRow row = new KeyboardRow();
        row.add("start");
        row.add("get random joke");
        keyboardRows.add(row);

        // Создаём вторую строку кнопок
        row = new KeyboardRow();
        row.add("help");                                        //тут я создал кнопки для бота
        row.add("/help");
        row.add("/start");
        keyboardRows.add(row);

        // Привязываем кнопки к клавиатуре
        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        // 2. Создаём объект SendMessage

        //

        // 3. Отправляем сообщение
        try {
            execute(message);
        } catch (TelegramApiRequestException e) {
            // Обрабатываем ошибку 429 (Too Many Requests)
            if (e.getErrorCode() == 429) {
                int retryAfter = e.getParameters().getRetryAfter();
                log.warn("Превышен лимит запросов. Повтор через {} секунд.", retryAfter);
                try {
                    Thread.sleep(retryAfter * 1000L);
                    execute(message);
                } catch (InterruptedException | TelegramApiException ex) {
                    log.error("Ошибка повторной отправки: {}", ex.getMessage());
                }
            } else {
                // Логируем все остальные ошибки
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
        } catch (TelegramApiException e) {
            // Обрабатываем любые другие ошибки Telegram API
            log.error("Ошибка Telegram API: {}", e.getMessage());
        }
    }


    private List<String> splitMessage(String message) { //вот тут как раз ограничение на кол-во символов
        List<String> result = new ArrayList<>();
        while (message.length() > 4096) {
            result.add(message.substring(0, 4096));
            message = message.substring(4096);
        }
        result.add(message);
        return result;
    }
}
