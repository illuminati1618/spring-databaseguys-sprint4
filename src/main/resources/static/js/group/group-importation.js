//fetch request to import multiple groups
async function importAllGroups(fileContent) {
    let data1;
    let groupsArray = JSON.parse(fileContent);
    // The API expects { "groups": [...] } format
    let requestBody = JSON.stringify({ groups: groupsArray });
    await fetch("/api/groups/bulk", {
        method: "POST",
        body: requestBody,
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//fetch request to update a group with a given id using a json file
async function importGroupWithId(fileContent, id) {
    let data1;
    // First get the current group
    let currentGroup = await fetch("/api/groups/" + String(id), {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json());

    let groupData = JSON.parse(fileContent);
    
    // Update group name and period
    if (groupData.name || groupData.period) {
        await fetch("/api/groups/" + String(id), {
            method: "PUT",
            body: JSON.stringify({
                name: groupData.name || currentGroup.name,
                period: groupData.period || currentGroup.period
            }),
            cache: "no-cache",
            headers: new Headers({
                "content-type": "application/json"
            })
        });
    }

    // Handle member updates
    if (groupData.memberIds && Array.isArray(groupData.memberIds)) {
        let currentMemberIds = currentGroup.members ? currentGroup.members.map(m => m.id) : [];
        
        // Remove members not in the new list
        for (let memberId of currentMemberIds) {
            if (!groupData.memberIds.includes(memberId)) {
                await fetch("/api/groups/" + String(id) + "/members/" + String(memberId), {
                    method: "DELETE",
                    cache: "no-cache"
                });
            }
        }
        
        // Add new members
        for (let memberId of groupData.memberIds) {
            if (!currentMemberIds.includes(memberId)) {
                await fetch("/api/groups/" + String(id) + "/members/" + String(memberId), {
                    method: "POST",
                    cache: "no-cache"
                });
            }
        }
    }

    // Return updated group
    data1 = await fetch("/api/groups/" + String(id), {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json());
    
    return data1;
}

//fetch request to create a new group using a json file
async function importGroup(fileContent) {
    let data1;
    let groupData = JSON.parse(fileContent);
    await fetch("/api/groups", {
        method: "POST",
        body: JSON.stringify({
            name: groupData.name,
            period: groupData.period || "",
            memberIds: groupData.memberIds || []
        }),
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

document.getElementById("import-all").addEventListener("click", async () => {
    const input = document.getElementById("groupAllFileUpload");
    if (input.files.length != 1) {
        alert("You must upload a file.")
        return;
    }
    let file = input.files[0];
    let text = await file.text()
    if (!Array.isArray(JSON.parse(text))) {
        alert("This import is expecting an array.")
        return;
    }
    let content = await importAllGroups(text);
    if (content) {
        alert("Groups imported successfully!");
        location.reload();
    }
})

document.getElementsByName("import").forEach((button) => {
    button.addEventListener("click", async () => {

        let input;
        for (const child of button.parentElement.children) {
            if (child.classList.contains("group-single-file-upload")) {
                input = child;
            }
        }

        if (!input || input.files.length != 1) {
            alert("You must upload a file.")
            return;
        }
        let file = input.files[0];
        let text = await file.text();
        let groupData = JSON.parse(text);
        if (!groupData.name) {
            alert("This import is expecting an Object with the key of \"name\".")
            return;
        }
        let content = await importGroupWithId(text, button.getAttribute("import-id"));
        if (content) {
            alert("Group imported successfully!");
            location.reload();
        }
    })
})

document.getElementById("import-single").addEventListener("click", async () => {
    const input = document.getElementById("groupSingleFileUpload");
    if (input.files.length != 1) {
        alert("You must upload a file.")
        return;
    }
    let file = input.files[0];
    let text = await file.text()
    let groupData = JSON.parse(text);
    if (!groupData.name) {
        alert("This import is expecting an Object with the key of \"name\".")
        return;
    }
    let content = await importGroup(text);
    if (content) {
        alert("Group imported successfully!");
        location.reload();
    }
})
