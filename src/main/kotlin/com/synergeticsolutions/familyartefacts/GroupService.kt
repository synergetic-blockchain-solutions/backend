package com.synergeticsolutions.familyartefacts

interface GroupService {
    fun createGroup(
            email: String,
            groupName: String,
            description: String,
            memberIDs: List<Long>
    ): Group
    fun addMembers(newMemberIDs: List<Long>, groupID: Long)
    fun addAdmins(newAdminIDs: List<Long>, groupID: Long)
    fun removeMembers(membersToRemove: List<Long>, groupID: Long)
    fun removeAdmins(adminsToRemove: List<Long>, groupID: Long)
    fun findGroups(email: String, ownerID: Long?, memberID: Long?): List<Group>
}