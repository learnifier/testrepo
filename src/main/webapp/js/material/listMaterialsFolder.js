/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define('CcbImodal', ['es6-shim'], function(){
    "use strict";

    function CcbImodal(options) {
        this.settings = Object.assign({
            serviceName: undefined,
            addProductUrl: undefined,
            callbacks: undefined,
            modalClass: undefined
        }, options);
    }

    CcbImodal.prototype.open = function() {
        this._iframe = $('<iframe width="100%" style="background:#ffffff;opacity:1.0;z-index:10000;color:#000000;display: none;height: 100%;position:fixed; top:0px; left:0px; bottom:0px; right:0px;" id="iframeLab"></iframe>').appendTo(document.body);
        if(this.settings.modalClass) {
            this._iframe.addClass(this.settings.modalClass);
        }
        this._iframe.attr('src', this.settings.addProductUrl).show();

        this._receiveMessage = this._receiveMessageInner.bind(this);
        window.addEventListener("message", this._receiveMessage, false);
    };

    CcbImodal.prototype._receiveMessageInner = function(event) {
        var origin = event.origin || event.originalEvent.origin;
        if (origin !== window.location.origin) // Ok to use window.location.origin?
            return;
        var data;
        try {
            data = JSON.parse(event.data);
        } catch(err) {
            return;
        }

        if(data.service === this.settings.serviceName) {
            if(this.settings.callbacks[data.command]) {
                this.settings.callbacks[data.command](data);
            }
            if(data.command === "add") {
                this.close();
            }
            if(data.command === "close") {
                this.close();
            }
        }
    };

    CcbImodal.prototype.close = function(){
        this._iframe.remove();
        window.removeEventListener("message", this._receiveMessage); // Does this work with bind?
    };
    return CcbImodal;
});

define(['knockout', 'CcbImodal', 'dabox-common', 'cocobox/ko-components/list/cocobox-list', 'es6-shim'], function (ko, CcbImodal) {
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
                    { label: "", name: "thumbnail", format: function(val){return '<a href><img src="' + val + '"></a>'},
                        cssClass: "material-thumbnail", clickFn: null},
                    { label: "Name", name: "name", format: function(val){return "<a href>" + val() + "</a>";},
                        cssClass: "", clickFn: null},
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
                }
            }
        };
    }

    exports.init = function(options) {
        settings = Object.assign({
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

    $(document).ready(function(){
        $(".add-material-ng").click(function(){
            var imodal = new CcbImodal({
                serviceName: "addProducts",
                addProductUrl: settings.addProductUrl,
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
                }
            });
            imodal.open();
        });
    });
    return exports;
});
