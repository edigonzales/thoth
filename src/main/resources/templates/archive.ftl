<#import "layout.ftl" as layout>
<@layout.page pageTitle="Archive">
<section class="archive-list">
  <h1>Archive</h1>
  <ul>
    <#list posts as post>
    <li>
      <span class="post-date">${post.date?html}</span>
      <a class="post-title" href="${post.url?html}">${post.title?html}</a>
    </li>
    </#list>
  </ul>
</section>
</@layout.page>
