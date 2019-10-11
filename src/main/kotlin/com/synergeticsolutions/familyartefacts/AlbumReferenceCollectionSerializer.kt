package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class AlbumReferenceCollectionSerializer(type: Class<List<Album>>? = null) : StdSerializer<List<Album>>(type) {
    override fun serialize(value: List<Album>?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val artifacts = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(artifacts.map { AlbumReferenceDto.toDto(it) })
    }
}

data class AlbumReferenceDto(
    val id: Long,
    val name: String,
    val description: String,
    val owners: List<Long>,
    val groups: List<Long>,
    val sharedWith: List<Long> = listOf(),
    val artifacts: List<Long> = listOf()
) {
    companion object {
        fun toDto(artifact: Album): AlbumReferenceDto {
            return AlbumReferenceDto(
                id = artifact.id,
                name = artifact.name,
                description = artifact.description,
                owners = artifact.owners.map(User::id),
                groups = artifact.groups.map(Group::id),
                sharedWith = artifact.sharedWith.map(User::id),
                artifacts = artifact.artifacts.map(Artifact::id)
            )
        }
    }
}
