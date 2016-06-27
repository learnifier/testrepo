/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
define(['cocobox/ccb-imodal', 'es6-shim'], function(ccbImodal) {
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
        console.log("editCourse: ", $(".modal"));
        // $(".modal")
        $(".modal").show();

    });
    return exports;



});

