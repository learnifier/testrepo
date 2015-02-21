[#ftl /]
<script id="detailsTable" type="text/x-jsrender">
    <div id="details">

        {{if expirationStr}}
        <div class="expirationdetails">
            {{if expired}}
                <div class="expired"><span>This participation expired </span><abbr class="timeago" title="{{:expirationAgo}}">{{:expirationAgo}}</abbr><span class="date"> ({{:expirationStr}})</span></div>
            {{else}}
                <div class="futureexpiration"><span>This participation will expire </span><abbr class="timeago" title="{{:expirationAgo}}">{{:expirationAgo}}</abbr><span class="date"> ({{:expirationStr}})</span></div>
            {{/if}}
        </div>
        {{/if}}
        <div class="progressdetails">
            <h3>Progress<span class="pull-right">{{:completedCids}} of {{:totalnumberOfCidsWithProgress}} activities completed</span></h3>
            <table class="table table-hover table-striped ccb-table dt-responsive" width="100%">
                {{:renderedProgress}}
            </table>
        </div>
        <div class="reportdetails">
            <h3>Reports</h3>
            {{:renderedReports}}
        </div>
        
        [#-- 
        <div class="adminlinks">
            <h3>Adminlinks</h3>
            {{:renderedAdminlinks}}
        </div> --]
		
        [#if prj.subtype != 'idproject'] [#-- Do not show invites section on idprojects --]
        <div class="invites">
            <h3>Invites</h3>
            <table style="width: 100%;">
            {{:renderedInvites}}
            </table>
        </div>
        [/#if]
    </div>
</script>
