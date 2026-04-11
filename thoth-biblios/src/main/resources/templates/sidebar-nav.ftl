<#-- sidebar-nav.ftl - Recursive sidebar navigation -->
<#macro renderNav items>
    <ul class="nav-list">
        <#list items as item>
            <li class="nav-item<#if (item.group)!false> has-children</#if>">
                <#if item.page?has_content>
                    <a href="${basePath}${item.route}"
                       class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                       <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${(item.chapterTitle)!item.title}"</#if>>
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
