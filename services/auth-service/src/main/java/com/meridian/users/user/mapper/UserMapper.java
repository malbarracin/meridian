package com.meridian.users.user.mapper;

import org.mapstruct.Mapper;

import com.meridian.users.user.dto.UserResponse;
import com.meridian.users.user.entity.User;

/** Convierte el usuario persistido a su representación pública. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
