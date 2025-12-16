// Load all persons for member selection
async function loadAllPersons() {
    try {
        const response = await fetch("/api/people");
        if (!response.ok) throw new Error("Failed to fetch persons");
        return await response.json();
    } catch (error) {
        console.error("Error loading persons:", error);
        return [];
    }
}

// Load current group data
async function loadGroupData(groupId) {
    try {
        const response = await fetch("/api/groups/" + groupId);
        if (!response.ok) throw new Error("Failed to fetch group");
        return await response.json();
    } catch (error) {
        console.error("Error loading group:", error);
        return null;
    }
}

// Populate member list with checkboxes
async function populateMemberList() {
    const memberList = document.getElementById("memberList");
    const memberSearch = document.getElementById("memberSearch");
    const groupId = document.getElementById("groupId").value;
    
    const [persons, groupData] = await Promise.all([
        loadAllPersons(),
        loadGroupData(groupId)
    ]);
    
    if (!groupData) {
        alert("Failed to load group data");
        return;
    }
    
    const currentMemberIds = groupData.members ? groupData.members.map(m => m.id) : [];
    
    memberList.innerHTML = "";
    
    persons.forEach(person => {
        const div = document.createElement("div");
        div.className = "form-check mb-2";
        const isChecked = currentMemberIds.includes(person.id);
        div.innerHTML = `
            <input class="form-check-input member-checkbox" type="checkbox" value="${person.id}" id="member-${person.id}" ${isChecked ? "checked" : ""}>
            <label class="form-check-label" for="member-${person.id}">
                ${person.name} (${person.uid}) - ${person.email}
            </label>
        `;
        memberList.appendChild(div);
    });
    
    // Add search functionality
    memberSearch.addEventListener("input", function() {
        const searchTerm = this.value.toLowerCase();
        const checkboxes = memberList.querySelectorAll(".form-check");
        checkboxes.forEach(checkbox => {
            const label = checkbox.querySelector("label");
            const text = label.textContent.toLowerCase();
            checkbox.style.display = text.includes(searchTerm) ? "" : "none";
        });
    });
}

// Handle form submission
document.getElementById("updateGroupForm").addEventListener("submit", async function(event) {
    event.preventDefault();
    
    const groupId = document.getElementById("groupId").value;
    const name = document.getElementById("name").value;
    const period = document.getElementById("period").value;
    const selectedMemberIds = Array.from(document.querySelectorAll(".member-checkbox:checked"))
        .map(cb => parseInt(cb.value));
    
    if (!name || name.trim() === "") {
        alert("Group name is required.");
        return;
    }
    
    try {
        // Get current group to compare members
        const currentGroup = await loadGroupData(groupId);
        if (!currentGroup) {
            throw new Error("Failed to load current group data");
        }
        
        const currentMemberIds = currentGroup.members ? currentGroup.members.map(m => m.id) : [];
        
        // Update group name and period
        const updateResponse = await fetch("/api/groups/" + groupId, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name,
                period: period || ""
            })
        });
        
        if (!updateResponse.ok) {
            throw new Error("Failed to update group");
        }
        
        // Determine which members to add and remove
        const toRemove = currentMemberIds.filter(id => !selectedMemberIds.includes(id));
        const toAdd = selectedMemberIds.filter(id => !currentMemberIds.includes(id));
        
        // Remove members
        for (const memberId of toRemove) {
            const removeResponse = await fetch("/api/groups/" + groupId + "/members/" + memberId, {
                method: "DELETE"
            });
            if (!removeResponse.ok) {
                console.warn("Failed to remove member " + memberId);
            }
        }
        
        // Add members
        for (const memberId of toAdd) {
            const addResponse = await fetch("/api/groups/" + groupId + "/members/" + memberId, {
                method: "POST"
            });
            if (!addResponse.ok) {
                console.warn("Failed to add member " + memberId);
            }
        }
        
        alert("Group updated successfully!");
        window.location.href = "/mvc/groups/read";
    } catch (error) {
        console.error("Error updating group:", error);
        alert("Error updating group: " + error.message);
    }
});

// Load members when page loads
document.addEventListener("DOMContentLoaded", populateMemberList);
