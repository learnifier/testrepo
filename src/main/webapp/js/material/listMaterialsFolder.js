/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout'], function (ko) {

    "use strict";

    var exports = {};

    var Item = function(id, name, typeTitle, thumbnail) {
        var self = {};
        self.id = id;
        self.name = name;
        self.typeTitle = typeTitle;
        self.thumbnail = thumbnail;
        return self;
    }

    var Folder = function (id, name, folders) {
        var self = Item(id, name, "", "Folder");
        self.folders = folders;
        self.materials = [];
        
        return self;
    }

    function Material(id, name, typeTitle, thumbnail) {
        var self = Item(id, name, typeTitle, thumbnail);
        self.name = name;
        self.typeTitle = typeTitle;
        self.thumbnail = thumbnail;
    }


    function parseFolders(json) {
        var folderHash = {};

        function parseFoldersInner(fs) {
            return $.map(fs, function(f){
                var nf = new Folder(f.id, f.name, parseFoldersInner(f.folders));
                folderHash[f.id] = nf;
                return nf;
            });
        }
        var nf = new Folder(1377, "/", parseFoldersInner(json));
        folderHash[1377] = nf;
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

        self.readAjax = function(url) {

            $.getJSON(url).done(function(data){
                console.log("Got data: ", data);

                var folderInfo = parseFolders(data.folders);
                self.folderHash = folderInfo.folderHash;
                self.folders = folderInfo.folders;
                var res = $.map(data.aaData, function(item) {
                    var r = new Material(item.id, item.title, item.typeTitle, item.thumbnail);
                    var materialFolderId  = item.materialFolderId;
                    if(materialFolderId === null || materialFolderId === undefined) {
                        materialFolderId = 1377;
                    }
                    console.log("Adding", materialFolderId, r);
                    self.folderHash[materialFolderId].materials.push(r);
                    return r;
                });
                console.log("Setting rows: ", self.folderHash[1377].folders, self.folderHash[1377].material);
                self.rows(self.folderHash[1377].folders.concat(self.folderHash[1377].materials));
            });

        }

        self.click = function(a, b, c){
            console.log("Klick: ", this, a, b, c);
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
