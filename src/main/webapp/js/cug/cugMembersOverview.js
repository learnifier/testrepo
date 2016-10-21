/* 
 * (c) Dabox AB 2015 All Rights Reserved
 */
define(['cocobox-icheck', 'es6-shim', 'messenger'], function() {
    "use strict";
    var exports = {}, settings;


    function runiCheck(cell) {
        $('input', cell).icheck({
            checkboxClass: 'icheckbox_square-red',
            radioClass: 'iradio_square-red'
        });
    }

    function rowcbChange(checkbox) {
        var pForm = $(checkbox).parents('form').first();

        if (pForm == null) {
            log('No form found');
            return false;
        }

        var hiddenString = $(pForm).find("input[name=__ids]").val();

        var val = String($(checkbox).data('rowid'));

        var allValues = hiddenString == "" ? [] : hiddenString.split(',');

        var checked = $(checkbox).prop('checked');

        if (checked) {
            if (jQuery.inArray(val, allValues) == -1) {
                allValues.push(val);
            }
        } else {
            //Remove state
            var index = jQuery.inArray(val, allValues);
            if (index != -1) {
                allValues.splice(index,1);
            }
        }

        hiddenString = allValues.join(',');

        $(pForm).find("input[name=__ids]").val(hiddenString);

        console.log('Setting selected values to ',hiddenString);

        return true;
    }

    exports.init = function(options) {
        settings = $.extend({
            removeMemberUrl: undefined
        }, options || {});

        $(document).ready(function() {
            require(['dataTables-bootstrap'], function() {
                var oTable = $('#listcugs').dataTable({
                    "dom": '<"row"<"col-sm-6"f><"col-sm-6">><"row"<"col-sm-12"rt>><"row"<"col-sm-6"i><"col-sm-6"p>>',
                    "order": [[0,'asc']],
                    "initComplete": function() {
                        $('#listcugs_filter input').attr('placeholder', 'Search  Groups');
                    },
                    "columnDefs": [
                        {
                            "targets": [0],
                            "data": "uid",
                            "orderable": false,
                            "width": "24px",
                            "render": function(data) {
                                return  '<input type="checkbox" class="rowcb" data-rowid="' + data + '"/>';
                            },
                            "createdCell": function (td, cellData, rowData, row, col) {
                                runiCheck(td);
                            }
                        },
                        {
                            "targets": [ 1 ],
                            "className": "block-link",
                            "data" : function(row, type, set) {
                                if (!row.nameDisplay) {
                                    var name = (row.name && row.name.length > 0) ? row.name : "(Name not set yet)";
                                    row.nameDisplay = '<a href="' + row.link + '">' +  name +'</a> ';
                                }

                                if (type === 'display') {
                                    return row.nameDisplay;
                                } else if (type === 'filter') {
                                    return row.name;
                                } else if (type === 'sort') {
                                    return row.name;
                                } else {
                                    //Anything else and raw row
                                    return row.name;
                                }
                            }
                        },
                        {
                            "targets": [ 2 ],
                            "data": "email"
                        }
                    ],
                    "ajax": listMembersAjaxSource,
                    "pageLength": 25,
                    "pagingType": "full_numbers",
                    "deferRender": true,
                    "language": {
                        "search": "",
                        "zeroRecords": "No group matches your query",
                        "emptyTable": "<span class='emptytable'>No memembers has been added.</a></span>",
                        "loadingRecords": "<p>Loading groups...</p><img src='" + spinnerUrl + "' />"
                    }
                });
            });
            $(document).on('change', 'input.rowcb', function(){
                rowcbChange(this);
            });
            // onclick="return cpweb.rosterDelete(this, $('#cugmembersform'), 'removeMembers', '${ctext("cpweb.cug.members.remove.title")?js_string}', '${ctext("cpweb.cug.members.remove.text")?js_string}');
            $("#remove-member-button").click(function(){
                var ids = $("input[name=__ids]").val();
                if(ids) {
                    var idArray = ids.split(",");
                    if(idArray.length > 0) {
                        require(['dabox-common'], function() {
                            cocobox.confirmationDialog('Remove member from group',
                                'Are you sure you want to remove ' + idArray.length + ' members from the group?',
                                function() {
                                    $.post(settings.removeMemberUrl, {id: idArray}).done(function(data){
                                        if(data.status == "ok") {
                                            window.location = window.location; // Error message will be generated from webaction for now
                                        } else {
                                            CCBMessengerError("Error when removing users: ", data.message);
                                        }
                                    }).fail(function(jqXHR, textStatus, errorThrown){
                                        CCBMessengerError("Error when removing users: ", errorThrown);
                                    });
                                });
                        });
                    }
                }
                return false;
            });
        });
    };
    return exports;
});
