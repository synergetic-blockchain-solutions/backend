package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/album"])
class AlbumController(
    @Autowired
    val albumService: AlbumService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun getAlbums(
            @RequestParam(name = "group", required = false) groupID: Long?,
            @RequestParam(name = "owner", required = false) ownerID: Long?,
            @RequestParam(name = "shared", required = false) sharedID: Long?
    ): ResponseEntity<List<Album>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Filtering albums for ${currentUser.principal} by group=$groupID, owner=$ownerID, shared=$sharedID")
        val albums =
                albumService.findAlbumsByOwner(
                        email = currentUser.principal as String,
                        groupID = groupID,
                        ownerID = ownerID,
                        sharedID = sharedID
                )
        logger.debug("Found ${albums.size} albums fitting the criteria")

        return ResponseEntity.ok(albums)
    }

    @GetMapping(path = ["/{id}"])
    fun getAlbumById(@PathVariable id: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Getting album $id for user ${currentUser.principal}")
        val album = albumService.findAlbumById(currentUser.principal as String, id)
        logger.debug("Found $album")
        return ResponseEntity.ok(album)
    }

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
        logger.info("Created album $createdAlbum")
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
