package com.synergeticsolutions.familyartefacts

interface ArtifactResourceService {
    fun create(email: String, artifactId: Long, resource: ByteArray): ArtifactResource
    fun findById(email: String, artifactId: Long, resourceId: Long): ByteArray
    fun update(email: String, artifactId: Long, resourceId: Long, byteArray: ByteArray): ArtifactResource
    fun delete(email: String, artifactId: Long, resourceId: Long): ByteArray
}
