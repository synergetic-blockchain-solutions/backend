package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

/**
 * The [User] entity is a representation of registered users.
 *
 * @param [id] Primary key used to identify the user. This is automatically set so when creating a new user this should
 *      be set to 0 (the default).
 * @param [name] Name of the user.
 * @param [email] Email user to identify the user. This field is unique.
 * @param [password] Hashed password used to authenticate the user. This field will not be serialised to JSON.
 */
@Entity
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @Column(unique = true)
    val email: String,
    @field:JsonIgnore
    val password: String
)
