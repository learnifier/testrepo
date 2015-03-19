define(['knockout', 'bootstrap/datepicker'], function (ko,datepicker) {
    "use strict";

    var PageModel = function () {
        var self = this;
        
        self.addLinkModel = ko.observable();
        self.creditsLeft = ko.observable();
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
        self.defaultLink = ko.observable();
        self.linkid = ko.observable();
        self.creditSectionVisible = ko.observable(false);
        self.credits = ko.observableArray();
        
        
        self.showAddCreditsModel = function(productModel, rootModel){
            rootModel.addLinkModel(self);
            
            $.post(listDeeplinksProducts.creditBalance+'/'+productModel.id(), function (data) {
                   
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            
        };
        
        self.addNewCredits = function(productModel, rootModel){

            
            $.post(listDeeplinksProducts.updateCredits, {credits: $('#creditsVal').val(),orgId: listDeeplinksProducts.orgId ,oplid: self.linkid()}, function (data) {
                    
                  if(data.valid == false)
                  {
                     console.log(data.fielderror[0].message);
                     $('#cand').html('* '+data.fielderror[0].message);
                  }
                  
                }).fail(function () {
                    alert('failed to post data');
                });
            
        };
        
        
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
        
        self.deleteLink = function (parent) {

                $.post(listDeeplinksProducts.deleteOrgMatLinkUrl, {prodlink: self.linkid()}, function (data) {
                   
                 parent.links.remove(self);
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            
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
        self.buttonStatus = ko.observable('Get Link');
        self.linkName = ko.observable('Default Link');
        self.links = ko.observableArray();
        
        

        self.toggleLinkSection = function () {
            if (self.linkSectionVisible() == true) {
                self.linkSectionVisible(false);
                self.buttonStatus('Get Link');
            } else {
                $.post(listDeeplinksProducts.listLinksUrl, {opid: self.id()}, function (data) {
                   
                    self.links.removeAll();
                    self.linkSectionVisible(true);
                    self.buttonStatus('Hide');
                    
                    $.each(data.aaData,function(){
                       var link = new DeepLinkModel();
                       
                       link.activeto(this.activeto);
                       link.defaultLink(this.defaultLink);
                       link.url(this.link);
                       link.balance(this.balance);
                       link.defaultLink(this.defaultLink);
                       link.linkid(this.linkid);
                       self.links.push(link);
                    });
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            }
        };
        
        self.addLink = function () {

                $.post(listDeeplinksProducts.newOrgMatUrl, {orgmatid: self.id()}, function (data) {
                   
                  $.each(data.aaData,function(){
                       var link = new DeepLinkModel();
                       
                       link.activeto(this.activeto);
                       link.defaultLink(this.defaultLink);
                       link.url(this.link);
                       link.balance(this.balance);
                       link.defaultLink(this.defaultLink);
                       link.linkid(this.linkid);
                       self.links.push(link);
                    });
                  
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            
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