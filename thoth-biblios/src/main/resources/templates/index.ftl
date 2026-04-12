<#-- index.ftl - Global start page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" basePath=(basePath)!"" locale=(locale)!"en"
    docSwitcher=(catalog.components)![] currentComponentId=""
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe">
    <div class="home">
        <h1>${siteTitle}</h1>
        <#if siteDescription??>
            <p class="description">${siteDescription}</p>
        </#if>

        <div class="components-grid">
            <#list catalog.components as component>
                <a href="${basePath}/${component.id}/${component.defaultVersion}/" class="component-card">
                    <h2>${component.displayName}</h2>
                    <p class="versions">
                        <#list component.versions as version>
                            <span class="version-tag">${version.displayVersion}</span>
                        </#list>
                    </p>
                </a>
            </#list>
        </div>
    </div>
</@layout.layout>
