package com.open.spring.mvc.groups;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupsJpaRepository extends JpaRepository<Groups, Long> {
    
    // Find a group by its ID
    Optional<Groups> findById(Long id);
    
    // Find all groups
    List<Groups> findAll();
    List<Groups> findAllByOrderByNameAsc();
    
    // Find groups containing a specific person by uid
    @Query("SELECT g FROM Groups g JOIN g.groupMembers p WHERE p.uid = :personUid")
    List<Groups> findGroupsByPersonUid(@Param("personUid") String personUid);
    
    // Find groups containing a specific person by id
    @Query("SELECT g FROM Groups g JOIN g.groupMembers p WHERE p.id = :personId")
    List<Groups> findGroupsByPersonId(@Param("personId") Long personId);
    
    // Find groups with a specific number of members
    @Query("SELECT g FROM Groups g WHERE SIZE(g.groupMembers) = :memberCount")
    List<Groups> findGroupsByMemberCount(@Param("memberCount") int memberCount);
    
    // Get raw member data for a group (avoids Hibernate hydration issues)
    @Query(value = "SELECT p.id, p.uid, p.name, p.email FROM group_members gm " +
                   "JOIN person p ON gm.person_id = p.id " +
                   "WHERE gm.group_id = :groupId " +
                   "ORDER BY p.id", nativeQuery = true)
    List<Object[]> findGroupMembersRaw(@Param("groupId") Long groupId);
}