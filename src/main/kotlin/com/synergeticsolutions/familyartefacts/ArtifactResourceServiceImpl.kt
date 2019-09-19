package com.synergeticsolutions.familyartefacts

import org.springframework.stereotype.Service

@Service
class ArtifactResourceServiceImpl : ArtifactResourceService {
    override fun findById(name: String, artifactId: Long, resourceId: Long): ByteArray {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun update(
        email: String,
        artifactId: Long,
        resourceId: Long,
        byteArray: ByteArray
    ): ArtifactResource {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(email: String, artifactId: Long, resourceId: Long): ByteArray {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun create(email: String, artifactId: Long, resource: ByteArray): ArtifactResource {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
