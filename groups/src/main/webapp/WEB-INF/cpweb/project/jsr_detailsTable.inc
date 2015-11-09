[#ftl /]
<script id="detailsTable" type="text/x-jsrender">
    <div id="details">

        {{if expirationStr}}
        <div class="expirationdetails">
            {{if expired}}
                <div class="expired"><span>Access ended </span><abbr class="timeago" title="{{:expirationAgo}}" data-toggle="tooltip" data-placement="top">{{:expirationAgo}}</abbr><span class="date"> on {{:expirationStr}}</span></div>
            {{else}}
                <div class="futureexpiration"><span>Access will end </span><abbr class="timeago" title="{{:expirationAgo}}" data-toggle="tooltip" data-placement="top">{{:expirationAgo}}</abbr><span class="date"> on {{:expirationStr}}</span></div>
            {{/if}}
        </div>
        {{/if}}

        <div class="progressdetails">
            <h4><span class="section-title">Progress</span><span class="showDrop"><span class="showOverview">Overview</span> | <span class="showDetails">Show details</span></span><span class="pull-right">{{:activitiesCompleted}} of {{:activityCount}} completed</span></h4>
            {{:renderedProgress}}
        </div>


        {{if reportsAvailable}} [#-- Only show if there are any reports available--]
            <div class="reportdetails">
                <h4><span class="section-title">Reports</span></h4>
                {{:renderedReports}}
            </div>
         {{/if}}

        [#--
            <div class="adminlinks">
                <h4><span class="section-title">Adminlinks</span></h4>
                {{:renderedAdminlinks}}
            </div>
        --]

        [#if prj.subtype != 'idproject'] [#-- Do not show invites section on idprojects --]
            <div class="invites">
                <h4><span class="section-title">Invites</span></h4>
                <table style="width: 100%;">
                {{:renderedInvites}}
                </table>
            </div>
        [/#if]

    </div>
</script>
