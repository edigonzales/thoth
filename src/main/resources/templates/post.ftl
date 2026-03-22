<#import "layout.ftl" as layout>
<@layout.page pageTitle=post.title>
<article class="post">
  <h1 class="post-title">${post.title?html}</h1>
  <p class="post-date">${post.date?html}</p>

  <div class="post-content">
    ${post.html}
  </div>

  <footer class="post-footer">
    <span class="post-author">Written by ${post.author?html}</span>
    <#if post.tags?has_content>
    <span class="post-footer-separator"> | </span>
    <span class="post-tags">
      <#list post.tags as tag>
      <a href="/tags/${tag.slug}/index.html">${tag.name?html}</a><#if tag_has_next>, </#if>
      </#list>
    </span>
    </#if>
  </footer>
</article>
</@layout.page>
