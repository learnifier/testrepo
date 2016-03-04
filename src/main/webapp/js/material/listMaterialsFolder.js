/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout'], function (ko) {

    "use strict";

    var exports = {};

    function ListMaterialModel() {
        var self = this;

        function Row(name, typeTitle, thumbnail) {
            var self = this;
            self.name = name;
            self.typeTitle = typeTitle;
            self.thumbnail = thumbnail;
        }

        self.rows = ko.observableArray();

        self.products = undefined;

        self.readAjax = function(url) {
            console.log("Calling ", url);

            $.getJSON(url).done(function(data){
                console.log("Got data: ", data);

                //parseFolders();

                var res = $.map(data.aaData, function(item) {
                   return new Row(item.title, item.typeTitle, item.thumbnail);
                });

                var folders = $.map(data.folders, function(item) {
                    //item.id, item.folders, item.name
                    return new Row(item.name, "Folder", item.thumbnail);
                });
                self.rows(res.concat(folders));
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
