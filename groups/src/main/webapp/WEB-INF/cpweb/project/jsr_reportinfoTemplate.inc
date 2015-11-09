<script id="reportinfoTemplate" type="text/x-jsrender">
    <div class="reportinfo">
        {{if empty}}
            <span>No reports available for this participant</span>
        {{else}}
        {{if isAdminReport}}
            <div>
                <a href="{{>adminReportLink}}" target="_blank">
                    <span class="pe-7s-news-paper pe-va pe-lg primaryColor"></span>
                    <span>{{>title}}</span>
                    {{if isUserReport}}
                        <span class="uservisibility">visible to user</span>
                    {{/if}}
                    {{if createdStr}}
                        <p class="created date"><span>generated </span>
                            <abbr class="timeago" title="{{>createdStr}}">{{>createdStr}}</abbr>
                        </p>
                    {{/if}}
                </a>
            </div>
        {{/if}}
        {{/if}}
    </div>
</script>
