define(['dabox-ajax-longrun-bootstrap', 'dataTables-bootstrap', 'dataTables-tableTools', 'dabox-common', 'dabox-jquery'], function (lr) {

    var exports = {};

    var settings;

    exports.init = function (options) {

        settings = $.extend({
            jsonUrl: undefined
        }, options);

        var tableColTitles = ["Name"];
        var tableColIds = ["displayName"];
        var tableJSON = [];
        var sliiProductId = "EL0873";

        lr.guiLongRun(settings.jsonUrl
                , {
                    type: 'GET',
                    progressTarget: '#loadingTh',
                    error: function () {
                        $('#r_project_activity').html($('<td class="errormessage">').text('We were unable to generate this report right now. Please, try again later.'));
                    },
                    success: function (data) {
                        for (var i = 0; i < data.aaData.length; i++) {
                            var row = data.aaData[i];
                            for (var j = 0; j < row.activities.length; j++) {
                                var activity = row.activities[j];
                                activity.progressPercent2 = activity.progressPercent;
                            }
                        }
                        var activities = data.aaData[0].activities;
                        var activityCount = activities.length;
                        extractCols(activities, activityCount);
                        renderCols(activityCount);
                        initializeTable(data.aaData);
                    }});

        function extractCols(activities, activityCount) {
            for (var i = 0; i < activityCount; i++) {
                if (activities[i].productId && activities[i].productId === sliiProductId) {
                    tableColIds.push("activities." + [i] + ".progressPercent");
                    tableColIds.push("activities." + [i] + ".progressPercent2");
                }
            }
        }

        function renderCols() {
            //create JSON array for aoColumnDefs
            tableJSON.push({
                "sTitle": "Name",
                "mData": "displayName",
                "aTargets": [0]
            });

            tableJSON.push(
                    {
                        "sTitle": "Completed",
                        "mData": tableColIds[1],
                        "aTargets": [1],
                        "sDefaultContent": "0",
                        "render": function (data) {
                            var completed = 12 * data / 100;
                            return completed.toFixed(0);
                        },
                        "createdCell": function (td, data, oData, row, col) {
                            if (data === 100) {
                                $(td).css('color', '#00C800');
                            }
                        }
                    });

            tableJSON.push(
                    {
                        "sTitle": "Completion rate (%)",
                        "mData": tableColIds[2],
                        "aTargets": [2],
                        "sDefaultContent": "0",
                        "fnCreatedCell": function (td, data, oData, row, col) {
                            $(td).append('%');
                            if (data === 100) {
                                $(td).css('color', '#00C800');
                            }
                        }
                    });
        }

        function initializeTable(JSONdata) {
            oTable = $('#r_project_activity').dataTable({
                "aoColumnDefs": tableJSON,
                "dom": '<"row"<"col-sm-6"f><"col-sm-6"T>><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                "oTableTools": {
                    "sSwfPath": "[@cdt.tableToolsSwf /]"
                },
                "bPaginate": false,
                "aaSorting": [[0, 'asc']],
                "aaData": JSONdata,
                "language": {
                    "sSearch": "",
                    "sEmptyTable": "<span class='emptytable'>No projects available on your account.</span>",
                    "sLoadingRecords": "<p>Loading report...</p><img src='[@common.spinnerUrl /]' />"
                },
                "rowCallback": function (nRow, iDisplayIndex) {
                    var sumCompleted = 0;
                    for (var i = 0; i < iDisplayIndex.activities.length; i++) {
                        if (iDisplayIndex.activities[i].productId && iDisplayIndex.activities[i].productId == sliiProductId) {
                            sumCompleted += iDisplayIndex.activities[i].progressPercent;
                        }
                    }
                    $('.completedSum', nRow).text(sumCompleted);
                },
                "initComplete": function () {
                    $('.pop').tooltip();
                }
            });

            var sum = 0;
            for (var j = 0; j < JSONdata.length; j++) {
                if (JSONdata[j].activities != null) {
                    for (var h = 0; h < JSONdata[j].activities.length; h++) {
                        if (JSONdata[j].activities[h].productId && JSONdata[j].activities[h].productId == sliiProductId) {
                            sum += JSONdata[j].activities[h].progressPercent;
                        }
                    }
                }
            }

            var nFootAverageRow = $('#completed_average');
            $(nFootAverageRow).append($('<th>').text('Average'));

            var participantsSum = parseInt(JSONdata.length);

            $(nFootAverageRow).append($('<td>').text((sum / participantsSum * 12 / 100).toFixed(0)));
            $(nFootAverageRow).append($('<td>').text((sum / participantsSum).toFixed(0) + '%'));

        }

    };

    return exports;
});
