/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'dabox-common', 'koComponents/cocobox-list'], function (ko) {
    "use strict";
    var exports = {};
    var settings;


    function ListMaterialModel() {
        var self = this;

        function readData(url) {
            var deferred = $.Deferred();
            $.getJSON(url).done(function(data){
                // TODO: Handle errors
                console.log("data", data);
                deferred.resolve({ rows: data.aaData, folders: data.folders});
            });
            return deferred.promise();
        }

        // Return parameters for cocobox-list component
        self.cocoboxListParams = function() {
            return {
                editMode: settings.editMode, // TODO: This is not used
                getData: function () {
                    return readData(settings.listUrl);
                },
                idField: "id", // TODO: Not used
                nameField: "name",
                typeField: "type",  // TODO: Not used
                columns: [
                    { label: "", name: "thumbnail", format: function(val){return '<a href><img src="' + val + '"></a>'}, cssClass: "material-thumbnail",
                        clickFn: function(){alert("Thumbnnail lolclick")}},
                    { label: "Name", name: "name", format: function(val){return "<a href>" + val() + "</a>";}, cssClass: "block-link",
                      clickFn: function(){alert("Material preview goes here")}},
                    { label: "Kind", name: "typeTitle", format: function(val){return val}, cssClass: "", clickFn: null},
                    { label: "Updated", name: "updated", format: function(val){return val}, cssClass: "", clickFn: null},
                ],
                //groupOps: [{
                //    html: '<span><span class="glyphicon glyphicon - trash"></span> Removelol</span>',
                //    callback: function (items) {
                //        console.log("Remove fn on", items);
                //    }
                //}],
                moveFn: function (items, toFolderId) {
                    var deferred = $.Deferred();
                    window.setTimeout(function () {
                        deferred.resolve($.map(items, function (item) {
                            return ({status: "ok", item: item});
                        }));
                    }, 500);
                    return deferred.promise();
                },
                copyFn: function (items, toFolder) {
                    var deferred = $.Deferred();
                    window.setTimeout(function () {
                        deferred.resolve($.map(items, function (item) {
                            return ({status: "ok", item: item});
                        }));
                    }, 500);
                    return deferred.promise();
                },
                removeFn: function (items) {
                    var deferred = $.Deferred();
                    window.setTimeout(function () {
                        deferred.resolve($.map(items, function (item) {
                            return ({status: "ok", item: item});
                        }));
                    }, 500);
                    return deferred.promise();
                },
                renameFn: function (item, name) {
                    var deferred = $.Deferred();
                    window.setTimeout(function () {
                        deferred.resolve({status: "ok", item: item});
                    }, 500);
                    return deferred.promise();
                },
                createFolderFn: function (name, folderId) {
                    var deferred = $.Deferred();
                    window.setTimeout(function () {
                        deferred.resolve({status: "ok", item: { name: name, id: Math.floor(Math.random() * (10000 - 1000) + 1000)}});
                    }, 500);
                    return deferred.promise();
                }

            }
        };
    }

    exports.init = function(options) {
        settings = $.extend({
            listUrl: undefined,
            editMode: false
        }, options || {});

        ko.applyBindings(new ListMaterialModel());

    };

    return exports;

});
