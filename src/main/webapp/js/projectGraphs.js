"use strict";
var pgraphs = pgraphs || {};

pgraphs.renderProjectGraphs = function(dataUrl, prjType, animationState) {

    var sumParticipants = 0;
    var onTrack = 0;
    var onTrackParticipants = [];
    var bounced = 0;
    var bouncedParticipants = [];
    var inError = 0;
    var inErrorParticipants = [];
    var overDue = 0;
    var overDueParticipants = [];
    var invited = 0;
    var invitedParticipants = [];
    var notInvited = 0;
    var notInvitedParticipants = [];
    var inProgress = 0;
    var inProgressParticipants = [];
    var completed = 0;
    var completedParticipants = [];

    $.ajax({type: 'GET', url: dataUrl,
        success: function(data) {
            calculateStatusData(data);
            renderProjectData();
            if (prjType === 2){
                renderOverdueGraph();
            }
            renderStatusGraph();
        }
    });

    function renderProjectData(data) {

        $('#prj-ch-total').html(sumParticipants);

        $('#prj-ch-bounced .chart-data').html(bounced);
        $('#prj-ch-bounced, #prj-ch-bounced span').prop('title', bouncedParticipants).tooltip();
        $('#prj-ch-notinvited .chart-data').html(notInvited);
        $('#prj-ch-notinvited, #prj-ch-notinvited span').prop('title', notInvitedParticipants).tooltip();
        $('#prj-ch-inerror .chart-data').html(inError);
        $('#prj-ch-inerror, #prj-ch-inerror span').prop('title', inErrorParticipants).tooltip();

        if (bounced > 0 | notInvited > 0 | inError > 0) {
            $('#prj-action-needed').addClass('error').html('!');
        } else {
            $('#prj-action-needed').html('✓');
        }
    }

    function renderOverdueGraph() {

        $('#prj-ch-overdue-overdue .chart-data').html(overDue);
        $('#prj-ch-overdue-overdue, #prj-ch-overdue-overdue span').prop('title', overDueParticipants).tooltip();
        $('#prj-ch-overdue-ontrack .chart-data').html(onTrack);
        $('#prj-ch-overdue-ontrack, #prj-ch-overdue-ontrack span').prop('title', onTrackParticipants).tooltip();

        var sum = onTrack + overDue;
        if ( sum > 0){
            $('#prj-ch-overdue-sum').html(sum);
        }
        var ctx = $("#prj-ch-overdue").get(0).getContext("2d");
        var data = [
            {
                value: onTrack,
                color: "green"
            },
            {
                value: overDue,
                color: "red"
            }
        ];

        new Chart(ctx).Doughnut(data, {animation: animationState});
    }

    function renderStatusGraph() {

        $('#prj-ch-status-invited .chart-data').html(invited);
        $('#prj-ch-status-invited, #prj-ch-status-invited span').prop('title', invitedParticipants).tooltip();
        $('#prj-ch-status-inprogress .chart-data').html(inProgress);
        $('#prj-ch-status-inprogress, #prj-ch-status-inprogress span').prop('title', inProgressParticipants).tooltip();
        $('#prj-ch-status-completed .chart-data').html(completed);
        $('#prj-ch-status-completed, #prj-ch-status-completed span').prop('title', completedParticipants).tooltip();

        var sum = invited + inProgress + completed;
        if ( sum > 0){
            $('#prj-ch-status-sum').html(sum);
        }
        var ctx = $("#prj-ch-status").get(0).getContext("2d");
        var data = [
            {
                value: invited,
                color: "red"
            },
            {
                value: inProgress,
                color: "orange"
            },
            {
                value: completed,
                color: "green"
            }
        ];
        new Chart(ctx).Doughnut(data, {animation: animationState});
    }
    ;

    function calculateStatusData(data) {
        sumParticipants = data.aaData.length;

        for (var i = 0; i < sumParticipants; i++) {
            var item = data.aaData[i];
            if (item.activated === false) {
                notInvited = notInvited + 1;
                notInvitedParticipants.push(item.displayName);
            } else {
                
                if (item.firstAccess === null) {
                    // invited but not accessed
                    invited = invited + 1;
                    invitedParticipants.push(' ' + item.displayName);
                } else if (item.activated && item.status !== 100) {
                    // accessed but not completed
                    inProgress = inProgress + 1;
                    inProgressParticipants.push(' ' + item.displayName);
                } else if (item.status === 100) {
                    completed = completed + 1;
                    completedParticipants.push(' ' + item.displayName);
                }
                
                if (item.bounced === true) {
                    bounced = bounced + 1;
                    bouncedParticipants.push(' ' + item.displayName);
                }
                
                if (item.overdue === true) {
                    overDue = overDue + 1;
                    overDueParticipants.push(' ' + item.displayName);
                } else {
                    onTrack = onTrack + 1;
                    onTrackParticipants.push(' ' + item.displayName);
                }
                
                if (item.inError === true) {
                    inError = inError + 1;
                    inErrorParticipants.push(' ' + item.displayName);
                }
            }
        }
    }
    ;

};

