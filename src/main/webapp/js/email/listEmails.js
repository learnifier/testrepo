/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([textSource], function(t) {
    "use strict";
    var exports = {};

    $(document).ready(function() {
        
    require(['dataTables-bootstrap', 'dataTables-responsive'], function() {
        var oTable = $('#listemails').dataTable({
            "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
            "paging": false,
            "order": [[0,'asc']],
            "initComplete": function() {
                $('#listemails_filter input').attr('placeholder', 'Search email templates');
            },
            "columnDefs": [
                {
                   "targets": [ 0 ],
                   "width": "70%",
                    "data" : function(row, type, set) {
                        if (!row.nameDisplay) {
                            row.nameDisplay = '<a href="'+ row.editlink + '" title="' + row.description  + '">' + row.name +'</a> ';
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
                                row.statusDisplay = t._T('cpweb.emailoverview.true');
                            } else {
                                row.statusDisplay = t._T('cpweb.emailoverview.false');
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
                },
				{
                    "targets": [ 2 ],
                    "data": "localeStr",
                    "className": "language"
                }
            ],

            "ajax": listEmailsAjaxSource,
            "language": {
                "search": "",
                "zeroRecords": "No email templates matches your query",
                "emptyTable": "<span class='emptytable'>You have no email templates on your account.</span>",
                "loadingRecords": "<p>Loading email templates...</p><img src='" + spinnerUrl + "' />"
            }
        });
        });
    } );

    return exports;
});

