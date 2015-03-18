define(['knockout', 'bootstrap/cocobox-editable-date', 'cocobox-knockout-bindings', 'bootstrap/toggle', 'dabox-common'], function (ko,datepicker) {
    "use strict";

    var DeepLinkModel = function () {
        var self = this;

        self.materials = ko.observableArray();
    };
    

    var MaterialModel = function () {
        var self = this;

        self.id = ko.observable();
        self.thumbnail = ko.observable();
        self.title = ko.observable();
        self.description = ko.observable();
        self.status = ko.observable(false);
        self.activeUntil = ko.observable();
        self.activeUntilString = ko.observable();
        self.url = ko.observable();
        self.linkId = ko.observable();
        self.linkSectionVisible = ko.observable(false);
           

        self.toggleLinkSection = function () {
            if (self.linkSectionVisible() === true) {
                self.linkSectionVisible(false);
            } else {
                $.post(listDeeplinksOrgMats.listOrgMatLinksUrl, {orgmatid: self.id()}, function (data) {
                    self.activeUntil(data.aaData[0].activeto);
                    self.activeUntilString(data.aaData[0].activetoStr);
                    self.url(data.aaData[0].deeplink);
                    self.status(data.aaData[0].active);

                    self.linkId(data.aaData[0].linkid);

                    //Important to have this last!
                    self.linkSectionVisible(true);                    
                }).fail(function () {
                    alert('failed to post data');
                });
            }

        };
       
        self.changeActiveUntil = function(){
            alert(self.activeUntil());
        };

        self.toggleStatus = function(element, ev) {
            var newStatus = !self.status();

            $(element).bootstrapToggle('disable');

            $.post(listDeeplinksOrgMats.toggleActiveUrl, {
                active: newStatus,
                linkid: self.linkId()
            }).always(function() {
                $(element).bootstrapToggle('enable');
            }).fail(cocobox.internal.ajaxErrorHandler)
            .success(function(data) {
                //Nothing to process
                $(element).bootstrapToggle(newStatus ? 'on' : 'off');
            });

            
            return false;
        };

        self.dateDisplay = function(data, text) {
            if (!text) {
                return null;
            }

            $(this).text(text);
        };
    };

    $.get(listDeeplinksOrgMats.listOrgMatsUrl, function (data) {
        var rootModel = new DeepLinkModel();

        $.each(data.aaData, function () {
            var mm = new MaterialModel();

            mm.id(this.materialId);
            mm.thumbnail(this.thumbnail);
            mm.title(this.title);
            mm.description(this.desc);
            mm.status(this.activeLinks > 0);
            rootModel.materials.push(mm);
        });

        ko.applyBindings(rootModel);

        window.model = rootModel;

    }).fail(function () {
        alert('failed to load data');
    });

});