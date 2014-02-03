[#ftl /]
<script id="detailsTable" type="text/x-jsrender">
    <div id="details">

        <div class="progressdetails">
            <h2>Progress<span>{{:completedCids}} of {{:totalnumberOfCidsWithProgress}} activities completed</span></h2>
            <table class=" list">
                {{:renderedProgress}}
            </table>
        </div>
        <div class="reportdetails">
            <h2>Reports</h2>
            {{:renderedReports}}
        </div>
        
        [#-- 
        <div class="adminlinks">
            <h2>Adminlinks</h2>
            {{:renderedAdminlinks}}
        </div> --]
		
        [#if prj.subtype != 'idproject'] [#-- Do not show invites section on idprojects --]
        <div class="invites">
            <h2>Invites</h2>
            <table style="width: 100%;">
            {{:renderedInvites}}
            </table>
        </div>
        [/#if]
    </div>
</script>
