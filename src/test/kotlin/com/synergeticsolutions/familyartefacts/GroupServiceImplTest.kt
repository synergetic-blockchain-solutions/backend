package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException

class GroupServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val groupService : GroupService = GroupServiceImpl(userRepository, groupRepository)

    @Test
    fun `it should not create the group when the current user is not in the database`() {
        Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)
        assertThrows<UsernameNotFoundException> {
            groupService.createGroup(
                    "example@example.com",
                    "Group Name",
                    "Group Description",
                    listOf(1))
        }
    }

    @Test
    fun `it should not create the group if one of the members are not in the database`() {
        Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                        User(
                                1,
                                "User1",
                                "example@example.com",
                                "password",
                                privateGroup = Group(
                                        1,
                                        "Group1",
                                        description = "description",
                                        members = mutableListOf(),
                                        admins = mutableListOf())
                        )
                )
        Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
        assertThrows<UserNotFoundException> {
            groupService.createGroup(
                    "example@example.com",
                    "Group Name",
                    description = "Group description",
                    memberIDs = listOf(2)
            )
        }
    }

/*    @Test
    fun `it should include the current user as the member and admin of the created group`() {
        Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
        Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                        User(
                                1,
                                "User1",
                                "example@example.com",
                                "password"
                        )
                )
        Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
        artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description"
        )
        val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
        Mockito.verify(artifactRepository).save(argCapturer.capture())
        val matcher = hasProperty<Artifact>("owners", contains(hasProperty("id", equalTo(1L))))
        assertThat(argCapturer.value, matcher)
    }*/

}
