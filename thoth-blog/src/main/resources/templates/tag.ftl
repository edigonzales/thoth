<#import "layout.ftl" as layout>
<@layout.page pageTitle="${messages.tag}: ${tagName}">
<section class="archive-list">
  <h1>${messages.tag?html}: ${tagName?html}</h1>
  <ul>
    <#list posts as post>
    <li>
      <span class="post-date">${post.date?html}</span>
      <a class="post-title" lang="${post.language!locale}" href="${post.url?html}">${post.title?html}</a>
      <#if (post.fallback)!false><span class="language-notice">${messages.onlyAvailable?html} ${post.languageName?html}</span></#if>
    </li>
    </#list>
  </ul>
</section>
</@layout.page>
