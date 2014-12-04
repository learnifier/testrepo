[#ftl strip_text="true" /]

[#macro addButton]
[@portalSecurity.permissionBlock permission="CP_CREATE_ORGMAT"]
<div class="ui-helper-hidden" data-anoncreateprodelement="show">
                <div class="btn btn-add navBackgroundColor" id="addmatbtn">Add Material</div>
</div>
[/@portalSecurity.permissionBlock]
[/#macro]

[#macro addScript]
<script>
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
</script>
[/#macro]