/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

   $(document).ready(function() {
        require(['dataTables-bootstrap', 'dataTables-responsive'], function() {
        var oTable = $('#listusers').dataTable({
            "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
            "order": [[0,'asc']],
            "initComplete": function() {
                $('#listusers_filter input').attr('placeholder', 'Search Client User Groups');
            },
            "columnDefs": [ 
                {
                    "targets": [ 0 ],
                    "className": "block-link",
                    "data" : function(row, type, set) {
                        if (!row.nameDisplay) {
                            var name = (row.name && row.name.length > 0) ? row.name : "(Name not set yet)";
                            row.nameDisplay = '<a href="'+ row.link + '">' +  name +'</a> ';
                        }

                        if (type === 'display') {
                            return row.nameDisplay;
                        } else if (type === 'filter') {
                            return row.name;
                        } else if (type === 'sort') {
                            return row.name;
                        } else {
                            //Anything else and raw row
                            return row.name;
                        }
                    }
                }
            ],
            "ajax": listUsersAjaxSource,
            "pageLength": 25,
            "pagingType": "full_numbers",
            "deferRender": true,
            "language": {
                "search": "",
                "zeroRecords": "No user matches your query",
                "emptyTable": "<span class='emptytable'>Start now by creating your <a href='" + newUserUrl + "'>first user</a></span>",
                "loadingRecords": "<p>Loading users...</p><img src='" + spinnerUrl + "' />"
            }
        });
        });
                
    } );

    return exports;
});

