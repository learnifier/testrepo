define(['knockout', 'bootstrap/datepicker'], function (ko,datepicker) {
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
        self.status = ko.observable();
        self.activeUntil = ko.observable();
        self.url = ko.observable();
        self.linkSectionVisible = ko.observable(false);

        self.toggleLinkSection = function () {
            if (self.linkSectionVisible() == true) {
                self.linkSectionVisible(false);
            } else {
                $.post(listDeeplinksOrgMats.listOrgMatLinksUrl, {orgmatid: self.id()}, function (data) {
                    self.activeUntil(data.aaData[0].activeto);
                    self.url(data.aaData[0].deeplink);
                    self.status(data.aaData[0].active);

                    self.linkSectionVisible(true);
                }).fail(function () {
                    alert('failed to post data');
                });
            }

        };
       
        self.changeActiveUntil = function(){
            alert(self.activeUntil());
        };
    };
    
    
        ko.bindingHandlers.datePicker = {
        init: function(element, valueAccessor, allBindings, viewModel, bindingContext) {
            
            $(element).datepicker({
                
            });
        }
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

    }).fail(function () {
        alert('failed to load data');
    });

});