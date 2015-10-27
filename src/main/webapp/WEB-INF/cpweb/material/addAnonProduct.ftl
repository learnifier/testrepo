[#ftl strip_text="true" /]

[#macro addButton]
[@portalSecurity.permissionBlock permission="CP_CREATE_ORGMAT"]
<div class="ui-helper-hidden" data-anoncreateprodelement="show">
    <div class="btn btn-primary" id="addmatbtn"><span class="glyphicon glyphicon-plus-sign"></span> Add Material</div>
</div>
[/@portalSecurity.permissionBlock]
[/#macro]

[#macro addScript]
<script type="text/javascript">
    require(["${dwsrt.config['apiweb.baseurl']}js/createanonproduct.js"], function() {
        //log("anonprodcreate loaded");
    });

    $("#addmatbtn").click(function() {
        CcbAnonymousProductUpload.open(function(pid) {
            if (pid) {
                //everything fine, reload page to make product show up in the list
                window.location.href=window.location.href;
            } else {
                //do nothing
            }
        }, {
            scope: "O${org.id?c}"
        });

    });
    
    function editItem(row){
      
     var productId = $(row).data("id");

     CcbAnonymousProductUpload.open(function(pid) {
            if (pid) {
                //everything fine, reload page to make product show up in the list
                window.location.href=window.location.href;
            } else {
                //do nothing
            }
        }, {
            scope: "O${org.id?c}",
            productId: productId
        });
        return false;
    }

</script>
[/#macro]