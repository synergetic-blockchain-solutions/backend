package com.synergeticsolutions.familyartefacts.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.synergeticsolutions.familyartefacts.serializers.AlbumReferenceCollectionSerializer
import com.synergeticsolutions.familyartefacts.serializers.ArtifactReferenceCollectionSerializer
import com.synergeticsolutions.familyartefacts.serializers.GroupReferenceCollectionSerializer
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
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
 * @param [groups] Collection of [Group]s the user is a member of.
 * @param [ownedArtifacts] Collection of [Artifact]s the user owns
 * @param [sharedArtifacts] [Artifact]s shared with the user
 * @param [ownedAlbums] [Album]s the user owns and can edit
 * @param [sharedAlbums] [Album]s shared with the user
 * @param [ownedGroups] [Group]s the user is an admin of can can edit
 * @param [privateGroup] [Group] private to the user
 * @param [contentType] content type of the user's image
 * @param [image] User's profile picture
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
    val privateGroup: Group,

    val contentType: String? = null,
    @JsonIgnore
    @Lob
    val image: ByteArray = byteArrayOf()

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (groups != other.groups) return false
        if (sharedArtifacts != other.sharedArtifacts) return false
        if (ownedArtifacts != other.ownedArtifacts) return false
        if (ownedGroups != other.ownedGroups) return false
        if (privateGroup != other.privateGroup) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + groups.hashCode()
        result = 31 * result + sharedArtifacts.hashCode()
        result = 31 * result + ownedArtifacts.hashCode()
        result = 31 * result + ownedGroups.hashCode()
        result = 31 * result + privateGroup.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
