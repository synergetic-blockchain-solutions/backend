package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/album"])
class AlbumController(
    @Autowired
    val albumService: AlbumService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun getAlbums(
        @RequestParam(name = "name", required = false) albumName: String?,
        @RequestParam(name = "group", required = false) groupID: Long?,
        @RequestParam(name = "owner", required = false) ownerID: Long?,
        @RequestParam(name = "shared", required = false) sharedID: Long?
    ): ResponseEntity<List<Album>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Filtering albums for ${currentUser.principal} by group=$groupID, owner=$ownerID, shared=$sharedID")
        val albums =
                albumService.findAlbumsByOwner(
                        email = currentUser.principal as String,
                        albumName = albumName,
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

    @PutMapping(path = ["/{id}"])
    fun updateAlbum(@PathVariable id: Long, @RequestBody album: AlbumRequest): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedAlbum = albumService.updateAlbum(currentUser.principal as String, id, album)
        logger.info("Updated album $updatedAlbum")
        return ResponseEntity.ok(updatedAlbum)
    }

    @PutMapping(path = ["/{albumID}/artifact/{artifactID}"])
    fun addArtifact(@PathVariable albumID: Long, @PathVariable artifactID: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedAlbum = albumService.addArtifact(currentUser.principal as String, albumID, artifactID)
        logger.info("Added artifact $artifactID to album $albumID")
        return ResponseEntity.ok(updatedAlbum)
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteAlbum(@PathVariable id: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val deletedAlbum = albumService.deleteAlbum(currentUser.principal as String, id)
        logger.info("Deleted album $deletedAlbum")
        return ResponseEntity.ok(deletedAlbum)
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
