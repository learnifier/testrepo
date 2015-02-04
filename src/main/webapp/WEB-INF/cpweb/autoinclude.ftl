<#ftl strip_text="true" />

<#import "/se/dabox/services/dwsfu/common.ftl" as common />
<#import "/se/dabox/services/dwsfu/commonCss.ftl" as commonCss />
<#import "/se/dabox/services/dwsfu/bsform.ftl" as bsform />
<#import "/se/dabox/services/dwsfu/modal.ftl" as modal />


<#assign cpweb_head>
    <@learnifierBootstrap />
    <link rel="stylesheet" href="${cocoboxCdn}/cocobox/ccss/clientportalweb/clientportal.css" />
    <@branding />    
</#assign>


<#assign orgName>
    ${(org.displayName)!''}
</#assign>

<#assign cpweb_foot>
    <#if dwsProductionMode>
        <script src="${contextPath}/js/cpweb-all.js?${cycle.application.formattedStartTime.base36String}"></script>
    <#else>
        <!-- Start time: ${cycle.application.formattedStartTime.base36String} -->
        <#assign inp><#include "/js/cpweb-all.js.include" /></#assign>
        ${inp?replace("src='","src='"+contextPath+"/")}

    </#if>
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
