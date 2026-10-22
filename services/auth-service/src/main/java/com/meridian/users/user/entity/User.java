package com.meridian.users.user.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Usuario persistido con email normalizado por el servicio antes de acceder al repositorio.
 */
@Document(collection = "users")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Instant createdAt;

    private Instant updatedAt;
}
