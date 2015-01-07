/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

    $(document).ready(function() {
       require(['dataTables-bootstrap'], function() {
         var oTable = $('#listdesigns').dataTable({
            "dom": '<"row"<"col-sm-6"><"col-sm-6"f>><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
            "paging": false,
            "order": [[0,'asc']],
            "initComplete": function() {
                $('#listdesigns_filter input').attr('placeholder', 'Search course templates');
            },
            "columnDefs": [
                {
                    "targets": [ 0 ],
                    "width": "70%",
                    "className": "block-link",
                    "data" : function(row, type, set) {
                        if (!row.nameDisplay) {
                            row.nameDisplay =  '<a href="'+ row.editlink + '" title="' + row.description  + '">' + row.name +'</a> ';
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
                 },
                {
                    "targets": [ 1 ],
                    "className": "control",
                    "data" : function(row, type, set) {
                        if (!row.statusDisplay) {
                            if ( row.sticky ) {
                                row.statusDisplay = '<span class="on">[@dws.txt key="cpweb.designoverview.true" /]</span>'
                            } else {
                                row.statusDisplay = '<span class="off">[@dws.txt key="cpweb.designoverview.false" /]</span>'
                            }
                        }

                        if (type === 'display') {
                            return row.statusDisplay;
                        } else if (type === 'filter') {
                            return row.statusDisplay;
                        } else if (type === 'sort') {
                            return row.statusDisplay;
                        } else {
                            //Anything else and raw row
                            return row.statusDisplay;
                        }
                    }
                }
            ],

            "ajax": listDesignsAjaxSource,
            "language": {
                "search": "",
                "zeroRecords": "No course templates matches your query",
                "emptyTable": "<span class='emptytable'>You have no course templates on your account.</span>",
                "loadingRecords": "<p>Loading course designs...</p><img src='" + spinnerUrl + "' />"
            }
        });
        });

    } );

    return exports;
});
