<#-- search.ftl - Dedicated search page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" pageTitle="Search" basePath=(basePath)!"." siteRootHref=(siteRootHref)!"./" searchPageHref=(searchPageHref)!"./search/" searchIndexUrl=(searchIndexUrl)!"./search-index.json" searchIndexScriptHref=(searchIndexScriptHref)!"" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] currentComponentId=""
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe"
    syntaxHighlightingEnabled=(syntaxHighlightingEnabled)!true
    prismCustomComponentUrls=(prismCustomComponentUrls)![]>
    <section class="search-page">
        <h1>Search</h1>
        <p id="search-query" class="search-status">Enter a search term.</p>
        <div id="search-results" class="search-results" aria-live="polite"></div>
    </section>
</@layout.layout>
