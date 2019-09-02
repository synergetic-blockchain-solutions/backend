package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.Table

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
    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER)
    var groups: MutableList<Group> = mutableListOf(),
    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER)
    val sharedArtifacts: MutableList<Artifact> = mutableListOf(),
    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER)
    val ownedArtifacts: MutableList<Artifact> = mutableListOf()
) {
    override fun toString(): String {
        return "User(id=$id, name=$name, email=$email, password=$password, groups=${groups.map(Group::id)}, sharedArtifacts=${sharedArtifacts.map(
            Artifact::id
        )}, ownedArtifacts=${ownedArtifacts.map(Artifact::id)}"
    }
}
