/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

    var settings;

    exports.init = function(options) {

        settings = $.extend({
            listCoursesAjaxSource: undefined,
            newProjectUrl: undefined
        }, options || {});

        $(document).ready(function () {
            require(['dataTables-bootstrap'], function () {
                $('#listcourses').dataTable({
                    "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                    "order": [[1, 'asc']],
                    "initComplete": function () {
                        $('#listprojects_filter input').attr('placeholder', 'Search projects');
                    },
                    "columnDefs": [
                        {
                            "targets": [ 0 ],
                            "orderable": false,
                            "width": "32px",
                            "data" : function(row, type, set) {
                                row.imagelinkDisplay = '<a href="'+ "#" + '"><img width="32" src="'+row.thumbnail+'" /></a> ';

                                if (type === 'display') {
                                    return row.imagelinkDisplay;
                                } else if (type === 'filter') {
                                    return row.imagelinkDisplay;
                                } else if (type === 'sort') {
                                    return row.imagelinkDisplay;
                                } else {
                                    //Anything else and raw row
                                    return row.imagelinkDisplay;
                                }
                            }
                        },
                        {
                            "targets": [1],
                            "width": "70%",
                            "className": "block-link",
                            "data": function (row, type, set) {
                                if (!row.nameDisplay | !row.nameFilter) {
                                    row.nameFilter = row.name + ' ' + row.id;
                                    row.nameDisplay = '<a href="' + row.link + '">' + row.name + '</a> ';
                                }

                                if (type === 'display') {
                                    return row.nameDisplay;
                                } else if (type === 'filter') {
                                    return row.nameFilter;
                                } else if (type === 'sort') {
                                    return row.name;
                                } else {
                                    //Anything else and raw row
                                    return row.name;
                                }
                            }
                        },
                        {
                            "targets": [2],
                            "data": "categories"
                        }
                    ],
                    "ajax": settings.listCoursesAjaxSource,
                    "pageLength": 25,
                    "pagingType": "full_numbers",
                    "deferRender": true,
                    "language": {
                        "search": "",
                        "zeroRecords": "No projects matches your query",
                        "emptyTable": "<span class='emptytable'>Start now by creating your <a href='" + settings.newCourseUrl + "'>first course</a></span>",
                        "loadingRecords": "<p>Loading courses...</p><img src='" + settings.spinnerUrl + "' />"
                    }
                });
            });
        });
    };
    return exports;
});

