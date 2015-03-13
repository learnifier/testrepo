define(['knockout'],function(ko){
    "use strict";
    
    var DeepLinkModel = function(){
      var self = this;
      
      self.materials = ko.observableArray();
    };
    
    
    var MaterialModel = function(){
       var self = this;
       
       self.id = ko.observable();
       self.thumbnail = ko.observable();
       self.title = ko.observable();
       self.description = ko.observable();
       self.status = ko.observable();
       self.activeUntil = ko.observable();
       self.url = ko.observable();
    };
    
    $.get(listDeeplinksOrgMats.listOrgMatsUrl,function(data){
        var rootModel = new DeepLinkModel();
        
        $.each(data.aaData, function(){
           var mm = new MaterialModel();
           
           mm.id(this.materialId);
           mm.thumbnail(this.thumbnail);
           mm.title(this.title);
           mm.description(this.desc);
           mm.status(this.activeLinks > 0);
           //mm.activeUntil(this.)
           //mm.url(this.)
           rootModel.materials.push(mm);
        });
        
        ko.applyBindings(rootModel);
        
    }).error(function(){
        alert('failed to load data');
    });
    
});