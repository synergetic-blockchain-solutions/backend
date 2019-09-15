package com.synergeticsolutions.familyartefacts

import java.security.Principal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

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
     * Create a resource associated with artifact [artifactId].
     */
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping
    fun createResource(
        @PathVariable artifactId: Long,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @RequestBody resource: ByteArrayResource,
        principal: Principal
    ): ArtifactResource = artifactResourceService.create(principal.name, artifactId, resource.byteArray)

    /**
     * GET /artifact/{artifactId}/resource/{resourceId}
     *
     * Get the resource [resourceId] associated with the artifact [artifactId].
     */
    @GetMapping(path = ["/{resourceId}"])
    fun getResourceById(@PathVariable artifactId: Long, @PathVariable resourceId: Long, principal: Principal): ByteArray =
        artifactResourceService.findById(principal.name, artifactId, resourceId)

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update the resource [resourceId] for artifact [artifactId]
     */
    @PutMapping(path = ["/{resourceId"])
    fun updateResource(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @RequestBody resource: ByteArrayResource,
        principal: Principal
    ): ArtifactResource {
        return artifactResourceService.updateResource(principal.name, artifactId, resourceId, resource.byteArray)
    }

    /**
     * DELETE /artifact/{artifactId}/resource/{resourceId}
     *
     * Delete the resource [resourceId] associated with artifact [artifactId]
     */
    @DeleteMapping(path = ["/{resourceId"])
    fun deleteResourceById(@PathVariable artifactId: Long, @PathVariable resourceId: Long, principal: Principal): ByteArray =
        artifactResourceService.delete(principal.name, artifactId, resourceId)
}
