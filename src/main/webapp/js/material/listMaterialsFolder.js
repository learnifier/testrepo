/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define("cocobox-list", ['knockout', 'dabox-common', 'messenger'], function (ko) {

    "use strict";

    var exports = {};

    var model;

    var Item = function(id, parentId, name, typeTitle, thumbnail) {
        var self = this;
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
    };

    var Folder = function (id, parentId, name, folders) {
        var self = this;
        Item.call(this, id, parentId, name, "Folder", "");
        self.folders = ko.observableArray(folders);
        self.materials = ko.observableArray();
        self.clickName = function(e) {
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
            if (index > -1) {
                a.splice(index, 1);
                return true;
            }
            return false;
        };
    };

    function Material(id, parentId, name, typeTitle, thumbnail) {
        var self = this;
        Item.call(this, id, parentId, name, typeTitle, thumbnail);
        self.name = name;
        self.typeTitle = typeTitle;

        self.clickName = function() {
            cocobox.infoDialog("Preview", "Nice material preview here.", function(){});
        }
    };


    function parseFolders(json) {
        var folderHash = {};

        function parseFoldersInner(fs, parentId) {
            return $.map(fs, function(f){
                var nf = new Folder(f.id, parentId, f.name, parseFoldersInner(f.folders, f.id));
                folderHash[f.id] = nf;
                return nf;
            });
        }
        var nf = new Folder(1337, undefined, "/", parseFoldersInner(json, 1337));
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
        //self.rows = ko.observableArray();
        self.rows = function(){
            console.log("Rows: ", self, self.selectedFolder());
            if(self.selectedFolder()) {
                return self.selectedFolder().folders().concat(self.selectedFolder().materials());
            } else {
                return [];
            }
        };
        self.products = undefined;

        self.folders = undefined;
        self.folderHash = undefined;

        self.selected = ko.observableArray();


        self.parents = function() {
            var f = this.selectedFolder(), a = [];
            if(f) {
                a.push(f);
                while (f.parentId !== undefined) {
                    f = self.folderHash[f.parentId];
                    a.push(f);
                }
            }
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
        };

        self.clearSelection = function() {
            $.each(self.selected(), function(i, item) {
                item.selected(false);
            });
            self.selected([]);
        };

        self.remove = function() {
            var selected = self.selected(),
                okCount = 0, failCount = 0, msg;

            if(params.removeFn) {
                params.removeFn(selected).done(function(res){
                    self.clearSelection();
                    $.each(res, function(i, r) {
                        if(r.status === "error") {
                            failCount++;
                        } else {
                            okCount++;
                            self.selectedFolder().removeChild(r.item);
                        }
                    });

                    if(okCount>0) {
                        CCBMessengerInfo("Removed " + okCount  + " item(s)");
                    }
                    if(failCount>0) {
                        CCBMessengerError("Failed to remove " + failCount + " item(s).");
                    }
                });
            }
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
        };

        params.getData().done(function(data){
            console.log("rows: ", data.rows);
            console.log("folders: ", data.folders);
            var folderInfo = parseFolders(data.folders);
            self.folderHash = folderInfo.folderHash;
            self.folders = folderInfo.folders;
            var res = $.map(data.rows, function(item) {
                var materialFolderId  = item.materialFolderId;
                var r = new Material(item.id, materialFolderId, item.title, item.typeTitle, item.thumbnail);
                if(materialFolderId === null || materialFolderId === undefined) {
                    materialFolderId = 1337;
                }
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
                    return $.map(items, function(item){
                        return ({ status: "ok", item: item});
                    });
                },
                copyFn: function(items, toFolder) {
                    return $.map(items, function(item){
                        return ({ status: "ok", item: item});
                    });
                },
                removeFn: function(items) {
                    var deferred = $.Deferred();
                    window.setTimeout(function(){
                        console.log("Resolving...", items);
                        deferred.resolve($.map(items, function(item){
                            return ({ status: "ok", item: item });
                        }));
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
