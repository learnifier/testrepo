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
        var name, description, crl;

        this.initializing = ko.observable(true);
        this.name = ko.observable();
        this.description = ko.observable();
        this.crl = ko.observable();
        this.progressPercent = ko.observable(0);
        this.viewLink = ko.observable();

        if(settings.courseId) {
            $.getJSON(settings.getCourseUrl + "/" + settings.courseId).done(function(data){
                console.log("read data: ", data);
                self.name(data.course.name);
                name = data.course.name;

                self.description(data.course.description);
                description = data.course.description;

                self.crl(data.course.crl);
                crl = data.course.crl;

                self.viewLink(data.thumbnailUrl);
                self.initializing(false);
            });
        } else {
            self.initializing(false);
        }

        this.isCreateMode = function(){
            return !settings.courseId;
        }

        this.closer = function(){
            imodalClient.close();
        };

        this.save = function(forward){
            var url;

            if(self.isCreateMode()) {
                url = settings.createCourseUrl;
            } else {
                url = settings.saveCourseUrl + "/" + settings.courseId;
            }

            $.ajax({
                type: "POST",
                url: url,
                data: {
                    name: self.name(),
                    description: self.description(),
                    thumbnailUrl: self.crl()
                }
            }).done(function(data){
                if(self.isCreateMode()) {
                    if(forward) {
                        imodalClient.send("createAndForward", {"url": settings.newSessionUrl + "/" + data.id.id});
                        imodalClient.close();
                    } else {
                       imodalClient.send("saveDone");
                        imodalClient.close();
                    }
                } else {
                    imodalClient.send("saveDone");
                    imodalClient.close();
                }
            }).fail(function(jqXHR, textStatus, errorThrown){
                CCBMessengerError("Error when saving course: ", textStatus);
            });
        };

        this.validate = function(){
            if(self.initializing()) {
                return false;
            }
            if(self.isCreateMode()) {
                return self.name() && self.description() && self.crl();
            } else {
                console.log("Name: ", self.name(), name);
                return self.name() && (self.name() != name || self.description() != description || self.crl() != crl)
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

        var courseModel = new CourseModel();
        ko.applyBindings(courseModel);

        $("#fileupload").fileupload({
            progress: function (e, data) {
                var progress = parseInt(data.loaded / data.total * 100, 10);
                courseModel.progressPercent(progress);
            },
            fail: function (e, data) {
                courseModel.progressPercent(0);
                require(['dabox-common'], function() {
                        cocobox.errorDialog("Upload failed", "Upload failed. Make sure that the file you selected is a valid image");
                });
                $("#uploadPbar").hide();
            },
            done: function (e, data) {
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

