package com.open.spring.mvc.groups;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Service managing user presence within specific chat groups.
 * Enables tracking the real-time participation of group members connected 
 * via WebSocket by retaining active session identifiers mapped to the corresponding 
 * usernames and actively joined groups.
 */
@Service
public class GroupChatPresenceService {

    private final ConcurrentMap<String, PresenceSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ConcurrentMap<String, Integer>> participantCountsByGroup = new ConcurrentHashMap<>();

    public void joinGroup(String sessionId, Long groupId, String username) {
        if (sessionId == null || groupId == null || username == null || username.isBlank()) {
            return;
        }

        PresenceSession presenceSession = sessions.computeIfAbsent(sessionId, key -> new PresenceSession(username));
        presenceSession.setUsername(username);
        presenceSession.getGroups().add(groupId);

        ConcurrentMap<String, Integer> groupCounts = participantCountsByGroup.computeIfAbsent(groupId, key -> new ConcurrentHashMap<>());
        groupCounts.compute(username, (key, count) -> count == null ? 1 : count + 1);
    }

    public void leaveGroup(String sessionId, Long groupId) {
        if (sessionId == null || groupId == null) {
            return;
        }

        PresenceSession presenceSession = sessions.get(sessionId);
        if (presenceSession == null) {
            return;
        }

        presenceSession.getGroups().remove(groupId);

        ConcurrentMap<String, Integer> groupCounts = participantCountsByGroup.get(groupId);
        if (groupCounts != null) {
            decrementParticipantCount(groupId, presenceSession.getUsername(), groupCounts);
            if (groupCounts.isEmpty()) {
                participantCountsByGroup.remove(groupId);
            }
        }

        if (presenceSession.getGroups().isEmpty()) {
            sessions.remove(sessionId);
        }
    }

    public boolean isSessionInGroup(String sessionId, Long groupId) {
        PresenceSession presenceSession = sessions.get(sessionId);
        return presenceSession != null && presenceSession.getGroups().contains(groupId);
    }

    public String getSessionUsername(String sessionId) {
        PresenceSession presenceSession = sessions.get(sessionId);
        return presenceSession == null ? null : presenceSession.getUsername();
    }

    public List<String> getParticipants(Long groupId) {
        Map<String, Integer> participantCounts = participantCountsByGroup.get(groupId);
        if (participantCounts == null) {
            return new ArrayList<>();
        }

        return participantCounts.keySet().stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    public List<DisconnectedGroup> removeSession(String sessionId) {
        PresenceSession presenceSession = sessions.remove(sessionId);
        if (presenceSession == null) {
            return new ArrayList<>();
        }

        List<DisconnectedGroup> disconnectedGroups = new ArrayList<>();
        for (Long groupId : new HashSet<>(presenceSession.getGroups())) {
            ConcurrentMap<String, Integer> groupCounts = participantCountsByGroup.get(groupId);
            if (groupCounts != null) {
                decrementParticipantCount(groupId, presenceSession.getUsername(), groupCounts);
                if (groupCounts.isEmpty()) {
                    participantCountsByGroup.remove(groupId);
                }
            }
            disconnectedGroups.add(new DisconnectedGroup(groupId, presenceSession.getUsername()));
        }

        return disconnectedGroups;
    }

    private void decrementParticipantCount(Long groupId, String username, ConcurrentMap<String, Integer> groupCounts) {
        groupCounts.computeIfPresent(username, (key, count) -> {
            int next = count - 1;
            return next > 0 ? next : null;
        });
        if (groupCounts.isEmpty()) {
            participantCountsByGroup.remove(groupId);
        }
    }

    public static class DisconnectedGroup {
        private final Long groupId;
        private final String username;

        public DisconnectedGroup(Long groupId, String username) {
            this.groupId = groupId;
            this.username = username;
        }

        public Long getGroupId() {
            return groupId;
        }

        public String getUsername() {
            return username;
        }
    }

    private static class PresenceSession {
        private volatile String username;
        private final Set<Long> groups = ConcurrentHashMap.newKeySet();

        private PresenceSession(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Set<Long> getGroups() {
            return groups;
        }
    }
}
