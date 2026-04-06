<#-- page.ftl - Documentation content page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle basePath=(basePath)!"" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] versionSwitcher=(versionSwitcher)![]
    currentComponentId=currentComponentId currentVersionStr=(currentVersion.version)!"" currentVersion=(currentVersion)!{} currentPagePath=currentPagePath
    navigation=(navigation)!{} breadcrumbs=(breadcrumbs)![]
    prevPage=(prevPage)!{} nextPage=(nextPage)!{}>
    <article class="doc-page">
        <h1>${page.title}</h1>
        <div class="doc-content">
            ${page.html}
        </div>
    </article>
</@layout.layout>
