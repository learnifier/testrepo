/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define("cocobox-list", ['knockout', 'dabox-common', 'messenger'], function (ko) {

    "use strict";

    var exports = {};

    var model;

    var Item = function(id, parentId, name, typeTitle, thumbnail) {
        var self = {};
        self.id = id;
        self.parentId = parentId;
        self.name = name;
        self.typeTitle = typeTitle;
        self.thumbnail = thumbnail;
        self.selected = ko.observable(false);
        self.selectRow = function(){
            var s = self.selected();
            model.updateSelected(self, !s);
            self.selected(!s);
        };
        return self;
    };

    var Folder = function (id, parentId, name, folders) {
        var self = Item(id, parentId, name, "Folder", "");
        self.folders = folders;
        self.materials = [];
        self.clickName = function() {
            if(model) {
                model.showFolder(id);
            }
        }

        self.removeChild = function(child){
            var a;
            if(child instanceof Folder) {
                a = model.selectedFolder().folders;
            } else {
                a = model.selectedFolder().materials;
            }
            var index = a.indexOf(child);
            console.log("*** index", a, child, index);
            if (index > -1) {
                console.log("*** Before: ", a);
                a.splice(index, 1);
                console.log("*** After: ", a);
                return true;
            }
            return false;
        };

        return self;
    };

    function Material(id, parentId, name, typeTitle, thumbnail) {
        var self = Item(id, parentId, name, typeTitle, thumbnail);
        self.name = name;
        self.typeTitle = typeTitle;

        self.clickName = function() {
            cocobox.infoDialog("Preview", "Nice material preview here.", function(){});
        }
        return self;
    };


    function parseFolders(json) {
        var folderHash = {};

        function parseFoldersInner(fs, parentId) {
            return $.map(fs, function(f){
                var nf = Folder(f.id, parentId, f.name, parseFoldersInner(f.folders, f.id));
                folderHash[f.id] = nf;
                return nf;
            });
        }
        var nf = Folder(1337, undefined, "/", parseFoldersInner(json, 1337));
        folderHash[1337] = nf;
        return {
            folders: nf,
            folderHash: folderHash
        };
    }


    function ListModel(params) {
        model = this; // TODO: Fix this
        var self = this;
        this.name = params.name + "!";

        self.selectedFolder = ko.observable();
        self.rows = ko.observableArray();

        self.products = undefined;

        self.folders = undefined;
        self.folderHash = undefined;

        self.selected = ko.observableArray();


        self.parents = function() {
            var f = this.selectedFolder(), a = [];
            console.log("parents: ", f);
            if(f) {
                a.push(f);
                while (f.parentId !== undefined) {
                    f = self.folderHash[f.parentId];
                    a.push(f);
                }
            }
            console.log("And the result = ", a);
            return a.reverse();
        };

        self.updateSelected = function(item, selectedFlag) {
            if(selectedFlag) {
                self.selected.push(item);
            } else {
                var index = self.selected.indexOf(item);
                if (index > -1) {
                    self.selected.splice(index, 1);
                }
            }
        }

        self.clearSelection = function() {
            console.log("Clearselection");
            $.each(self.selected(), function(i, item) {
                item.selected(false);
            });
            self.selected([]);
        };

        self.remove = function() {
            var selected = self.selected(),
                res = params.removeFn(selected), okCount = 0, failCount = 0, msg;
            self.clearSelection();
            console.log("Remove fn", res);
            $.each(res, function(i, r) {
                console.log("Remove callback");
                if(r.status === "error") {
                    failCount++;
                } else {
                    okCount++;
                    self.selectedFolder().removeChild(r.item);
                }
            });

            if(okCount>0) {
                CCBMessengerInfo("Removed " + okCount);
            }
            if(failCount>0) {
                CCBMessengerError("Failed to remove " + errorCount);
            }
            console.log("Remove");
        };

        self.move = function() {
            console.log("Move");
        };

        self.copy = function() {
            console.log("Copy");
        };

        self.showFolder = function(folderId) {
            console.log("showFolder", folderId);
            self.selectedFolder(self.folderHash[folderId]);
            self.rows(self.folderHash[folderId].folders.concat(self.folderHash[folderId].materials));
        };

        params.getData().done(function(data){
            console.log("rows: ", data.rows);
            console.log("folders: ", data.folders);
            var folderInfo = parseFolders(data.folders);
            self.folderHash = folderInfo.folderHash;
            self.folders = folderInfo.folders;
            var res = $.map(data.rows, function(item) {
                var materialFolderId  = item.materialFolderId;
                var r = Material(item.id, materialFolderId, item.title, item.typeTitle, item.thumbnail);
                if(materialFolderId === null || materialFolderId === undefined) {
                    materialFolderId = 1337;
                }
                console.log("self.folderHash[materialFolderId]", materialFolderId, self.folderHash[materialFolderId]);
                self.folderHash[materialFolderId].materials.push(r);
                return r;
            });
            self.showFolder(1337);

        });
    }

    $(document).ready(function() {
        ko.components.register('cocobox-list', {
            viewModel: ListModel,
            template: {element: "cocobox-list"}
        });
    });
    return exports;
});


define(['knockout', 'dabox-common', 'cocobox-list'], function (ko) {
    "use strict";
    var exports = {};
    var settings;


    function ListMaterialModel() {
        var self = this;

        function readData(url) {
            var deferred = $.Deferred();
            $.getJSON(url).done(function(data){
                // TODO: Handle errors
                deferred.resolve({ rows: data.aaData, folders: data.folders});
            });
            return deferred.promise();
        }

        // Return parameters for cocobox-list component
        self.cocoboxListParams = function() {
            return {
                editMode: settings.editMode,
                getData: function(){
                    return readData(settings.listUrl);
                },
                groupOps: [{
                    html: '<span><span class="glyphicon glyphicon - trash"></span> Removelol</span>',
                    callback: function(items) {
                        console.log("Remove fn on", items);
                    }
                }],
                moveFn: function(items, toFolder) {
                    console.log("Moving ", items, toFolder);
                    return true;
                },
                removeFn: function(items) {
                    console.log("removeFn callback ", items);
                    return $.map(items, function(item){
                        console.log("Creating from ", item);
                       return ({ status: "ok", item: item});
                    });
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
