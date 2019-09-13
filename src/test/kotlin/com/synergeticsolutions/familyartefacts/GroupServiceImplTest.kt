package com.synergeticsolutions.familyartefacts

//import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Nested
//import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
//import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*

class GroupServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val groupService : GroupService = GroupServiceImpl(userRepository, groupRepository)

    @Nested
    inner class CreateGroup {

        @Test
        fun `it should not create the group when the current user is not in the database`() {
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)
            assertThrows<GroupNotFoundException> {
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
                                            admins = mutableListOf())))
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<UserNotFoundException> {
                groupService.createGroup(
                        "example@example.com",
                        "Group Name",
                        description = "Group description",
                        memberIDs = listOf(2))
            }
        }

        @Test
        fun `it should include the current user as the member and admin of the created group`() {
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
                                            admins = mutableListOf())))
            Mockito.`when`(userRepository.existsById(anyLong()))
                    .thenReturn(true)
            Mockito.`when`(groupRepository.save(any<Group>())).then { it.arguments[0] as Group }
            val group = groupService.createGroup(
                    "example@example.com",
                    "Group Name",
                    description = "Group description",
                    memberIDs = listOf(2))


            val argCapturer = ArgumentCaptor.forClass(Group::class.java)
            Mockito.verify(groupRepository).save(argCapturer.capture())
            val matcher0 = hasProperty<Group>(
                    "members",
                    hasItem<User>(hasProperty("email", equalTo("example@example.com"))))
            assertThat(argCapturer.value, matcher0)
            val matcher1 = hasProperty<Group>(
                    "admins",
                    hasItem<User>(hasProperty("email", equalTo("example@example.com"))))
            assertThat(argCapturer.value, matcher1)
        }

    }

    @Nested
    inner class AddMembers {

        @Test
        fun `it should not add members if the current user is not in the database`() {
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)
            assertThrows<UsernameNotFoundException> {
                groupService.addMembers(
                        email = "example@example.com",
                        newMemberIDs = listOf(2),
                        groupID = 1)
            }
        }

        @Test
        fun `it should not add members if the group is not in the database`() {
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
                                            admins = mutableListOf())))
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.empty<Group>() }
            assertThrows<GroupNotFoundException> {
                groupService.addMembers(
                        email = "example@example.com",
                        newMemberIDs = listOf(2),
                        groupID = 1)
            }
        }

        @Test
        fun `it should not allow adding members if user is not an admin`() {
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
                                            admins = mutableListOf())))
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong()))
                    .then {
                            Optional.of(Group(
                                    id = 2,
                                    name = "Group Name",
                                    description = "Group description",
                                    members = mutableListOf(),
                                    admins = mutableListOf()))
                    }
            assertThrows<ActionNotAllowedException> {
                groupService.addMembers(
                        email = "example@example.com",
                        newMemberIDs = listOf(2),
                        groupID = 1)
            }
        }

        @Test
        fun `it should not add members if one of the member IDs are not in the database`() {
            val user = User(
                    1,
                    "User1",
                    "example@example.com",
                    "password",
                    privateGroup = Group(
                            1,
                            "Group1",
                            description = "description",
                            members = mutableListOf(),
                            admins = mutableListOf()))
            Mockito.`when`(userRepository.findByEmail(anyString()))
                    .thenReturn(user)
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong()))
                    .then {
                        Optional.of(Group(
                                id = 2,
                                name = "Group Name",
                                description = "Group description",
                                members = mutableListOf(user),
                                admins = mutableListOf(user)))
                    }
            assertThrows<UserNotFoundException> {
                groupService.addMembers("example@example.com", listOf(2), 2)
            }
        }



    }

}
