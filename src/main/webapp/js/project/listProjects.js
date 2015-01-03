/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

    $(document).ready(function() {
        require(['dataTables-bootstrap'], function() {
            $('#listprojects').dataTable({
                "dom": 'f<"clear">rt<"dataTables_footer clearfix"ip>',
                "order": [[1, 'asc']],
                "columnDefs": [
                    {
                        "orderable": false,
                        "data": function(data, type, val) {
                            if (type === 'set') {
                                data.favorite = val;
                                
                                if (val) {
                                    data.favoriteDisplay = '<div class="favorite isfav" onclick="toggleFavorite(this)"></div>';
                                } else {
                                    data.favoriteDisplay = '<div class="favorite isnotfav" onclick="toggleFavorite(this)"></div>';
                                }
                            } else if (type === 'display') {
                                return data.favoriteDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return data.favorite;
                            } else {
                                //Anything else and raw data
                                return data.favorite;
                            }
                        },
                        "targets": [0]
                    },
                    {
                        "width": "70%",
                        "data": function(data, type, val) {
                            if (type === 'set') {
                                data.name = val;
                                data.nameFilter = val + ' ' + data.id;
                                data.nameDisplay = '<a href="'+ data.link + '">' + val +'</a> ';
                            } else if (type === 'display') {
                                return data.nameDisplay;
                            } else if (type === 'filter') {
                                return data.nameFilter;
                            } else if (type === 'sort') {
                                return data.name;
                            } else {
                                //Anything else and raw data
                                return data.name;
                            }
                        },
                        
                        "targets": [1]
                    },
                    {
                        "data": "added",
                        "targets": [2]
                    },
                    {
                        "data": "invited",
                        "targets": [3]
                    }
                ],
                "sAjaxSource": listProjectsAjaxSource,
                "iDisplayLength": 25,
                "sPaginationType": "full_numbers",
                "oLanguage": {
                    "sSearch": "",
                    "sZeroRecords": "No projects matches your query",
                    "sEmptyTable": "<span class='emptytable'>Start now by creating your <a href='" + newProjectUrl + "'>first project</a></span>",
                    "sLoadingRecords": "<p>Loading projects...</p><img src='" + spinnerUrl + "' />"
                }
            });
        });
        $('#listprojects_filter input').attr('placeholder', 'Search projects');
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

