define([], function () {
    "use strict";

    var exports = {};

    var refreshStatus = function(jsonUrl) {
        $.get(jsonUrl).success(function(data) {
            var animTime = 400;
            var refresh = false;
            if (data.status === "publishing") {
                $("#cp-project-publishing").show(animTime);
                $("#cp-project-stage").hide(animTime);
                $("#cp-project-editdesign button").prop("disabled", true);
                refresh = true;
            } else if (data.status === "unstaged") {
                $("#cp-project-publishing").hide(animTime);
                $("#cp-project-stage").show(animTime);
                $("#cp-project-editdesign button").prop("disabled", false);
            } else if (data.status === "normal") {
                $("#cp-project-publishing").hide(animTime);
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
