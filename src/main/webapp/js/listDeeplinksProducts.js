/* 
 * (c) Dabox AB 2012 All Rights Reserved
 * 
 * <p>Active: ' + oObj.aData.activeLinks +
                        '</p><p>Inactive: ' + oObj.aData.inactiveLinks+'</p>
 */
var listDeeplinksProducts = (function() {
    //Shared variables
    var pub = {
        listOrgMatsUrl: "",
        listPurchasedMatsUrl: "",
        listUrl: "",
        listLinksUrl: "",
        listLinksHistoryUrl: "",
        newOrgMatUrl: "",
        deleteOrgMatLinkUrl: "",
        toggleActiveUrl: "",
        toggleActiveToUrl: "",
        toggleAutoaddUrl: "",
        contextPath: "",
        pTable: null
    };

    var creditHistory = function (linkId){

        $('#quantitydetails' + linkId).slideToggle('slow');

        var ex = $('#linkcredithistory' + linkId);
        if ( ! $.fn.DataTable.fnIsDataTable($( ex ).get(0)) ) {
            
 
            var cTable = $(ex).dataTable({
                "sDom": 'rt',
                "bPaginate": false,
                "aaSorting": [[2,'desc']],
                "aoColumnDefs": [
                {
                    "aTargets": [ 0 ],
                    "mData": null,
                    "fnRender": function ( oObj ) {
                        return oObj.aData.amount;
                    }
                },
                {
                    "aTargets": [ 1 ],
                    "mData": null,
                    "sClass": "control activelinks",
                    "fnRender": function ( oObj ) {
                        if (oObj.aData.createdBy === '') {
                            return "Auto-assigned";
                        } else {
                            return oObj.aData.createdBy;
                        }
                    }
                },
                {
                    "aTargets": [ 2 ],
                    "mData": "created",
                    "fnRender": function ( oObj ) {
                        return oObj.aData.createdStr;
                    }
                },
                {
                    "aTargets": [ 3 ],
                    "mData": null,
                    "bSortable": false,
                    "sClass": "",
                    "fnRender": function ( oObj ) {
                        return renderDeleteTokenColumn(oObj, linkId);
                    }
                }                
                ],

                "sAjaxSource": pub.listLinksHistoryUrl + '/' + linkId,
                "oLanguage": {
                    "sEmptyTable": "<span class='emptytable'>This link has no credit history</span>",
                    "sLoadingRecords": "<p>Loading credit history...</p><img src='../../img/spinner-threedots.gif' />"
                }
            });
            
        } else {
            $('#linkcredithistory' + linkId).dataTable().fnDestroy();
        }
    };
    
    var deleteToken = function (deleteLink, linkId, amount) {
        cocobox.confirmationDialog("Delete credits", "Do you want to delete these credits for this link?", function() { 
            cocobox.ajaxPost(deleteLink, {
                success: function() {
                    // Calling function twice. First to destroy and then to rebuild
                    creditHistory(linkId);
                    creditHistory(linkId);
                    var totId = "#assigned"+linkId;
                    //TODO: This is not correct. We need the right amount to this method
                    $(totId).text(parseInt($(totId).text(),10)-amount);
                }
            });
        });
        return false;
    };
    

    var tableInit = function() {
        var pTable = $('#listproducts').dataTable({
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
                    if ( oObj.aData.allowDeeplink ) {
                        var active = oObj.aData.activeLinks;
                        var activestate = 'OFF';
                        var activeclass = 'off';
                        if (active > 0) {
                            activestate = 'ON';
                            activeclass = 'on';
                        }

                        var total = oObj.aData.activeLinks+oObj.aData.inactiveLinks;
                        if (total == 0) {
                            total = 1;
                        }

                        var activeSpan = "<span id='opal"+oObj.aData.opid+"'>"+oObj.aData.activeLinks+"</span>";
                        var totalSpan = "<span id='opat"+oObj.aData.opid+"'>"+total+"</span>";
                        var linkSpan = "<span id='opl"+oObj.aData.opid+"'>"+oObj.aData.linkCredits+"</span>";

                        return  '<button>Get Link</button><div><p class="title">' +
                        'Active Links</p><p class="value">'+activeSpan+' of '+totalSpan+'</p>' +
                        '<p class="title">Assigned credits</p><p class="value">'+linkSpan+'</p></div>';
                    } else {
                        return  '';                        
                    }
                }
            }
            ],

            "sAjaxSource": pub.listUrl,
            "sPaginationType": "full_numbers",
            "oLanguage": {
                "sSearch": "",
                "sZeroRecords": "No materials matches your query",
                "sEmptyTable": "<span class='emptytable'>No purchased products</span>",
                "sLoadingRecords": "<p>Loading materials...</p><img src='../../img/spinner-threedots.gif' />"
            }
        });

        pub.pTable = pTable;
        //Some code uses this instead
        window.pTable = pTable;
         
    };

    var init = function() {
        pub.listUrl = pub.listPurchasedMatsUrl;
        tableInit();

        $('#listproducts_filter input').attr('placeholder', pub.sLang);


        $('#tab_products').on('click','.addlinkbutton', function() {
            var td = $(this).closest('td.details');
            var rowdata = pTable.fnGetData($(td).parent().prev().get(0));

            $(".loadspinner", td).toggleClass('hidden',false);

            cocobox.ajaxPost(pub.newOrgMatUrl, {
                data: {
                    "orgmatid": rowdata.opid
                },
                success: function(response) {
                    lmProcessResults($(td).parent().get(0), response, false);
                    $('div.linkdetails', rowdata).slideDown('fast', function () {
                        $('html, body').animate({
                            scrollTop: $(".linkdetails").offset().top-80
                        }, 500);
                    });

                    var totId = "#opat"+response.aaData[0].opid;
                    $(totId).text(parseInt($(totId).text(),10)+1);
                }
            });
        });

        $('#tab_products').on('click','.deleteplinkbutton', function(event) {
            event.preventDefault();

            var form = $(this).closest('form');

            var id = $(form).data('linkid');

            $.post(pub.deleteOrgMatLinkUrl, {
                "prodlink": id
            }, function(response) {
                if (response.error != null) {
                    cocobox.errorDialog("Unable to delete link", response.error);
                } else if (response.status && response.status == "OK") {
                    $("#linkdetailsdiv"+id).slideUp(1000, this.remove);
                    var totId = "#opat"+response.opid;
                    $(totId).text(parseInt($(totId).text(),10)-1);
                } else {
                    cocobox.dialog("Unable to delete link", "Unknown error when deleting link");
                }
            });

        });

        var anOpen = [];
        $('#tab_products').on('click','td.control', function () {
            var nTr = this.parentNode;
            var i = $.inArray(nTr, anOpen);
            if (i === -1) {
                //Row to be expanded
                var rowdata = pTable.fnGetData($(this).closest('tr').get(0));

                var nDetailsRow = pTable.fnOpen(nTr, fnFormatProdDetails(pTable, nTr, 1), 'details');
                
                $.post(pub.listLinksUrl, {
                    "opid": rowdata.opid
                }, function(response) {
                    log('Render response');
                    lmProcessResults(nDetailsRow, response, true);
                });

                $(nTr).addClass("showdetails");
                $('button', nTr).removeClass("getlink");
                $('button', nTr).addClass("hide");
                $('button', nTr).html("Hide");

                $('div.linkdetails', nDetailsRow).slideDown('fast', function () {
                    $('html, body').animate({
                        scrollTop: $(".linkdetails").offset().top-80
                    }, 500);
                });
                anOpen.push(nTr);
            }
            else {
                //Row to be collapsed
                $(this).parent('tr').removeClass("showdetails");
                $('button', this).removeClass("hide");
                $('button', this).addClass("getlink");
                $('button', this).html("Get Link");                
                $('div:first', $(nTr).next()[0]).slideUp(function () {
                    pTable.fnClose(nTr);
                    anOpen.splice(i, 1);
                });
                $('button', this).removeClass("active");
            }
        });
    };

    var lmProcessResults = function (nDetailsRow, response, replace) {

        var target = $("td div:first", nDetailsRow).first();

        if (replace) {
            $(".linkdetails", target).remove();
            target.prepend($("#prodlinkinfo").render(response.aaData));
        } else {
            $(".linkdetails", target).last().after($("#prodlinkinfo").render(response.aaData));
        }

        $.each(response.aaData, function(rowIndex, rowData) {           
            var lss = $( "#linkstatussetting"+rowData.linkid);
            lss.buttonset();
            log($('#linktitle' + rowData.linkid));

            // ACTIVATE JEDITABLE FOR THE LINK TITLES
            $('#linktitle' + rowData.linkid + ' .edit').click( function() {
                $('#linktitle' + rowData.linkid + ' .text').click();
            });
            $('#linktitle' + rowData.linkid + ' .text').editable(function(value) {
                updateProductLinkTitle(rowData.linkid, value);
                return value;
            },
            {
                width: '300',
                onblur : 'ignore',
                cancel    : 'Cancel',
                submit    : 'Save',
                indicator : 'Saving...',
                tooltip   : 'Click to edit...',
                callback  : function(){
                    $('#linktitle' + rowData.linkid + ' .edit').show();
                },
                onreset: function(){
                    $('#linktitle' + rowData.linkid + ' .edit').show();
                }
     }
            ).click(function(){
                    $('#linktitle' + rowData.linkid + ' .edit').hide(); 
            });
            
            var afterToggleFn = function(response) {
                var opalId = "#opal"+response.opid;
                var diff = response.active ? 1 : -1;
                $(opalId).text(parseInt($(opalId).text(),10)+diff);
            };

            $('input:radio', lss).change(function() {
                var newState = this.value;
                var linkid = $(this).closest("*[data-linkid]").data("linkid");
                $.post(pub.toggleActiveUrl, {
                    "linkid": linkid,
                    "active": newState
                }, afterToggleFn);
            });
            var lass = $( "#autosetting"+rowData.linkid);
            lass.buttonset();
            $('input:radio', lass).change(function() {
                var newState = this.value;
                var linkid = $(this).closest("*[data-linkid]").data("linkid");
                $.post(pub.toggleAutoaddUrl, {
                    "linkid": linkid,
                    "autoadd": newState
                }, afterToggleFn);
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
                $( '.linkfooter.tooltip' ).tooltip({
                    position: {
                        my: "left top",
                        at: "left bottom+10"
                    }
                });
        });

        $(".loadspinner", target).toggleClass('hidden',true);

        $('.linkfooter', target).show();
    };


    //Public methods
    pub.init = init;
    pub.creditHistory = creditHistory; 
    pub.deleteToken = deleteToken; 

    return pub;

})();

