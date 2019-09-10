package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.Table
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption

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
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "groups")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val members: MutableList<User>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val artifacts: MutableList<Artifact> = mutableListOf()
) {
    override fun toString(): String {
        return "Group(id=$id, name=$name, members=${members.map(User::id)}, artifacts=${artifacts.map(Artifact::id)}"
    }
}
