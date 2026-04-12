<#-- component.ftl - Component landing page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" basePath=(basePath)!"" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] versionSwitcher=(versionSwitcher)![]
    currentComponentId=currentComponentId currentVersionStr=(currentVersion.version)!"" currentVersion=(currentVersion)!{} navigation=(navigation)!{}
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe"
    syntaxHighlightingEnabled=(syntaxHighlightingEnabled)!true
    prismCustomComponentUrls=(prismCustomComponentUrls)![]>
    <div class="component-home">
        <h1>${component.displayName}</h1>
        <p class="version-info">
            Current version: <strong>${currentVersion.displayVersion}</strong>
        </p>

        <#if navigation?? && navigation.items?? && navigation.items?size gt 0>
            <h2>Documentation</h2>
            <nav class="component-nav">
                <#include "sidebar-nav.ftl">
            </nav>
        <#else>
            <h2>Available Pages</h2>
            <ul class="page-list">
                <#if currentVersion.pages??>
                    <#list currentVersion.pages as page>
                        <li><a href="${basePath}/${component.id}/${component.defaultVersion}/${page.route?replace('/' + component.id + '/' + component.defaultVersion + '/', '')}">${page.title}</a></li>
                    </#list>
                </#if>
            </ul>
        </#if>

        <#if component.versions?size gt 1>
            <h2>Versions</h2>
            <ul class="version-list">
                <#list component.versions as version>
                    <li>
                        <a href="${basePath}/${component.id}/${version.version}/">
                            ${version.displayVersion}
                        </a>
                    </li>
                </#list>
            </ul>
        </#if>
    </div>
</@layout.layout>
