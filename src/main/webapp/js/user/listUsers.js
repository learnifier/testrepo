/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {}, settings;


    exports.init = function(options) {
        settings = $.extend({
            createUserFn: undefined
        }, options || {});

        $(document).ready(function() {
            require(['dataTables-bootstrap', 'dabox-common'], function() {
                var oTable = $('#listusers').dataTable({
                    "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                    "order": [[1,'asc']],
                    "initComplete": function() {
                        $('#listusers_filter input').attr('placeholder', 'Search users');
                    },
                    "columnDefs": [
                        {
                            "targets": [ 0 ],
                            "orderable": false,
                            "width": "32px",
                            "data" : function(row, type, set) {
                                if (!row.imagelink24Display) {
                                    if (row.imagelink24) {
                                        row.imagelink24Display = '<a href="'+ row.link + '"><img src="'+row.imagelink24+'" class="userimage" /></a> ';
                                    } else {
                                        row.imagelink24Display = '<a href="'+ row.link + '"><img src="${cocoboxCdn}/cocobox/img/cp/userImage_24x24.png" class="userimage" /></a> ';
                                    }
                                }

                                if (type === 'display') {
                                    return row.imagelink24Display;
                                } else if (type === 'filter') {
                                    return row.imagelink24Display;
                                } else if (type === 'sort') {
                                    return row.imagelink24Display;
                                } else {
                                    //Anything else and raw row
                                    return row.imagelink24Display;
                                }
                            }
                        },
                        {
                            "targets": [ 1 ],
                            "className": "block-link",
                            "data" : function(row, type, set) {
                                if (!row.nameDisplay) {
                                    var name = (row.name && row.name.length > 0) ? row.name : "Name not set yet...";
                                    row.nameDisplay = '<a href="'+ row.link + '">' +  cocobox.internal.escapeHTML(name) +'</a> ';
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
                            "targets": [ 2 ],
                            "data": "email",
                            "render": $.fn.dataTable.render.text()
                        }
                    ],
                    "ajax": listUsersAjaxSource,
                    "pageLength": 25,
                    "pagingType": "full_numbers",
                    "deferRender": true,
                    "language": {
                        "search": "",
                        "zeroRecords": "No user matches your query",
                        "emptyTable": '<span class="emptytable">Start now by creating your <a href="#" id="addFirstUser">first user</a></span>',
                        "loadingRecords": "<p>Loading users...</p><img src='" + spinnerUrl + "' />"
                    }
                });
            });
        });
        $(document).on('click', '#addFirstUser', settings.createUserFn);
    };
    return exports;
});

