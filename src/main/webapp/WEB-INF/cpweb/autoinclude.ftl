<#ftl strip_text="true" />

<#import "/se/dabox/services/dwsfu/common.ftl" as common />
<#import "/se/dabox/services/dwsfu/commonCss.ftl" as commonCss />
<#import "/se/dabox/services/dwsfu/form.ftl" as form />
<#import "/se/dabox/services/dwsfu/mbform.ftl" as mbform />
<#import "/se/dabox/services/dwsfu/ctxMenu.ftl" as ctxMenu />


<#assign portalswitch>
<#include "portalswitch.html">
</#assign>

<#assign cpweb_head>
    <@branding />
    <@learnifierBootstrap />
    
    <link href="${contextPath}/css/cpweb.css?${cycle.application.formattedStartTime.base36String}" rel="stylesheet" type="text/css" media="screen" />

    <link href="${cocoboxCdn}/cocobox/jqueryui-editable/css/jqueryui-editable.css" rel="stylesheet"/>

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


<#macro cpweb_ctxMenu org>

<nav id="createnav">
    <ul>
        <li>
            <p><span>Add</span></p>
            <ul>
                <li id="cm_material"><a href="${helper.urlFor('CpMainModule','newMaterial',[org.id])}"><span>Add Material</span></a></li>
                <li id="cm_project"><a href="${helper.urlFor('CpMainModule','createProjectSelectDesign',[org.id])}"><span>Add Project</span></a></li>
                <li id="cm_user"><a href="${helper.urlFor('CreateUserModule','create',[org.id])}"><span>Add User</span></a></li>
            </ul>
        </li>
    </ul>
</nav>  

</#macro>
