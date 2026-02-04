<#macro page pageTitle>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${pageTitle?html} | ${site.title?html}</title>
  <link id="theme-style" rel="stylesheet" href="/assets/styles-light.css" data-light="/assets/styles-light.css" data-dark="/assets/styles-dark.css">
  <link rel="stylesheet" href="/assets/highlight/styles/github.min.css">
  <link rel="alternate" type="application/rss+xml" title="${site.title?html}" href="/feed.xml">
  <script src="/assets/theme.js" defer></script>
  <script src="/assets/lunr.min.js" defer></script>
  <script src="/assets/highlight/highlight.min.js" defer></script>
  <script src="/assets/search.js" defer></script>
</head>
<body>
  <nav id="navbar">
    <div class="nav-left">
      <a href="/index.html">Home</a>
      <a href="/archive.html">Archive</a>
      <a href="/feed.xml">Subscribe</a>
    </div>
    <div class="nav-right">
      <form id="search-form" action="/search.html" method="get">
        <input id="search-input" type="search" name="q" value="${searchQuery!}" placeholder="Search posts">
      </form>
      <button id="theme-toggle" type="button" aria-label="Toggle dark mode">Toggle</button>
    </div>
  </nav>

  <main id="content">
    <#nested>
  </main>
</body>
</html>
</#macro>
