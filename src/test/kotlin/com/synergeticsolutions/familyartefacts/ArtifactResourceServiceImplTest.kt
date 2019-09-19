package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArtifactResourceServiceImplTest {
    val artifactResourceServiceImpl = ArtifactResourceServiceImpl()

    @Nested
    inner class Create {
        @Test
        fun `it should not create the resource if the artifact ID does not exist`() {
            TODO()
        }

        @Test
        fun `it should not create the resource if the creator's email does not correspond to a user in the database`() {
            TODO()
        }

        @Test
        fun `it should save the resource's content (mime) type`() {
            TODO()
        }

        @Test
        fun `it should associate the resource with the specified artifact`() {
            TODO()
        }

        @Test
        fun `it should save the resource in object storage`() {
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `it should allow artifact owners to update the resource`() {
            TODO()
        }

        @Test
        fun `it should not allow non artifact owners to update the resource`() {
            TODO()
        }

        @Test
        fun `it should update the resource in object storage if not null`() {
        }

        @Test
        fun `it should update the metadata in the artifact resource repository if not null`() {
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `it should allow artifact owners to delete the resource`() {
            TODO()
        }

        @Test
        fun `it should not allow non artifact owners to delete the resource`() {
            TODO()
        }

        @Test
        fun `it should remove the resource from the artifact resource repository`() {
            TODO()
        }

        @Test
        fun `it should update the artifact in the artifact repository to remove the resource from it`() {
            TODO()
        }

        @Test
        fun `it should delete the resource from object storage`() {
            TODO()
        }
    }
}
