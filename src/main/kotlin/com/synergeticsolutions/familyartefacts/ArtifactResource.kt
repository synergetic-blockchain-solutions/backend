package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.ManyToOne
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption

@Entity
data class ArtifactResource(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    val description: String,
    val contentType: String,
    // @JsonIgnore
    // val path: String,
    @JsonIgnore
    @Lob
    val resource: ByteArray,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    val artifact: Artifact,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ElementCollection
    val tags: MutableList<String> = mutableListOf()
) {
    override fun toString(): String {
        return listOf(
            "id=$id",
            "name=$name",
            "description=$description",
            "contentType=$contentType",
            "artifact=${artifact.id}",
            "tags=$tags"
        ).joinToString(separator = ", ", prefix = "ArtifactResource(", postfix = ")")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArtifactResource

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (contentType != other.contentType) return false
        if (!resource.contentEquals(other.resource)) return false
        if (artifact != other.artifact) return false
        if (tags != other.tags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + resource.contentHashCode()
        result = 31 * result + artifact.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }
}
