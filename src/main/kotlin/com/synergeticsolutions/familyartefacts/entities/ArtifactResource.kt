package com.synergeticsolutions.familyartefacts.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.synergeticsolutions.familyartefacts.serializers.ArtifactReferenceSerializer
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.ManyToOne
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption

/**
 * Resource associated with an [Artifact].
 *
 * @param[id] Unique ID for the resource
 * @param[name] Name of the resource
 * @param[description] Description of the artifact
 * @param[contentType] Mime type of the resource saved in [resource]. This is used when sending the resource back to a user
 * @param[resource] Actual resource being stored. This is just a binary blob and can be man different types, to differentiate [contentType] is used
 * @param[artifact] Artifact the resource is associated with
 */
@Entity
data class ArtifactResource(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    @Lob
    val description: String,
    val contentType: String,
    @JsonIgnore
    @Lob
    val resource: ByteArray,
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @ManyToOne
    @JsonSerialize(using = ArtifactReferenceSerializer::class)
    val artifact: Artifact
) {
    override fun toString(): String {
        return listOf(
            "id=$id",
            "name=$name",
            "description=$description",
            "contentType=$contentType",
            "artifact=${artifact.id}"
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

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + resource.contentHashCode()
        result = 31 * result + artifact.hashCode()
        return result
    }
}
