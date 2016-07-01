define(['dabox-ajax-longrun-bootstrap', 'dataTables-bootstrap', 'dabox-common', 'dataTables-buttons'], function (longrun) {
    "use strict";

    var exports = {};

    var extStatusLabels;
    var projectUrl;
    var contextPath;
    var productId;

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
                var href = contextPath + '/project.report/innerActivityReport/' +
                        rowData.projectId + '/' + rowData.masterProject;

                var a = $("<a/>", {"href": href, "target": "_blank"});
                a.append(data);
                $(td).html(a);
            }
        },
        {
            "targets": [colCount++],
            "data": "completedPercent",
            "title": "Completed",
            "render": function(data, type, row) {
                if (type === "display") {
                    return row.completed + " of " + row.activity.length;
                }

                return data;
            }
        }

    ];

    var extraColsStart = colCount;

    var extraColsMap = {};
    var extraCols = [];

    var initTable = function (data) {
        console.log("init would take place here");

        createSummaryColumn(data);
        addActivityColumns(data.list);
        addFullActivityArray(data.list);
        initFooter();

        console.log("Extra columns fixed", data.list, colDefs);

        $("#r_report tbody").empty();
        $("#r_report").dataTable({
            "dom": '<"row"<"col-sm-12"W>><"row"<"col-sm-6"f>><"row"<"col-sm-6"B>><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>><"row"<"col-sm-12"l>>',
            "lengthMenu": [[10, 20, 50, 100, 1000, -1], [10, 20, 50, 100, 1000, "All"]],
            "pageLength": 10,
            buttons: [
                    'copy', 'excel', 'csv', 'print', 'colvis'
            ],
            "deferRender": true,
            "data": data.list,
            "columnDefs": colDefs,
            footerCallback: recalculateSummary
        });
    };

    var initFooter = function() {
        var tr = $("<tr>", {"id": "completed_absolute"});
        for (var i = 0; i < colCount; i++) {
            var td = $("<td/>");
            if (i === 0) {
                td.text("Completed");
            }
            tr.append(td);
        }

        $("#r_report tfoot").append(tr);
    };

    var isCompleted = function(extStatus) {
        if (extStatus ==="completed" || extStatus === "passed" || extStatus === "failed") {
            return 1;
        }

        return 0;
    };

    var recalculateSummary = function (tfoot, data, start, end, display) {
        console.log("Footer time!");

        var api = this.api();

        for (var i = extraColsStart; i < colDefs.length; i++) {
            $(api.column(i).footer()).html(
                    api.column(i, {page:'current'}).data().reduce(function (a, b) {
                return a + isCompleted(b);
            }, 0));
        }

    };

    /**
     * Create a new list `act` that are have as many elements as extraCols;
     *
     * @param {type} data
     * @returns {undefined}
     */
    var addFullActivityArray = function(data) {
        for(var i=0;i<data.length;i++) {
            var row = data[i];
            row.act = [];
            for (var a = 0; a < extraCols.length; a++) {
                var activity = findActivity(row, extraCols[a]);
                row.act.push(activity);
            }
        }
    };

    var findActivity = function(row, title) {
        for(var a=0;a<row.activity.length;a++) {
            var act = row.activity[a];
            if (act.title === title) {
                return act;
            }
        }

        return null;
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
        if (activity.title in extraColsMap) {
            return;
        }

        extraColsMap[activity.title] = null;
        extraCols.push(activity.title);

        colDefs.push({
            "targets": [colCount++],
            "data": "act."+(extraCols.length-1)+".extendedStatus",
            "title": activity.title,
            "defaultContent": "",
            "order": [[0, 'asc']],
            "render": function(data) {
                return extStatusLabels[data];
            },
            "createdCell": function(td, data) {
                var status = data;
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

    var createSummaryColumn = function(data) {
        for (var i = 0; i < data.list.length; i++) {
            var row = data.list[i];

            var completed = countCompleted(row);

            var percent = 0;

            if (row.activity.length > 0) {
                percent = completed / row.activity.length;
            }

            row.completed = completed;
            row.completedPercent = percent;
        }
    };

    var countCompleted = function(row) {
        var comp = 0;
        for (var i = 0; i < row.activity.length; i++) {
            var act = row.activity[i];

            var st = act.extendedStatus;

            if (st === "completed" || st === "failed" || st === "passed") {
                comp++;
            }
        }

        return comp;
    };

    exports.init = function (opts) {
        extStatusLabels = opts.statusNames;
        projectUrl = opts.projectUrl;
        contextPath = opts.contextPath;
        productId = opts.productId;

        longrun.guiLongRun(opts.jsonUrl, {
            progressTarget: "#pbar",
            success: initTable,
            error: cocobox.internal.ajaxErrorHandler
        });

    };

    return exports;
});
