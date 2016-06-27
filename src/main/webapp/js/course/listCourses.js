/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define(['cocobox/ccb-imodal', 'es6-shim'], function(ccbImodal) {
    "use strict";
    var exports = {};

    var settings;

    function openModal(imodalId, url){
        // TODO: Not sure what to share between calls here
        var imodal = new ccbImodal.Server({
            serviceName: imodalId,
            url: url,
            callbacks: {
                "close": function(data) {
                    console.log("Close lol");
                }
            }
        });
        imodal.open();
    }

    exports.init = function(options) {

        settings = $.extend({
            listCoursesUrl: undefined,
            listSessionsUrl: undefined,
            newProjectUrl: undefined,
            courseDetails: undefined,
            sessionDetails: undefined
        }, options || {});

        function formatSession ( d ) {
            var deferred = $.Deferred();

            $.ajax(settings.listSessionsUrl + "/" + d.id).done(function(data){
                console.log("listSessions: ", data);
                var table = $('<table/>', { "class": "lol"}),
                    tbody = $('<tbody>').appendTo(table);
                data.forEach(function(item){
                    tbody
                        .append($('<tr />')
                            .append($('<td />')
                                .append($('<a />', {href: settings.sessionDetails + "/" + "1212"})
                                    .text(item.name))));
                });
                deferred.resolve(table);
            });
            return deferred.promise();
        }

        $(document).ready(function () {
            require(['dataTables-bootstrap'], function () {
                var dt = $('#listcourses').DataTable({
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
                                row.imagelinkDisplay = '<a class="courseLink" href="'+ "#" + '"><img width="32" src="'+row.thumbnail+'" /></a>';

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
                                    row.nameDisplay = '<a data-courseid="' + row.id + '" class="courseLink" href="' + row.link + '">' + row.name + '</a> ';

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
                        },
                        {
                            "targets": [3],
                            "class":          "details-control",
                            "orderable":      false,
                            "data":           null,
                            "defaultContent": ""
                        }
                    ],
                    "ajax": settings.listCoursesUrl,
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
                // Array to track the ids of the details displayed rows
                var detailRows = [];

                $(document).on('click', '.courseLink', function(e){
                    var id = $(this).data("courseid");
                    e.preventDefault();
                    openModal("course", settings.courseDetails + "/" + id);
                });

                $('#listcourses').on( 'click', 'tr td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = dt.row( tr );
                    var idx = $.inArray( tr.attr('id'), detailRows );

                    if ( row.child.isShown() ) {
                        tr.removeClass( 'details' );
                        row.child.hide();

                        // Remove from the 'open' array
                        detailRows.splice( idx, 1 );
                    }
                    else {
                        tr.addClass( 'details' );

                        // row.child( formatLoading( row.data() ) ).show();
                        formatSession(row.data()).done(function(res){
                            row.child(res).show();
                        });

                        // Add to the 'open' array
                        if ( idx === -1 ) {
                            detailRows.push( tr.attr('id') );
                        }
                    }
                } );
                // On each draw, loop over the detailRows array and show any child rows
                dt.on( 'draw', function () {
                    $.each( detailRows, function ( i, id ) {
                        $('#'+id+' td.details-control').trigger( 'click' );
                    } );
                } );
            });



        });
    };
    return exports;
});

