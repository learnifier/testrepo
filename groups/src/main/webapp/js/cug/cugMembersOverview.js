/* 
 * (c) Dabox AB 2015 All Rights Reserved
 */
define(['cocobox-icheck'], function() {
    "use strict";
    var exports = {};

    
    function runiCheck(cell) {
      $('input', cell).icheck({
        checkboxClass: 'icheckbox_square-red',
        radioClass: 'iradio_square-red'
      });
    };

   $(document).ready(function() {
        require(['dataTables-bootstrap', 'dataTables-responsive'], function() {
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
                        return  '<input type="checkbox" class="rowcb" onchange="return cpweb.rowcbChange(this)" data-rowid="' + data + '"/>';
                    },
                    "createdCell": function (td, cellData, rowData, row, col) {
                        runiCheck(td);
                    }
                },
                {
                    "targets": [ 1 ],
                    "className": "block-link",
                    "data" : function(row, type, set) {
                        console.log("---", row, type, set);
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
                "emptyTable": "<span class='emptytable'>No groups have been added.</a></span>",
                "loadingRecords": "<p>Loading groups...</p><img src='" + spinnerUrl + "' />"
            }
        });
        });
                
    } );

    return exports;
});
