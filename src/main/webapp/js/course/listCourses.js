/* 
 * (c) Dabox AB 2013 All Rights Reserved
 */
define(['cocobox/ccb-imodal', 'es6-shim'], function(ccbImodal) {
    "use strict";
    var exports = {},
        settings;

    var sessionHash = {
        _sessionHash: {},
        _orphanSessions: [],
        addSession: function(id, val){
            this._sessionHash[id] = val;
        },
        addOrphan: function(val) {
            this._orphanSessions.push(val);
        },
        lookup: function(id) {
            return this._sessionHash[id];
        },
        getOrphans: function(){
            return this._orphanSessions;
        }
    };

    function lookupName(id) {
        var entry = sessionHash.lookup([id]);
        if(entry && entry.name) {
            return  entry.name
        }
        else return "<em>Empty course session name</em>";
    }

    exports.init = function(options) {

        settings = $.extend({
            listCoursesUrl: undefined,
            listSessionsUrl: undefined,
            newProjectUrl: undefined,
            newCourseUrl: undefined,
            courseDetailsUrl: undefined,
            sessionDetailsUrl: undefined
        }, options || {});

        function formatSession ( d ) {
            function genHtml(items, addUrl) {
                var table = $('<table/>', { "class": "", "style": "width: 100%;"}),
                    tbody = $('<tbody>', { "style": "border: 0;"}).appendTo(table),
                    addBtn = $("<a />", {"class": "btn btn-primary pull-right", "style": "margin-top: 10px;", "href": addUrl}).html("<i class='glyphicon glyphicon-plus-sign'></i> Add Session"),
                    title = $("<h3 />", {"class": "", "style": "margin-top: 10px;", "href": addUrl}).html("Sessions"),
                    header = '';

                if(addUrl) {
                    tbody.append($('<tr />').append($('<td />').append(addBtn).append(title)));
                } else {
                    tbody.append($('<tr />').append($('<td />').append(addBtn).append(title)));
                };
                    
                items.forEach(function(item){
                    tbody
                        .append($('<tr />', { "style": "border-bottom: 1px solid #f2f2f2;"})
                            .append($('<td />')
                                .append($('<a />', {href: item.url})
                                    .html(item.name))));
                });
                return table;
            }
            var deferred = $.Deferred();
            if(d.id === "orphans") {
                console.log(sessionHash.getOrphans());
                deferred.resolve(genHtml(sessionHash.getOrphans().map(function(item){
                    return {
                        name: item.name,
                        url: settings.projectDetailsUrl + "/" + item.id
                    }
                })));
            } else {
                $.ajax(settings.listSessionsUrl + "/" + d.course.id.id).done(function (data) {
                    deferred.resolve(
                        genHtml(data.map(function (item) {
                            return {
                                name: lookupName(item.id.id),
                                url: settings.sessionDetailsUrl + "/" + item.id.id
                            }
                        }), settings.newSessionUrl + "/" + d.course.id.id));

                });
            }
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
                                row.imagelinkDisplay = '<a data-courseid="' + row.course.id.id + '" class="courseLink" href="'+ row.link + '"><img width="32" src="'+row.thumbnailUrl+'" /></a>';

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
                                console.log();
                                if (!row.nameDisplay | !row.nameFilter) {
                                        row.nameFilter = row.course.name + ' ' + row.course.id.id;
                                        row.nameDisplay = '<a data-courseid="' + row.course.id.id + '" class="courseLink" href="' + "#" + '">' + row.course.name + '</a> ';
                                }

                                if (type === 'display') {
                                    return row.nameDisplay;
                                } else if (type === 'filter') {
                                    return row.nameFilter;
                                } else if (type === 'sort') {
                                    return row.course.name;
                                } else {
                                    //Anything else and raw row
                                    return row.course.name;
                                }
                            }
                        },
                        {
                            "targets": [2],
                            "class":          "details-control",
                            "orderable":      false,
                            "data":           function(){return '<a class="btn btn-link">Show sessions</a>'},
                            "defaultContent": ""
                        }
                    ],
                    "ajax": {
                        "url": settings.listCoursesUrl,
                        "dataSrc": ""
                    },
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

                function openModal(imodalId, url){
                    var imodal = new ccbImodal.Server({
                        serviceName: imodalId,
                        url: url,
                        callbacks: {
                            "close": function(data) {
                                console.log("Close lol");
                            },
                            "createAndForward": function(data){
                                window.location = data.url;
                            },
                            "saveDone": function(data){
                                dt.ajax.reload();
                                console.log("SaveDone: ", data);
                            }
                        }
                    });
                    imodal.open();
                }

                $.getJSON(settings.listProjectsUrl).done(function(data) {
                    data.aaData.forEach(function(project){
                        if(project.courseSessionId) {
                            sessionHash.addSession(project.courseSessionId, project);
                        } else {
                            sessionHash.addOrphan(project);
                        }
                    });
                });
                // dt.row.add({id: "orphans", categories: [], name: "Orphan projects"});

                $(document).on('click', '.courseLink', function(e){
                    console.log("Click .courseLink: ", $(this).data("courseid"));
                    var id = $(this).data("courseid");
                    e.preventDefault();
                    openModal("course", settings.courseDetailsUrl + "/" + id);
                });

                $("#addcourse-btn").click(function(e){
                    e.preventDefault();
                    openModal("course", settings.newCourseUrl);
                });


                $('#listcourses').on( 'click', 'tr td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = dt.row( tr );
                    var idx = $.inArray( tr.attr('id'), detailRows );
                    var link = tr.find(".details-control a");

                    if ( row.child.isShown() ) {
                        // tr.removeClass( 'details' );
                        link.text( 'Show Sessions' );
                        row.child.hide();

                        // Remove from the 'open' array
                        detailRows.splice( idx, 1 );
                    }
                    else {
                        // tr.addClass( 'details' );
                        link.text( 'Hide Sessions' );

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

