package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.*
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import org.hibernate.annotations.LazyToOne
import org.hibernate.annotations.LazyToOneOption
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import javax.persistence.*

/**
 * The [User] entity is a representation of registered users.
 *
 * @param [id] Primary key used to identify the user. This is automatically set so when creating a new user this should
 *      be set to 0 (the default).
 * @param [name] Name of the user.
 * @param [email] Email user to identify the user. This field is unique.
 * @param [password] Hashed password used to authenticate the user. This field will not be serialised to JSON.
 * @param [groups] Collection of userGroups the user is a member of.
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @Column(unique = true)
    val email: String,
    @field:JsonIgnore
    val password: String,
    @ManyToMany
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    var groups: MutableList<Group> = mutableListOf(),
    @ManyToMany
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    var ownedGroups: MutableList<Group> = mutableListOf(),
    @OneToOne
    @LazyToOne(value = LazyToOneOption.FALSE)
    val privateGroup: Group
) {
    override fun toString(): String {
        return "User $id"
    }
}
