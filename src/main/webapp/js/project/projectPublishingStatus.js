define(['messenger'], function () {
    "use strict";

    var exports = {};

    var msg;

    var showMessage = function() {
        if (msg) {
            return;
        }

        msg = Messenger({theme: "flat"}).post({
            message: "Publishing project",
            type: "info",
            hideAfter: 0
        });
    }

    var hideMessage = function() {
        if (msg) {
            msg.update({
                message: "Publishing completed",
                type: "success",
                hideAfter: 7
            })
            msg = null;
        }
    }

    var refreshStatus = function(jsonUrl) {
        $.get(jsonUrl).success(function(data) {
            var animTime = 400;
            var refresh = false;
            if (data.status === "publishing") {
                showMessage();
                $("#cp-project-stage").hide(animTime);
                $("#cp-project-editdesign button").prop("disabled", true);
                refresh = true;
            } else if (data.status === "unstaged") {
                hideMessage();
                $("#cp-project-stage").show(animTime);
                $("#cp-project-editdesign button").prop("disabled", false);
            } else if (data.status === "normal") {
                hideMessage();
                $("#cp-project-stage").hide(animTime);
                $("#cp-project-editdesign button").prop("disabled", false);
            }

            if (refresh) {
                setTimeout(function() {
                    refreshStatus(jsonUrl);
                }, 2000);
            }
        });
    };

    exports.init = function(jsonUrl) {
        refreshStatus(jsonUrl);
    };


    return exports;
});
