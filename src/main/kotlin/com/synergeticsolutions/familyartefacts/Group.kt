package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.Table

/**
 * [Group] represents a group of users in which artefacts can be shared.
 *
 * @param [id] Unique identifier for the group.
 * @param [name] Group's name. This is how users would identify the group but it does not necessarily uniquely identify it.
 * @param [members] Collection of [User]s who are a member of the group.
 */
@Entity
@Table(name = "groups")
data class Group(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @JsonBackReference
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "groups")
    val members: MutableList<User>,
    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER)
    val artifacts: MutableList<Artifact> = mutableListOf()
) {
    override fun toString(): String {
        return "Group(id=$id, name=$name, members=${members.map(User::id)}, artifacts=${artifacts.map(Artifact::id)}"
    }
}
