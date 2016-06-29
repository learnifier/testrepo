/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
define(['knockout', 'cocobox/ccb-imodal', 'es6-shim', 'ckeditor4', 'cocobox-knockout-bindings'], function(ko, ccbImodal) {
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

        if(settings.courseId) {
            $.getJSON(settings.getCourseUrl + "/" + settings.courseId).done(function(data){
                console.log("Read data: ", data);
                self.name("fake name");
                self.description("fake description");
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

            imodalClient.close();

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
            })
        };

        this.validate = function(){
            return this.name();
        };
    }

    exports.init = function(options) {
        settings = $.extend({
            getCourseUrl: undefined,
            saveCourseUrl: undefined,
            createCourseUrl: undefined,
            courseId: undefined
        }, options || {});

        console.log("");
        var courseModel = new CourseModel();
        ko.applyBindings(courseModel);

    };


    return exports;

});

