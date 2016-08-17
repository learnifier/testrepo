/* global participationActivationSpinnerUrl, cpwebMainJs, cpwebProjectCommon, projectRosterUrl, spinnerUrl, impersonateEnabled, moveEnabled, rapsUrl, impersonateLink, cocobox, cpweb, permissionEditProjectRoster, groupInfoUrl, addMembersByGroupUrl, listGroupMembersUrl */

define(['cocobox-handlebars', 'dataTables-bootstrap', 'jquery.timeago', 'cocobox-icheck', cpwebProjectCommon, cpwebMainJs], function (hb) {
    "use strict";

    $(document).ready(function () {
        var source = $("#roster-cell-action-template").html();
        var rosterCellActionTemplate = hb.compile(source);
        var cbCounter = 0;

        var createRowCallback = function (nRow, oObj) {
            $(nRow).removeClass("error inactive unverified expired");

            if (oObj.inError && !oObj.activationPending) {
                $(nRow).addClass('error');
                $(nRow).find('td').css('background', 'rgba(255,0,0,0.1)');
                return nRow;
            } else if (!oObj.activated) {
                $(nRow).addClass('inactive');
                $(nRow).find('.name').css('opacity', '0.6');
                return nRow;
            } else if (oObj.verificationStatus === 'UNVERIFIED') {
                $(nRow).addClass('unverified');
                $(nRow).attr('title', 'The email address has not been verified.');
                return nRow;
            } else if (oObj.expired) {
                $(nRow).attr('title', 'This participation has expired');
                $(nRow).addClass('expired');
                return nRow;
            }
        };

        window.oTable = $('#projectroster').dataTable({
            "dom": '<"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>><"row"<"col-sm-12"l>>',
            "lengthMenu": [[20, 50, 100, 1000, -1], [20, 50, 100, 1000, "All"]],
            "pageLength": 100,
            "order": [[2, 'asc']],
            "deferRender": true,
            "createdRow": createRowCallback,
            "initComplete": function (settings, json) {
                log('DataTables has finished its initialisation.');
                toggleAllCheck();
                refreshLoop(json);
            },
            "columnDefs": [
                {
                    "targets": [0],
                    "data": function (row, type, val, meta) {
                        var hiddenString = $("input[name=__ids]").val(); // TODO: Handle multiple datatables
                        var allValues = (!hiddenString) ? [] : hiddenString.split(',');

                        if (type === 'set') {
                            if (val === true) {
                                if ($.inArray(String(row.id), allValues) === -1) {
                                    allValues.push(row.id);
                                }
                            } else {
                                //Remove state
                                var index = jQuery.inArray(String(row.id), allValues);
                                if (index !== -1) {
                                    allValues.splice(index, 1);
                                }
                            }
                            hiddenString = allValues.join(',');
                            $("input[name=__ids]").val(hiddenString);
                            return;
                        }
                        return  (jQuery.inArray(String(row.id), allValues) !== -1);
                    },
                    "orderable": false,
                    "width": "24px",
                    "render": function (data, type, row, meta) {
                        var ids = $("input[name=__ids]").val().split(","),
                                checked = ($.inArray(String(row.id), ids) !== -1),
                                cbId = "memberCb" + cbCounter,
                                html = '<div><input id="' + cbId + '" type="checkbox" class="rowcb" data-rowid="' + row.id + '"' + (checked ? " checked" : "") + ' /></div>';
                        var $toProcess = $(html);
                        try {
                            $toProcess.icheck({
                                checkboxClass: 'icheckbox_square-red',
                                radioClass: 'iradio_square-red'
                            });
                        } catch (e) {
                            console.log("icheck error. Ignoring it");
                        }
                        return $toProcess.html();
                    }
                },
                {
                    //Activation error column
                    "targets": [1],
                    "orderable": false,
                    "width": "25px",
                    "data": function (row, type, set) {
                        if (!row.iconDisplay) {
                            if (row.inError && !row.activationPending) {
                                var msg = row.errorMsg;
                                if (!msg) {
                                    msg = 'Participation error reason is not known';
                                }

                                window.partError = window.partError || {};
                                window.partError[row.id] = msg;
                                var onclick = "onclick='showRosterPartErrorBox(window.partError[" + row.id + "])'";
                                var data = '<a" ' + onclick + ' class="btn btn-link participationTriangle" alt="Participation Link"><span class="glyphicon glyphicon-alert text-danger"></span></a>';

                                row.iconDisplay = data;
                            } else  {
                                row.iconDisplay = '<span class="pe-7s-plus pe-2x pull-right primaryColor expand"></span>';
                            }
                        }

                        if (type === 'display') {
                            return row.iconDisplay;
                        } else if (type === 'filter') {
                            return row.iconDisplay;
                        } else if (type === 'sort') {
                            return row.iconDisplay;
                        } else {
                            //Anything else and raw row
                            return row.iconDisplay;
                        }
                    },
                    "createdCell": function (td, cellData, rowData, row, col) {
                        showRow(td);
                    }
                },
                {
                    "targets": [2],
                    "width": "17%",
                    "class": "name",
                    "data": function (row, type, set) {
                        if (!row.displayNameDisplay) {
                            var data = '<a class="name" href="' + row.link + '" title="' + row.email + '">' + row.displayName + '</a> ';
                            row.displayNameDisplay = data;
                        }

                        if (type === 'display') {
                            return row.displayNameDisplay;
                        } else if (type === 'filter') {
                            return row.displayName;
                        } else if (type === 'sort') {
                            return row.displayName;
                        } else {
                            //Anything else and raw row
                            return row.displayName;
                        }
                    }
                },
                {
                    "targets": [3],
                    "width": "15%",
                    "data": function (row, type, set) {
                        if (!row.firstEmailDisplay) {
                            if (row.firstEmailStr) {
                                row.firstEmailDisplay = row.firstEmailStr;
                            } else if (row.sending && !row.firstEmailStr) {
                                row.firstEmailDisplay = "Sending...";
                            } else {
                                return null;
                            }
                        }

                        if (type === 'display') {
                            return row.firstEmailDisplay;
                        } else if (type === 'filter') {
                            return row.firstEmailStr;
                        } else if (type === 'sort') {
                            return row.firstEmail;
                        } else {
                            //Anything else and raw row
                            return row.firstEmail;
                        }
                    }
                },
                {
                    "targets": [4],
                    "width": "15%",
                    "data": function (row, type, set) {
                        if (!row.lastEmailDisplay) {
                            if (row.activationPending) {
                                row.lastEmailDisplay = "<i class='pe-7s-refresh-2 pe-va pe-spin'></i> Publishing course...";
                            } else if (row.sending) {
                                row.lastEmailDisplay = "Sending...";
                            } else if (row.bounced) {
                                row.lastEmailDisplay = '<a href="#" onclick="showBounceInfo(' + row.id + '); return false" class="text-danger" title="' + row.lastEmailStr + '"><strong>Bounced back</strong></a>';
                            } else {
                                row.lastEmailDisplay = row.lastEmailStr;
                            }
                        }

                        if (type === 'display') {
                            return row.lastEmailDisplay;
                        } else if (type === 'filter') {
                            return row.lastEmailStr;
                        } else if (type === 'sort') {
                            return row.lastEmail;
                        } else {
                            //Anything else and raw row
                            return row.lastEmail;
                        }
                    }
                },
                {
                    "targets": [5],
                    "data": function (row, type, set) {
                        if (row.activationPending) {
                            return "";
                        }
                        if (!row.lastAccessDisplay) {
                            if (row.lastAccess) {
                                row.lastAccessDisplay = '<abbr data-toggle="tooltip" class="timeago" title="' + row.lastAccessAgo + '">' + row.lastAccessStr + '</abbr>';
                            } else if (row.activated) {
                                row.lastAccessDisplay = '<span class="text-danger">Not yet logged in</span>';
                            } else {
                                row.lastAccessDisplay = '<span class="text-danger">Not yet activated</span>';
                            }
                        }

                        if (type === 'display') {
                            return row.lastAccessDisplay;
                        } else if (type === 'filter') {
                            return row.lastAccessStr;
                        } else if (type === 'sort') {
                            return row.lastAccess;
                        } else {
                            //Anything else and raw row
                            return row.lastAccess;
                        }
                    },
                    "createdCell": function (td, cellData, rowData, row, col) {
                        $('.timeago', td).timeago();
                        $('[data-toggle="tooltip"]', td).tooltip();
                    }
                },
                {
                    //Status column
                    "targets": [6],
                    "width": "120px",
                    "data": function (row, type, set) {
                        if (row.activationPending) {
                            return "";
                        }
                        if (!row.statusDisplay && row.activated) {
                            row.statusDisplay = '<div class="progress"><div class="progress-bar progress-bar-success" role="progressbar" aria-valuenow="' + row.status + '" aria-valuemin="0" aria-valuemax="100" style="width:' + row.status + '%;">' + row.status + '%</div</div>';
                        } else if (!row.statusDisplay && !row.activated) {
                            row.statusDisplay = '';
                        }

                        if (type === 'display') {
                            return row.statusDisplay;
                        } else if (type === 'filter') {
                            return row.status;
                        } else if (type === 'sort') {
                            return row.status;
                        } else {
                            //Anything else and raw row
                            return row.status;
                        }
                    }
                },
                {
                    //Participant menu column
                    "targets": [7],
                    "orderable": false,
                    "width": "80px",
                    "data": function (row, type, set) {
                        if (!row.actionsDisplay) {
                            row.impersonateAllowed = impersonateEnabled && row.activated && !row.inError;
                            row.moveAllowed = moveEnabled && !row.inError;
                            row.showLinkAllowed = row.activated && !row.inError;
                            var html = rosterCellActionTemplate(row);
                            row.actionsDisplay = html;
                        }

                        if (type === 'display') {
                            return row.actionsDisplay;
                        } else if (type === 'filter') {
                            return null;
                        } else if (type === 'sort') {
                            return null;
                        } else {
                            //Anything else and raw row
                            return row.actionsDisplay;
                        }
                    }
                }
            ],
            "sAjaxSource": projectRosterUrl,
            "oLanguage": {
                "sEmptyTable": "<span class='emptytable'>The roster is empty. Add a participant above to get started. </span>",
                "sLoadingRecords": "<p>Loading roster...</p><img src='" + spinnerUrl + "' />"
            }
        });


        /**
         * Checks if the json data contains participants with activationPending flag set
         * and schedules a json refresh it happens.
         *
         * @param {type} json
         * @returns {undefined}
         */
        var refreshLoop = function(json) {
            var pending = 0;
            $.each(json.aaData, function() {
                if (this.activationPending) {
                    pending++;
                }
            });

            if (pending) {
                setTimeout(function() {
                    refreshRoster();
                }, 2000);
            }
        };

        /**
         * Starts a ajax fetch of the roster and then updates all rows that are set with the
         * activationPending flag and then calls refreshLoop. If a ajax error occur this
         * method silently stops and no further looping will occur.
         *
         * @returns {undefined}
         */
        var refreshRoster = function() {
            $.get(projectRosterUrl).success(function(data) {

                //Create a map based on the id:s
                var map = {};
                $.each(data.aaData, function() {
                    map[this.id] = this;
                });

                var updated = false;
                oTable.DataTable().rows().every(function() {
                    if (!this.data().activationPending) {
                        return;
                    }

                    var rowId = this.data().id;

                    if (rowId in map) {
                        this.data(map[rowId]);

                        var tr = this.node();
                        if (tr) {
                            createRowCallback(tr, this.data());
                        }
                        updated = true;
                    }
                });

                if (updated) {
                    oTable.DataTable().draw();
                }

                refreshLoop(data);
            });
        };

        $(document).on('change', 'input.rowcb[type=checkbox]', function () {
            return cpweb.rowcbChange(this);
        });

        $('#projectroster').on('click', 'a[data-trigger=roster-row-action]', function (ev) {
            var row = oTable.DataTable().row($(this).parents('tr').first()).data();
            var action = $(this).data('action');
            if (action === 'link') {
                rowLinkAction(row);
            } else if (action === 'impersonate') {
                rowImpersonateAction(row);
            } else if (action === 'move') {
                rowMoveAction(row);
            } else if (action === "raps") {
                window.location = rapsUrl + row.id;
            } else {
                alert('Undefined row action: ' + action);
            }

            return false;
        });
        function rowLinkAction(row) {
            cocobox.infoDialog('Participation access link', row.participationLink);
            log(row);
        }
        ;

        function rowImpersonateAction(row) {
            window.open(impersonateLink + '/' + row.id, '_blank');
        }
        ;

        function rowMoveAction(row) {
            moveParticipation(row.id);
        }
        ;

        cpweb.registerListform($("#projectrosterform").first());

        $('#listprojects_filter input').attr('placeholder', 'Search for projects');

        $.fn.clearForm = function () {
            return this.each(function () {
                var type = this.type, tag = this.tagName.toLowerCase();
                if (tag === "form")
                    return $(":input", this).clearForm();
                if (type === "text" || type === "email")
                    this.value = "";
            });
        };

        if (permissionEditProjectRoster) {
            require(['jquery.form', 'dabox-common', 'dabox-formbeanjs'], function () {

                var handler = cocobox.getAjaxFormbeanHandler("uploadRoster", undefined, {sectionClass: "alert alert-danger"});

                var oldBefore = handler.beforeSend;

                var oldSuccess = handler.success;

                var longOp;

                handler.beforeSend = function () {
                    longOp = cocobox.longOp();

                    if (oldBefore) {
                        oldBefore();
                    }
                };

                handler.success = function () {
                    longOp.abort();

                    oldSuccess.apply(this, arguments);
                };

                $("#uploadRoster").ajaxForm(handler);
            });
        }

        $("#cball").on('click', function (ev) {
            var checked = this.checked;
            $.each(oTable.api().rows().indexes(), function (i, ri) {
                oTable.api().cell(ri, 0).data(checked);
            });
        });

        function toggleAllCheck() {

            $('#cball').icheck({
                checkboxClass: 'icheckbox_square-red',
                radioClass: 'iradio_square-red'
            });
        }
        ;

        $("#projectrosterform").submit(cpweb.projectRosterListForm);

        require(['select2-4.min'], function () {
            function templateUser(item) {
                var res = "";
                if (item.firstname) {
                    res += item.firstname;
                }
                if (item.lastname) {
                    res += " " + item.lastname;
                }
                if(item.primaryEmail) {
                    res += " (" + item.primaryEmail + ")";
                }
                return res;
            }

            // Add user "Individual" tab
            $("#lookupUser").select2({
                ajax: {
                    url: searchUserUrl,
                    dataType: 'json',
                    delay: 250,
                    data: function (params) {
                        return {
                            q: params.term,
                            pageLimit: 8,
                            page: params.page || 1
                        };
                    },
                    processResults: function (data, params) {
                        params.page = params.page || 1;
                        return {
                            results: data.items,
                            pagination: {
                                more: (params.page * 8) < data.total_count
                            }
                        };
                    },
                    cache: true,
                },
                placeholder: "Search for existing user",
                allowClear: true,
                minimumInputLength: 1,
                templateResult: function (item) {
                    return templateUser(item);
                },
                templateSelection: function (item) {
                    if(item.id) {
                        return templateUser(item);
                    }
                    return item.text;
                },

            }).change(function(e){
                var data = $(this).data("select2").data();
                if(data && data.length) {
                    $("#memberemail").val(data[0].primaryEmail?data[0].primaryEmail:"");
                    $("#memberfirstname").val(data[0].primaryEmail?data[0].firstname:"");
                    $("#memberlastname").val(data[0].primaryEmail?data[0].lastname:"");
                }
            });

            // Group tab
            $("#addByGroup").select2({
                placeholder: "Select a group",
                allowClear: true,
                formatSelection: function (opt) {
                    return opt.text.trim();
                }
                // formatResult is templateSelection in 4.0
            });
            $("#addByGroupBtn").on('click', function (ev) {
                var cugId = $("#group select").val()
                        , cugName = $("#group select option:selected").text()
                        , infoUrl = groupInfoUrl + cugId;
                if (!cugId) {
                    return;
                }
                $.getJSON(infoUrl).done(function (data) {
                    if (data.members === 0) {
                        cocobox.infoDialog("Add members", "There are no members in this group.");
                    } else {
                        cocobox.confirmationDialogYesNo("Add members", "Add " + data.members + " members from the group " + cugName + " to the project?", function () {
                            var addUrl = addMembersByGroupUrl + cugId;
                            cocobox.longOp();
                            $.getJSON(addUrl).done(function (data) {
                                window.location.reload();
                            }).fail(function (jqxhr, textStatus, error) {
                                cocobox.infoDialog("Error", "There was a problem adding members: " + textStatus);
                            });
                        });
                    }
                }).fail(function (jqxhr, textStatus, error) {
                    console.error("Could not look up group id " + cugId + ". " + textStatus + ", " + error);
                });

                return false;
            });

            // "Mark by group"
            $("#markByGroup").select2({
                placeholder: "Select members in group",
                allowClear: true,
                formatSelection: function (opt) {
                    return opt.text.trim();
                }
                // formatResult is templateSelection in 4.0
            }).change(function () {
                var cugId = $("#markByGroup").val(),
                        url = listGroupMembersUrl + cugId;
                if (cugId) {
                    $.getJSON(url).done(function (data) {
                        var h = {};
                        $.each(data.aaData, function (i, member) {
                            h[member.userId] = true;
                        });
                        $.each(oTable.api().rows().indexes(), function (i, ri) {
                            if (oTable.api().row(ri).data().userId in h) {
                                oTable.api().cell(ri, 0).data(true);
                            }
                        });
                    }).fail(function (jqxhr, textStatus, error) {
                        cocobox.infoDialog("Error", "There was a problem getting members from group: " + textStatus);
                    }).always(function () {
                        $("#markByGroup").select2("val", "");
                    });
                }
            });

        });

    }); // End document ready
});
