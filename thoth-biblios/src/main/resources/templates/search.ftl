<#-- search.ftl - Dedicated search page -->
<#import "layout.ftl" as layout>
<@layout.layout siteTitle=siteTitle siteLogo=(siteLogo)!"" pageTitle="Search" basePath=(basePath)!"" locale=(locale)!"en"
    docSwitcher=(docSwitcher)![] currentComponentId=""
    searchLanguageMode=(searchLanguageMode)!"multilingual_safe">
    <section class="search-page">
        <h1>Search</h1>
        <p id="search-query" class="search-status">Enter a search term.</p>
        <div id="search-results" class="search-results" aria-live="polite"></div>
    </section>
</@layout.layout>
