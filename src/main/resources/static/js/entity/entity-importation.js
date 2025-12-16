// Generic entity import logic (used for person, game, etc.)
// Auto-detect entity name from page
const entityName = document.querySelector('[data-entity-name]')?.getAttribute('data-entity-name') || 'person';

//fetch request to import multiple entities
async function importAllEntities(fileContent) {
    let data1;
    await fetch(`/mvc/import/all/${entityName}`, {
        method: "POST",
        body: fileContent,
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//fetch request to update an entity with a given id using a json file
async function importEntityWithId(fileContent, id) {
    let data1;
    await fetch(`/mvc/import/${entityName}/${id}`, {
        method: "POST",
        body: fileContent,
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//fetch request to create a new entity using a json file
async function importEntity(fileContent) {
    let data1;
    await fetch(`/mvc/import/${entityName}`, {
        method: "POST",
        body: fileContent,
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

document.getElementById("import-all")?.addEventListener("click", async () => {
    const input = document.getElementById(entityName + "AllFileUpload");
    if (!input || input.files.length != 1) {
        alert("You must upload a file.")
        return;
    }
    let file = input.files[0];
    let text = await file.text()
    if (!Array.isArray(JSON.parse(text))) {
        alert("This import is expecting an array.")
        return;
    }
    await importAllEntities(text);
    alert("Import completed!");
    location.reload();
})

document.getElementsByName("import").forEach((button) => {
    button.addEventListener("click", async () => {

        let input;
        for (const child of button.parentElement.children) {
            const id = child.getAttribute("id");
            if (id && id.includes("SingleFileUpload")) {
                input = child;
            }
        }

        if (!input || input.files.length != 1) {
            alert("You must upload a file.")
            return;
        }
        let file = input.files[0];
        let text = await file.text();
        let parsed = JSON.parse(text);
        
        // Basic validation - just check it's an object
        if (typeof parsed !== 'object' || Array.isArray(parsed)) {
            alert("This import is expecting a JSON object.")
            return;
        }
        
        await importEntityWithId(text, button.getAttribute("import-id"));
        alert("Import completed!");
        location.reload();
    })
})

document.getElementById("import-single")?.addEventListener("click", async () => {
    const input = document.getElementById(entityName + "SingleFileUpload");
    if (!input || input.files.length != 1) {
        alert("You must upload a file.")
        return;
    }
    let file = input.files[0];
    let text = await file.text()
    let parsed = JSON.parse(text);
    
    // Basic validation - just check it's an object
    if (typeof parsed !== 'object' || Array.isArray(parsed)) {
        alert("This import is expecting a JSON object.")
        return;
    }
    
    await importEntity(text);
    alert("Import completed!");
    location.reload();
})
