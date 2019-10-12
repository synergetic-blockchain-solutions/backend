package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import org.hibernate.annotations.LazyToOne
import org.hibernate.annotations.LazyToOneOption

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
    @JsonSerialize(using = GroupReferenceCollectionSerializer::class)
    val groups: MutableList<Group> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = ArtifactReferenceCollectionSerializer::class)
    val sharedArtifacts: MutableList<Artifact> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = ArtifactReferenceCollectionSerializer::class)
    val ownedArtifacts: MutableList<Artifact> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = AlbumReferenceCollectionSerializer::class)
    val sharedAlbums: MutableList<Album> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = AlbumReferenceCollectionSerializer::class)
    val ownedAlbums: MutableList<Album> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = GroupReferenceCollectionSerializer::class)
    val ownedGroups: MutableList<Group> = mutableListOf(),
    @OneToOne
    @LazyToOne(value = LazyToOneOption.FALSE)
    val privateGroup: Group

) {
    override fun toString(): String {
        return listOf(
                "id=$id",
                "name=$name",
                "email=$email",
                "password=[Secured]",
                "groups=${groups.map(Group::id)}",
                "sharedArtifacts=${sharedArtifacts.map(Artifact::id)}",
                "ownedArtifacts=${ownedArtifacts.map(Artifact::id)}",
                "privateGroup=${privateGroup.id}"
        ).joinToString(separator = ", ", prefix = "User(", postfix = ")")
    }
}
