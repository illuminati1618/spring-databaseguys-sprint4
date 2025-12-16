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

// Populate member list with checkboxes
async function populateMemberList() {
    const memberList = document.getElementById("memberList");
    const memberSearch = document.getElementById("memberSearch");
    const persons = await loadAllPersons();
    
    memberList.innerHTML = "";
    
    persons.forEach(person => {
        const div = document.createElement("div");
        div.className = "form-check mb-2";
        div.innerHTML = `
            <input class="form-check-input member-checkbox" type="checkbox" value="${person.id}" id="member-${person.id}">
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
document.getElementById("createGroupForm").addEventListener("submit", async function(event) {
    event.preventDefault();
    
    const name = document.getElementById("name").value;
    const period = document.getElementById("period").value;
    const selectedMemberIds = Array.from(document.querySelectorAll(".member-checkbox:checked"))
        .map(cb => parseInt(cb.value));
    
    if (!name || name.trim() === "") {
        alert("Group name is required.");
        return;
    }
    
    try {
        // Create group
        const response = await fetch("/api/groups", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name,
                period: period || "",
                memberIds: selectedMemberIds
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || "Failed to create group");
        }
        
        alert("Group created successfully!");
        window.location.href = "/mvc/groups/read";
    } catch (error) {
        console.error("Error creating group:", error);
        alert("Error creating group: " + error.message);
    }
});

// Load members when page loads
document.addEventListener("DOMContentLoaded", populateMemberList);
