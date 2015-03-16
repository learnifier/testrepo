define(['knockout', 'bootstrap/datepicker'], function (ko,datepicker) {
    "use strict";

    var DeepLinkModel = function () {
        var self = this;

        self.materials = ko.observableArray();
    };
    
    var CreditHistoryModel = function() {
        var self = this;
        
        self.credits = ko.observableArray();
    };
    

    var PageModel = function () {
        var self = this;

        self.id = ko.observable();
        self.thumbnail = ko.observable();
        self.title = ko.observable();
        self.description = ko.observable();
        self.activeLinks = ko.observable();
        self.activeUntil = ko.observable();
        self.url = ko.observable();
        self.linkCredits = ko.observable();
        self.inactiveLinks = ko.observable();
        self.linkSectionVisible = ko.observable(false);
           

        self.toggleLinkSection = function () {
            if (self.linkSectionVisible() == true) {
                self.linkSectionVisible(false);
            } else {
                $.post(listDeeplinksOrgMats.listOrgMatLinksUrl, {orgmatid: self.id()}, function (data) {
                    self.activeUntil(data.aaData[0].activeto);
                    self.url(data.aaData[0].deeplink);
                    self.activeLinks(data.aaData[0].active);

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
    
  
    

    $.get(listDeeplinksOrgMats.listPurchasedMatsUrl, function (data) {
        var ProductModel = new DeepLinkModel();

        $.each(data.aaData, function () {
            var mm = new PageModel();

            mm.id(this.opid);
            mm.thumbnail(this.thumbnail);
            mm.title(this.title);
            mm.description(this.desc);
            mm.activeLinks(this.activeLinks);
            mm.inactiveLinks(this.inactiveLinks);
            mm.linkCredits(this.linkCredits);
            ProductModel.materials.push(mm);
        });

        ko.applyBindings(ProductModel);

    }).fail(function () {
        alert('failed to load data');
    });

});