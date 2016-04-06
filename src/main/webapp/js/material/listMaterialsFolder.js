/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'cocobox/ccb-imodal', 'dabox-common', 'cocobox/ko-components/list/cocobox-list-ng', 'es6-shim'], function (ko, ccbImodal) {
    "use strict";
    var exports = {};
    var settings;


    function ListMaterialModel() {
        var self = this;

        self.api = null;
        
        function readData(url) {
            var deferred = $.Deferred();
            $.getJSON(url).done(function(data){
                // TODO: Handle errors
                deferred.resolve({ rows: data.aaData, folders: data.folders});
            });
            return deferred.promise();
        }

        // Return parameters for cocobox-list component

        function decorateLink(isLink, html) {
            if(isLink) {
                return "<a href>" + html + "</a>";
            } else {
                return html;
            }
        }

        self.cocoboxListParams = function() {
            return {
                editMode: settings.editMode, // TODO: This is not used
                getData: function () {
                    return readData(settings.listUrl);
                },
                vfs: settings.vfs,
                idField: "id", // TODO: Not used
                nameField: "name",
                typeField: "type",  // TODO: Not used
                columns: [
                    { label: "", name: "thumbnail", format: function(item){return decorateLink(item.typeTitle==="Folder", '<img src="' + item.thumbnail() + '">')},
                        cssClass: "material-thumbnail", clickFn: null},
                    { label: "Name", name: "name", format: function(item){return decorateLink(item.typeTitle==="Folder", item.name())},
                        cssClass: "", clickFn: null},
                    { label: "Kind", name: "typeTitle", format: function(item){return item.typeTitle}, cssClass: "", clickFn: null},
                    { label: "Updated", name: "updated", format: function(item){return item.updated}, cssClass: "", clickFn: null}
                ],
                editOrgMatUrl: settings.editOrgMatUrl,
                folderThumbnail: window.cocoboxCdn + "/cocobox/img/producttypes/folder.svg",
                itemActions: function(item) {
                    var actions = [];
                    if (item.editorUrl) {
                        actions.push({
                            html: "Edit", action: function () {
                                window.open(item.editorUrl, '_blank');
                            }
                        });
                    } else {
                        if (item.anonymous) {
                            actions.push({
                                html: "Edit", action: function () {
                                    editItemById(item.id); // Ugly global spotted here...
                                }
                            });
                        }
                    }
                    return actions;
                },
                setApi: function(api){
                    self.api = api;
                }
            }
        };
        function openCreateMaterial(url, types) {
            var folderPath = self.api.currentFolderPath();

            types.map(function(type){
               url += "&type[]=" + type;
            });

            console.log("url: ", url);
            if(folderPath) { // Can be 0 which means false
                url += "&folder=" + folderPath;
            }
            var imodal = new ccbImodal.Server({
                serviceName: "addProducts",
                url: url,
                callbacks: {
                    "add": function (data) {
                        if(data.products instanceof Array) {
                            data.products.forEach(function(prod){
                                console.log("*** main: Adding product: ", prod.id);
                            });
                        }
                        console.log("*** main: Add: ", data);
                    },
                    "close": function(data) {
                        console.log("*** main: Close");
                    }
                },
                modalClass: "add-product-iframe"
            });
            imodal.open();
        }

        self.addMaterial = function(type) {
            openCreateMaterial(settings.addProductUrl, type);
        };
    }

    exports.init = function(options) {
        settings = Object.assign({
            vfs: undefined,
            listUrl: undefined,
            moveFolderUrl: undefined,
            createFolderUrl: undefined,
            removeFoldersItemsUrl: undefined,
            renameFolderUrl: undefined,
            renameItemUrl: undefined,
            editOrgMatUrl: undefined,
            editMode: true // TODO: Check permissions here.
        }, options);
        ko.applyBindings(new ListMaterialModel());
    };

    return exports;
});
