define(['dabox-ajax-longrun-bootstrap', 'dataTables-bootstrap', 'dabox-common'], function (longrun) {
    "use strict";

    var exports = {};

    var extStatusLabels;
    var projectUrl;

    var colCount = 0;
    var colDefs = [
        {
            "targets": [colCount++],
            "data": "name",
            "title": "Name"
        },
        {
            "targets": [colCount++],
            "data": "email",
            "title": "E-mail"
        },
        {
            "targets": [colCount++],
            "data": "masterProjectName",
            "title": "Project",
            "createdCell": function(td, data, rowData) {
                var a = $("<a/>", {"href": projectUrl + rowData.masterProject, "target": "_blank"});
                a.append(data);
                $(td).html(a);
            }
        }
    ];

    var extraCols = {};

    var initTable = function (data) {
        console.log("init would take place here");

        addActivityColumns(data.list);

        console.log("Extra columns fixed");

        $("#r_report tbody").empty();
        $("#r_report").dataTable({
            "data": data.list,
            "columnDefs": colDefs,
            "deferRender": true
        });
    };

    var addActivityColumns = function(data) {
        for(var i=0;i<data.length;i++) {
            var row = data[i];
            for(var a=0;a<row.activity.length;a++) {
                var activity = row.activity[a];
                addActivityColumn(activity);
            }
        }
    };

    var addActivityColumn = function(activity) {
        if (activity.title in extraCols) {
            return;
        }

        extraCols[activity.title] = null;

        var cacheName = "extraCol" + colCount;

        colDefs.push({
            "targets": [colCount++],
            "data": null,
            "title": activity.title,
            "defaultContent": "",
            "order": [[0, 'asc']],
            "render": function(data) {
                if (cacheName in data) {
                    return extStatusLabels[data[cacheName]];
                }

                for(var x=0;x<data.activity.length;x++) {
                    var act = data.activity[x];
                    if (act.title === activity.title) {
                        data[cacheName] = act.extendedStatus;
                        return data[cacheName];
                    }
                }

                data[cacheName] = null;
            },
            "createdCell": function(td, data) {
                var status = data[cacheName];
                if (status === "notAttempted") {
                    $(td).addClass('text-danger');
                } else if (status === "incomplete") {
                    $(td).addClass('text-warning');
                } else if (status === "completed" || status === "passed" || status === "failed") {
                    $(td).addClass('text-success');
                } else if (status === "locked" || status === "notTracked") {
                    $(td).addClass('text-muted');
                }
            }
        });
    };

    exports.init = function (opts) {
        extStatusLabels = opts.statusNames;
        projectUrl = opts.projectUrl;

        longrun.guiLongRun(opts.jsonUrl, {
            progressTarget: "#pbar",
            success: initTable,
            error: cocobox.internal.ajaxErrorHandler
        });

    };

    return exports;
});
