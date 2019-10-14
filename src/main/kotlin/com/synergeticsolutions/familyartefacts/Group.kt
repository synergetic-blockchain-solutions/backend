package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
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
    val description: String,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "groups")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val members: MutableList<User> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "ownedGroups")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val admins: MutableList<User> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = ArtifactReferenceCollectionSerializer::class)
    val artifacts: MutableList<Artifact> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany
    @JsonSerialize(using = AlbumReferenceCollectionSerializer::class)
    val albums: MutableList<Album> = mutableListOf(),
    val contentType: String = "",
    @JsonIgnore
    @Lob
    val image: ByteArray = byteArrayOf()
) {
    override fun toString(): String {
        return listOf(
            "id=$id",
            "name=$name",
            "description=$description",
            "members=${members.map(User::id)}",
            "admins=${admins.map(User::id)}",
            "artifacts=${artifacts.map(Artifact::id)}",
            "contentType=$contentType"
        ).joinToString(separator = ", ", prefix = "Group(", postfix = ")")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (members != other.members) return false
        if (admins != other.admins) return false
        if (artifacts != other.artifacts) return false
        if (albums != other.albums) return false
        if (contentType != other.contentType) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + members.hashCode()
        result = 31 * result + admins.hashCode()
        result = 31 * result + artifacts.hashCode()
        result = 31 * result + albums.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
