$(document).ready(function () {
    // Support both personTable and groupTable
    let table;
    if ($('#personTable').length) {
        table = $('#personTable').DataTable({
            columnDefs: [
                { targets: [3, 4, 6, 8], visible: false } // Hide Email (index 3) and SID (index 6) by default
            ]
        });
    } else if ($('#groupTable').length) {
        table = $('#groupTable').DataTable({
            columnDefs: [
                { targets: [5], visible: false } // Hide Import/Export (index 5) by default
            ]
        });
    }

    if (table) {
        // Toggle column visibility and update button styles
        $('.toggle-column').on('click', function () {
            const column = table.column($(this).attr('data-column'));
            const isVisible = column.visible();
            column.visible(!isVisible);

            // Update button styles
            $(this).toggleClass('active', !isVisible);
            $(this).toggleClass('inactive', isVisible);
        });
    }
});