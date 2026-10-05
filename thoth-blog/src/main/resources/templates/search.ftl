<#import "layout.ftl" as layout>
<@layout.page pageTitle=messages.search>
<section class="search-results-page">
  <h1>${messages.search?html}</h1>
  <p id="search-query" class="teaser"></p>
  <div id="search-results"></div>
</section>
</@layout.page>
