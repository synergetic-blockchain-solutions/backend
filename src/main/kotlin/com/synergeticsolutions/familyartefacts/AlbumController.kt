package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/album"])
class AlbumController(
    @Autowired
    val albumService: AlbumService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createAlbum(@RequestBody newAlbum: AlbumRequest): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val createdAlbum = albumService.createAlbum(
                email = currentUser.principal as String,
                name = newAlbum.name,
                description = newAlbum.description,
                ownerIDs = newAlbum.owners ?: listOf(),
                groupIDs = newAlbum.groups ?: listOf(),
                sharedWithIDs = newAlbum.sharedWith ?: listOf(),
                artifactIDs = newAlbum.artifacts ?: listOf()
        )
        logger.info("Created artifact $createdAlbum")
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAlbum)
    }
}
data class AlbumRequest(
    val name: String,
    val description: String,
    val owners: List<Long>,
    val groups: List<Long>,
    val sharedWith: List<Long>,
    val artifacts: List<Long>
)
