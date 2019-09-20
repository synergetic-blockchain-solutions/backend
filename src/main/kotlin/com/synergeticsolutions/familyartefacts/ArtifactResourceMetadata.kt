package com.synergeticsolutions.familyartefacts

data class ArtifactResourceMetadata(
    val name: String,
    val description: String,
    val tags: List<String>? = listOf()
)
