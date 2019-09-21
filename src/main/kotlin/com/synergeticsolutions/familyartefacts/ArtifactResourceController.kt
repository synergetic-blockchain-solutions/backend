package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.Principal
import javax.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest

@RestController
@RequestMapping(path = ["/artifact/{artifactId}/resource"])
class ArtifactResourceController(
    @Autowired
    val artifactResourceService: ArtifactResourceService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * POST /artifact/{artifactId}/resource
     *
     * Create a new artifact resource associated with [artifactId].
     */
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createResourceWithMetadata(
        @PathVariable artifactId: Long,
        request: HttpServletRequest,
        principal: Principal
    ): ArtifactResource {
        if (!(request.parameterMap.containsKey("metadata"))) {
            throw MetadataPartRequired()
        } else if (!(request.parameterMap.containsKey("resource"))) {
            throw ResourcePartRequired()
        }

        val metadataPart = checkNotNull(
            request.parameterMap["metadata"]?.firstOrNull(),
            { "Checked that the parameter map contained 'metadata' earlier" }
        )
        val resource = request.getPart("resource").inputStream.toByteArray()
        val contentType = (request as StandardMultipartHttpServletRequest).getMultipartContentType("resource")
        val metadata = ObjectMapper().registerKotlinModule().readValue<ArtifactResourceMetadata>(metadataPart)
        return artifactResourceService.create(
            principal.name, artifactId,
            metadata = metadata,
            contentType = contentType,
            resource = resource
        )
    }

    /**
     * GET /artifact/{artifactId}/resource/{resourceId}/metadata
     *
     * Get the metadata for resource [resourceId] associated with the artifact [artifactId].
     */
    @GetMapping(path = ["/{resourceId}/metadata"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getResourceMetadataById(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        principal: Principal
    ): ArtifactResourceMetadata {
        return artifactResourceService.findMetadataById(principal.name, artifactId, resourceId)
    }

    @GetMapping(path = ["/{resourceId}/resource"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getResourceById(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        principal: Principal
    ): ResponseEntity<ByteArrayResource> {
        val resource = artifactResourceService.findResourceById(principal.name, artifactId, resourceId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(resource.contentType))
            .body(ByteArrayResource(resource.resource))
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update both the metadata and resource of an artifact resource.
     */
    @PutMapping(path = ["/{resourceId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateResourceAndMetadata(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        request: HttpServletRequest,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating metadata and resource of resource $resourceId")
        if (!(request.parameterMap.containsKey("metadata"))) {
            throw MetadataPartRequired()
        } else if (!(request.parameterMap.containsKey("resource"))) {
            throw ResourcePartRequired()
        }
        val metadataPart = checkNotNull(
            request.parameterMap["metadata"]?.firstOrNull(),
            { "Checked that the parameter map contained 'metadata' earlier" }
        )
        val resource = request.getPart("resource").inputStream.toByteArray()
        val contentType = (request as StandardMultipartHttpServletRequest).getMultipartContentType("resource")
        val metadata = ObjectMapper().registerKotlinModule()
            .readValue<ArtifactResourceMetadata>(metadataPart)
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            metadata = metadata,
            contentType = contentType,
            resource = resource
        )
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update the artifact resource's metadata.
     */
    @PutMapping(path = ["/{resourceId}"], consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun updateResourceMetadata(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestBody metadata: ArtifactResourceMetadata,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating metadata of resource $resourceId")
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            metadata = metadata
        )
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update the artifact resource's resource.
     */
    @PutMapping(path = ["/{resourceId}"])
    fun updateResource(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @RequestBody resource: ByteArray,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating resource of resource $resourceId")
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            resource = resource,
            contentType = contentType
        )
    }

    /**
     * DELETE /artifact/{artifactId}/resource/{resourceId}
     *
     * Delete the resource [resourceId] associated with artifact [artifactId]
     */
    @DeleteMapping(path = ["/{resourceId}"])
    fun deleteResourceById(@PathVariable artifactId: Long, @PathVariable resourceId: Long, principal: Principal): ArtifactResourceMetadata =
        artifactResourceService.delete(principal.name, artifactId, resourceId)
}

private fun InputStream.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    this.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    return outputStream.toByteArray()
}
