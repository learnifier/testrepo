<#ftl strip_text="true" />

<#import "/se/dabox/services/dwsfu/common.ftl" as common />
<#import "/se/dabox/services/dwsfu/commonCss.ftl" as commonCss />
<#import "/se/dabox/services/dwsfu/mbform.ftl" as mbform />
<#import "/se/dabox/services/dwsfu/bsform.ftl" as bsform />


<#assign portalswitch>
<#include "portalswitch.html">
</#assign>

<#assign cpweb_head>
    <@branding />
    <@learnifierBootstrap />
    
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
