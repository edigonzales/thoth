<#import "layout.ftl" as layout>
<@layout.page pageTitle=post.title>
<article class="post">
  <h1 class="post-title">${post.title?html}</h1>
  <p class="post-date">${post.date?html}</p>

  <div class="post-content">
    ${post.html}
  </div>

  <footer class="post-footer">
    <span class="post-author">${post.author?html}</span>
    <span class="post-tags">
      <#list post.tags as tag>
      <a class="tag" href="/tags/${tag.slug}/index.html">${tag.name?html}</a>
      </#list>
    </span>
  </footer>
</article>
</@layout.page>
