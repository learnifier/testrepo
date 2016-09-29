/*
 * (c) Dabox AB 2016 All Rights Reserved
 */

define(['knockout', 'cocobox/ccb-imodal', 'dabox-common', 'cocobox/ko-components/list/cocobox-list', 'es6-shim'], function (ko, ccbImodal) {
    "use strict";
    var exports = {};
    var settings;
    var realmProducts;


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
                filterTypes: undefined,
                groupOps: true,
                folderSelectable: true,
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
                    format: function(val, item){ return '<span class="material-name">' + decorateLink(item.file.type==="DIRECTORY", val)  + '</span>'; },
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
                    value: function(item){return item.file.modified && item.file.modified.raw},
                    format: function(val, item){return item.file.modified && item.file.modified.shortstr ? item.file.modified.shortstr : ""},
                    sortable: true,
                    cssClass: "material-updated",
                    clickFn: null
                }],
                vfsActions: {
                    preview: { html: '<span class="glyphicon glyphicon-eye-open"></span> Preview' },
                    rename: { html: '<span class="glyphicon glyphicon-pencil"></span> Rename' },
                    edit: { html: '<span class="glyphicon glyphicon-edit"></span> Edit' },
                    move: { html: '<span class="glyphicon glyphicon-share"></span> Move' },
                    remove: { html: '<span class="glyphicon glyphicon-trash"></span> Delete' }
                },
                postDeleteErrorAction: function(file) {
                    var prodId, url;
                    if(file.attributes) {
                        prodId = file.attributes.productId;
                        url = settings.postDeleteErrorUrl + "?productId=" + prodId;
                        $.ajax(url).done(function(data){
                            if(data.projects) {
                                var msg = "Unable to delete product. It is used in the following project(s):\n\n" +
                                    data.projects.map(function (p) {
                                        return '"' + p.name + '"';
                                    }).join("\n");
                                cocobox.errorDialog("Unable to delete", msg, function () {
                                });
                            }
                        });
                    }
                },
                setApi: function(api){
                    self.cocoboxListApi(api);
                }
            }
        };
        function openCreateMaterial(url, types) {

            function createAndMove(prod){
                return new Promise(function(resolve, reject) {
                    $.ajax(settings.resolveIdToVfs + "?productId=" + prod.id).done(function (prod) {
                        if(folderPath != "/") {
                            settings.vfs.rename(prod.path, folderPath).then(function () {
                                resolve(folderPath + prod.path);
                            }).catch(function (e) {
                                reject(e);
                            });
                        } else {
                            resolve(prod.path);
                        }
                    });
                });
            }

            function createAndMoveMulti(prods){
                return Promise.all(prods.map(function(prod){
                    return createAndMove(prod)
                }));
            }

            var folderPath = self.cocoboxListApi().currentFolderPath();


            types.map(function(type){
               url += "&type[]=" + type;
            });

            var imodal = new ccbImodal.Server({
                serviceName: "addProducts",
                url: url,
                callbacks: {
                    "add": function (data) {
                        if(data.products instanceof Array) {
                            createAndMoveMulti(data.products).then(function(){
                                self.cocoboxListApi().refresh();
                            }).catch(function(e){
                                self.cocoboxListApi().refresh();
                                console.log("Error moving products: ", e);
                                CCBMessengerError("There was a problem moving a newly created product. Your new product may be in the home folder.");
                            });
                        }
                    },
                    "addAndEdit": function (data) {
                        // Products is a list, but with at most one member
                        if(data.products instanceof Array && data.products.length > 0) {
                            createAndMove(data.products[0]).then(function (prodPath) {
                                self.cocoboxListApi().refresh();
                                self.cocoboxListApi().runOp(prodPath, "edit");
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

        self.canWriteFolder = function() {
            return self.cocoboxListApi() && self.cocoboxListApi().canWriteFolder();
        };

        self.addMenu = function(){
            var rows = [];

            settings.addMenu.forEach(function(c){
                var first = true;
                c.category = true;
                c.types.forEach(function(t) {
                    t.category = false;
                    if (t.alwaysShown || realmProducts[t.pType]) {
                        if (first) {
                            rows.push(c);
                            first = false;
                        }
                        rows.push(t);
                    }
                });
            });
            return rows;
        };
    }

    exports.init = function(options) {
        settings = Object.assign({
            vfs: undefined,
            listUrl: undefined,
            addProductUrl: undefined,
            resolveIdToVfs: undefined,
            postDeleteErrorUrl: undefined,
            realmProductsUrl: undefined,
            addMenu: [],
            editMode: true
        }, options);

        $.getJSON(settings.realmProductsUrl).done(function(data){
            realmProducts = data;
        }).fail(function(){
            CCBMessengerError("There was a problem loading product types, it may not be possible to create new materials.");
            realmProducts = [];
        }).always(function(){
            ko.applyBindings(new ListMaterialModel(), $("#pagewrapper").get(0));
        });
    };
    return exports;
});
