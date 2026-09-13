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
            <#assign hasEntryWrapper = singlePageSidebar && isTopLevel>
            <#assign isCollapsible = hasEntryWrapper && hasChildren>
            <#assign childrenId = "nav-children-" + itemKey>

            <li class="nav-item<#if (item.group)!false> has-children</#if><#if isCollapsible> is-collapsible</#if>">
                <#if isCollapsible>
                    <div class="nav-entry">
                        <button type="button"
                                class="nav-toggle"
                                aria-expanded="true"
                                aria-controls="${childrenId}"
                                aria-label="Toggle subsections for ${(item.plainTitle)!item.title?html}">
                            <span class="nav-toggle-chevron" aria-hidden="true"></span>
                        </button>
                        <#if item.page?has_content>
                            <a href="${basePath}${item.route}"
                               class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                               <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${((item.plainChapterTitle)!item.plainTitle)!item.title?html}"</#if>>
                                ${(item.plainTitle)!item.title?html}
                            </a>
                        <#else>
                            <span class="nav-group">${(item.plainTitle)!item.title?html}</span>
                        </#if>
                    </div>
                <#elseif hasEntryWrapper && item.page?has_content>
                    <div class="nav-entry">
                        <button type="button"
                                class="nav-toggle nav-toggle--leaf"
                                aria-hidden="true"
                                tabindex="-1">
                            <span class="nav-toggle-chevron nav-toggle-chevron--leaf" aria-hidden="true"></span>
                        </button>
                        <a href="${basePath}${item.route}"
                           class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                           <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${((item.plainChapterTitle)!item.plainTitle)!item.title?html}"</#if>>
                            ${(item.plainTitle)!item.title?html}
                        </a>
                    </div>
                <#elseif item.page?has_content>
                    <a href="${basePath}${item.route}"
                       class="nav-link<#if (currentPagePath!'__none__') == item.page> active</#if><#if (item.chapter)!false> chapter-link</#if>"
                       <#if item.chapterId?? && item.chapterId?has_content>data-chapter-id="${item.chapterId}" data-chapter-title="${((item.plainChapterTitle)!item.plainTitle)!item.title?html}"</#if>>
                        ${(item.plainTitle)!item.title?html}
                    </a>
                <#else>
                    <span class="nav-group">${(item.plainTitle)!item.title?html}</span>
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
