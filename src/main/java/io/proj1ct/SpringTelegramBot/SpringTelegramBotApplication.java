//этот файл - точка входа в программу
package io.proj1ct.SpringTelegramBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringTelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringTelegramBotApplication.class, args);//Запускает приложение, загружает все конфигурации и компоненты.
	}

}
