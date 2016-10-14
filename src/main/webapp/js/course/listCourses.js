/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
define(['handlebars', 'cocobox/ccb-imodal', 'es6-shim', 'messenger'], function( Handlebars ) {
    "use strict";
    var exports = {},
        settings;

    function getExpanded() {
      try {
        var stored = localStorage.getItem( 'session-expanded' ) || '[]';
        return JSON.parse( stored );
      } catch ( e ) {
        return [];
      }
    }

    function setExpanded( id, isExpanded ) {
      try {
        var parsed = getExpanded();
        var i = parsed.indexOf( id );
        if ( isExpanded ) {
          if ( i !== -1 ) {
            return;
          }
          parsed.push( id );
        } else {
          if ( i === -1 ) {
            return;
          }
          parsed.splice( i, 1 )
        }
        localStorage.setItem( 'session-expanded', JSON.stringify( parsed ) );
      } catch ( e ) {}
    }

    var sessionHash = { // TODO: Can probably simplify this now that "orphan projects" concept is gone.
        _sessionHash: {},
        addSession: function(id, val){
            this._sessionHash[id] = val;
        },
        lookup: function(id) {
            return this._sessionHash[id];
        }
    };

    function lookupName(id) {
        var entry = sessionHash.lookup([id]);
        if(entry && entry.name) {
            return  entry.name;
        }
        else return "Empty course session name";
    }

    function deleteCourse(id) {
        var performDelete = function() {
            var dlg = cocobox.longOp();

            $.post(settings.deleteSessionUrl, {"id": id}).
                    always(function (data) {
                        dlg.abort();

                        return data;
                    }).
                    done(function (data) {
                        if (data.status === "ok") {
                            window.location.href = window.location.href;
                        } else if (data.status === "notempty") {
                            cocobox.infoDialog("Unable to delete", "Only empty courses can be deleted");
                        } else {
                            cocobox.errorDialog("Unknown error", "Unknown status code from server: "+data.status);
                        }
                    }).fail(function() {
                        dlg.abort();
                    }).fail(cocobox.internal.ajaxError);
        };

        require(['dabox-common'], function() {
            cocobox.confirmationDialogYesNo("Delete course?", "Do you want to delete this course?", performDelete);
        });
    }

    exports.init = function(options) {

        settings = $.extend({
            listCoursesUrl: undefined,
            listSessionsUrl: undefined,
            newProjectUrl: undefined,
            newCourseUrl: undefined,
            courseDetailsUrl: undefined,
            sessionDetailsUrl: undefined,
            deleteSessionUrl: undefined,
            selectedCourses : []
        }, options || {});

        var itemTemplate = Handlebars.compile($('#sessions-template').html());

        function formatSession ( row ) {
            console.log("Format: ", row.id);
            function genHtml(sessions) {
                console.log("sessions = ", sessions);
                return itemTemplate({courseId: row.id, sessions: sessions});
            }
            var deferred = $.Deferred();
            $.ajax(settings.listSessionsUrl + "/" + row.id).done(function (data) {
                deferred.resolve(
                    genHtml(data.map(function (item) {
                        return {
                            id: item.id.id,
                            name: lookupName(item.id.id)
                        }
                    }), settings.newSessionUrl + "/" + row.id.id));

            });
            return deferred.promise();
        }

        $(document).ready(function () {

            require(['dataTables-bootstrap'], function () {
                var columnDefs = [
                    {
                        "targets": [0],
                        "orderable": false,
                        "width": "32px",
                        "data": function (row, type, set) {
                            row.imagelinkDisplay = '<a data-courseid="' + row.course.id.id + '" class="courseLink" href="' + row.link + '"><img width="32" src="' + row.thumbnailUrl + '" /></a>';

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
                        "class": "details-control",
                        "orderable": false,
                        "render": function (row, type, val, meta) {
                            return '<a data-btntype="showhide" href="#">Edit</a>'
                        },
                        "defaultContent": ""
                    }
                ];

                if (settings.deleteSessionUrl) {
                    columnDefs.push({
                        "targets": [3],
                        "class": "details-control",
                        "orderable": false,
                        "data": function () {
                            return '<a data-btntype="delcourse" href="#">Delete</a>';
                        },
                        "defaultContent": ""
                    });
                }


                var dt = $('#listcourses').DataTable({
                    "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                    "order": [[1, 'asc']],
                    "columnDefs": columnDefs,
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
                        "emptyTable": "<span class='emptytable'>Start now by creating your <a id='addcourse-link' href='#'>first course</a></span>",
                        "loadingRecords": "<p>Loading courses...</p><img src='" + settings.spinnerUrl + "' />"
                    },
                    "createdRow": function ( row, data, index ) {
                        $( row ).attr( 'data-id', data.id );
                        var expanded = getExpanded();
                        if ( expanded.indexOf( data.id ) !== -1 ||
                             settings.selectedCourses.indexOf(data.id) !== -1) {
                            drawSessionSection(dt.row(row));
                        }
                    }
                });

                function openModal(imodalId, url){
                    var imodal = new ccbImodal.Server({
                        serviceName: imodalId,
                        url: url,
                        callbacks: {
                            "close": function(data) {},
                            "createAndForward": function(data){
                                window.location = data.url;
                            },
                            "saveDone": function(data){
                                window.location = settings.listCoursesPage;
                                // dt.ajax.reload();
                            }
                        }
                    });
                    imodal.open();
                }

                $.getJSON(settings.listProjectsUrl).done(function(data) {
                    data.aaData.forEach(function(project){
                        sessionHash.addSession(project.courseSessionId, project);
                    });
                });

                $('#listcourses').on('click', '.details-control', function(e) {
                    var id = $(this).data("courseid");
                    e.preventDefault();
                    console.log("edit: ", $(this), id);
                    openModal("course", settings.courseDetailsUrl + "/" + id);
                });

                function addCourse(e){
                    if(e) {
                        e.preventDefault();
                    }
                    openModal("course", settings.newCourseUrl);
                }

                $("#addcourse-btn").click(addCourse);
                $(document).on("click", "#addcourse-link", addCourse);
                if(settings.initiateCreate) {
                    addCourse();
                }

                $('#listcourses').on( 'click', 'a[data-btntype=delcourse]', function (e) {
                    var row = dt.row($(this).closest('tr'));
                    deleteCourse(row.data().id);

                    //Do not bubble this event, otherwise we get a toggle show/hide as well
                    e.preventDefault();
                    return false;
                });

                function drawSessionSection(row) {
                  var id = row.data().id;
                  if ( row.child.isShown() ) {
                      row.child.hide();
                      setExpanded( id, false );
                  }
                  else {
                      row.child("Loading...").show();
                      setExpanded( id, true );
                      formatSession(row.data()).done(function(res){
                          row.child(res);
                      });
                  }
                }

                $('#listcourses').on( 'click', 'tr', function ( e ) {
                    if ( $( e.target ).closest( '.details-control' ).length === 0 ) {
                      var tr = $(this).closest('tr');
                      var row = dt.row( tr );
                      drawSessionSection(row);
                      return false;
                    }
                } );
                // Event handlers for template sessions-template

                $(document).on('click', '.session-name', function(){
                    var sessionId = $(this).parent('tr').data("session-id");
                    window.location = settings.sessionDetailsUrl + "/" + sessionId;
                    return false;
                });

                $(document).on('click', '.session-copy', function() {
                    var sessionId = $(this).parent('tr').data("session-id");
                    $.post(settings.copySessionUrl, {"sessionId": sessionId}).done(function(data){
                        console.log("copied: ", data);
                        if(data.status == "ok") {
                            window.location = settings.projectDetailsUrl + "/" + data.projectId;
                        } else {
                            CCBMessengerError("Could not copy session: ", data.message);
                        }
                    }).fail(function(jqXHR, textStatus, errorThrown){
                        CCBMessengerError("Could not copy session.");
                        console.log("Could not copy session:", errorThrown);
                    });
                    return false;
                });

                $(document).on('click', '.session-create', function() {
                    var courseId = $(this).parents('table').data("course-id");
                    $.post(settings.createSessionUrl, {"courseId": courseId}).done(function (data) {
                        if (data.status == "ok") {
                            window.location = settings.projectDetailsUrl + "/" + data.projectId;
                        } else {
                            CCBMessengerError("Could not create empty course session: ", data.message);
                        }
                    }).fail(function (jqXHR, textStatus, errorThrown) {
                        CCBMessengerError("Could not create empty course session.");
                        console.log("Could not create empty course session:", errorThrown);
                    });
                    return false;
                });
            });



        });
    };
    return exports;
});
