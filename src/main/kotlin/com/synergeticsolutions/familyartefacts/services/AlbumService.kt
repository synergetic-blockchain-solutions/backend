package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.controllers.AlbumRequest
import com.synergeticsolutions.familyartefacts.entities.Album

interface AlbumService {
    fun createAlbum(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long>,
        groupIDs: List<Long>,
        sharedWithIDs: List<Long>,
        artifactIDs: List<Long>
    ): Album
    fun findAlbumById(email: String, id: Long): Album
    fun findAlbums(
        email: String,
        groupID: Long?,
        ownerID: Long?,
        sharedID: Long?,
        albumName: String?
    ): List<Album>
    fun updateAlbum(email: String, id: Long, update: AlbumRequest): Album
    fun deleteAlbum(email: String, id: Long): Album
    fun addArtifact(email: String, albumID: Long, artifactID: Long): Album
}
