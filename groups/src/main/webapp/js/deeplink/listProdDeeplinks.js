define(['knockout', 'bootstrap/cocobox-editable-date', 'cocobox-knockout-bindings', 'bootstrap/toggle', 'dabox-common'], function (ko,datepicker) {
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
        self.linkTokenId = ko.observable();
        self.deleteLink = ko.observable();
        
        
        self.deleteCreditHistory = function (parent) {
            
            
            cocobox.confirmationDialog("Delete credits",
                    "Do you want to delete these credits for this link?",
                    function () {
                        cocobox.ajaxPost(self.deleteLink());
                        parent.credits.remove(self); 
                        parent.balance(parent.balance() - self.amount());
                    }
            );

        };
        
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
        self.active = ko.observable();
        self.activeUntilString = ko.observable();
        self.parent = ko.observable();
        
        console.log(self.balance);
        self.dateDisplay = function(data, text) {
            if (!text) {
                return null;
            }

            $(this).text(text);
        };
        
         self.toggleStatus = function(element, parent) {
            var newStatus = !self.active();

            $(element).bootstrapToggle('disable');

            $.post(listDeeplinksProducts.toggleActiveUrl, {
                active: newStatus,
                linkid: self.linkid()
            }).always(function() {
                $(element).bootstrapToggle('enable');
            }).fail(cocobox.internal.ajaxErrorHandler)
            .success(function(data) {
                //Nothing to process
                $(element).bootstrapToggle(newStatus ? 'on' : 'off');
                if(newStatus == true)
                {
                 parent.activeLinks(parent.activeLinks() + 1);
                 parent.inactiveLinks(parent.inactiveLinks() - 1);
                }
                else
                {
                  parent.activeLinks(parent.activeLinks() - 1);
                  parent.inactiveLinks(parent.inactiveLinks() + 1);
                }
            });

            
            return false;
        };
        
        
        self.showAddCreditsModel = function(productModel, rootModel){
            rootModel.addLinkModel(self);
            self.creditSectionVisible(false);
            $.post(listDeeplinksProducts.creditBalance+'/'+productModel.id(), function (data) {
                   
                       
                }).fail(function () {
                    alert('failed to post data');
                });
            
        };
        
        self.addNewCredits = function(productModel){
           
           var insertedCredits = $('#creditsVal').val();
           
            $.post(listDeeplinksProducts.updateCredits, {credits: insertedCredits,orgId: listDeeplinksProducts.orgId ,oplid: self.linkid()}, function (data) {
                    
                  if(data.valid == false)
                  {
                     $('#cand').html('* '+data.fielderror[0].message);
                  }
                  self.parent().linkCredits(self.parent().linkCredits() + parseInt(insertedCredits));
                  self.balance(self.balance() + parseInt(insertedCredits));
                  
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
                       credit.deleteLink(this.deleteLink);
                       credit.linkTokenId(this.linkTokenId);

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
                 if(self.active() == false)
                 {
                     parent.inactiveLinks(parent.inactiveLinks() - 1);
                 }
                 else
                 {
                     parent.activeLinks(parent.activeLinks() - 1);
                 }
                       
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
        self.activeUntilString = ko.observable();
        
        self.linkCredits = ko.observable();
        self.inactiveLinks = ko.observable();
      
       
        self.totalLinks = ko.computed(function() {
            return self.activeLinks() + self.inactiveLinks();
        }, self);
        
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
                       
                       link.parent(self); 
                       link.active(this.active);
                       link.activeto(this.activeto);
                       link.activeUntilString(this.activeToStr);
                       link.defaultLink(this.defaultLink);
                       link.url(this.link);
                       link.balance(this.balance);
                       link.defaultLink(this.defaultLink);
                       link.linkid(this.linkid);
                       self.links.push(link);
                    });
                       $('.linksLoader').hide();
                }).fail(function () {
                    alert('failed to post data');
                });
            }
        };
        
        self.addLink = function () {

                $.post(listDeeplinksProducts.newOrgMatUrl, {orgmatid: self.id()}, function (data) {
                 
                  $.each(data.aaData,function(){
                       var link = new DeepLinkModel();
                       
                       link.parent(self); 
                       link.activeto(this.activeto);
                       link.defaultLink(this.defaultLink);
                       link.url(this.link);
                       link.balance(this.balance);
                       link.defaultLink(this.defaultLink);
                       link.linkid(this.linkid);
                       self.links.push(link);
                    });
                  
                  self.inactiveLinks(self.inactiveLinks() +1);     
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
            mm.totalLinks();
            mm.linkCredits(this.linkCredits);
            mo.materials.push(mm);
        });

        ko.applyBindings(mo);
        
       
        $('#koFix').show();
        $('.PageLoader').hide();
    }).fail(function () {
        alert('failed to load data');
    });

});