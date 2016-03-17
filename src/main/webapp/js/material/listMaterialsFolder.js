/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'dabox-common', 'cocobox/ko-components/list/cocobox-list'], function (ko) {
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

        function preview(item){
            console.log("Item = ", item.name());
            alert("Preview of " + item.name());
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
                    { label: "", name: "thumbnail", format: function(val){return '<a href><img src="' + val + '"></a>'},
                        cssClass: "material-thumbnail", clickFn: preview},
                    { label: "Name", name: "name", format: function(val){return "<a href>" + val() + "</a>";},
                        cssClass: "block-link", clickFn: preview},
                    { label: "Kind", name: "typeTitle", format: function(val){return val}, cssClass: "", clickFn: null},
                    { label: "Updated", name: "updated", format: function(val){return val}, cssClass: "", clickFn: null}
                ],
                moveFn: function (folders, items, toFolderId) {
                    return $.ajax({
                        url: settings.moveToFolderUrl,
                        data: {
                            folderIds: folders,
                            itemIds: items,
                            toFolderId: toFolderId
                        }
                    });
                },
                removeFn: function(folders, items) {
                    return $.ajax({
                        url: settings.removeFoldersItemsUrl,
                        data: {
                            folderIds: folders,
                            itemIds: items
                        }
                    });
                },
                renameFolderFn: function (id, newName) {
                    return $.ajax({
                        url: settings.renameFolderUrl,
                        data: {
                            folderId: id,
                            name: newName
                        }
                    });
                },
                renameItemFn: function (id, newName) {
                    return $.ajax({
                        url: settings.renameItemUrl,
                        data: {
                            itemId: id,
                            name: newName
                        }
                    });
                },
                createFolderFn: function (name, folderId) {
                    return $.ajax({
                        url: settings.createFolderUrl,
                        data: {
                            folderId: folderId,
                            name: name
                        }
                    });
                },
                editOrgMatUrl: settings.editOrgMatUrl
            }
        };
    }

    exports.init = function(options) {
        settings = $.extend({
            listUrl: undefined,
            moveFolderUrl: undefined,
            createFolderUrl: undefined,
            removeFoldersItemsUrl: undefined,
            renameFolderUrl: undefined,
            renameItemUrl: undefined,
            editOrgMatUrl: undefined,
            editMode: true // TODO: Check permissions here.

        }, options || {});

        ko.applyBindings(new ListMaterialModel());

    };

    return exports;

});
