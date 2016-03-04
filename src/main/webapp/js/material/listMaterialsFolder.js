/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'dabox-common'], function (ko) {

    "use strict";

    var exports = {};
    var model;

    var Item = function(id, name, typeTitle, thumbnail) {
        var self = {};
        self.id = id;
        self.name = name;
        self.typeTitle = typeTitle;
        self.thumbnail = thumbnail;
        self.selected = ko.observable(false);
        self.selectRow = function(){
            var s = self.selected();
            model.updateSelected(self, !s);
            self.selected(!s);
            console.log("Klick: ", this);
        };
        return self;
    };

    var Folder = function (id, name, folders) {
        var self = Item(id, name, "Folder", "");
        self.folders = folders;
        self.materials = [];

        self.clickName = function() {
            model.showFolder(id);
        }
        return self;
    };

    function Material(id, name, typeTitle, thumbnail) {
        var self = Item(id, name, typeTitle, thumbnail);
        self.name = name;
        self.typeTitle = typeTitle;

        self.clickName = function() {
            cocobox.infoDialog("Preview", "Nice material preview here.", function(){});
        }
        return self;
    };


    function parseFolders(json) {
        var folderHash = {};

        function parseFoldersInner(fs) {
            return $.map(fs, function(f){
                var nf = Folder(f.id, f.name, parseFoldersInner(f.folders));
                folderHash[f.id] = nf;
                return nf;
            });
        }
        var nf = Folder(1337, "/", parseFoldersInner(json));
        folderHash[1337] = nf;
        return {
            folders: nf,
            folderHash: folderHash
        };
    }


    function ListMaterialModel() {
        var self = this;

        self.rows = ko.observableArray();

        self.products = undefined;

        self.folders = undefined;
        self.folderHash = undefined;

        self.selected = ko.observableArray();

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

        self.showFolder = function(folderId) {
            console.log("showFolder", folderId);
            self.rows(self.folderHash[folderId].folders.concat(self.folderHash[folderId].materials));
        };

        self.readAjax = function(url) {
            $.getJSON(url).done(function(data){
                var folderInfo = parseFolders(data.folders);
                self.folderHash = folderInfo.folderHash;
                self.folders = folderInfo.folders;
                var res = $.map(data.aaData, function(item) {
                    var r = Material(item.id, item.title, item.typeTitle, item.thumbnail);
                    var materialFolderId  = item.materialFolderId;
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


    }

    var settings;

    exports.init = function(options) {
        settings = $.extend({
            listUrl: undefined,
            editMode: false
        }, options || {});

        model = new ListMaterialModel();
        ko.applyBindings(model);

        model.readAjax(settings.listUrl);

        console.log("model = ", model);
    };

    return exports;

});
