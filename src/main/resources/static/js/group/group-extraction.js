import BlobBuilder from "../blob-builder.js";

//Fetch request to extract all groups
async function getAllGroups() {
    let data1;
    await fetch("/api/groups", {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//Fetch request to extract a group with a given id
async function getSingleGroup(id) {
    let data1;
    await fetch("/api/groups/" + String(id), {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

//fetch request to fetch groups in ranges (formatted as: [[i0,i1],[i0,i1]])
async function getAllGroupsInRanges(ranges) {
    let data1;
    // Since there's no bulk range endpoint, we'll fetch all and filter
    let allGroups = await getAllGroups();
    let filteredGroups = [];
    for (let range of ranges) {
        for (let i = range[0]; i <= range[1]; i++) {
            let group = allGroups.find(g => g.id === i);
            if (group) {
                filteredGroups.push(group);
            }
        }
    }
    return filteredGroups;
}

document.getElementById("export-selected").addEventListener("click", async () => {
    let ranges = [];
    document.getElementsByName("export-select").forEach((input)=>{
        if(input.checked){
            let id = Number(input.getAttribute("export-select-id"));
            ranges.push([id,id]);
        }
    })
    console.log(ranges);
    let content = await getAllGroupsInRanges(ranges);
    // Format for export: { name, period, memberIds }
    let exportData = content.map(group => ({
        name: group.name,
        period: group.period,
        memberIds: group.members ? group.members.map(m => m.id) : []
    }));
    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, exportData);
    blob.downloadBlob("groups");
})

document.getElementsByName("export").forEach((button)=>{
    button.addEventListener("click",async ()=>{
        let content = await getSingleGroup(button.getAttribute("export-id"));
        // Format for export: { name, period, memberIds }
        let exportData = {
            name: content.name,
            period: content.period,
            memberIds: content.members ? content.members.map(m => m.id) : []
        };
        const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, exportData);
        blob.downloadBlob("group");
    })
})

document.getElementById("export-all").addEventListener("click",async ()=>{
    let content = await getAllGroups();
    // Format for export: array of { name, period, memberIds }
    let exportData = content.map(group => ({
        name: group.name,
        period: group.period,
        memberIds: group.members ? group.members.map(m => m.id) : []
    }));
    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, exportData);
    blob.downloadBlob("groups");
})
