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

        function lookupType(file) {
            if(file.type === "DIRECTORY") {
                return "Folder";
            }

            if ("fileType" in file.attributes) {
                return file.attributes["fileType"];
            }

            return "Material";
        }

        function chdir(item) {
            if (item.chdir) {
                item.chdir();
                return true;
            }
            return false;
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
                    clickFn: chdir
                }, {
                    label: "Name",
                    name: "name",
                    value: function(item){return item.file.displayName},
                    format: function(val, item){return decorateLink(item.file.type==="DIRECTORY", val)},
                    sortable: true,
                    cssClass: "",
                    clickFn: chdir
                }, {
                    label: "Kind",
                    name: "typeTitle",
                    value: function(item){return lookupType(item.file)},
                    sortable: true,
                    cssClass: "material-kind",
                    clickFn: null
                }, {
                    label: "Updated",
                    name: "updated",
                    value: function(item){return ""}, // TODO: Need something to sort on from vfs here.
                    sortable: true,
                    cssClass: "material-updated",
                    clickFn: null
                }],
                vfsActions: {
                    preview: { html: '<span class="glyphicon glyphicon-grain"></span> Preview' },
                    rename: { html: '<span class="glyphicon glyphicon-pencil"></span> Rename' },
                    edit: { html: '<span class="glyphicon glyphicon-grain"></span> Edit' },
                    move: { html: '<span class="glyphicon glyphicon-share"></span> Move' },
                    remove: { html: '<span class="glyphicon glyphicon-trash"></span> Delete' }
                },
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
                            self.cocoboxListApi().refresh();
                        }
                    },
                    "addAndEdit": function (data) {
                        var editId = null;
                        if(data.products instanceof Array) {
                            // Should only get one product here, but edit last one in case something breaks.
                            data.products.forEach(function(prod){
                                console.log("*** main: Adding product: ", prod.id);
                                editId = prod.id;
                            });
                            if(editId) {
                                self.cocoboxListApi().refreshAndEdit(editId);
                            }
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
