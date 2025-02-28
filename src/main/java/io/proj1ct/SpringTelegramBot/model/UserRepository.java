package io.proj1ct.SpringTelegramBot.model;

import org.springframework.data.repository.CrudRepository;
//нужен для работы с сущностью User
public interface UserRepository extends CrudRepository<User, Long>{ //этот интерфейс расширяет этот класс
    //Этот файл UserRepository.java нужен, чтобы удобно работать с базой данных, где хранятся пользователи бота.
    //Что он делает?
    //Он помогает сохранять, искать, удалять и обновлять пользователей в базе, без написания сложного SQL-кода.
}