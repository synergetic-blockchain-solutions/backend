package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import javax.persistence.CascadeType
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Table
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption

@Entity
@Table(name = "artifacts")
data class Artifact(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    val description: String,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "ownedArtifacts")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val owners: MutableList<User>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "artifacts")
    @JsonSerialize(using = GroupReferenceCollectionSerializer::class)
    val groups: MutableList<Group>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "sharedArtifacts")
    @JsonSerialize(using = UserReferenceCollectionSerializer::class)
    val sharedWith: MutableList<User> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @OneToMany(cascade = [CascadeType.ALL])
    @JsonSerialize(using = ArtifactResourceReferenceCollectionSerializer::class)
    val resources: MutableList<ArtifactResource> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonSerialize(using = AlbumReferenceCollectionSerializer::class)
    val albums: MutableList<Album> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ElementCollection
    val tags: MutableList<String> = mutableListOf()
) {
    override fun toString(): String {
        return listOf(
            "id=$id",
            "name=$name",
            "description=$description",
            "owners=${owners.map(User::id)}",
            "groups=${groups.map(Group::id)}",
            "sharedwith=${sharedWith.map(User::id)}",
            "tags=$tags"
        ).joinToString(separator = ", ", prefix = "Artifact(", postfix = ")")
    }
}
