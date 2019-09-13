package com.synergeticsolutions.familyartefacts

interface GroupService {
    fun createGroup(
            email: String,
            groupName: String,
            description: String,
            memberIDs: List<Long>
    ): Group
    fun addMembers(email: String, newMemberIDs: List<Long>, groupID: Long): Group
    fun addAdmins(email: String, newAdminIDs: List<Long>, groupID: Long)
    fun removeMembers(email: String, membersToRemove: List<Long>, groupID: Long)
    fun removeAdmins(email: String, adminsToRemove: List<Long>, groupID: Long)
    fun findGroups(email: String, ownerID: Long?, memberID: Long?): List<Group>
    fun deleteGroup(email: String, groupID: Long): Group
    fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest): Group
}