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
        var name, description, thumbnailUrl;

        console.log("Init coursemodel: ", settings);
        this.initializing = ko.observable(true);
        this.name = ko.observable();
        this.description = ko.observable();
        this.crl = ko.observable();
        this.progressPercent = ko.observable(0);
        this.thumbnailUrl = ko.observable();

        if(settings.courseId) {
            $.getJSON(settings.getCourseUrl + "/" + settings.courseId).done(function(data){
                console.log("Read data: ", data);
                self.name(data.name);
                name = data.name;

                self.description(data.description);
                description = data.description;

                self.thumbnailUrl(data.thumbnailUrl);
                thumbnailUrl = data.thumbnailUrl;

                self.initializing(false);
            });
        } else {
            self.initializing(false);
        }

        this.isCreate = function(){
            return !settings.courseId;
        }

        this.closer = function(){
            imodalClient.close();
        };

        this.save = function(){
            var url;

            if(self.isCreate()) {
                url = settings.createCourseUrl;
            } else {
                url = settings.saveCourseUrl + "/" + settings.courseId;
            }

            $.ajax({
                type: "POST",
                url: url,
                data: {
                    name: self.name,
                    description: self.description,
                    thumbnailUrl: self.thumbnailUrl
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
            if(self.initializing()) {
                return false;
            }
            if(self.isCreate()) {
                return this.name();
            } else {
                console.log("Name: ", this.name(), name);
                return this.name() && (this.name() != name || this.description() != description || this.thumbnailUrl() != thumbnailUrl)
            }
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
        courseModel.thumbnailUrl(settings.defaultImage);
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
                    courseModel.thumbnailUrl(data.result.viewLink);
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

