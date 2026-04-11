<?xml version="1.0"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <title>${siteTitle?xml}</title>
    <link>${siteLink?xml}</link>
    <atom:link href="${feedSelf?xml}" rel="self" type="application/rss+xml" />
    <description>${siteDescription?xml}</description>
    <language>${siteLanguage?xml}</language>
    <pubDate>${pubDate?xml}</pubDate>
    <lastBuildDate>${lastBuildDate?xml}</lastBuildDate>
    <#list items as item>
    <item>
      <title>${item.title?xml}</title>
      <link>${item.link?xml}</link>
      <pubDate>${item.pubDate?xml}</pubDate>
      <guid isPermaLink="false">${item.guid?xml}</guid>
      <description><![CDATA[${item.description}]]></description>
    </item>
    </#list>
  </channel>
</rss>
