[#ftl /]
<script id="invitesTemplate" type="text/x-jsrender">
    {{if (#index%2 == 0)}}
    <tr class="even">
    {{else}}
    <tr class="odd">
    {{/if}}
            <td class="productname"><a href="{{>link}}">{{>name}}</a></td>

            <td class="invited">
            {{if invited}}
                    <span> {{>invited}} invited</span>
            {{else}}	
                    <span> No one invited</span>
            {{/if}}
            </td>

            <td class="completed">
            {{if completed}}
                    <span> {{>completed}} completed</span>
            {{else}}
                    <span> No completed</span>
            {{/if}}

            </td>
    </tr>

</script>
