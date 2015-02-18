<script id="progressTemplate" type="text/x-jsrender">

    
    <tr>
        <td>
            {{if productThumbnail}}
                <img src="{{>productThumbnail}}" alt="{{>productShortTitle}}"/>
            {{else type=="TEMPORAL"}}
                <img src="${contextPath}/img/temporal_event_64x64.png" alt="{{>productShortTitle}}"/>
            {{else type=="ORGMAT"}}
                <img src="${contextPath}/img/orgmaterial_64x64.png" title="{{>productShortTitle}}" alt="{{>productShortTitle}}"/>
            {{else type=="CRISP"}}
                <img src="${contextPath}/img/crisp_product_64x64.png" title="{{>productShortTitle}}" alt="{{>productShortTitle}}"/>
			{{else type="PRODUCT"}}
				<img src="${contextPath}/img/product_64x64.png" title="{{>productShortTitle}}" alt="{{>productShortTitle}}"/>
            {{/if}}
        </td>

        {{if componentTitle}}
            <td class="title">{{>componentTitle}}</td>
        {{else productTitle}}
            <td class="title">{{>productTitle}}</td>
        {{/if}}
        
        <td class="completiondate">
            {{if completedDate}}
                <abbr class="timeago" title="{{>completedDateAgo}}">{{>completedDateStr}}</abbr>
            {{/if}}
        </td>
        <td class="progresscol">    
            {{if completed}}    
                <div class="progress"><div class="progress-bar progress-bar-success" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%;">Completed</div></div>
            {{else}}
                <div class="progress"><div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="0" style="width: 100%;">To be completed</div></div>
            {{/if}}
        </td>
        
    </tr>

</script>