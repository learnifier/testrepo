<script id="reportinfoTemplate" type="text/x-jsrender">
    <div class="reportinfo">
        {{if empty}}
            <span>No reports available for this participant</span>
        {{else}}
        
  
                {{if filetype}}
                    <div class="{{>filetype}}"> <!-- typically pdf -->
                {{else}}
                    <div class="reportgeneral">
                {{/if}}
                    {{if isAdminReport}}
                        <a href="{{>adminReportLink}}" target="_blank">{{>title}}</a>

                        {{if isUserReport}}
                            <span class="uservisibility">visible to user</span>
                        {{/if}}

                        {{if createdStr}}
                            <p class="created date"><span>generated </span>
                                <abbr class="timeago" title="{{>createdStr}}">{{>createdStr}}</abbr>
                            </p>
                        {{/if}}
                    {{/if}}
                    </div>
        {{/if}}
    </div>
</script>
