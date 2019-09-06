package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonBackReference
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table(name = "artifacts")
data class Artifact(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    val description: String,
    @JsonBackReference(value = "ownedArtifacts-owners")
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "ownedArtifacts")
    val owners: MutableList<User>,
    @JsonBackReference(value = "artifacts-groups")
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "artifacts")
    val groups: MutableList<Group>,
    @JsonBackReference(value = "sharedArtifacts-sharedWith")
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "sharedArtifacts")
    val sharedWith: MutableList<User> = mutableListOf()
) {
    override fun toString(): String {
        return "Artifact(id=$id, name=$name, description=$description, owners=${owners.map(User::id)}, groups=${groups.map(
            Group::id
        )}, sharedWith=${sharedWith.map(User::id)}"
    }
}
