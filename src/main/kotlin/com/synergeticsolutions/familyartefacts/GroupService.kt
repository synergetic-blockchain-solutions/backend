package com.synergeticsolutions.familyartefacts

interface GroupService {
    fun createGroup(
        email: String,
        groupName: String,
        description: String,
        memberIDs: List<Long>
    ): Group
    fun removeAdmins(email: String, adminsToRemove: List<User>, group: Group): Group
    fun findGroups(email: String, adminID: Long?, memberID: Long?): List<Group>
    fun deleteGroup(email: String, groupID: Long): Group
    fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest): Group
    fun removeMembers(email: String, membersToRemove: List<User>, group: Group): Group
    fun addAdmins(email: String, adminsToAdd: List<User>, group: Group): Group
    fun addMembers(email: String, membersToAdd: List<User>, group: Group): Group
    fun findGroupById(email: String, groupID: Long): Group
}
