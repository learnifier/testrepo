/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'cocobox/ccb-imodal', 'dabox-common', 'cocobox/ko-components/list/cocobox-list-ng', 'es6-shim'], function (ko, ccbImodal) {
    "use strict";
    var exports = {};
    var settings;


    function ListMaterialModel() {
        var self = this;

        self.cocoboxListApi = ko.observable();

        function decorateLink(isLink, html) {
            if(isLink) {
                return "<a href>" + html + "</a>";
            } else {
                return html;
            }
        }

        function lookupType(type) {
            if(type === "DIRECTORY") {
                return "Folder";
            }
            return "Material";
        }

        self.cocoboxListParams = function() {
            return {
                editMode: settings.editMode, // TODO: This is not used
                vfs: settings.vfs,
                idField: "id", // TODO: Not used
                nameField: "name",
                typeField: "type",  // TODO: Not used
                columns: [{
                    label: "",
                    name: "thumbnail",
                    value: function(item){return item.thumbnail()},
                    format: function(val, item){return decorateLink(item.file.type==="DIRECTORY", '<img src="' + val + '">')},
                    cssClass: "material-thumbnail",
                    clickFn: null
                }, {
                    label: "Name",
                    name: "name",
                    value: function(item){return item.file.displayName},
                    format: function(val, item){return decorateLink(item.file.type==="DIRECTORY", val)},
                    cssClass: "",
                    clickFn: null
                }, {
                    label: "Kind",
                    name: "typeTitle",
                    value: function(item){return lookupType(item.file.type)},
                    cssClass: "",
                    clickFn: null
                }, {
                    label: "Updated",
                    name: "updated",
                    value: function(item){return ""},
                    cssClass: "",
                    clickFn: null
                }],
                vfsActions: {
                    preview: { html: '<span class="glyphicon glyphicon-grain"></span> Preview' },
                    edit: { html: '<span class="glyphicon glyphicon-grain"></span> Edit' },
                    remove: { html: '<span class="glyphicon glyphicon-trash"></span> Delete' },
                    rename: { html: '<span class="glyphicon glyphicon-pencil"></span> Rename' },
                    move: { html: '<span class="glyphicon glyphicon-share"></span> Move' }
                },
                folderThumbnail: window.cocoboxCdn + "/cocobox/img/producttypes/folder.svg",
                setApi: function(api){
                    self.cocoboxListApi(api);
                }
            }
        };
        function openCreateMaterial(url, types) {
            var folderPath = self.cocoboxListApi().currentFolderPath();

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
                    },
                    "close": function(data) {
                    }
                },
                modalClass: "add-product-iframe"
            });
            imodal.open();
        }

        self.addMaterial = function(type) {
            openCreateMaterial(settings.addProductUrl, type);
        };

        self.addFolder = function() {
            if(self.cocoboxListApi()) {
                self.cocoboxListApi().addFolder();
            }
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
