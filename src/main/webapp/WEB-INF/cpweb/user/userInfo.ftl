[#ftl /]
<section id="userinfo">
        <div id="itemactions">
            [#include "userActions.html" /]
        </div>
        <div id="usericon">
            [#if userimg??]
            <img src="${userimg}" alt="${user.displayName?xhtml} Profile Image"/>
            [#else]
            <img src="${cocoboxCdn}/cocobox/img/cp/userImage_64x64.png" alt="${user.displayName?xhtml} Profile Image" />
            [/#if]
            <p>User</p><h1>${user.displayName?xhtml}</h1>
        </div>
        <div class="itemdetails">
            <ul>
                <li><span class="label">Email</span><span class="setting clearfix">${user.primaryEmail?xhtml}</span></li>
                <li><span class="label">Language</span><span class="setting clearfix">${locale.getDisplayName(userLocale)?xhtml}</span></li>
            </ul>
        </div>
</section>