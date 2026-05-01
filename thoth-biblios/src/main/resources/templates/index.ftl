<#-- index.ftl - Global start page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" basePath=(basePath)!"." siteRootHref=(siteRootHref)!"./" searchPageHref=(searchPageHref)!"./search/" searchIndexUrl=(searchIndexUrl)!"./search-index.json" locale=(locale)!"en"
    docSwitcher=(catalog.components)![] currentComponentId=""
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe"
    syntaxHighlightingEnabled=(syntaxHighlightingEnabled)!true
    prismCustomComponentUrls=(prismCustomComponentUrls)![]>
    <div class="home">
        <h1>${siteTitle}</h1>
        <#if siteDescription??>
            <p class="description">${siteDescription}</p>
        </#if>

        <div class="components-grid">
            <#list catalog.components as component>
                <article class="component-card">
                    <a href="${basePath}/${component.id}/${component.defaultVersion}/"
                       class="component-card-default-link"
                       aria-label="Open ${component.displayName} (${component.defaultVersion})"></a>
                    <h2>${component.displayName}</h2>
                    <p class="versions">
                        <#list component.versions as version>
                            <a href="${basePath}/${component.id}/${version.version}/" class="version-tag">${version.displayVersion}</a>
                        </#list>
                    </p>
                </article>
            </#list>
        </div>
    </div>
</@layout.layout>
