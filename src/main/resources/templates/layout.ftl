<#macro page pageTitle>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${pageTitle?html} | ${site.title?html}</title>
  <link rel="stylesheet" href="/assets/zurich.css">
  <link id="theme-style" rel="stylesheet" href="/assets/styles-light.css" data-light="/assets/styles-light.css" data-dark="/assets/styles-dark.css">
  <link rel="stylesheet" href="/assets/prism/prism.css">
  <link rel="stylesheet" href="/assets/prism/plugins/line-highlight/prism-line-highlight.min.css">
  <link rel="stylesheet" href="/assets/prism/plugins/line-numbers/prism-line-numbers.min.css">
  <link rel="alternate" type="application/rss+xml" title="${site.title?html}" href="/feed.xml">
  <script src="/assets/theme.js" defer></script>
  <script src="/assets/prism/prism.js" defer></script>
  <script src="/assets/prism/components/prism-markup.min.js" defer></script>
  <script src="/assets/prism/components/prism-clike.min.js" defer></script>
  <script src="/assets/prism/components/prism-javascript.min.js" defer></script>
  <script src="/assets/prism/components/prism-css.min.js" defer></script>
  <script src="/assets/prism/components/prism-ini.min.js" defer></script>
  <script src="/assets/prism/components/prism-interlis.js" defer></script>
  <script src="/assets/prism/components/prism-java.min.js" defer></script>
  <script src="/assets/prism/components/prism-typescript.min.js" defer></script>
  <script src="/assets/prism/components/prism-json.min.js" defer></script>
  <script src="/assets/prism/components/prism-bash.min.js" defer></script>
  <script src="/assets/prism/components/prism-sql.min.js" defer></script>
  <script src="/assets/prism/components/prism-python.min.js" defer></script>
  <script src="/assets/prism/components/prism-yaml.min.js" defer></script>
  <script src="/assets/prism/components/prism-kotlin.min.js" defer></script>
  <script src="/assets/prism/components/prism-go.min.js" defer></script>
  <script src="/assets/prism/components/prism-c.min.js" defer></script>
  <script src="/assets/prism/components/prism-cpp.min.js" defer></script>
  <script src="/assets/prism/plugins/line-highlight/prism-line-highlight.min.js" defer></script>
  <script src="/assets/prism/plugins/line-numbers/prism-line-numbers.min.js" defer></script>
  <script src="/assets/lunr.min.js" defer></script>
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
      <button id="theme-toggle" class="theme-toggle" type="button" title="system mode" aria-label="Switch between dark and light mode (currently system mode)">
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" class="theme-toggle__icon theme-toggle__icon--light">
          <path fill="currentColor" d="M12,9c1.65,0,3,1.35,3,3s-1.35,3-3,3s-3-1.35-3-3S10.35,9,12,9 M12,7c-2.76,0-5,2.24-5,5s2.24,5,5,5s5-2.24,5-5 S14.76,7,12,7L12,7z M2,13l2,0c0.55,0,1-0.45,1-1s-0.45-1-1-1l-2,0c-0.55,0-1,0.45-1,1S1.45,13,2,13z M20,13l2,0c0.55,0,1-0.45,1-1 s-0.45-1-1-1l-2,0c-0.55,0-1,0.45-1,1S19.45,13,20,13z M11,2v2c0,0.55,0.45,1,1,1s1-0.45,1-1V2c0-0.55-0.45-1-1-1S11,1.45,11,2z M11,20v2c0,0.55,0.45,1,1,1s1-0.45,1-1v-2c0-0.55-0.45-1-1-1C11.45,19,11,19.45,11,20z M5.99,4.58c-0.39-0.39-1.03-0.39-1.41,0 c-0.39,0.39-0.39,1.03,0,1.41l1.06,1.06c0.39,0.39,1.03,0.39,1.41,0s0.39-1.03,0-1.41L5.99,4.58z M18.36,16.95 c-0.39-0.39-1.03-0.39-1.41,0c-0.39,0.39-0.39,1.03,0,1.41l1.06,1.06c0.39,0.39,1.03,0.39,1.41,0c0.39-0.39,0.39-1.03,0-1.41 L18.36,16.95z M19.42,5.99c0.39-0.39,0.39-1.03,0-1.41c-0.39-0.39-1.03-0.39-1.41,0l-1.06,1.06c-0.39,0.39-0.39,1.03,0,1.41 s1.03,0.39,1.41,0L19.42,5.99z M7.05,18.36c0.39-0.39,0.39-1.03,0-1.41c-0.39-0.39-1.03-0.39-1.41,0l-1.06,1.06 c-0.39,0.39-0.39,1.03,0,1.41s1.03,0.39,1.41,0L7.05,18.36z"></path>
        </svg>
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" class="theme-toggle__icon theme-toggle__icon--dark">
          <path fill="currentColor" d="M9.37,5.51C9.19,6.15,9.1,6.82,9.1,7.5c0,4.08,3.32,7.4,7.4,7.4c0.68,0,1.35-0.09,1.99-0.27C17.45,17.19,14.93,19,12,19 c-3.86,0-7-3.14-7-7C5,9.07,6.81,6.55,9.37,5.51z M12,3c-4.97,0-9,4.03-9,9s4.03,9,9,9s9-4.03,9-9c0-0.46-0.04-0.92-0.1-1.36 c-0.98,1.37-2.58,2.26-4.4,2.26c-2.98,0-5.4-2.42-5.4-5.4c0-1.81,0.89-3.42,2.26-4.4C12.92,3.04,12.46,3,12,3L12,3z"></path>
        </svg>
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" class="theme-toggle__icon theme-toggle__icon--system">
          <path fill="currentColor" d="m12 21c4.971 0 9-4.029 9-9s-4.029-9-9-9-9 4.029-9 9 4.029 9 9 9zm4.95-13.95c1.313 1.313 2.05 3.093 2.05 4.95s-0.738 3.637-2.05 4.95c-1.313 1.313-3.093 2.05-4.95 2.05v-14c1.857 0 3.637 0.737 4.95 2.05z"></path>
        </svg>
      </button>
    </div>
  </nav>

  <main id="content">
    <#nested>
  </main>
</body>
</html>
</#macro>
