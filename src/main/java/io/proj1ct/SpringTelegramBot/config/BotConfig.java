//Этот файл отвечает за хранение и загрузку конфигураций для бота.
package io.proj1ct.SpringTelegramBot.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration //Указывает, что это класс конфигурации.
@Data //Аннотация Lombok, которая автоматически добавляет геттеры, сеттеры, toString, equals, hashCode к полям.
@PropertySource("application.properties") //Говорит Spring загружать значения из файла application.properties.
public class BotConfig {

    @Value("${bot.name}") //Загружает значение из файла application.properties с ключом bot.name в переменную botName.
    String botName;

    @Value("${bot.token}")
    String token;


}
