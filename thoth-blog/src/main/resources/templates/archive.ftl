<#import "layout.ftl" as layout>
<@layout.page pageTitle=messages.archiveTitle>
<section class="archive-list">
  <h1>${messages.archiveTitle?html}</h1>
  <#list groups as group>
  <section class="archive-group">
    <h2 class="archive-group-heading">${group.heading?html}</h2>
    <ul class="archive-group-posts">
      <#list group.posts as post>
      <li>
        <span class="archive-item-day">${post.day?html}</span>
        <span class="archive-item-separator">-</span>
        <a class="post-title" lang="${post.language!locale}" href="${post.url?html}">${post.title?html}</a>
        <#if (post.fallback!"false") == "true"><span class="language-notice">${messages.onlyAvailable?html} ${post.languageName?html}</span></#if>
      </li>
      </#list>
    </ul>
  </section>
  </#list>
</section>
</@layout.page>
