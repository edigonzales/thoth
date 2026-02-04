<#import "layout.ftl" as layout>
<@layout.page pageTitle=site.title>
<section class="post-grid">
  <#list posts as post>
  <article class="post-card">
    <h2 class="post-title"><a href="${post.url?html}">${post.title?html}</a></h2>
    <p class="post-date">${post.date?html}</p>
    <#if post.coverImage?? && post.coverImage?has_content>
    <a class="cover-link" href="${post.url?html}"><img class="post-cover" src="${post.coverImage?html}" alt="${post.title?html}"></a>
    </#if>
    <p class="teaser">${post.teaser?html}</p>
  </article>
  </#list>
</section>
</@layout.page>
