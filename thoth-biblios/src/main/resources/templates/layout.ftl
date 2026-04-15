<#macro layout siteTitle siteLogo="" pageTitle="" basePath="" locale="en" docSwitcher=[] versionSwitcher=[] currentComponentId="" currentVersionStr="" currentVersion={} currentPagePath="" navigation={} breadcrumbs=[] prevPage={} nextPage={} searchQuery="" searchLanguageMode="multilingual_safe" syntaxHighlightingEnabled=true prismCustomComponentUrls=[] singlePageMode=false chapterBreadcrumbEnabled=false initialChapterId="">
<!DOCTYPE html>
<html lang="${locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><#if pageTitle?has_content>${pageTitle} - </#if>${siteTitle}</title>
    <link rel="stylesheet" href="${basePath}/site-assets/jetbrainsmono.css">
    <link rel="stylesheet" href="${basePath}/site-assets/zurich-light.css">
    <link rel="stylesheet" href="${basePath}/site-assets/zurich.css">
    <link rel="stylesheet" href="${basePath}/site-assets/zurich-condensed.css">
    <link rel="stylesheet" href="${basePath}/site-assets/notosans.css">
    <link rel="stylesheet" href="${basePath}/site-assets/notoserif.css">
    <link rel="stylesheet" href="${basePath}/site-assets/inter.css">
    <link rel="stylesheet" href="${basePath}/site-assets/literata.css">
    <link rel="stylesheet" href="${basePath}/site-assets/inter-tight.css">
    <link rel="stylesheet" href="${basePath}/site-assets/geist.css">
    <link rel="stylesheet" href="${basePath}/site-assets/familjen-grotesk.css">
    <link rel="stylesheet" href="${basePath}/site-assets/source-serif-4.css">
    <link rel="stylesheet" href="${basePath}/site-assets/spectral.css">
    <link rel="stylesheet" href="${basePath}/site-assets/merriweather.css">
    <link rel="stylesheet" href="${basePath}/site-assets/source-sans-3.css">
    <link rel="stylesheet" href="${basePath}/site-assets/texgyreheroes.css">
    <link rel="stylesheet" href="${basePath}/site-assets/frutiger-serif.css">
    <link rel="stylesheet" href="${basePath}/site-assets/styles.css">
    <#if syntaxHighlightingEnabled>
    <link rel="stylesheet" href="${basePath}/site-assets/prism/prism.css">
    <link rel="stylesheet" href="${basePath}/site-assets/prism-overrides.css">
    <link rel="stylesheet" href="${basePath}/site-assets/prism/plugins/line-highlight/prism-line-highlight.min.css">
    <link rel="stylesheet" href="${basePath}/site-assets/prism/plugins/line-numbers/prism-line-numbers.min.css">
    <link rel="stylesheet" href="${basePath}/site-assets/prism/plugins/toolbar/prism-toolbar.min.css">
    </#if>
    <script src="${basePath}/site-assets/lunr.min.js" defer></script>
    <script src="${basePath}/site-assets/search.js" defer></script>
    <#if syntaxHighlightingEnabled>
    <script src="${basePath}/site-assets/prism/prism.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-markup.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-clike.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-javascript.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-css.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-ini.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-interlis.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-java.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-typescript.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-json.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-bash.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-sql.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-python.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-yaml.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-kotlin.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-go.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-c.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/components/prism-cpp.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/plugins/line-highlight/prism-line-highlight.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/plugins/line-numbers/prism-line-numbers.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/plugins/toolbar/prism-toolbar.min.js" defer></script>
    <script src="${basePath}/site-assets/prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js" defer></script>
    <#if prismCustomComponentUrls?has_content>
        <#list prismCustomComponentUrls as customComponentUrl>
    <script src="${basePath}${customComponentUrl}" defer></script>
        </#list>
    </#if>
    </#if>
</head>
<body data-search-language-mode="${searchLanguageMode}" data-single-page-mode="${singlePageMode?string('true', 'false')}" data-initial-chapter-id="${initialChapterId}">
    <header class="site-header">
        <div class="header-content">
            <a href="${basePath}/" class="site-title">
                <#if siteLogo?has_content>
                    <img src="${siteLogo}" alt="${siteTitle} logo" class="brand-logo">
                </#if>
                <span class="site-title-text">${siteTitle}</span>
            </a>
            <#if docSwitcher?has_content>
                <nav class="doc-switcher">
                    <label for="doc-switch">Documentation:</label>
                    <select id="doc-switch" onchange="window.location.href=this.value">
                        <option value="" disabled <#if !currentComponentId?has_content>selected</#if>>Select documentation</option>
                        <#list docSwitcher as doc>
                            <option value="${basePath}/${doc.id}/${doc.defaultVersion}/"
                                <#if currentComponentId?has_content && currentComponentId == doc.id>selected</#if>>
                                ${doc.displayName}
                            </option>
                        </#list>
                    </select>
                </nav>
            </#if>
            <#if versionSwitcher?has_content>
                <nav class="version-switcher">
                    <label for="version-switch">Version:</label>
                    <select id="version-switch" onchange="window.location.href=this.value">
                        <option disabled>Version</option>
                        <#list versionSwitcher as ver>
                            <option value="${basePath}${ver.route}"
                                <#if currentVersionStr == ver.version>selected</#if>>
                                ${ver.displayVersion}
                            </option>
                        </#list>
                    </select>
                </nav>
            </#if>
            <form class="search-form" action="${basePath}/search/" method="get" role="search">
                <label for="search-input">Search:</label>
                <input id="search-input" type="search" name="q" value="${searchQuery}" placeholder="Search documentation">
            </form>
        </div>
    </header>

    <div class="layout">
        <#if navigation?has_content && navigation.items?has_content>
        <aside class="sidebar">
            <nav class="sidebar-nav">
                <#include "sidebar-nav.ftl">
            </nav>
        </aside>
        </#if>

        <main class="content">
            <#if breadcrumbs?has_content && breadcrumbs?size gt 1>
            <nav class="breadcrumbs" data-chapter-breadcrumb="${chapterBreadcrumbEnabled?string('enabled', 'disabled')}">
                <#if chapterBreadcrumbEnabled>
                    <#assign rootCrumb = breadcrumbs[0]>
                    <#if rootCrumb.route?has_content>
                        <a href="${basePath}${rootCrumb.route}" id="chapter-breadcrumb-root">${rootCrumb.title}</a>
                    <#else>
                        <span class="current" id="chapter-breadcrumb-root">${rootCrumb.title}</span>
                    </#if>
                    <span class="separator">/</span>
                    <span id="chapter-breadcrumb-trail">
                        <span class="current" id="chapter-breadcrumb-current">${breadcrumbs[breadcrumbs?size - 1].title}</span>
                    </span>
                <#else>
                    <#list breadcrumbs as crumb>
                        <#if crumb_index gt 0><span class="separator">/</span></#if>
                        <#if crumb.route?has_content>
                            <a href="${basePath}${crumb.route}">${crumb.title}</a>
                        <#else>
                            <span class="current">${crumb.title}</span>
                        </#if>
                    </#list>
                </#if>
            </nav>
            </#if>

            <#nested>

            <#if !singlePageMode && (prevPage?has_content || nextPage?has_content)>
            <nav class="prev-next">
                <#if prevPage?has_content>
                    <a href="${basePath}${prevPage.route}" class="prev">${prevPage.title}</a>
                </#if>
                <#if nextPage?has_content>
                    <a href="${basePath}${nextPage.route}" class="next">${nextPage.title}</a>
                </#if>
            </nav>
            </#if>
        </main>
    </div>

    <footer class="site-footer">
        <p>Generated by <a href="https://github.com/stefan/thoth">Thoth Biblios</a></p>
    </footer>
</body>
</html>
</#macro>
