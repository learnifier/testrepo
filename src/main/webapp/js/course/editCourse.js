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
        $(".modal button.close").click(function(){
            imodalClient.close();
        });
        $(".modal button#courseModalClose").click(function(){
            imodalClient.close();
        });
        $(".modal button#courseModalSave").click(function(){
            console.log("And we are saving");
            imodalClient.close();
        });
        $(".modal").show();

    });
    return exports;



});

