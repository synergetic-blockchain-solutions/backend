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
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val owners: MutableList<User>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "artifacts")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val groups: MutableList<Group>,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "sharedArtifacts")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val sharedWith: MutableList<User> = mutableListOf()
) {
    override fun toString(): String {
        return "Artifact(id=$id, name=$name, description=$description, owners=${owners.map(User::id)}, groups=${groups.map(
            Group::id
        )}, sharedWith=${sharedWith.map(User::id)}"
    }
}
