package ru.practicum.service;

import ru.practicum.dto.NewUserRequestDto;
import ru.practicum.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto addUser(NewUserRequestDto newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);
}
