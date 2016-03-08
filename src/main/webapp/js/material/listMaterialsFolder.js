/*
 * (c) Dabox AB 2016 All Rights Reserved
 */


define("cocobox-list", ['knockout', 'dabox-common', 'messenger'], function (ko) {

    "use strict";

    var exports = {};
    function ListModel(params) {
        var model = this, self = this;

        this.dialogContext = ko.observable();

        // ----------------- modal stuff, can this be moved??? -----------------

        this.replaceKoDialog = function (templateName, data, opts) {
            innerShowKoDialog(templateName, data, opts, false);
        };

        this.showKoDialog = function (templateName, data, opts) {
            innerShowKoDialog(templateName, data, opts, true);
        };

        var innerShowKoDialog = function (templateName, data, opts, activate) {

            if ($("#" + templateName).length === 0) {
                alert("Knockout template with name " + templateName + " doesn't exist. Unable to show dialog");
                return;
            }
            var dlgOpts = $.extend({}, opts);

            if (!dlgOpts.buttons) {
                dlgOpts.buttons = [];
            }

            if (!dlgOpts.title) {
                dlgOpts.title = "";
            }

            //Setup default values for all buttons
            for (var i = 0; i < dlgOpts.buttons.length; i++) {
                var btn = dlgOpts.buttons[i];

                if (!btn.extraCss) {
                    btn.extraCss = {};
                }

                if (typeof btn.enable === "undefined") {
                    btn.enable = true;
                }

                btn.enableFn = function (val) {
                    if (typeof val === "function") {
                        return val();
                    } else {
                        return ko.unwrap(val);
                    }
                };

            }

            var ctx = {data: data, name: templateName, opts: dlgOpts};

            try {
                model.dialogContext(ctx);
            } catch (e) {
                alert("Failed to activate modal " + templateName + ": " + e);
                return;
            }

            if (activate) {
                $("#listModal").modal();

                //Use this to let transitions complete
                $("#listModal").one("hidden.bs.modal", function () {
                    model.dialogContext(null);
                });
            }

        };

        this.runWhenKoDialogClosed = function (fn) {
            if ($("#listModal").length && $('#listModal').is(':visible')) {
                $("#listModal").one("hidden.bs.modal", function () {
                    fn();
                });
            } else {
                fn();
            }
        };

        this.hideKoDialog = function () {
            $("#listModal").modal('hide');
        };

        // ----------------- end modal stuff ---------------------

        // ----------------------------------
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
                console.log("Removechild", self, child);
                var a;
                if(child instanceof Folder) {
                    a = self.folders;
                } else {
                    a = self.materials;
                }
                var index = a.indexOf(child);
                if (index > -1) {
                    a.splice(index, 1);
                    return true;
                }
                return false;
            };

            self.addChild = function(child){
                console.log("Addchild", self, child);
                var a;
                if(child instanceof Folder) {
                    a = self.folders;
                } else {
                    a = self.materials;
                }
                a.push(child);
                return false;
            };

            self.path = function() {
                return "lol/" + self.name;
            }
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
            var nf = new Folder(1337, undefined, "home", parseFoldersInner(json, 1337));
            folderHash[1337] = nf;
            return {
                folders: nf,
                folderHash: folderHash
            };
        }
        // ----------------------------------

        self.selectedFolder = ko.observable();
        self.rows = function(){
            if(self.selectedFolder()) {
                return self.selectedFolder().folders().concat(self.selectedFolder().materials());
            } else {
                return [];
            }
        };
        self.folderHash = undefined;
        self.selected = ko.observableArray();

        self.listFolders = function() {
            var acc = [];
            function listFoldersInner(fs) {
                $.each(fs, function(i, f) {
                    acc.push(f);
                    if(f.folders()) {
                        listFoldersInner(f.folders());
                    }
                });
            }
            listFoldersInner(self.folderHash[1337].folders());
            return acc;
        };

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
                okCount = 0, failCount = 0;

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
            var selected = self.selected();

            var PickFolderModel = function() {
                var self = this;

                self.folder = ko.observable();
                self.folders = model.listFolders();
                self.executeMove = function() {
                    console.log("onPick: ", selected, self.folder());
                    var okCount = 0, failCount = 0;
                    if(params.moveFn) {
                        params.moveFn(selected, self.folder().id).done(function(res){
                            model.clearSelection();
                            $.each(res, function(i, r) {
                                if(r.status === "error") {
                                    failCount++;
                                } else {
                                    okCount++;
                                    model.selectedFolder().removeChild(r.item);
                                    self.folder().addChild(r.item);
                                }
                            });
                            if(okCount>0) {
                                CCBMessengerInfo("Moved " + okCount  + " item(s)");
                            }
                            if(failCount>0) {
                                CCBMessengerError("Failed to move " + failCount + " item(s).");
                            }
                        });
                    }
                };

                self.selectFolder = function() {
                    self.folder(this);
                };
            };

            var pickModel = new PickFolderModel();

            self.showKoDialog("folderDialog", pickModel, {
                title: "Move",
                buttons: [{
                    text: "<span class='pe-7s-close pe-lg pe-va'></span> Close",
                    action: "close",
                    extraCss: {'btn-link': true}
                }, {
                    text: "<span class='pe-7s-check pe-lg pe-va'></span> Move to folder",
                    action: function(){
                        self.hideKoDialog();
                        pickModel.executeMove();
                    },
                    extraCss: {'btn-primary': true},
                    enable: function () {
                        return pickModel.folder;
                    }
                }
                ]
            });
        };

        self.copy = function() {
            cocobox.infoDialog("Copy", "Copy is not implemented yet", function(){});
        };

        self.showFolder = function(folderId) {
            self.selectedFolder(self.folderHash[folderId]);
        };

        params.getData().done(function(data){
            var folderInfo = parseFolders(data.folders);
            self.folderHash = folderInfo.folderHash;
            $.each(data.rows, function(i, item) {
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
                moveFn: function(items, toFolderId) {
                    var deferred = $.Deferred();
                    window.setTimeout(function(){
                        deferred.resolve($.map(items, function(item){
                            return ({ status: "ok", item: item });
                        }));
                    }, 500);
                    return deferred.promise();
                },
                copyFn: function(items, toFolder) {
                    var deferred = $.Deferred();
                    window.setTimeout(function(){
                        deferred.resolve($.map(items, function(item){
                            return ({ status: "ok", item: item });
                        }));
                    }, 500);
                    return deferred.promise();
                },
                removeFn: function(items) {
                    var deferred = $.Deferred();
                    window.setTimeout(function(){
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
