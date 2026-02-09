<#import "layout.ftl" as layout>
<@layout.page pageTitle=site.title>
<section class="post-grid">
  <#list posts as post>
  <article class="post-card">
    <h2 class="post-title"><a href="${post.url?html}">${post.title?html}</a></h2>
    <p class="post-date">${post.date?html}</p>
    <#if post.coverImage?? && post.coverImage?has_content>
    <div class="post-card-body post-card-body--with-cover">
      <a class="cover-link" href="${post.url?html}"><img class="post-cover" src="${post.coverImage?html}" alt="${post.title?html}"></a>
      <p class="teaser">${post.teaser?html}<a class="teaser-more" href="${post.url?html}" aria-label="Read ${post.title?html}">...</a></p>
    </div>
    <#else>
    <p class="teaser">${post.teaser?html}<a class="teaser-more" href="${post.url?html}" aria-label="Read ${post.title?html}">...</a></p>
    </#if>
  </article>
  </#list>
</section>
</@layout.page>
