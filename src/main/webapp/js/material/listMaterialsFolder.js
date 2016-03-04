/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout'], function (ko) {

    "use strict";

    var exports = {};

    function ListMaterialModel() {
        var self = this;

        function Row(name, type) {
            var self = this;
            self.name = name;
            self.type = type;
        }

        self.rows = ko.observableArray();

        self.readAjax = function(url) {
            console.log("Calling ", url);
            $.getJSON(url).done(function(data){
                var res = $.map(data.aaData, function(item) {
                   return new Row(item.title, "lol");
                });
                self.rows(res);
               console.log("Got data: ", data);
            });
        }

    }

    var settings;

    exports.init = function(options) {
        settings = $.extend({
            listUrl: undefined,
            editMode: false
        }, options || {});

        var model = new ListMaterialModel();
        ko.applyBindings(model);
        model.readAjax(settings.listUrl);

        console.log("model = ", model);
    };

    return exports;

});
