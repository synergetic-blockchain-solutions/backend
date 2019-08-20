package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Entity
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @Column(unique = true)
    val email: String,
    val password: String
)
