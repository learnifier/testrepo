<script id="adminlinkTemplate" type="text/x-jsrender">
    <div class="adminlink">
        
        {{if productThumbnail}}
            <img src="{{>productThumbnail}}" alt="{{>productShortTitle}}" />
        {{/if}}
        
        {{if adminLink}}
            <a href="{{>adminLink}}">
                {{if productShortTitle}}
                    <span>{{>productShortTitle}}</span>
                {{else productTitle}}
                    <span>{{>productTitle}}</span>
                {{/if}}
            </a>
        {{/if}}
    </div>
</script>
