/* 
 * (c) Dabox AB 2012 All Rights Reserved
 * 
 * <p>Active: ' + oObj.aData.activeLinks +
                        '</p><p>Inactive: ' + oObj.aData.inactiveLinks+'</p>
 */
var listMaterialsProducts = (function() {
    "use strict";
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

    var tableInit = function() {
        var pTable = $('#listproducts').dataTable({
            "sDom": 'f<"clear">rt<"dataTables_footer clearfix"i>',
            "bPaginate": false,
            "aaSorting": [[1,'asc']],
            "aoColumnDefs": [
            {
                "aTargets": [ 0 ],
                "mDataProp": "type",
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
                "mDataProp": "title",
                "fnRender": function ( oObj ) {
                    var desc = oObj.aData.desc || "";
                    return  '<h1>' + oObj.aData.title + '</h1><p class="lang">'
                    + 'English' + '</p><p class="description">' + desc + '</p>';
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
            },
            "fnInitComplete": function() {
                var nMaterialsProducts = listMaterialsProducts.pTable._('tr').length;
                $('#sm_purchased span').append('<span class="count">' + nMaterialsProducts + '</span>');
            }

        });

        pub.pTable = pTable;
         
    }

    var init = function() {
        pub.listUrl = pub.listPurchasedMatsUrl;
        tableInit();

        $('#listproducts_filter input').attr('placeholder', pub.sLang);
    };

    //Public methods
    pub.init = init;

    return pub;

})();

