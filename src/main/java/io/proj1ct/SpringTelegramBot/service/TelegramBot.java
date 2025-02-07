package io.proj1ct.SpringTelegramBot.service;

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
                    default:
                        sendMessage(chatId, "Извините, такой команды нет!");
                }
            } else {
                log.warn("Пользователь {} отправил пустое сообщение", chatId);
            }
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("Пользователь сохранён: {}", user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Шалом, " + name + " мир и благодать вам!";
        log.info("Ответ пользователю {} отправлен", name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        List<String> messageParts = splitMessage(textToSend);

        for (String part : messageParts) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(part);

            try {
                execute(message);
            } catch (TelegramApiRequestException e) {
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
                    log.error("Ошибка отправки сообщения: {}", e.getMessage());
                }
            } catch (TelegramApiException e) {
                log.error("Ошибка Telegram API: {}", e.getMessage());
            }
        }
    }

    private List<String> splitMessage(String message) {
        List<String> result = new ArrayList<>();
        while (message.length() > 4096) {
            result.add(message.substring(0, 4096));
            message = message.substring(4096);
        }
        result.add(message);
        return result;
    }
}
