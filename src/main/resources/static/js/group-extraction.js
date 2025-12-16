import BlobBuilder from "./blob-builder.js";

async function getAllGroup() {
    let data1;
    await fetch("/api/groups", {
        method: "GET",
        cache: "no-cache",
    }).then((response) => response.json()).then((data) => {
        data1 = data;
    })
    return data1;
}

document.getElementById("export-all-groups").addEventListener("click", async () => {
    let groups = await getAllGroup();

    // Map groups to expected import format
    let exportData = groups.map(group => ({
        name: group.name,
        period: group.period,
        memberIds: group.members ? group.members.map(member => member.id) : []
    }));

    const blob = new BlobBuilder(BlobBuilder.fileTypeEnum.json, exportData);
    blob.downloadBlob("groups");
});


