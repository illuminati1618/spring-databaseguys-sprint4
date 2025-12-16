// Generic entity extraction logic (used for person, game, etc.)
import BlobBuilder from "../blob-builder.js";

// Auto-detect entity name from page
const entityName = document.querySelector('[data-entity-name]')?.getAttribute('data-entity-name') || 'person';

//Fetch request to extract all entities
async function getAllEntities() {
    let data1;
    await fetch(`/mvc/extract/all/${entityName}`, {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//Fetch request to extract a single entity with a given id
async function getSingleEntity(id) {
    let data1;
    await fetch(`/mvc/extract/${entityName}/${id}`, {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//fetch request to fetch entities in ranges (formatted as: [[i0,i1],[i0,i1]])
async function getAllEntitiesInRanges(ranges) {
    let data1;
    await fetch(`/mvc/extract/all/${entityName}/fromRanges`, {
        method: "POST",
        body: JSON.stringify(ranges),
        cache: "no-cache",
        headers: new Headers({
            "content-type": "application/json"
        })
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

document.getElementById("export-ranges")?.addEventListener("click", async () => {
    let ranges = [];
    let body = document.getElementById("exportTableBody");
    if (!body) return;
    while (body.lastElementChild) {
        if (body.lastElementChild.nodeName == "TR") {
            const row = body.lastElementChild;
            if (row.firstElementChild.nodeName == "TD") {
                const col = row.firstElementChild;
                if (col.firstElementChild.firstElementChild.nodeName == "INPUT"
                    && col.firstElementChild.lastElementChild.nodeName == "INPUT") {
                    let value1 = Number(col.firstElementChild.firstElementChild.value||0);
                    let value2 = Number(col.lastElementChild.lastElementChild.value||0);
                    ranges.push([value1,value2]);
                }
            }
        }
        body.lastElementChild.remove();
    }
    let content = await getAllEntitiesInRanges(ranges);
    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, content);
    blob.downloadBlob(entityName + "s");
})

document.getElementById("export-selected")?.addEventListener("click", async () => {
    let ranges = [];
    document.getElementsByName("export-select").forEach((input)=>{
        if(input.checked){
            let id = Number(input.getAttribute("export-select-id"));
            ranges.push([id,id]);
        }
    })
    let content = await getAllEntitiesInRanges(ranges);
    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, content);
    blob.downloadBlob(entityName + "s");
})

document.getElementsByName("export").forEach((button)=>{
    button.addEventListener("click",async ()=>{
        let content = await getSingleEntity(button.getAttribute("export-id"));
        const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, content);
        blob.downloadBlob(entityName);
    })
})

document.getElementById("export-all")?.addEventListener("click",async ()=>{
    let content = await getAllEntities();
    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, content);
    blob.downloadBlob(entityName);
})

// keep any existing exportTableAddRow logic here if needed
