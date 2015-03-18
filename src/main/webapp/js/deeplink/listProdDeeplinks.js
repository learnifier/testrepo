define(['knockout', 'bootstrap/datepicker'], function (ko,datepicker) {
    "use strict";

    var PageModel = function () {
        var self = this;

        self.materials = ko.observableArray();
    };
    
    var CreditHistoryModel = function() {
        var self = this;
      
        
        self.amount = ko.observable();
        self.createdStr = ko.observable();
        self.createdBy = ko.observable();
    };
    
    var DeepLinkModel = function () {
        var self = this;
       
        self.activeto = ko.observable();
        self.defaultLink = ko.observable();
        self.url = ko.observable();
        self.balance = ko.observable();
        self.linkid = ko.observable();
        self.creditSectionVisible = ko.observable(false);
        self.credits = ko.observableArray();
        
        self.toggleCreditHistorySection = function () {
            if (self.creditSectionVisible() == true) {
                self.creditSectionVisible(false);
            } else {
                $.post(listDeeplinksProducts.listLinksHistoryUrl+'/'+self.linkid(), function (data) {
                   
                    self.credits.removeAll();
                    self.creditSectionVisible(true);
                    
                    $.each(data.aaData,function(){
                       var credit = new CreditHistoryModel();
                       
                       credit.amount(this.amount);
                       credit.createdBy(this.createdBy);
                       credit.createdStr(this.createdStr);

                       self.credits.push(credit);
                    });
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            }
        };
           
    };

    var ProductModel = function () {
        var self = this;

        self.id = ko.observable();
        self.thumbnail = ko.observable();
        self.title = ko.observable();
        self.description = ko.observable();
        self.activeLinks = ko.observable();
        self.activeUntil = ko.observable();
       
        self.linkCredits = ko.observable();
        self.inactiveLinks = ko.observable();
        self.status = ko.observable();
        self.linkSectionVisible = ko.observable(false);
        self.links = ko.observableArray();
        

        self.toggleLinkSection = function () {
            if (self.linkSectionVisible() == true) {
                self.linkSectionVisible(false);
            } else {
                $.post(listDeeplinksProducts.listLinksUrl, {opid: self.id()}, function (data) {
                   
                    self.links.removeAll();
                    self.linkSectionVisible(true);
                    
                    $.each(data.aaData,function(){
                       var link = new DeepLinkModel();
                       
                       link.activeto(this.activeto);
                       link.defaultLink(this.defaultLink);
                       link.url(this.link);
                       link.balance(this.balance);
                       link.linkid(this.linkid);
                       self.links.push(link);
                    });
                       
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
    
  
    

    $.get(listDeeplinksProducts.listPurchasedMatsUrl, function (data) {
        var mo = new PageModel();

        $.each(data.aaData, function () {
            var mm = new ProductModel();

            mm.id(this.opid);
            mm.thumbnail(this.thumbnail);
            mm.title(this.title);
            mm.description(this.desc);
            mm.activeLinks(this.activeLinks);
            mm.inactiveLinks(this.inactiveLinks);
            mm.linkCredits(this.linkCredits);
            mo.materials.push(mm);
        });

        ko.applyBindings(mo);

    }).fail(function () {
        alert('failed to load data');
    });

});