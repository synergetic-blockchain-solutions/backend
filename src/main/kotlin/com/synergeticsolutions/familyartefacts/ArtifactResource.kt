package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption

@Entity
data class ArtifactResource(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val contentType: String,
    @JsonIgnore
    val path: String,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val artifact: Artifact
) {
    override fun toString(): String {
        return listOf(
            "id=$id",
            "contentType=$contentType",
            "path=$path",
            "artifact=${artifact.id}"
        ).joinToString(separator = ", ", prefix = "ArtifactResource(", postfix = ")")
    }
}
