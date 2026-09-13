<#-- index.ftl - Global start page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" basePath=(basePath)!"." siteRootHref=(siteRootHref)!"./" searchPageHref=(searchPageHref)!"./search/" searchIndexUrl=(searchIndexUrl)!"./search-index.json" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] currentComponentId=""
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe"
    syntaxHighlightingEnabled=(syntaxHighlightingEnabled)!true
    prismCustomComponentUrls=(prismCustomComponentUrls)![]>
    <div class="home">
        <h1>${siteTitle}</h1>
        <#if siteDescription??>
            <p class="description">${siteDescription}</p>
        </#if>

        <#list docSwitcher as entry>
            <#if entry.documents??>
                <section class="component-group" aria-labelledby="component-group-${entry?index}">
                    <h2 id="component-group-${entry?index}" class="component-group-title">${entry.title?html}</h2>
                    <@cards documents=entry.documents grouped=true />
                </section>
            <#else>
                <#-- With no groups configured, render the original flat grid once. -->
                <#if entry?is_first><@cards documents=docSwitcher grouped=false /></#if>
            </#if>
        </#list>
    </div>
</@layout.layout>


<#macro cards documents grouped>
    <div class="components-grid">
        <#list documents as component>
            <article class="component-card"<#if component.cardBackgroundColor??> style="background-color: ${component.cardBackgroundColor?html};"</#if>>
                <a href="${basePath}/${component.id}/${component.defaultVersion}/"
                   class="component-card-default-link"
                   aria-label="Open ${component.displayName?html} (${component.defaultVersion?html})"></a>
                <#if grouped>
                    <h3>${component.displayName?html}</h3>
                <#else>
                    <h2>${component.displayName?html}</h2>
                </#if>
                <p class="versions">
                    <#list component.versions as version>
                        <a href="${basePath}/${component.id}/${version.version}/" class="version-tag">${version.displayVersion?html}</a>
                    </#list>
                </p>
            </article>
        </#list>
    </div>
</#macro>
