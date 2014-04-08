/* 
 * (c) Dabox AB 2012 All Rights Reserved
 * 
 */
var listMaterialsOrgMats = (function() {
    "use strict";
    //Shared variables
    var pub = {
        listOrgMatsUrl: "",
        listUrl: "",
        listOrgMatLinksUrl: "",
        newOrgMatUrl: "",
        createOrgMatUrl: "",
        editOrgMatUrl: "",
        deleteOrgMatLinkUrl: "",
        deleteOrgMatUrl: "",
        toggleActiveUrl: "",
        toggleActiveToUrl: "",
        contextPath: "",
        oTable: null
    };

    var tableInit = function() {
        var oTable = $('#listmaterials').dataTable({
            "sDom": 'f<"clear">rt<"dataTables_footer clearfix"i>',
            "bPaginate": false,
            "aaSorting": [[1,'asc']],
            "aoColumnDefs": [
                {
                    "aTargets": [ 0 ],
                    "mData": "type",
                    "sClass": "type",
                    "fnRender": function ( oObj ) {
                        if (oObj.aData.thumbnail) {
                            return  '<img src="'+oObj.aData.thumbnail+'" />';
                        } else {
                            return  '<div class="'+oObj.aData.type+'"></div>';
                        }
                    }
                },
                {
                    "aTargets": [ 1 ],
                    "mData": "title",
                    "fnRender": function ( oObj ) {
                        var desc = oObj.aData.desc || "";

                        return  '<h1>' + oObj.aData.title + '</h1><div class="itemactions"><a href="'+
                            oObj.aData.viewLink+ '" target="_blank">'+ 'Preview</a> | <a href="'+ pub.editOrgMatUrl
                            + '/' + oObj.aData.id
                            +'">Edit</a> | <a href="" onclick="listMaterialsOrgMats.deleteOrgMat('+oObj.aData.id+'); return false">Delete</a></div><p class="lang">'
                            + 'English' + '</p><p class="description">' + desc + '</p>';
                    }
                }
            ],

            "sAjaxSource": pub.listUrl,
            "oLanguage": {
                "sSearch": "",
                "sZeroRecords": "No materials matches your query",
                "sEmptyTable": "<span class='emptytable'>No uploaded materials. <a href='" + pub.createOrgMatUrl + "'>Add material.</a></span>",
                "sLoadingRecords": "<p>Loading materials...</p><img src='../../img/spinner-threedots.gif' />"
            },
            "fnInitComplete": function() {
                updateCount();
            }
        });

        pub.oTable = oTable;
    };

    var updateCount = function() {
        var nMaterialsOrgMats = listMaterialsOrgMats.oTable._('tr').length;
        $("#sm_added span.count").remove();
        $('#sm_added span').append('<span class="count">' + nMaterialsOrgMats + '</span>');
    };

    var init = function() {
        pub.listUrl = pub.listOrgMatsUrl;
        tableInit();

        $('#listmaterials_filter input').attr('placeholder', pub.sLang);

    };

    var deleteOrgMat = function(orgMatId) {
        var ajaxData = {"orgmatid": orgMatId};
        var ajaxSettings = {};
        ajaxSettings.data = ajaxData;
        ajaxSettings.success = processDeleteOrgMatAjaxResponse;
        require(['dabox-common'], function() {
            cocobox.confirmationDialog("Delete material",
                    "Do you want to permanently delete this material?",
                    function() {
                        cocobox.ajaxPost(pub.deleteOrgMatUrl, ajaxSettings);
                    }
            );
        });
     
    };

    var processDeleteOrgMatAjaxResponse = function(data) {
        log('Ajax response', data);

        if (data.status == "OK") {
            //Delete row with error
            deleteOrgMatRow(data);
        } else {
            displayDeleteOrgMatError(data);
        }
    };

    var deleteOrgMatRow = function(data) {
        var orgmatid = data.orgmatid;

        var rows = pub.oTable.fnGetData();

        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            if (row.id == orgmatid) {
                pub.oTable.fnDeleteRow(i);
                updateCount();
                return;
            }
        }
    };

    var displayDeleteOrgMatError = function(data) {
        var lines = [];
        lines.push("This material cannot be deleted due to the foilowing reason.");
        lines.push("");
        if (data.activeLinkCount > 0) {
            lines.push("This material has an active access link");
            lines.push();
        }

        var pns = data.linkedProjectNames;
        if (pns && pns.length > 0) {
            lines.push("This material is used in the following projects:");
            lines.push();
            for(var i = 0; i<pns.length; i++) {
                var pn = pns[i];
                lines.push(pn);
            }
        }

        cocobox.infoDialog("Unable to delete material", lines);
    };

    //Public methods
    pub.init = init;
    pub.deleteOrgMat = deleteOrgMat;

    return pub;

})();

