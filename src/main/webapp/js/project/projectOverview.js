/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

    $(document).ready(function() {
        require(['dataTables-bootstrap'], function() {
            $('#listprojects').dataTable({
                "dom": '<"row"<"col-sm-6"><"col-sm-6"f>><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                "order": [[1, 'asc']],
                "initComplete": function() {
                    $('#listprojects_filter input').attr('placeholder', 'Search projects');
                },
                "columnDefs": [
                    {
                        "targets": [0],
                        "orderable": false,
                        "data": function(row, type, set) {
                            if (row.favorite) {
                                row.favoriteDisplay = '<a onclick="toggleFavorite(this)"><span class="glyphicon glyphicon-star favorite-star" ></span></a>';
                            } else {
                                row.favoriteDisplay = '<a onclick="toggleFavorite(this)"><span class="glyphicon glyphicon-star-empty favorite-star"></span></a>';
                            }
                            if (type === 'display') {
                                return row.favoriteDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return row.favorite;
                            } else {
                                //Anything else and raw row
                                return row.favorite;
                            }
                        }
                    },
                    {
                        "targets": [1],
                        "width": "70%",
                        "className": "block-link",
                        "data" : function(row, type, set) {
                            if (!row.nameDisplay | !row.nameFilter) {
                                row.nameFilter = row.name + ' ' + row.id;
                                row.nameDisplay = '<a href="'+ row.link + '">' + row.name +'</a> ';
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
                        "data": "added"
                    },
                    {
                        "targets": [3],
                        "data": "invited"
                    }
                ],
                "ajax": listProjectsAjaxSource,
                "pageLength": 25,
                "pagingType": "full_numbers",
                "deferRender": true,
                "language": {
                    "search": "",
                    "zeroRecords": "No projects matches your query",
                    "emptyTable": "<span class='emptytable'>Start now by creating your <a href='" + newProjectUrl + "'>first project</a></span>",
                    "loadingRecords": "<p>Loading projects...</p><img src='" + spinnerUrl + "' />"
                }
            });
        });
    });

    window.toggleFavorite = function(target) {
        //log('Toggle ',target);
        requirejs(['dabox-common'], function() {
            var tr = $(target).closest('tr')[0];
            var rowData = $('#listprojects').dataTable().fnGetData(tr);

            var prjId = rowData.id;
            
            var ajax = {};
            var ajaxData = {projectId: prjId};
            ajax.data = ajaxData;
            ajax.success = function() {
                rowData.favorite = !rowData.favorite;
                $('#listprojects').dataTable().fnUpdate(rowData, tr, undefined, false);
            };

            if (rowData.favorite) {
                cocobox.ajaxPost(deleteFavorite, ajax);
            } else {
                cocobox.ajaxPost(addFavorite, ajax);
            }
        });
    };

    return exports;
});

