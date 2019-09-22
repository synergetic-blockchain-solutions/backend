package com.synergeticsolutions.familyartefacts

import java.util.Optional
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException

class GroupServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val groupService: GroupService = GroupServiceImpl(userRepository, groupRepository)

    @Nested
    inner class GetGroup {
        @Test
        fun `it should find all the groups accessible by the user`() {
            val email = "example@example.com"
            val user = User(
                    id = 1,
                    name = "User 1",
                    email = email,
                    password = "password",
                    groups = mutableListOf(),
                    privateGroup = Group(7, "Group 7", members = mutableListOf(), description = ""))
            Mockito.`when`(userRepository.findByEmail(email))
                    .thenReturn(user)
            val groups = listOf(
                    Group(1, "Group 1", "Description 1", members = mutableListOf(user), admins = mutableListOf()),
                    Group(2, "Group 2", "Description 2", members = mutableListOf(user), admins = mutableListOf()),
                    Group(3, "Group 3", "Description 3", members = mutableListOf(user), admins = mutableListOf())
            )
            val ownedGroups = listOf(
                    Group(4, "Group 4", "Description 4", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(5, "Group 5", "Description 5", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(6, "Group 6", "Description 6", members = mutableListOf(user), admins = mutableListOf(user))
            )
            val allGroups = groups + ownedGroups

            Mockito.`when`(groupRepository.findByAdmins_Email(anyString())).thenReturn(ownedGroups)
            Mockito.`when`(groupRepository.findByMembers_Email(anyString())).thenReturn(allGroups)
            val foundGroups = groupService.findGroups(email = email, adminID = null, memberID = null)

            assertEquals(allGroups.size, foundGroups.size)
            assertThat(foundGroups, containsInAnyOrder(*allGroups.toTypedArray()))
        }

        @Test
        fun `it should return all group where the user is admin`() {
            val email = "example@example.com"
            val user = User(
                    id = 1,
                    name = "User 1",
                    email = email,
                    password = "password",
                    groups = mutableListOf(),
                    privateGroup = Group(7, "Group 7", members = mutableListOf(), description = ""))
            val groups = listOf(
                    Group(1, "Group 1", "Description 1", members = mutableListOf(user), admins = mutableListOf()),
                    Group(2, "Group 2", "Description 2", members = mutableListOf(user), admins = mutableListOf()),
                    Group(3, "Group 3", "Description 3", members = mutableListOf(user), admins = mutableListOf())
            )
            val ownedGroups = listOf(
                    Group(4, "Group 4", "Description 4", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(5, "Group 5", "Description 5", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(6, "Group 6", "Description 6", members = mutableListOf(user), admins = mutableListOf(user))
            )
            val allGroups = groups + ownedGroups
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(groupRepository.findByAdmins_Email(anyString())).thenReturn(ownedGroups)
            Mockito.`when`(groupRepository.findByMembers_Email(anyString())).thenReturn(allGroups)

            val foundGroups = groupService.findGroups(email, adminID = user.id, memberID = null)
            val expectedGroups =
                    (ownedGroups + groups).filter { it.admins.firstOrNull()?.id == user.id }
            assertEquals(expectedGroups.size, foundGroups.size)
            assertThat(foundGroups, containsInAnyOrder(*expectedGroups.toTypedArray()))
        }

        @Test
        fun `it should return all groups where the user is member`() {
            val email = "example@example.com"
            val user = User(
                    id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(),
                    privateGroup = Group(7, "Group 7", members = mutableListOf(), description = ""))
            val groups = listOf(
                    Group(1, "Group 1", "Description 1", members = mutableListOf(user), admins = mutableListOf()),
                    Group(2, "Group 2", "Description 2", members = mutableListOf(user), admins = mutableListOf()),
                    Group(3, "Group 3", "Description 3", members = mutableListOf(user), admins = mutableListOf())
            )
            val ownedGroups = listOf(
                    Group(4, "Group 4", "Description 4", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(5, "Group 5", "Description 5", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(6, "Group 6", "Description 6", members = mutableListOf(user), admins = mutableListOf(user))
            )
            val allGroups = groups + ownedGroups
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(groupRepository.findByAdmins_Email(anyString())).thenReturn(ownedGroups)
            Mockito.`when`(groupRepository.findByMembers_Email(anyString())).thenReturn(allGroups)

            val foundGroups = groupService.findGroups(email, adminID = null, memberID = user.id)
            val expectedGroups =
                    (ownedGroups + groups).filter { it.members.firstOrNull()?.id == user.id }
            assertEquals(expectedGroups.size, foundGroups.size)
            assertThat(foundGroups, containsInAnyOrder(*expectedGroups.toTypedArray()))
        }

        @Test
        fun `it should return all groups where the user is both admin and member`() {
            val email = "example@example.com"
            val user = User(
                    id = 1,
                    name = "User 1",
                    email = email,
                    password = "password",
                    groups = mutableListOf(),
                    privateGroup = Group(7, "Group 7", members = mutableListOf(), description = "")
            )
            val groups = listOf(
                    Group(1, "Group 1", "Description 1", members = mutableListOf(user), admins = mutableListOf()),
                    Group(2, "Group 2", "Description 2", members = mutableListOf(user), admins = mutableListOf()),
                    Group(3, "Group 3", "Description 3", members = mutableListOf(user), admins = mutableListOf())
            )
            val ownedGroups = listOf(
                    Group(4, "Group 4", "Description 4", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(5, "Group 5", "Description 5", members = mutableListOf(user), admins = mutableListOf(user)),
                    Group(6, "Group 6", "Description 6", members = mutableListOf(user), admins = mutableListOf(user))
            )
            val allGroups = groups + ownedGroups
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(groupRepository.findByAdmins_Email(anyString())).thenReturn(ownedGroups)
            Mockito.`when`(groupRepository.findByMembers_Email(anyString())).thenReturn(allGroups)

            val foundGroups = groupService.findGroups(email, adminID = user.id, memberID = user.id)
            val expectedGroups = (ownedGroups).toSet().toList()
            assertEquals(expectedGroups.size, foundGroups.size)
            assertThat(foundGroups, containsInAnyOrder(*expectedGroups.toTypedArray()))
        }
    }

    @Nested
    inner class CreateGroup {

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

        @Test
        fun `it should make the specified member IDs the members of the group`() {
            Mockito.`when`(groupRepository.save(any<Group>())).then { it.arguments[0] as Group }

            Mockito.`when`(userRepository.findByEmail(anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(1, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    User(
                            id = id,
                            name = "User $id",
                            email = "example$id@example.com",
                            password = "password",
                            privateGroup = Group(
                                    2, "Group2", members = mutableListOf(), description = ""
                            )
                    )
                }
            }
            groupService.createGroup(
                    "example@example.com",
                    "Group 3",
                    description = "Group description",
                    memberIDs = listOf(2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Group::class.java)
            Mockito.verify(groupRepository).save(argCapturer.capture())
            val matcher =
                    hasProperty<Group>(
                            "members",
                            hasItems<User>(
                                    hasProperty("id", equalTo(2L)),
                                    hasProperty("id", equalTo(3L))
                            )
                    )
            assertThat(argCapturer.value, matcher)
        }
    }

    @Nested
    inner class UpdateGroup {

        @Test
        fun `it should not update group if the current user is not in the database`() {
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Group description",
                    members = listOf(2),
                    admins = listOf(2)
            )
            assertThrows<UsernameNotFoundException> {
                groupService.updateGroup(
                        email = "example@example.com",
                        groupID = 1,
                        groupRequest = groupRequest
                )
            }
        }

        @Test
        fun `it should not update group if the group is not in the database`() {
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
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Group description",
                    members = listOf(2),
                    admins = listOf(2)
            )
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.empty<Group>() }
            assertThrows<GroupNotFoundException> {
                groupService.updateGroup(
                        email = "example@example.com",
                        groupRequest = groupRequest,
                        groupID = 1
                        )
            }
        }

        @Test
        fun `it should not allow updating group if user is not an admin`() {
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
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Group description",
                    members = listOf(2),
                    admins = listOf(2)
            )
            assertThrows<ActionNotAllowedException> {
                groupService.updateGroup(
                        email = "example@example.com",
                        groupRequest = groupRequest,
                        groupID = 1)
            }
        }

        @Test
        fun `it should not update group if one of the members are not in the database`() {
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
            val group = Group(name = "Group 1",
                    description = "Group description",
                    members = mutableListOf(user),
                    admins = mutableListOf(user))
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.of(group) }
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Group description",
                    members = listOf(2),
                    admins = listOf(2)
            )
            assertThrows<UserNotFoundException> {
                groupService.updateGroup(
                        "example@example.com",
                        groupRequest = groupRequest,
                        groupID = 2)
            }
        }

        @Test
        fun `it should allow admins to update group`() {
            val email = "example@example.com"
            var group = Group(
                    id = 1,
                    name = "Group 1",
                    description = "",
                    members = mutableListOf(),
                    admins = mutableListOf())
            var owningUser = User(
                    id = 2,
                    name = "User 2",
                    email = "example@example2.com",
                    password = "password",
                    groups = mutableListOf(group),
                    ownedGroups = mutableListOf(group),
                    privateGroup = Group(
                            2, "Group 2", members = mutableListOf(), description = ""))

            group = group.copy(admins = mutableListOf(owningUser), members = mutableListOf(owningUser))
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == group.id) {
                    Optional.of(group)
                } else {
                    Optional.empty()
                }
            }

            Mockito.`when`(groupRepository.save(any<Group>())).then { it.arguments[0] as Group }

            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
                val args = it.arguments[0] as Iterable<Long>
                when {
                    args.toList() == group.admins.map(User::id) -> group.admins
                    args.toList() == group.members.map(User::id) -> group.members
                    else -> listOf<User>()
                }
            }
            val updatedGroup = groupService.updateGroup(
                    owningUser.email,
                    group.id,
                    GroupRequest(
                        group.name,
                        "updated description",
                        group.admins.map(User::id),
                        group.members.map(User::id)))
            assertThat(
                    updatedGroup, equalTo(group.copy(description = "updated description"))
            )
            Mockito.verify(groupRepository).save(group.copy(description = "updated description"))
        }
    }
    @Nested
    inner class DeleteGroup {

        @Test
        fun `it should not allow ordinary members to delete the group`() {
            val email = "example@example.com"
            var group = Group(
                    id = 1,
                    name = "Group 1",
                    members = mutableListOf(),
                    description = "")
            var owningUser = User(
                    id = 2,
                    name = "User 2",
                    email = "example@example2.com",
                    password = "password",
                    groups = mutableListOf(group),
                    ownedGroups = mutableListOf(group),
                    privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            group = group.copy(admins = mutableListOf(owningUser), members = mutableListOf(owningUser))
            val user = User(
                    id = 1,
                    name = "User 1",
                    email = email,
                    password = "password",
                    groups = mutableListOf(Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = "")),
                    privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == group.id) {
                    Optional.of(group)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            assertThrows<ActionNotAllowedException> { groupService.deleteGroup(user.email, group.id) }
        }
        @Test
        fun `it should allow the group admins to delete the group`() {
            val email = "example@example.com"
            var group = Group(
                    id = 1,
                    name = "Group 1",
                    members = mutableListOf(),
                    description = "")
            var owningUser = User(
                    id = 2,
                    name = "User 2",
                    email = "example@example2.com",
                    password = "password",
                    groups = mutableListOf(group),
                    ownedGroups = mutableListOf(group),
                    privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            group = group.copy(admins = mutableListOf(owningUser), members = mutableListOf(owningUser))
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == group.id) {
                    Optional.of(group)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(groupRepository.save(any<Group>())).then { it.arguments[0] as Group }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
                val args = it.arguments[0] as Iterable<Long>
                if (args.toList() == group.admins.map(User::id)) {
                    group.admins
                } else {
                    listOf<User>()
                }
            }
            val deletedGroup = groupService.deleteGroup(owningUser.email, group.id)
            assertThat(deletedGroup, equalTo(group))
            Mockito.verify(groupRepository).delete(group)
        }
    }
}
