<#import "layout.ftl" as layout>

<#macro teaserBlock post>
  <p class="teaser">${post.teaser?html} <a class="teaser-more" href="${post.url?html}" aria-label="Read ${post.title?html}">[read more]</a></p>
  <#if post.tags?has_content>
  <p class="teaser-tags">Tags:
    <#list post.tags as tag>
    <a href="/tags/${tag.slug}/index.html">#${tag.name?html}</a><#sep> </#sep>
    </#list>
  </p>
  </#if>
</#macro>

<@layout.page pageTitle=site.title pageClass="page-home" homeHero=true heroTitle="">
<section class="post-grid">
  <#list posts as post>
  <article class="post-card">
    <h2 class="post-title"><a href="${post.url?html}">${post.title?html}</a></h2>
    <p class="post-date">
      <span class="post-meta-item">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-calendar3 post-meta-icon" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M14 0H2a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2M1 3.857C1 3.384 1.448 3 2 3h12c.552 0 1 .384 1 .857v10.286c0 .473-.448.857-1 .857H2c-.552 0-1-.384-1-.857z"/>
          <path d="M6.5 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m-9 3a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m-9 3a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2m3 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2"/>
        </svg>
        Posted on ${post.date?html}
      </span>
      <span class="post-meta-separator"> | </span>
      <span class="post-meta-item">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-file-text post-meta-icon" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M5 4a.5.5 0 0 0 0 1h6a.5.5 0 0 0 0-1zm-.5 2.5A.5.5 0 0 1 5 6h6a.5.5 0 0 1 0 1H5a.5.5 0 0 1-.5-.5M5 8a.5.5 0 0 0 0 1h6a.5.5 0 0 0 0-1zm0 2a.5.5 0 0 0 0 1h3a.5.5 0 0 0 0-1z"/>
          <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2zm10-1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1"/>
        </svg>
        ${post.wordCount?c} words
      </span>
      <span class="post-meta-separator"> | </span>
      <span class="post-meta-item">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-person-fill post-meta-icon" viewBox="0 0 16 16" aria-hidden="true">
          <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6"/>
        </svg>
        ${post.author?html}
      </span>
    </p>
    <#if post.coverImage?? && post.coverImage?has_content>
    <div class="post-card-body post-card-body--with-cover">
      <a class="cover-link" href="${post.url?html}"><img class="post-cover" src="${post.coverImage?html}" alt="${post.title?html}"></a>
      <@teaserBlock post=post />
    </div>
    <#else>
    <@teaserBlock post=post />
    </#if>
  </article>
  </#list>
</section>
</@layout.page>
