(() => {
  const SEARCH_SCOPE_GLOBAL = "global";
  const SEARCH_SCOPE_ACTIVE = "active";
  const DESKTOP_BREADCRUMB_BREAKPOINT = 768;
  const SINGLE_PAGE_BREADCRUMB_DEPTH = 3;
  const HIGHLIGHT_EXCLUDED_TAGS = new Set([
    "SCRIPT",
    "STYLE",
    "PRE",
    "CODE",
    "NOSCRIPT",
    "TEXTAREA",
    "INPUT",
    "SELECT",
    "OPTION",
    "BUTTON",
    "MARK"
  ]);

  function normalizeMode(rawMode) {
    return rawMode === "english_default" ? "english_default" : "multilingual_safe";
  }

  function readSearchLanguageMode() {
    const fromBody = document.body && document.body.dataset
      ? document.body.dataset.searchLanguageMode
      : "";
    return normalizeMode((fromBody || "").trim());
  }

  function readSiteRootHref() {
    const fromBody = document.body && document.body.dataset
      ? document.body.dataset.siteRootHref
      : "";
    const normalized = (fromBody || "").trim();
    return normalized || "./";
  }

  function readSearchIndexUrl() {
    const fromBody = document.body && document.body.dataset
      ? document.body.dataset.searchIndexUrl
      : "";
    const normalized = (fromBody || "").trim();
    return normalized || "search-index.json";
  }

  function readInlineSearchIndex() {
    return Array.isArray(window.__BIBLIOS_SEARCH_INDEX__)
      ? window.__BIBLIOS_SEARCH_INDEX__
      : null;
  }

  function normalizeSearchScope(rawScope) {
    return rawScope === SEARCH_SCOPE_ACTIVE ? SEARCH_SCOPE_ACTIVE : SEARCH_SCOPE_GLOBAL;
  }

  function readCurrentDocContext() {
    if (!document.body || !document.body.dataset) {
      return { component: "", version: "" };
    }
    return {
      component: (document.body.dataset.currentComponentId || "").trim(),
      version: (document.body.dataset.currentVersion || "").trim()
    };
  }

  function parseSearchState() {
    const params = new URLSearchParams(window.location.search);
    const query = (params.get("q") || "").trim();
    const scope = normalizeSearchScope((params.get("scope") || "").trim());
    const component = (params.get("component") || "").trim();
    const version = (params.get("version") || "").trim();

    if (scope === SEARCH_SCOPE_ACTIVE && (!component || !version)) {
      return {
        query,
        scope: SEARCH_SCOPE_GLOBAL,
        component: "",
        version: ""
      };
    }

    return { query, scope, component, version };
  }

  function resolveScopeContext(searchState, currentContext) {
    if (currentContext.component && currentContext.version) {
      return currentContext;
    }
    if (searchState.component && searchState.version) {
      return {
        component: searchState.component,
        version: searchState.version
      };
    }
    return { component: "", version: "" };
  }

  function syncSearchFormState(searchState) {
    const queryInput = document.getElementById("search-input");
    if (queryInput) {
      queryInput.value = searchState.query;
    }

    const scopeInput = document.getElementById("search-scope-input");
    const componentInput = document.getElementById("search-component-input");
    const versionInput = document.getElementById("search-version-input");
    const scopeCheckbox = document.getElementById("search-scope-active");
    const form = scopeInput ? scopeInput.form : null;

    if (!scopeInput || !componentInput || !versionInput || !scopeCheckbox) {
      return;
    }

    const currentContext = readCurrentDocContext();
    const scopeContext = resolveScopeContext(searchState, currentContext);
    const hasScopeContext = !!(scopeContext.component && scopeContext.version);
    scopeCheckbox.disabled = !hasScopeContext;

    if (hasScopeContext &&
        searchState.scope === SEARCH_SCOPE_ACTIVE &&
        searchState.component === scopeContext.component &&
        searchState.version === scopeContext.version) {
      scopeCheckbox.checked = true;
    } else {
      scopeCheckbox.checked = false;
    }

    const applyScope = () => {
      if (!scopeCheckbox.disabled && scopeCheckbox.checked) {
        scopeInput.value = SEARCH_SCOPE_ACTIVE;
        componentInput.value = scopeContext.component;
        versionInput.value = scopeContext.version;
      } else {
        scopeInput.value = SEARCH_SCOPE_GLOBAL;
        componentInput.value = "";
        versionInput.value = "";
      }
    };

    applyScope();
    scopeCheckbox.addEventListener("change", applyScope);
    if (form) {
      form.addEventListener("submit", applyScope);
    }
  }

  function renderMessage(container, message) {
    container.innerHTML = "";
    const p = document.createElement("p");
    p.className = "search-empty";
    p.textContent = message;
    container.appendChild(p);
  }

  function tokenizeQuery(query) {
    const seen = new Set();
    const tokens = [];
    for (const rawToken of (query || "").split(/\s+/)) {
      const token = rawToken.trim();
      if (!token) {
        continue;
      }
      const normalized = token.toLowerCase();
      if (seen.has(normalized)) {
        continue;
      }
      seen.add(normalized);
      tokens.push(token);
    }
    return tokens.sort((a, b) => b.length - a.length);
  }

  function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  function buildTokenRegex(tokens) {
    if (!tokens || tokens.length === 0) {
      return null;
    }
    const escaped = tokens.map((token) => escapeRegExp(token)).filter(Boolean);
    if (escaped.length === 0) {
      return null;
    }
    return new RegExp("(" + escaped.join("|") + ")", "gi");
  }

  function appendHighlightedText(container, text, regex, className) {
    if (!text) {
      return;
    }
    regex.lastIndex = 0;
    let lastIndex = 0;
    let match;
    while ((match = regex.exec(text)) !== null) {
      const start = match.index;
      const end = start + match[0].length;

      if (start > lastIndex) {
        container.appendChild(document.createTextNode(text.slice(lastIndex, start)));
      }

      const mark = document.createElement("mark");
      mark.className = className;
      mark.textContent = text.slice(start, end);
      container.appendChild(mark);

      lastIndex = end;
      if (regex.lastIndex === start) {
        regex.lastIndex += 1;
      }
    }

    if (lastIndex < text.length) {
      container.appendChild(document.createTextNode(text.slice(lastIndex)));
    }
  }

  function buildResultHref(route, searchState) {
    const safeRoute = route || "";
    if (!safeRoute) {
      return "#";
    }

    const hashIndex = safeRoute.indexOf("#");
    const baseRoute = hashIndex >= 0 ? safeRoute.slice(0, hashIndex) : safeRoute;
    const hashPart = hashIndex >= 0 ? safeRoute.slice(hashIndex) : "";
    const resolvedBaseRoute = resolveRouteHref(baseRoute);

    const params = [];
    if (searchState.query) {
      params.push("q=" + encodeURIComponent(searchState.query));
    }
    const scope = normalizeSearchScope((searchState.scope || "").trim());
    params.push("scope=" + encodeURIComponent(scope));
    if (scope === SEARCH_SCOPE_ACTIVE && searchState.component && searchState.version) {
      params.push("component=" + encodeURIComponent(searchState.component));
      params.push("version=" + encodeURIComponent(searchState.version));
    }

    if (params.length === 0) {
      return resolvedBaseRoute + hashPart;
    }

    const joiner = resolvedBaseRoute.includes("?") ? "&" : "?";
    return resolvedBaseRoute + joiner + params.join("&") + hashPart;
  }

  function resolveRouteHref(route) {
    const safeRoute = (route || "").trim();
    if (!safeRoute || safeRoute === "#") {
      return safeRoute || "#";
    }
    if (!safeRoute.startsWith("/")) {
      return safeRoute;
    }
    const siteRootHref = readSiteRootHref();
    if (safeRoute === "/") {
      return siteRootHref;
    }
    return siteRootHref + safeRoute.replace(/^\/+/, "");
  }

  function findFirstTokenIndex(content, tokens) {
    const normalized = content.toLowerCase();
    let index = -1;
    for (const token of tokens) {
      const next = normalized.indexOf(token.toLowerCase());
      if (next < 0) {
        continue;
      }
      if (index < 0 || next < index) {
        index = next;
      }
    }
    return index;
  }

  function createSnippet(doc, tokens) {
    const content = (doc.content || "").replace(/\s+/g, " ").trim();
    if (!content) {
      return "";
    }

    const hitAt = findFirstTokenIndex(content, tokens);
    if (hitAt < 0) {
      return content.length > 220 ? content.slice(0, 220) + "..." : content;
    }

    const longestTokenLength = tokens.length > 0 ? tokens[0].length : 0;
    const start = Math.max(0, hitAt - 80);
    const end = Math.min(content.length, hitAt + longestTokenLength + 120);
    let snippet = content.slice(start, end);
    if (start > 0) {
      snippet = "..." + snippet;
    }
    if (end < content.length) {
      snippet += "...";
    }
    return snippet;
  }

  function describeResultContext(doc) {
    const sectionPath = (doc.sectionPath || "").trim();
    const pageTitle = (doc.pageTitle || "").trim();
    if (sectionPath) {
      return sectionPath;
    }
    return pageTitle || "Page";
  }

  function createResultElement(doc, searchState, queryTokens, queryRegex) {
    const article = document.createElement("article");
    article.className = "search-result";

    const heading = document.createElement("h2");
    heading.className = "search-result-title";

    const link = document.createElement("a");
    link.href = buildResultHref(doc.route, searchState);
    link.textContent = doc.title || doc.route || "Untitled";
    heading.appendChild(link);

    const meta = document.createElement("p");
    meta.className = "search-result-meta";
    const component = doc.component || "unknown";
    const displayVersion = doc.displayVersion || doc.version || "unknown";
    meta.textContent = component + " · " + displayVersion + " · " + describeResultContext(doc);

    const snippet = createSnippet(doc, queryTokens);
    const teaser = document.createElement("p");
    teaser.className = "search-result-snippet";
    if (snippet && queryRegex) {
      appendHighlightedText(teaser, snippet, queryRegex, "search-highlight");
    } else {
      teaser.textContent = snippet;
    }

    article.appendChild(heading);
    article.appendChild(meta);
    if (snippet) {
      article.appendChild(teaser);
    }

    return article;
  }

  function renderResults(container, searchState, results) {
    container.innerHTML = "";
    const query = searchState.query;

    if (results.length === 0) {
      renderMessage(container, 'No results for "' + query + '".');
      return;
    }

    const queryTokens = tokenizeQuery(query);
    const queryRegex = buildTokenRegex(queryTokens);

    const list = document.createElement("div");
    list.className = "search-results-list";
    for (const doc of results) {
      list.appendChild(createResultElement(doc, searchState, queryTokens, queryRegex));
    }
    container.appendChild(list);
  }

  function dedupeResultsByRoute(results) {
    const deduped = [];
    const seen = new Set();
    for (const result of results) {
      const route = (result && result.route ? result.route : "").trim();
      if (!route || seen.has(route)) {
        continue;
      }
      seen.add(route);
      deduped.push(result);
    }
    return deduped;
  }

  function filterDocumentsByScope(documents, searchState) {
    if (searchState.scope !== SEARCH_SCOPE_ACTIVE) {
      return documents;
    }
    if (!searchState.component || !searchState.version) {
      return documents;
    }
    return documents.filter((doc) => {
      const component = (doc.component || "").trim();
      const version = (doc.version || "").trim();
      return component === searchState.component && version === searchState.version;
    });
  }

  function fallbackSearch(query, documents) {
    const normalized = query.toLowerCase();
    const matches = documents.filter((doc) => {
      const blob = [
        doc.title || "",
        doc.pageTitle || "",
        doc.sectionPath || "",
        String(doc.sectionLevel || ""),
        doc.kind || "",
        doc.component || "",
        doc.displayVersion || "",
        doc.version || "",
        doc.route || "",
        doc.content || ""
      ].join(" ").toLowerCase();
      return blob.includes(normalized);
    });
    return dedupeResultsByRoute(matches);
  }

  function removePipelinesForMultilingualSafe(indexBuilder) {
    if (window.lunr && window.lunr.stopWordFilter) {
      indexBuilder.pipeline.remove(window.lunr.stopWordFilter);
      indexBuilder.searchPipeline.remove(window.lunr.stopWordFilter);
    }
    if (window.lunr && window.lunr.stemmer) {
      indexBuilder.pipeline.remove(window.lunr.stemmer);
      indexBuilder.searchPipeline.remove(window.lunr.stemmer);
    }
  }

  function trySearch(index, query) {
    try {
      return index.search(query);
    } catch (error) {
      const wildcardQuery = query
        .split(/\s+/)
        .filter(Boolean)
        .map((token) => token + "*")
        .join(" ");

      if (!wildcardQuery) {
        return [];
      }

      try {
        return index.search(wildcardQuery);
      } catch (ignored) {
        return [];
      }
    }
  }

  function lunrSearch(query, documents, mode) {
    const docsByRef = new Map();
    const documentsWithRef = documents.map((doc, idx) => {
      const ref = doc.route || String(idx);
      const enriched = {
        _ref: ref,
        title: doc.title || "",
        pageTitle: doc.pageTitle || "",
        sectionPath: doc.sectionPath || "",
        sectionLevel: String(doc.sectionLevel || ""),
        kind: doc.kind || "",
        component: doc.component || "",
        displayVersion: doc.displayVersion || doc.version || "",
        content: doc.content || ""
      };
      docsByRef.set(ref, doc);
      return enriched;
    });

    const index = window.lunr(function () {
      this.ref("_ref");
      this.field("title", { boost: 8 });
      this.field("sectionPath", { boost: 6 });
      this.field("pageTitle", { boost: 4 });
      this.field("sectionLevel");
      this.field("kind", { boost: 2 });
      this.field("component", { boost: 2 });
      this.field("displayVersion", { boost: 2 });
      this.field("content", { boost: 1 });

      if (mode === "multilingual_safe") {
        removePipelinesForMultilingualSafe(this);
      }

      for (const doc of documentsWithRef) {
        this.add(doc);
      }
    });

    const results = trySearch(index, query);
    const mapped = results.map((entry) => docsByRef.get(entry.ref)).filter(Boolean);
    return dedupeResultsByRoute(mapped);
  }

  function initSearchPage(searchState) {
    const resultsContainer = document.getElementById("search-results");
    if (!resultsContainer) {
      return;
    }

    const query = searchState.query;

    const queryLabel = document.getElementById("search-query");
    if (queryLabel) {
      const scoped = searchState.scope === SEARCH_SCOPE_ACTIVE;
      queryLabel.textContent = query
        ? ('Results for "' + query + '"' + (scoped ? " (active docs version)." : "."))
        : "Enter a search term in the header search field.";
    }

    if (!query) {
      renderMessage(resultsContainer, "Enter a search term above.");
      return;
    }

    const inlineDocuments = readInlineSearchIndex();
    if (inlineDocuments) {
      renderSearchDocuments(resultsContainer, searchState, inlineDocuments);
      return;
    }

    fetch(readSearchIndexUrl())
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load search index");
        }
        return response.json();
      })
      .then((documents) => renderSearchDocuments(resultsContainer, searchState, documents))
      .catch(() => {
        renderMessage(resultsContainer, "Search index could not be loaded.");
      });
  }

  function renderSearchDocuments(resultsContainer, searchState, documents) {
    if (!Array.isArray(documents)) {
      throw new Error("Invalid search index format");
    }

    const scopedDocuments = filterDocumentsByScope(documents, searchState);
    const mode = readSearchLanguageMode();
    let results;

    if (window.lunr && typeof window.lunr === "function") {
      try {
        results = lunrSearch(searchState.query, scopedDocuments, mode);
      } catch (error) {
        results = fallbackSearch(searchState.query, scopedDocuments);
      }
    } else {
      results = fallbackSearch(searchState.query, scopedDocuments);
    }

    renderResults(resultsContainer, searchState, dedupeResultsByRoute(results));
  }

  function shouldSkipHighlightNode(textNode) {
    if (!textNode || !textNode.parentElement) {
      return true;
    }

    let current = textNode.parentElement;
    while (current) {
      if (HIGHLIGHT_EXCLUDED_TAGS.has(current.tagName)) {
        return true;
      }
      if (current.tagName === "A" && current.classList.contains("anchor")) {
        return true;
      }
      current = current.parentElement;
    }

    return false;
  }

  function initDocumentHighlights(query) {
    const root = document.querySelector(".doc-content");
    if (!root || !query) {
      return;
    }

    const tokens = tokenizeQuery(query);
    const regex = buildTokenRegex(tokens);
    if (!regex) {
      return;
    }

    const walker = document.createTreeWalker(
      root,
      NodeFilter.SHOW_TEXT,
      {
        acceptNode(node) {
          if (!node || !node.nodeValue || !node.nodeValue.trim()) {
            return NodeFilter.FILTER_REJECT;
          }
          if (shouldSkipHighlightNode(node)) {
            return NodeFilter.FILTER_REJECT;
          }
          return NodeFilter.FILTER_ACCEPT;
        }
      }
    );

    const textNodes = [];
    let next;
    while ((next = walker.nextNode())) {
      textNodes.push(next);
    }

    for (const textNode of textNodes) {
      const text = textNode.nodeValue;
      regex.lastIndex = 0;
      if (!regex.test(text)) {
        continue;
      }

      const fragment = document.createDocumentFragment();
      appendHighlightedText(fragment, text, regex, "search-highlight");
      textNode.parentNode.replaceChild(fragment, textNode);
    }
  }

  function decodeHashChapterId() {
    const hash = (window.location.hash || "").trim();
    if (!hash || hash === "#") {
      return "";
    }
    try {
      return decodeURIComponent(hash.slice(1));
    } catch (error) {
      return hash.slice(1);
    }
  }

  function syncStickyOffsets() {
    const root = document.documentElement;
    if (!root) {
      return;
    }

    const header = document.querySelector(".site-header");
    const breadcrumbs = document.querySelector(".breadcrumbs");
    const isDesktop = window.innerWidth > DESKTOP_BREADCRUMB_BREAKPOINT;

    const headerHeight = header ? header.getBoundingClientRect().height : 0;
    const breadcrumbHeight = isDesktop && breadcrumbs
      ? breadcrumbs.getBoundingClientRect().height
      : 0;
    const anchorOffset = headerHeight + breadcrumbHeight;

    root.style.setProperty("--site-header-height", String(headerHeight) + "px");
    root.style.setProperty("--breadcrumb-height", String(breadcrumbHeight) + "px");
    root.style.setProperty("--anchor-offset", String(anchorOffset) + "px");
  }

  function readAnchorOffset() {
    const root = document.documentElement;
    if (!root) {
      return 0;
    }
    const rawValue = window.getComputedStyle(root).getPropertyValue("--anchor-offset");
    const parsed = Number.parseFloat(rawValue);
    return Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
  }

  function initSinglePageChapterUi() {
    if (!document.body || document.body.dataset.singlePageMode !== "true") {
      return;
    }

    const chapterCurrent = document.getElementById("chapter-breadcrumb-current");
    const chapterTrail = document.getElementById("chapter-breadcrumb-trail");
    const navLinks = Array.from(document.querySelectorAll(".sidebar-nav .nav-link[data-chapter-id]"));
    const collapsibleItems = Array.from(document.querySelectorAll(".sidebar-nav .nav-item.is-collapsible"));
    const sidebarRoot = document.querySelector(".sidebar-nav > .nav-list");
    if (!chapterCurrent || !chapterTrail || !sidebarRoot || navLinks.length === 0) {
      return;
    }

    const chapterPathById = new Map();
    const chapterTitleById = new Map();

    function collectChapterPaths(listElement, ancestors) {
      const items = Array.from(listElement.children).filter((child) => child.matches("li.nav-item"));
      for (const item of items) {
        let currentPath = ancestors;
        const children = Array.from(item.children);
        const chapterLink = children.find((child) => child.matches(".nav-link[data-chapter-id]"));

        if (chapterLink) {
          const chapterId = (chapterLink.dataset.chapterId || "").trim();
          const chapterTitle = (chapterLink.dataset.chapterTitle || "").trim();
          if (chapterId && chapterTitle) {
            currentPath = ancestors.concat([{ id: chapterId, title: chapterTitle }]);
            if (!chapterPathById.has(chapterId)) {
              chapterPathById.set(chapterId, currentPath);
            }
            if (!chapterTitleById.has(chapterId)) {
              chapterTitleById.set(chapterId, chapterTitle);
            }
          }
        }

        const childLists = children.filter((child) => child.matches(".nav-list"));
        for (const childList of childLists) {
          collectChapterPaths(childList, currentPath);
        }
      }
    }

    collectChapterPaths(sidebarRoot, []);

    for (const link of navLinks) {
      const chapterId = (link.dataset.chapterId || "").trim();
      const chapterTitle = (link.dataset.chapterTitle || "").trim();
      if (!chapterId || !chapterTitle) {
        continue;
      }
      if (!chapterTitleById.has(chapterId)) {
        chapterTitleById.set(chapterId, chapterTitle);
      }
      if (!chapterPathById.has(chapterId)) {
        chapterPathById.set(chapterId, [{ id: chapterId, title: chapterTitle }]);
      }
    }

    if (chapterPathById.size === 0) {
      return;
    }

    const initialChapterId = (document.body.dataset.initialChapterId || "").trim();
    const sectionByChapterId = new Map();
    const contentSections = Array.from(document.querySelectorAll(".doc-content [id]"));
    for (const section of contentSections) {
      if (!section.id || !chapterPathById.has(section.id)) {
        continue;
      }
      if (!sectionByChapterId.has(section.id)) {
        sectionByChapterId.set(section.id, section);
      }
    }

    const chapterIdsInDocumentOrder = Array.from(sectionByChapterId.keys());
    let activeChapterId = "";
    let rafId = 0;
    let viewportObserver = null;
    let fallbackScrollHandler = null;

    function findToggle(item) {
      const entry = Array.from(item.children).find((child) => child.matches(".nav-entry"));
      if (!entry) {
        return null;
      }
      return entry.querySelector(".nav-toggle");
    }

    function findPanel(item) {
      return Array.from(item.children)
        .find((child) => child.matches(".nav-list[data-collapsible-panel='true']")) || null;
    }

    function setBranchExpanded(item, expanded) {
      if (!item) {
        return;
      }
      item.classList.toggle("is-expanded", expanded);
      item.classList.toggle("is-collapsed", !expanded);

      const toggle = findToggle(item);
      if (toggle) {
        toggle.setAttribute("aria-expanded", expanded ? "true" : "false");
      }

      const panel = findPanel(item);
      if (panel) {
        panel.hidden = !expanded;
      }
    }

    function bindCollapsibleToggles() {
      for (const item of collapsibleItems) {
        const toggle = findToggle(item);
        if (!toggle) {
          continue;
        }
        toggle.addEventListener("click", (event) => {
          event.preventDefault();
          const expanded = toggle.getAttribute("aria-expanded") === "true";
          setBranchExpanded(item, !expanded);
        });
      }
    }

    const collapsibleByChapterId = new Map();
    for (const link of navLinks) {
      const chapterId = (link.dataset.chapterId || "").trim();
      if (!chapterId || collapsibleByChapterId.has(chapterId)) {
        continue;
      }
      const branch = link.closest("li.nav-item.is-collapsible");
      if (branch) {
        collapsibleByChapterId.set(chapterId, branch);
      }
    }

    function syncExpandedBranchesForChapter(chapterId) {
      const activeBranch = chapterId ? (collapsibleByChapterId.get(chapterId) || null) : null;
      for (const item of collapsibleItems) {
        setBranchExpanded(item, item === activeBranch);
      }
    }

    function initCollapsibleState() {
      for (const item of collapsibleItems) {
        setBranchExpanded(item, false);
      }
      bindCollapsibleToggles();
    }

    function setActiveChapter(chapterId) {
      for (const link of navLinks) {
        if ((link.dataset.chapterId || "").trim() === chapterId) {
          link.classList.add("active");
        } else {
          link.classList.remove("active");
        }
      }
    }

    function setChapterUi(chapterId) {
      if (!chapterId || !chapterPathById.has(chapterId)) {
        return;
      }
      syncExpandedBranchesForChapter(chapterId);
      if (chapterId === activeChapterId) {
        return;
      }
      activeChapterId = chapterId;
      renderChapterTrail(chapterId);
      setActiveChapter(chapterId);
    }

    function appendSeparator(container) {
      const separator = document.createElement("span");
      separator.className = "separator";
      separator.textContent = "/";
      container.appendChild(separator);
    }

    function renderChapterTrail(chapterId) {
      const path = chapterPathById.get(chapterId) || [];
      const visiblePath = path.length > SINGLE_PAGE_BREADCRUMB_DEPTH
        ? path.slice(path.length - SINGLE_PAGE_BREADCRUMB_DEPTH)
        : path;

      chapterTrail.innerHTML = "";
      if (visiblePath.length === 0) {
        chapterCurrent.textContent = chapterTitleById.get(chapterId) || chapterCurrent.textContent;
        chapterTrail.appendChild(chapterCurrent);
        return;
      }

      for (let i = 0; i < visiblePath.length; i++) {
        const chapter = visiblePath[i];
        const isLast = i === visiblePath.length - 1;

        if (i > 0) {
          appendSeparator(chapterTrail);
        }

        if (isLast) {
          const current = document.createElement("span");
          current.className = "current";
          current.id = "chapter-breadcrumb-current";
          current.textContent = chapter.title;
          chapterTrail.appendChild(current);
        } else {
          const link = document.createElement("a");
          link.href = "#" + encodeURIComponent(chapter.id);
          link.textContent = chapter.title;
          chapterTrail.appendChild(link);
        }
      }
    }

    function chapterFromViewport() {
      if (chapterIdsInDocumentOrder.length === 0) {
        return "";
      }
      const anchorOffset = readAnchorOffset();
      let candidate = chapterIdsInDocumentOrder[0];
      for (const chapterId of chapterIdsInDocumentOrder) {
        const section = sectionByChapterId.get(chapterId);
        if (!section) {
          continue;
        }
        const top = section.getBoundingClientRect().top;
        if ((top - anchorOffset) <= 1) {
          candidate = chapterId;
        } else {
          break;
        }
      }
      return candidate;
    }

    function scheduleViewportSync() {
      if (rafId !== 0) {
        return;
      }
      rafId = window.requestAnimationFrame(() => {
        rafId = 0;
        const viewportChapterId = chapterFromViewport();
        if (viewportChapterId) {
          setChapterUi(viewportChapterId);
        }
      });
    }

    function bindViewportObserver() {
      if (!("IntersectionObserver" in window) || chapterIdsInDocumentOrder.length === 0) {
        return false;
      }
      if (viewportObserver) {
        viewportObserver.disconnect();
      }
      const topMargin = Math.ceil(readAnchorOffset()) + 8;
      viewportObserver = new IntersectionObserver(
        () => {
          scheduleViewportSync();
        },
        {
          root: null,
          rootMargin: "-" + String(topMargin) + "px 0px -60% 0px",
          threshold: [0, 0.01, 0.25]
        }
      );
      for (const chapterId of chapterIdsInDocumentOrder) {
        const section = sectionByChapterId.get(chapterId);
        if (section) {
          viewportObserver.observe(section);
        }
      }
      return true;
    }

    function ensureViewportTracking() {
      if (fallbackScrollHandler) {
        window.removeEventListener("scroll", fallbackScrollHandler);
        fallbackScrollHandler = null;
      }
      const observerBound = bindViewportObserver();
      if (!observerBound) {
        fallbackScrollHandler = () => {
          scheduleViewportSync();
        };
        window.addEventListener("scroll", fallbackScrollHandler, { passive: true });
      }
    }

    function applyChapterFromLocation() {
      const chapterIdFromHash = decodeHashChapterId();
      if (chapterIdFromHash && chapterPathById.has(chapterIdFromHash)) {
        setChapterUi(chapterIdFromHash);
        return;
      }
      const chapterIdFromViewport = chapterFromViewport();
      if (chapterIdFromViewport) {
        setChapterUi(chapterIdFromViewport);
        return;
      }
      const fallbackChapterId = chapterPathById.has(initialChapterId)
        ? initialChapterId
        : (chapterPathById.keys().next().value || "");
      if (fallbackChapterId && chapterPathById.has(fallbackChapterId)) {
        setChapterUi(fallbackChapterId);
      }
    }

    initCollapsibleState();
    ensureViewportTracking();
    window.addEventListener("hashchange", applyChapterFromLocation);
    window.addEventListener("resize", () => {
      syncStickyOffsets();
      ensureViewportTracking();
      scheduleViewportSync();
    });
    applyChapterFromLocation();
    scheduleViewportSync();
  }

  function init() {
    const searchState = parseSearchState();
    syncSearchFormState(searchState);
    syncStickyOffsets();
    window.addEventListener("resize", syncStickyOffsets);
    window.addEventListener("hashchange", syncStickyOffsets);
    initSearchPage(searchState);
    initSinglePageChapterUi();
    initDocumentHighlights(searchState.query);
    syncStickyOffsets();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
