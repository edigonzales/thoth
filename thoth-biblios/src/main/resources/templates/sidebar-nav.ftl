<#-- sidebar-nav.ftl - Recursive sidebar navigation -->
<#assign singlePageSidebar = (singlePageMode)!false>

<#macro renderNav items level=0 pathPrefix="" listId="" collapsiblePanel=false>
    <ul class="nav-list<#if collapsiblePanel> nav-subtree</#if>"<#if listId?has_content> id="${listId}"</#if><#if collapsiblePanel> data-collapsible-panel="true"</#if>>
        <#list items as item>
            <#if pathPrefix?has_content>
                <#assign itemKey = pathPrefix + "-" + item?index>
            <#else>
                <#assign itemKey = "" + item?index>
            </#if>
            <#assign hasChildren = item.children?has_content && item.children?size gt 0>
            <#assign isTopLevel = level == 0>
            <#assign isCollapsible = singlePageSidebar && isTopLevel && hasChildren>
            <#assign childrenId = "nav-children-" + itemKey>

            <li class="nav-item<#if (item.group)!false> has-children</#if><#if isCollapsible> is-collapsible</#if>">
                <#if isCollapsible>
                    <div class="nav-entry">
                        <button type="button"
                                class="nav-toggle"
                                aria-expanded="true"
                                aria-controls="${childrenId}"
                                aria-label="Toggle subsections for ${item.title?html}">
                            <span class="nav-toggle-chevron" aria-hidden="true"></span>
                        </button>
                        <#if item.page?has_content>
                            <a href="${basePath}${item.route}"
                               class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                               <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${(item.chapterTitle)!item.title}"</#if>>
                                ${item.title}
                            </a>
                        <#else>
                            <span class="nav-group">${item.title}</span>
                        </#if>
                    </div>
                <#elseif item.page?has_content>
                    <a href="${basePath}${item.route}"
                       class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                       <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${(item.chapterTitle)!item.title}"</#if>>
                        ${item.title}
                    </a>
                <#else>
                    <span class="nav-group">${item.title}</span>
                </#if>
                <#if hasChildren>
                    <#if isCollapsible>
                        <@renderNav items=item.children level=level + 1 pathPrefix=itemKey listId=childrenId collapsiblePanel=true/>
                    <#else>
                        <@renderNav items=item.children level=level + 1 pathPrefix=itemKey/>
                    </#if>
                </#if>
            </li>
        </#list>
    </ul>
</#macro>

<#if navigation?has_content && navigation.items?has_content>
    <@renderNav items=navigation.items level=0 pathPrefix=""/>
</#if>
