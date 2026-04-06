<#-- sidebar-nav.ftl - Recursive sidebar navigation -->
<#macro renderNav items>
    <ul class="nav-list">
        <#list items as item>
            <li class="nav-item<#if item.group?has_content> has-children</#if>">
                <#if item.page?has_content>
                    <a href="${basePath}/${currentComponentId}/${currentVersionStr}/${item.page?replace('.adoc', '')}/"
                       class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if>">
                        ${item.title}
                    </a>
                <#else>
                    <span class="nav-group">${item.title}</span>
                </#if>
                <#if item.children?has_content && item.children?size gt 0>
                    <@renderNav item.children/>
                </#if>
            </li>
        </#list>
    </ul>
</#macro>

<#if navigation?has_content && navigation.items?has_content>
    <@renderNav navigation.items/>
</#if>
