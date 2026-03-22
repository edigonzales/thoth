<#import "layout.ftl" as layout>
<@layout.page pageTitle="Blog Archive">
<section class="archive-list">
  <h1>Blog Archive</h1>
  <#list groups as group>
  <section class="archive-group">
    <h2 class="archive-group-heading">${group.heading?html}</h2>
    <ul class="archive-group-posts">
      <#list group.posts as post>
      <li>
        <span class="archive-item-day">${post.day?html}</span>
        <span class="archive-item-separator">-</span>
        <a class="post-title" href="${post.url?html}">${post.title?html}</a>
      </li>
      </#list>
    </ul>
  </section>
  </#list>
</section>
</@layout.page>
