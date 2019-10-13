package com.synergeticsolutions.familyartefacts

import org.springframework.core.io.ByteArrayResource

interface GroupService {
    fun createGroup(
        email: String,
        groupName: String,
        description: String,
        memberIDs: List<Long>,
        adminIDs: List<Long>
    ): Group
    fun removeAdmins(email: String, adminsToRemove: List<User>, group: Group): Group
    fun findGroups(email: String, adminID: Long?, memberID: Long?): List<Group>
    fun deleteGroup(email: String, groupID: Long): Group
    fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest): Group
    fun removeMembers(membersToRemove: List<User>, group: Group): Group
    fun addAdmins(adminsToAdd: List<User>, group: Group): Group
    fun addMembers(membersToAdd: List<User>, group: Group): Group
    fun findGroupById(email: String, groupID: Long): Group
    fun addImage(s: String, contentType: String, id: Long, image: ByteArray): Group
    fun findGroupImageById(email: String, id: Long): ByteArrayResource
}
