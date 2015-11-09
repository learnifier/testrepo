define(["dataTables-bootstrap", 'dataTables-responsive'], function () {
    "use strict";

    $('#listreports').dataTable({
        "dom": '<"row"<"col-sm-6"><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
        "paging": false,
        "columnDefs": [
            {
                "targets": [0],               
                "data": function (row, type, set) {
                    if (!row.nameDisplay) {
                        var extra = row.ownWindow ? 'target="_blank"' : '';

                        row.nameDisplay = '<a href="' + row.link + '" title="' + row.title + '" ' + extra + '>' + row.title + '</a> ';
                    }

                    if (type === 'display') {
                        return row.nameDisplay;
                    } else if (type === 'filter') {
                        return row.title;
                    } else if (type === 'sort') {
                        return row.title;
                    } else {
                        //Anything else and raw row
                        return row.title;
                    }
                }
            }
        ],
        "ajax": reportJson,
        "language": {
            "loadingRecords": "<img src='"+spinnerUrl+"' />",
        }
    });


});