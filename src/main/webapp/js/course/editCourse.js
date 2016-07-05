/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
define(['knockout', 'cocobox/ccb-imodal', 'es6-shim', 'ckeditor4', 'cocobox-knockout-bindings', 'messenger', 'jquery.fileupload'], function(ko, ccbImodal) {
    "use strict";
    var exports = {}, settings, imodalClient;

    imodalClient = new ccbImodal.Client({
        serviceName: "course"
    });

    $(document).ready(function(){
        $(".modal").show();
    });

    function CourseModel() {
        var self = this;

        console.log("Init coursemodel: ", settings);
        this.initializing = ko.observable(true);
        this.name = ko.observable();
        this.description = ko.observable();
        this.crl = ko.observable();
        this.progressPercent = ko.observable(0);
        this.viewLink = ko.observable();

        if(settings.courseId) {
            $.getJSON(settings.getCourseUrl + "/" + settings.courseId).done(function(data){
                console.log("Read data: ", data);
                self.name(data.name);
                self.description(data.description);
                self.initializing(true);
            });
        } else {
            self.initializing(true);
        }

        this.closer = function(){
            imodalClient.close();
        };

        this.save = function(){
            var url;


            if(settings.courseId) {
                url = settings.saveCourseUrl + "/" + settings.courseId;
            } else {
                url = settings.createCourseUrl;
            }

            $.ajax({
                type: "POST",
                url: url,
                data: {
                    name: self.name,
                    description: self.description
                }
            }).done(function(data){
                if(settings.courseId) {
                    imodalClient.send("saveDone");
                } else {
                    imodalClient.send("createDone", {"forwardUrl": settings.newSessionUrl + "?courseId=" + 1011}); // TODO: Extract courseId from data once it is implemented
                }
            }).fail(function(jqXHR, textStatus, errorThrown){
                CCBMessengerError("Error when saving course: ", textStatus);
            });
        };

        this.validate = function(){
            return this.name() && this.crl();
        };
    }

    exports.init = function(options) {
        settings = $.extend({
            getCourseUrl: undefined,
            saveCourseUrl: undefined,
            createCourseUrl: undefined,
            courseId: undefined,
            defaultImage: undefined
        }, options || {});

        console.log("");
        var courseModel = new CourseModel();
        courseModel.viewLink(settings.defaultImage);
        ko.applyBindings(courseModel);

        $("#fileupload").fileupload({
            progress: function (e, data) {
                console.log(data);
                var progress = parseInt(data.loaded / data.total * 100, 10);
                courseModel.progressPercent(progress);
            },
            fail: function (e, data) {
                console.log("fail", data);
                courseModel.progressPercent(0);
                require(['dabox-common'], function() {
                        cocobox.errorDialog("Upload failed", "Upload failed. Make sure that the file you selected is a valid image");
                });
                $("#uploadPbar").hide();
            },
            done: function (e, data) {
                console.log("done", data);
                $("#uploadPbar").hide();
                if (data.result.status === 'ok') {
                    courseModel.crl(data.result.crl);
                    courseModel.viewLink(data.result.viewLink);
                } else {
                    require(['dabox-common'], function() {
                        cocobox.errorDialog("Upload failed", "Upload failed. Make sure that the file you selected is a valid image");
                    });
                }
                courseModel.progressPercent(0);
            }
        });
    };


    return exports;

});

