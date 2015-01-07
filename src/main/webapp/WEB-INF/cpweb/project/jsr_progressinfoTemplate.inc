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
        
        <td class="progresscol">    
            {{if completed}}    
                <div class="progress"><div class="bar" style="width:150px;"><div class="percent">completed</div></div></div>
            {{else}}
                <div class="progress"><div class="percent">to be completed</div></div>            
            {{/if}}
        </td>
        
        <td class="completiondate">
            {{if completedDate}}
                <abbr class="timeago" title="{{>completedDateAgo}}">{{>completedDateStr}}</abbr>
            {{/if}}
        </td>
    </tr>

</script>