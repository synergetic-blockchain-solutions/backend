package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

class UserIsNotMemberException(msg: String) : RuntimeException(msg)
class UserIsNotAdminException(msg: String) : RuntimeException(msg)
class GroupNotFoundException(msg: String) : RuntimeException(msg)
class UserNotFoundException(msg: String) : RuntimeException(msg)
class MemberAlreadyInGroupException(msg: String) : RuntimeException(msg)
class MemberIsAlreadyAdminException(msg: String) : RuntimeException(msg)
class ActionNotAllowedException() : AuthenticationException()

@Service
class GroupServiceImpl(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository

) : GroupService {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun findGroups(email: String, ownerID: Long?, memberID: Long?): List<Group> {
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        val ownedGroups = groupRepository.findByAdmins_Email(email)
        val memberGroup = groupRepository.findByMembers_Email(email)
        var groups = ownedGroups.union(memberGroup).toList()

        if (ownerID != null) {
            groups = groups.filter { it.admins.map(User::id).contains(ownerID) }
        }

        if (memberID != null) {
            groups = groups.filter { it.members.map(User::id).contains(memberID) }
        }

        return groups
    }

    override fun createGroup(
        email: String,
        groupName: String,
        description: String,
        memberIDs: List<Long>
    ): Group {
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        memberIDs.forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        val members = userRepository.findAllById(memberIDs).toMutableList()
        if (!members.contains(owner)) {
            members.add(owner)
        }
        val group = Group(name = groupName, description = description, members = members, admins = mutableListOf(owner))
        val savedGroup = groupRepository.save(group)
        owner.ownedGroups.add(group)
        members.forEach {
            it.groups.add(group)
        }
        userRepository.saveAll(members)
        return savedGroup
    }


    override fun addMembers(email: String, newMemberIDs: List<Long>, groupID: Long): Group {
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException()
        }
        newMemberIDs.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        val newMembers = userRepository.findAllById(newMemberIDs).toMutableList()
        newMembers.forEach {
            if (!group.members.contains(it)) {
                group.members.add(it)
            } else throw MemberAlreadyInGroupException("Member with email ${it.email} is already in the group")
        }
        val updatedGroup = groupRepository.save(group)
        newMembers.forEach {
            it.groups.add(group)
        }
        userRepository.saveAll(newMembers)
        return updatedGroup
    }

    override fun addAdmins(email: String, newAdminIDs: List<Long>, groupID: Long) {
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException()
        }
        newAdminIDs.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        val newAdmins = userRepository.findAllById(newAdminIDs).toMutableList()
        newAdmins.forEach {
            if (!group.members.contains(it)) {
                throw UserIsNotMemberException("User with email ${it.email} is not a member")
            } else {
                if (group.admins.contains(it)) {
                    throw MemberIsAlreadyAdminException("User with email ${it.email} is already an admin")
                } else {
                    group.admins.add(it)
                }
            }
        }
        groupRepository.save(group)
        newAdmins.forEach {
            it.ownedGroups.add(group)
        }
        userRepository.saveAll(newAdmins)
    }

    override fun removeMembers(email: String, memberIDs: List<Long>, groupID: Long) {
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException()
        }
        memberIDs.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        val membersToRemove = userRepository.findAllById(memberIDs).toMutableList()
        membersToRemove.forEach {
            if (group.members.contains(it)) {
                group.members.remove(it)
                it.groups.remove(group)
                if (group.admins.contains(it)) {
                    group.admins.remove(it)
                    it.ownedGroups.remove(group)
                }

            } else {
                throw UserIsNotMemberException("User with email ${it.email} is not a member")
            }
        }
        userRepository.saveAll(membersToRemove)
        groupRepository.save(group)
    }

    override fun removeAdmins(email: String, adminIDs: List<Long>, groupID: Long) {
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException()
        }
        adminIDs.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        val adminsToRemove = userRepository.findAllById(adminIDs).toMutableList()
        adminsToRemove.forEach {
            if (group.admins.contains(it)) {
                group.admins.remove(it)
                it.ownedGroups.remove(group)
            } else {
                throw UserIsNotAdminException("User with email ${it.email} is not an admin")
            }
        }
        userRepository.saveAll(adminsToRemove)
        groupRepository.save(group)
    }

    //Not finished
    override fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest) : Group {
        return Group(id = 2,
                name = "Group Name",
                description = "Group description",
                members = mutableListOf(),
                admins = mutableListOf())
    }

    override fun deleteGroup(email: String, groupID: Long): Group {
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException()
        }
        group.members.forEach { it.groups.remove(group) }
        group.admins.forEach { it.ownedGroups.remove(group) }
        groupRepository.delete(group)
        return group
    }




}
