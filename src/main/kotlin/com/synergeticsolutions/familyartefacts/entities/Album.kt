package com.synergeticsolutions.familyartefacts.entities

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.synergeticsolutions.familyartefacts.serializers.ArtifactReferenceCollectionSerializer
import com.synergeticsolutions.familyartefacts.serializers.GroupReferenceCollectionSerializer
import com.synergeticsolutions.familyartefacts.serializers.UserReferenceCollectionSerializer
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
 * Representation of the album entity.
 *
 * @param id ID of the album
 * @param name Name of the album
 * @param description Description of the album
 * @param owners [User] entities that can make modifications to the album
 * @param groups [Group] entities the album is associated with
 * @param sharedWith [User] entities the album is shared with
 * @param artifacts [Artifact] entities that make up the album
 */
@Entity
@Table(name = "albums")
data class Album(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @Lob
    val description: String,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "ownedAlbums")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val owners: MutableList<User>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "albums")
    @JsonSerialize(using = GroupReferenceCollectionSerializer::class)
    val groups: MutableList<Group> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "sharedAlbums")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val sharedWith: MutableList<User> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "albums")
    @JsonSerialize(using = ArtifactReferenceCollectionSerializer::class)
    val artifacts: MutableList<Artifact> = mutableListOf()

) {
    override fun toString(): String {
        return listOf(
                "id=$id",
                "name=$name",
                "description=$description",
                "owners=${owners.map(User::id)}",
                "groups=${groups.map(Group::id)}",
                "sharedWith=${sharedWith.map(User::id)}",
                "artifacts=${artifacts.map(Artifact::id)}"
        ).joinToString(separator = ", ", prefix = "Album(", postfix = ")")
    }
}
