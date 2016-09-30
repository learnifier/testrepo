/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
define(['cocobox/ccb-imodal', 'es6-shim'], function( ccbImodal ) {
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
        else return "<em>Empty course session name</em>";
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

    };

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

        function formatSession ( d ) {
            function genHtml(items, addUrl) {
                var $container = $( '<div/>', { style: "width: 100%; overflow: hidden;" } );

                var $button = $( '<a/>', { 'class': 'btn btn-primary pull-right', href: addUrl, style: 'margin-top: 6px;' } );
                $button.append( $( '<i/>', { 'class': 'glyphicon glyphicon-plus-sign' } ) );
                $button.append( $( '<span> Add Session</span>' ) );
                $container.append( $button );

                var $sessionList = $( '<ul />', { style: 'max-width: 65vw; margin-top: 12px;' } );
                $container.append( $sessionList );

                if( items.length === 0 ) {
                  $container.append( $( '<p>No sessions have beend added yet</p>', { style: 'text-align: center;' } ) );
                }
                items.forEach( function ( item ) {
                  var $item = $( '<li/>', { } );
                  // TODO: Preparations for marking sessions as favorites
                  /*$item
                    .append( $( '<span/>', {
                      class: [ 'glyphicon', 'favorite-star', item.favorite ? 'glyphicon-star' : 'glyphicon-star-empty' ].join( ' ' ),
                      style: 'font-size: 18px; margin-right: 4px; display: inline-block; top: -1px;'
                    } )
                      .on( 'click', function () {
                        toggleFavorite( item.id )
                          .then( function () {
                            console.log( arguments )
                          });
                      } )
                  );*/
                  $item
                    .append( $( '<a/>', { href: item.url, title: item.name, style: 'overflow: hidden; text-overflow: ellipsis;' } )
                      .text( item.name )
                    );
                  $sessionList.append( $item );
                } );

                return $container;
            }
            var deferred = $.Deferred();
            $.ajax(settings.listSessionsUrl + "/" + d.course.id.id).done(function (data) {
                deferred.resolve(
                    genHtml(data.map(function (item) {
                        return {
                            id: item.id.id,
                            name: lookupName(item.id.id),
                            url: settings.sessionDetailsUrl + "/" + item.id.id
                        }
                    }), settings.newSessionUrl + "/" + d.course.id.id));

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
                                dt.ajax.reload();
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
                    openModal("course", settings.courseDetailsUrl + "/" + id);
                });

                function addCourse(e){
                    e.preventDefault();
                    openModal("course", settings.newCourseUrl);
                }

                $("#addcourse-btn").click(addCourse);
                $(document).on("click", "#addcourse-link", addCourse);

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
            });



        });
    };
    return exports;
});
