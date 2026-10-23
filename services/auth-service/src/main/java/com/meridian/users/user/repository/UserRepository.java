package com.meridian.users.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.meridian.users.user.entity.User;

/**
 * Persistencia de usuarios en MongoDB.
 */
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Busca un usuario por su email previamente normalizado (trim y minúsculas).
     *
     * @param email email normalizado
     * @return usuario encontrado o un resultado vacío si no existe
     */
    Optional<User> findByEmail(String email);
}
