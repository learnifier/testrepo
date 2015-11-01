/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
/* global cocobox */

define(['dabox-common'], function () {
    "use strict";
    var exports = {};

    var pageInfo;

    var deleteMaterial = function () {
        var id = $(this).attr("data-id");

        var deleteResponse = function(data) {
            var status = data.status;
            if (status === "notfound" || status === "ok") {
                //Reload page
                console.log("Reloading page");
                window.location.href = window.location.href;
            } else if (status === "denied") {
                cocobox.errorDialog("Not allowed", "You are not allowed to delete this material");
            } else if (status === "inuse") {
                cocobox.infoDialog("Not allowed", "The material you selected is in use");
            } else {
                cocobox.errorDialog("Error", "Unexpected response from delete org material: "+data.status);
            }
        };

        var deleteStep = function () {
            cocobox.confirmationDialog("Delete material",
            "Are you sure you want to delete this material?", function() {
                $.ajax(pageInfo.productDelete, {
                    "method": "post",
                    "data": {
                        "productId": id
                    }
                }).error(cocobox.internal.ajaxError)
                        .done(deleteResponse);
            });
        };

        $.ajax(pageInfo.productDeleteCheck, {
            "data": {
                "productId": id
            },
            "method": "get"
        }).fail(cocobox.internal.ajaxError)
                .done(function (data) {

                    if (data.status === "notfound") {
                        cocobox.errorDialog("Not found", "Product doesn't exist");
                    } else if (data.status === "denied") {
                        cocobox.infoDialog("Not allowed", "You are not allowed to delete this material");
                    } else if (data.status !== "ok") {
                        cocobox.errorDialog("Error", "Unexpected server response: " + data.status);
                    } else {
                        //status === "ok"

                        if (data.projects.length === 0) {
                            deleteStep();
                            return;
                        }

                        //Product exists in project

                        var errorMessage = $("<div />");
                        errorMessage.append($("<p />").text("Unable to delete product. It is used in the following projects: "));
                        for(var i=0;i<data.projects.length;i++) {
                            var project = data.projects[i];
                            var projectP = $("<p />");

                            errorMessage.append(projectP.text(project.name));
                        }

                        cocobox.errorDialog("Unable to delete", errorMessage);
                    }

                });

        //important for click listener
        return false;
    };

    exports.init = function (initData) {
        pageInfo = initData;

        $("#materialstable").on("click", "[data-action=delete]", deleteMaterial);
    };

    return exports;
});

