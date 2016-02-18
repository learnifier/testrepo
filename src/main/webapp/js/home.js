/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define([], function() {
    "use strict";
    var exports = {};

    $(document).ready(function() {
        require(['dataTables-bootstrap','dabox-common', 'jquery.timeago'], function() {
            var actTable = $('#tw_actions').dataTable({
                "dom": '<"row"<"col-sm-12"rt>>',
                "paging": false,
                "order": [[3,'asc']],
                "initComplete": function() {
                    var itemCount = actTable._('tr').length;
                    if (itemCount > 0) {
                        $('#actions').slideDown();
                        $('.timeago').timeago();
                    }
                    if (itemCount > 1) {
                        $('#multiple-actions').slideDown();
                    } else {
                        $('#list-actions').slideDown();
                    }
                },
                "columnDefs": [
                    {
                        "targets": [ 0 ],
                        "width": "64",
                        "orderable": false,
                        "className": "material-thumbnail",
                        "data": function(row, type, set) {
                            if (!row.thumbnailDisplay) {
                                row.thumbnailDisplay = '<a href="'+ row.link + '"><img src="'+ row.thumbnail + '" alt="' + row.projectName +' thumbnail" /></a>';
                            }
                            if (type === 'display') {
                                return row.thumbnailDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return row.thumbnailDisplay;
                            } else {
                                //Anything else and raw row
                                return row.thumbnailDisplay;
                            }
                        }
                    },
                    {
                        "targets": [ 1 ],
                        "width": "500",
                        "orderable": false,
                        "className": "action-info",
                        "data": function(row, type, set) {
                            if (!row.templateDisplay) {
                                row.templateDisplay = '<a href="'+ row.link + '"><p class="action-name">' + row.projectName +'</p>';

                                if (row.bounceCount > 0 ){
                                    row.templateDisplay = row.templateDisplay + '<div class="bounces-action"><span class="bounce-count">' + row.bounceCount + ' </span>' + 
                                    '<span class="bounce-text">email(s) bounced back</span></div>';
                                }

                                if (row.alerts > 0) {
                                    row.templateDisplay = row.templateDisplay + '<div class="alerts-action"><span class="alert-count">' +  row.alerts + ' </span>' + 
                                    '<span class="alert-text">participant(s) could not be activated</span></div>'
                                }

                                row.templateDisplay = row.templateDisplay + '</a>';
                            }
                            if (type === 'display') {
                                return row.templateDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return row.projectName;
                            } else {
                                //Anything else and raw row
                                return row.templateDisplay;
                            }
                        }
                    },
                    {
                        "targets": [ 2 ],
                        "width": "150",
                        "orderable": false,
                        "className": "action-date",
                        "data": function(row, type, set) {
                            if (!row.oldestDisplay) {
                            row.oldestDisplay = '<abbr class="timeago" title="' + row.oldestAgo + '">' + row.oldestStr+ '</abbr>';
                            }
                            if (type === 'display') {
                                return row.oldestDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return row.oldestStr;
                            } else {
                                //Anything else and raw row
                                return row.oldestDisplay;
                            }
                        }
                    },
                    {
                       "targets": [ 3 ],
                       "width": "120",
                       "orderable": false,
                       "className": "action-button",
                        "data": function(row, type, set) {
                            if (!row.actionDisplay) {
                            row.actionDisplay = '<a href="'+ row.link + '" class="btn btn-primary-outlined">Action</a>';
                            }
                            if (type === 'display') {
                                return row.actionDisplay;
                            } else if (type === 'filter') {
                                return null;
                            } else if (type === 'sort') {
                                return row.actionDisplay;
                            } else {
                                //Anything else and raw row
                                return row.actionDisplay;
                            }
                        }
                    }

                ],
                "ajax": listActionsAjaxSource,
                "language": {
                    "emptyTable": "<span class='emptytable'>No bounced back emails.</span>",
                    "loadingRecords": "<p>Loading bounce back notifications...</p><img src='" + spinnerUrl + "' />"
                }
            });
            
            var favTable = $('#tw-favorites').dataTable({
                "dom": '<"row"<"col-sm-12"rt>>',
                "paging": false,
                "order": [[1,'asc']],
                "columnDefs": [
                    {
                        "aTargets": [ 0 ],
                        "orderable": false,
                        "width": "5%",
                        "data": function(row, type, set) {
                            if (row.favorite) {
                                row.favoriteDisplay = '<a onclick="toggleFavorite(this)"><span class="glyphicon glyphicon-star favorite-star fav-star" ></span></a>';
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
                        "targets": [ 1 ],
                        "width": "95%",
                        "orderable": false,
                        "className": "favdetails",
                        "data" : function(row, type, set) {
                            if (!row.nameDisplay) {
                                row.nameDisplay =   '<a href="'+ row.link + '"><p class="name">' + row.name +'</p>' +
                                '<p class="details"><span>Added: ' + row.added + '</span> <span>Invited: ' +  row.invited + '</span></p></a>';
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
                "ajax": listFavoritesAjaxSource,
                "language": {
                    "emptyTable": "<span class='emptytable'><a href='" + projectListUrl + "'>Click the star next to a project to add it as a favorite.</a></span>",
                    "loadingRecords": "<p>Loading favorite projects...</p><img src='" + spinnerUrl + "' />"
                }
            });            

        });
    });
 
    return exports;
});

