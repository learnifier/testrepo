/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function () {
    "use strict";
    var exports = {};

    window.toggleFavorite = function (target) {
        //log('Toggle ',target);
        requirejs(['dabox-common'], function () {
            var tr = $(target).closest('tr')[0];
            var rowData = $('#listprojects').dataTable().fnGetData(tr);

            var prjId = rowData.id;

            var ajax = {};
            var ajaxData = {projectId: prjId};
            ajax.data = ajaxData;
            ajax.success = function () {
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

    $(document).ready(function () {
        require(['dataTables-bootstrap', 'dataTables-responsive'], function () {


            var userTable = $('#listusers').dataTable({
                "dom": '<"row"<"col-xs-12"i>><"row"<"col-xs-12"rt>>',
                "paging": false,
                "order": [[1, 'asc']],
                "initComplete": function () {
                    var nUsers = userTable._('tr').length;
                    $('#lusers a').append('<span> (' + nUsers + ')</span>');
                },
                "columnDefs": [
                    {
                        "targets": [0],
                        "orderable": false,
                        "data": function (row, type, set) {
                            if (!row.imagelink24Display) {
                                if (row.imagelink24) {
                                    row.imagelink24Display = '<a href="' + row.link + '"><img src="' + row.imagelink24 + '" class="userimage" /></a> ';
                                } else {
                                    row.imagelink24Display = '<a href="' + row.link + '"><img src="${cocoboxCdn}/cocobox/img/cp/userImage_24x24.png" class="userimage" /></a> ';
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
                        "targets": [1],
                        "width": "50%",
                        "className": "block-link",
                        "data": function (row, type, set) {
                            if (!row.nameDisplay) {
                                row.nameDisplay = '<a href="' + row.link + '">' + row.name + '</a> ';
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
                        "targets": [2],
                        "data": "email"
                    }
                ],
                "ajax": listUsersAjaxSource,
                "language": {
                    "info": "Users - _TOTAL_ hits",
                    "infoEmpty": "Users - _TOTAL_ hits",
                    "zeroRecords": "No user matches your query",
                    "emptyTable": "No user matches your query",
                    "loadingRecords": "<p>Loading users...</p><img src='" + spinnerUrl + "' />"
                }
            });


            var projectTable = $('#listprojects').dataTable({
                "dom": '<"row"<"col-xs-12"i>><"row"<"col-xs-12"rt>>',
                "paging": false,
                "order": [[1, 'asc']],
                "initComplete": function () {
                    var nProjects = projectTable._('tr').length;
                    $('#lprojects a').append('<span> (' + nProjects + ')</span>');
                },
                "columnDefs": [
                    {
                        "targets": [0],
                        "orderable": false,
                        "data": function (row, type, set) {
                            if (!row.favoriteDisplay) {
                                if (row.favorite) {
                                    row.favoriteDisplay = '<a onclick="toggleFavorite(this)"><span class="glyphicon glyphicon-star favorite-star" ></span></a> ';
                                } else {
                                    row.favoriteDisplay = '<a onclick="toggleFavorite(this)"><span class="glyphicon glyphicon-star-empty favorite-star"></span></a> ';
                                }
                            }

                            if (type === 'display') {
                                return row.favoriteDisplay;
                            } else if (type === 'filter') {
                                return row.favoriteDisplay;
                            } else if (type === 'sort') {
                                return row.favoriteDisplay;
                            } else {
                                //Anything else and raw row
                                return row.favoriteDisplay;
                            }
                        }
                    },
                    {
                        "targets": [1],
                        "width": "70%",
                        "className": "block-link",
                        "data": function (row, type, set) {
                            if (!row.nameDisplay) {
                                row.nameDisplay = '<a href="' + row.link + '">' + row.name + '</a> ';
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
                        "aTargets": [2],
                        "data": "added"
                    },
                    {
                        "aTargets": [3],
                        "data": "invited"
                    }
                ],
                "ajax": listProjectsAjaxSource,
                "language": {
                    "info": "Projects - _TOTAL_ hits",
                    "infoEmpty": "Projects - _TOTAL_ hits",
                    "zeroRecords": "No project matches your query",
                    "emptyTable": "No project matches your query",
                    "loadingRecords": "<p>Loading projects...</p><img src='" + spinnerUrl + "' />"
                },
            });

            var productTable = $('#listproducts').dataTable({
                "dom": '<"row"<"col-xs-12"i>><"row"<"col-xs-12"rt>>',
                "paging": false,
                "order": [[1, 'asc']],
                "initComplete": function () {
                    var nProducts = productTable._('tr').length;
                    $('#lproducts a').append('<span> (' + nProducts + ')</span>');
                },
                "columnDefs": [
                    {
                        "targets": [0],
                        "orderable": false,
                        "className": "material-thumbnail",
                        "data": function (row, type, set) {
                            if (!row.thumbnailDisplay) {
                                if (row.thumbnail) {
                                    return  '<img src="' + row.thumbnail + '" />';
                                } else {
                                    return  '<div class="' + row.type + '"></div>';
                                }
                            }

                            if (type === 'display') {
                                return row.thumbnailDisplay;
                            } else if (type === 'filter') {
                                return row.type;
                            } else if (type === 'sort') {
                                return row.type;
                            } else {
                                //Anything else and raw row
                                return row.thumbnailDisplay;
                            }
                        }
                    },
                    {
                        "targets": [1],
                        "data": function (row, type, set) {
                            if (!row.titleDisplay) {
                                row.titleDisplay = row.title;
                            }

                            if (type === 'display') {
                                return row.titleDisplay;
                            } else if (type === 'filter') {
                                return row.title;
                            } else if (type === 'sort') {
                                return row.title;
                            } else {
                                //Anything else and raw row
                                return row.titleDisplay;
                            }
                        }
                    },
                    {
                        "targets": [2],
                        "data": function (row, type, set) {
                            if (!row.typeDisplay) {
                                row.typeDisplay = row.type;
                            }

                            if (type === 'display') {
                                return row.typeDisplay;
                            } else if (type === 'filter') {
                                return row.type;
                            } else if (type === 'sort') {
                                return row.type;
                            } else {
                                //Anything else and raw row
                                return row.typeDisplay;
                            }
                        }
                    }
                ],
                "ajax": listMaterialsAjaxSource,
                "language": {
                    "info": "Materials - _TOTAL_ hits",
                    "infoEmpty": "Materials - _TOTAL_ hits",
                    "zeroRecords": "No material matches your query",
                    "emptyTable": "No material matches your query",
                    "loadingRecords": "<p>Loading materials...</p><img src='" + spinnerUrl + "' />"
                }
            });

        });
    });

    return exports;
});

