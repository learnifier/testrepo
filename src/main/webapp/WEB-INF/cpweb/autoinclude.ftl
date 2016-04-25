<#ftl strip_text="true" />

<#import "/se/dabox/services/dwsfu/common.ftl" as common />
<#import "/se/dabox/services/dwsfu/commonCss.ftl" as commonCss />
<#import "/se/dabox/services/dwsfu/bsform.ftl" as bsform />
<#import "/se/dabox/services/dwsfu/modal.ftl" as modal />
<#import "/se/dabox/services/dwsfu/respondjs.ftl" as respondJs />


<#assign cpweb_head>
    <@learnifierBootstrap />
    <link rel="stylesheet" href="${cocoboxCdn}/cocobox/ccss/clientportalweb/clientportal.css" />
    <@branding />
    <@respondJs.respondJsWithCdnSupport />
</#assign>


<#assign orgName>
    ${(org.displayName)!''}
    <#if (org.clientNo)??>
        <#if org.clientNo != "">
        - (${org.clientNo!""})
        </#if>
    </#if>
</#assign>

<#assign cpweb_foot>
    <script>var cpweb = cpweb || {};</script>
</#assign>

<#macro localeLanguage locale>${locale.getDisplayLanguage(resp.locale)?cap_first}</#macro>


<#macro userNameAndEmail userId>
    <#if infoHelper.getMiniUserInfo(userId)??>
        <#local userInfo =  infoHelper.getMiniUserInfo(userId) />
        <span data-toggle="tooltip" data-placement="top" title="${userInfo.displayName?xml} &lt;${userInfo.email?xml!''}&gt;">${userInfo.displayName?xml}</span>
    <#else>
        &nbsp;
    </#if>
</#macro>
