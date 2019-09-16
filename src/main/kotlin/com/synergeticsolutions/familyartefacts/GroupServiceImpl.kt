package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import javax.persistence.EntityManager

class UserIsNotMemberException(msg: String) : RuntimeException(msg)
class UserIsNotAdminException(msg: String) : RuntimeException(msg)
@ResponseStatus(HttpStatus.BAD_REQUEST)
class GroupNotFoundException(msg: String) : RuntimeException(msg)
class UserNotFoundException(msg: String) : RuntimeException(msg)
class MemberAlreadyInGroupException(msg: String) : RuntimeException(msg)
class MemberIsAlreadyAdminException(msg: String) : RuntimeException(msg)
@ResponseStatus(HttpStatus.FORBIDDEN)
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


    override fun addMembers(email: String, membersToAdd: List<User>, group: Group): Group {

        membersToAdd.forEach {
            if (!group.members.contains(it)) {
                group.members.add(it)
            } else throw MemberAlreadyInGroupException("Member with email ${it.email} is already in the group")
        }
        val updatedGroup = groupRepository.save(group)
        membersToAdd.forEach {
            it.groups.add(group)
        }
        userRepository.saveAll(membersToAdd)
        return updatedGroup
    }

    override fun addAdmins(email: String, adminsToAdd: List<User>, group: Group): Group {
        adminsToAdd.forEach {
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
        val savedGroup = groupRepository.save(group)
        adminsToAdd.forEach {
            it.ownedGroups.add(group)
        }
        userRepository.saveAll(adminsToAdd)
        return savedGroup
    }

    override fun removeMembers(email: String, membersToRemove: List<User>, group: Group): Group {

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
        return groupRepository.save(group)
    }

    override fun removeAdmins(email: String, adminsToRemove: List<User>, group: Group): Group {
        adminsToRemove.forEach {
            if (group.admins.contains(it)) {
                group.admins.remove(it)
                it.ownedGroups.remove(group)
            } else {
                throw UserIsNotAdminException("User with email ${it.email} is not an admin")
            }
        }
        userRepository.saveAll(adminsToRemove)
        return groupRepository.save(group)
    }

    //Not finished
    override fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest) : Group {
        val user =
                userRepository.findByEmail(email)
                        ?: throw UsernameNotFoundException("User with email $email does not exist")
        var group =
                groupRepository.findByIdOrNull(groupID)
                        ?: throw GroupNotFoundException("Could not find group with ID $groupID")
        if (!group.admins.contains(user)) {
            throw ActionNotAllowedException()
        }
        groupRequest.members?.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        groupRequest.admins?.forEach{
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }

        val updatedAdmins = userRepository.findAllById(groupRequest.admins ?: listOf())
        val adminsToRemove = group.admins.subtract(updatedAdmins).toList()
        val adminsToAdd = updatedAdmins.subtract(group.admins).toList()
        group = addAdmins(email, adminsToAdd, group)
        group = removeAdmins(email, adminsToRemove, group)

        val updatedMembers = userRepository.findAllById(groupRequest.members ?: listOf())
        val membersToRemove = group.members.subtract(updatedMembers).toList()
        val membersToAdd = updatedAdmins.subtract(group.members).toList()
        group = addMembers(email, membersToAdd, group)
        group = removeMembers(email, membersToRemove, group)

        return groupRepository.save(group)
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
