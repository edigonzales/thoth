<#-- page.ftl - Documentation content page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" basePath=(basePath)!"" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] versionSwitcher=(versionSwitcher)![]
    currentComponentId=currentComponentId currentVersionStr=(currentVersion.version)!"" currentVersion=(currentVersion)!{} currentPagePath=currentPagePath
    navigation=(navigation)!{} breadcrumbs=(breadcrumbs)![]
    prevPage=(prevPage)!{} nextPage=(nextPage)!{}
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe"
    singlePageMode=(singlePageMode)!false
    chapterBreadcrumbEnabled=(chapterBreadcrumbEnabled)!false
    initialChapterId=(initialChapterId)!"">
    <article class="doc-page">
        <h1>${page.title}</h1>
        <#if (showEditLink)!false && (editUrl?? || sourceUrl??)>
        <div class="page-actions">
            <#if editUrl??>
                <a href="${editUrl}" class="page-action" target="_blank" rel="noopener">Edit this page</a>
            </#if>
            <#if sourceUrl??>
                <a href="${sourceUrl}" class="page-action" target="_blank" rel="noopener">View source</a>
            </#if>
        </div>
        </#if>
        <div class="doc-content">
            ${page.html}
        </div>
    </article>
</@layout.layout>
