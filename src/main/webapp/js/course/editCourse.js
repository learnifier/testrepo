/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
define(['knockout', 'cocobox/ccb-imodal', 'es6-shim', 'ckeditor4', 'cocobox-knockout-bindings'], function(ko, ccbImodal) {
    "use strict";
    var exports = {}, settings, imodalClient;

    exports.init = function(options) {
        settings = $.extend({
        }, options || {});
    };
    imodalClient = new ccbImodal.Client({
        serviceName: "course"
    });

    $(document).ready(function(){
        $(".modal").show();

    });

    function CourseModel() {
        var self = this;

        this.name = ko.observable("lolname");
        this.description = ko.observable("loldesc");

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

    var courseModel = new CourseModel();
    ko.applyBindings(courseModel);

    return exports;

});

