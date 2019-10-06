package com.synergeticsolutions.familyartefacts

interface AlbumService {
    fun createAlbum(email: String, name: String, description: String, ownerIDs: List<Long>, groupIDs: List<Long>, sharedWithIDs: List<Long>, artifactIDs: List<Long>): Album
}
