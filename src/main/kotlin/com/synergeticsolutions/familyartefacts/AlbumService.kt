package com.synergeticsolutions.familyartefacts

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
    fun findAlbumsByOwner(
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
