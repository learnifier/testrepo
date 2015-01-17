"use strict";
/* 
 * (c) Dabox AB 2012 All Rights Reserved
 * 
 */
var listDeeplinksOrgMats = (function() {
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

                        return  '<h1>' + oObj.aData.title + '</h1><p class="lang">'
                            + 'English' + '</p><p class="description">' + desc + '</p>';
                    }
                },
                {
                    "aTargets": [ 2 ],
                    "mData": "activeLinks",
                    "sClass": "control activelinks",
                    "fnRender": function ( oObj ) {
                        var active = oObj.aData.activeLinks;
                        var activestate = 'OFF';
                        var activeclass = 'off';
                        if (active > 0) {
                            activestate = 'ON';
                            activeclass = 'on';
                        }                    
                        return  '<button>Get Link</button><div><p class="title">' +
                            'Link Status</p><p class="value ' + activeclass + '">'+ activestate + '</p></div>';
                    }
                }
            ],

            "sAjaxSource": pub.listUrl,
            "oLanguage": {
                "sSearch": "<p>Search</p>",
                "sZeroRecords": "No materials matches your query",
                "sEmptyTable": "<span class='emptytable'>No uploaded materials. <a href='" + pub.createOrgMatUrl + "'>Add material.</a></span>",
                "sLoadingRecords": "<p>Loading materials...</p><img src='[@common.spinnerUrl /]' />"
            }
        });

        pub.oTable = oTable;
    };


    var init = function() {
        pub.listUrl = pub.listOrgMatsUrl;
        tableInit();

        $('#listmaterials_filter input').attr('placeholder', pub.sLang);

        //new FixedHeader( oTable );


        var anOpen = [];
        $('#listmaterials').on('click','td.control', function () {
            var nTr = this.parentNode;
            var i = $.inArray(nTr, anOpen);
            if (i === -1) {
                //Row to be expanded
                var rowdata = pub.oTable.fnGetData($(this).closest('tr').get(0));

                var nDetailsRow = pub.oTable.fnOpen(nTr, fnFormatOrgMatDetails(pub.oTable, nTr, 1), 'details');

                $.post(pub.listOrgMatLinksUrl, {
                    "orgmatid": rowdata.id
                }, function(response) {
                    lmProcessResults(nDetailsRow, response, true);
                });

                $(nTr).addClass("showdetails");
                $('button', nTr).removeClass("getlink");
                $('button', nTr).addClass("hide");
                $('button', nTr).html("Hide");

                //$('div.linkdetails', nDetailsRow).slideDown('fast', function () {
                //  $('html, body').animate({scrollTop: $(".linkdetails").offset().top-80}, 500);
                //});
                anOpen.push(nTr);
            }
            else {
                //Row to be collapsed
                $(this).parent('tr').removeClass("showdetails");
                $('button', this).removeClass("hide");
                $('button', this).addClass("getlink");
                $('button', this).html("Get Link");
                $('div.linkdetails', $(nTr).next()[0]).slideUp(function () {
                    pub.oTable.fnClose(nTr);
                    anOpen.splice(i, 1);
                });
                $('button', this).removeClass("active");
            }
        });
    };

    var lmProcessResults = function (nDetailsRow, response, replace) {

        var target = $("td", nDetailsRow).first();

        if (replace) {
            $(".linkdetails", target).remove();
            target.prepend($("#linkinfo").render(response.aaData));
        } else {
            $(".linkdetails", target).last().after($("#linkinfo").render(response.aaData));
        }

        $.each(response.aaData, function(rowIndex, rowData) {
            var lss = $( "#linkstatussetting"+rowData.linkid);
            lss.buttonset();
            $('input:radio', lss).change(function() {
                var newState = this.value;
                var linkid = $(this).closest("*[data-linkid]").data("linkid");
                $.post(pub.toggleActiveUrl, {
                    "linkid": linkid,
                    "active": newState
                });
                var linkStatus = 'ON';
                if (newState === 'false') {
                    linkStatus = 'OFF';
                }
                $(this).parents('tr').prev().find('.value').text(linkStatus);
                $(this).parents('tr').prev().find('.value').toggleClass('off on');
            });
            var datePickerId="#datepicker"+rowData.linkid;
            $(datePickerId, nDetailsRow).datepicker({
                'defaultDate': "+1d",
                'minDate': +1,
                'showAnim': 'slideDown',
                'dateFormat': 'yy-mm-dd',
                'showOn': "both",
                'buttonImage': pub.contextPath+"/img/calendar-icon2.png",
                'buttonImageOnly': true
            });

            $(datePickerId, nDetailsRow).change(function() {
                var newState = this.value;
                var linkid = $(this).closest("*[data-linkid]").data("linkid");
                $.post(pub.toggleActiveToUrl, {
                    "linkid": linkid,
                    "activeTo": newState
                });
            });
           // THIS FUNCTION ACTIVATES TOOLTIPS FOR LINK ITEMS WITH CLASS TOOLTIP
                $( '#linkdetailsdiv' + rowData.linkid + ' .tooltip' ).tooltip({
                    position: {
                        my: "left top",
                        at: "left bottom+10"
                    }
                });
         });

        $(".loadspinner", target).toggleClass('hidden',true);

        $('.linkfooter', target).show();
    };

    var deleteOrgMat = function(orgMatId) {        
        var ajaxData = {"orgmatid": orgMatId};
        var ajaxSettings = {};
        ajaxSettings.data = ajaxData;
        ajaxSettings.success = processDeleteOrgMatAjaxResponse;

        require(['dabox-common'], function() {
            cocobox.confirmationDialog("Delete material", "Do you want to permanently delete this material?", function() {
                cocobox.ajaxPost(pub.deleteOrgMatUrl, ajaxSettings);
            });
        });
     
    };

    var processDeleteOrgMatAjaxResponse = function(data) {
        log('Ajax response', data);

        if (data.status === "OK") {
            //Delete row with error
            deleteOrgMatRow(data);
        } else {
            displayDeleteOrgMatError(data);
        }
    };

    var deleteOrgMatRow = function(data) {
        var orgmatid = data.orgmatid;

        rows = pub.oTable.fnGetData();

        for (i = 0; i < rows.length; i++) {
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

