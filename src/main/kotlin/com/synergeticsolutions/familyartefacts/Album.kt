package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javax.persistence.CascadeType
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
data class Album(
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
    val groups: MutableList<Group> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "sharedArtifacts")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val sharedWith: MutableList<User> = mutableListOf(),
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @OneToMany(cascade = [CascadeType.ALL])
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val artifacts: MutableList<Artifact> = mutableListOf()

)
