(() => {
  const SEARCH_INDEX_URL = "/search-index.json";
  const DESKTOP_BREADCRUMB_BREAKPOINT = 768;
  const SINGLE_PAGE_BREADCRUMB_DEPTH = 3;

  function parseQuery() {
    const params = new URLSearchParams(window.location.search);
    return (params.get("q") || "").trim();
  }

  function ensureSearchInputSync(query) {
    const input = document.getElementById("search-input");
    if (input) {
      input.value = query;
    }
  }

  function normalizeMode(rawMode) {
    return rawMode === "english_default" ? "english_default" : "multilingual_safe";
  }

  function readSearchLanguageMode() {
    const fromBody = document.body && document.body.dataset
      ? document.body.dataset.searchLanguageMode
      : "";
    return normalizeMode((fromBody || "").trim());
  }

  function renderMessage(container, message) {
    container.innerHTML = "";
    const p = document.createElement("p");
    p.className = "search-empty";
    p.textContent = message;
    container.appendChild(p);
  }

  function createSnippet(doc, query) {
    const content = (doc.content || "").replace(/\s+/g, " ").trim();
    if (!content) {
      return "";
    }

    const normalizedContent = content.toLowerCase();
    const normalizedQuery = query.toLowerCase();
    const hitAt = normalizedContent.indexOf(normalizedQuery);

    if (hitAt < 0) {
      return content.length > 220 ? content.slice(0, 220) + "..." : content;
    }

    const start = Math.max(0, hitAt - 80);
    const end = Math.min(content.length, hitAt + query.length + 120);
    let snippet = content.slice(start, end);
    if (start > 0) {
      snippet = "..." + snippet;
    }
    if (end < content.length) {
      snippet += "...";
    }
    return snippet;
  }

  function createResultElement(doc, query) {
    const article = document.createElement("article");
    article.className = "search-result";

    const heading = document.createElement("h2");
    heading.className = "search-result-title";

    const link = document.createElement("a");
    link.href = doc.route;
    link.textContent = doc.title || doc.route || "Untitled";
    heading.appendChild(link);

    const meta = document.createElement("p");
    meta.className = "search-result-meta";
    const component = doc.component || "unknown";
    const displayVersion = doc.displayVersion || doc.version || "unknown";
    meta.textContent = component + " · " + displayVersion + " · " + (doc.route || "");

    const snippet = createSnippet(doc, query);
    const teaser = document.createElement("p");
    teaser.className = "search-result-snippet";
    teaser.textContent = snippet;

    article.appendChild(heading);
    article.appendChild(meta);
    if (snippet) {
      article.appendChild(teaser);
    }

    return article;
  }

  function renderResults(container, query, results) {
    container.innerHTML = "";

    if (results.length === 0) {
      renderMessage(container, 'No results for "' + query + '".');
      return;
    }

    const list = document.createElement("div");
    list.className = "search-results-list";
    for (const doc of results) {
      list.appendChild(createResultElement(doc, query));
    }
    container.appendChild(list);
  }

  function fallbackSearch(query, documents) {
    const normalized = query.toLowerCase();
    return documents.filter((doc) => {
      const blob = [
        doc.title || "",
        doc.component || "",
        doc.displayVersion || "",
        doc.version || "",
        doc.route || "",
        doc.content || ""
      ].join(" ").toLowerCase();
      return blob.includes(normalized);
    });
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
    return results.map((entry) => docsByRef.get(entry.ref)).filter(Boolean);
  }

  function initSearchPage() {
    const resultsContainer = document.getElementById("search-results");
    if (!resultsContainer) {
      return;
    }

    const query = parseQuery();
    ensureSearchInputSync(query);

    const queryLabel = document.getElementById("search-query");
    if (queryLabel) {
      queryLabel.textContent = query
        ? 'Results for "' + query + '"'
        : "Enter a search term in the header search field.";
    }

    if (!query) {
      renderMessage(resultsContainer, "Enter a search term above.");
      return;
    }

    fetch(SEARCH_INDEX_URL)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load search index");
        }
        return response.json();
      })
      .then((documents) => {
        if (!Array.isArray(documents)) {
          throw new Error("Invalid search index format");
        }

        const mode = readSearchLanguageMode();
        let results;

        if (window.lunr && typeof window.lunr === "function") {
          try {
            results = lunrSearch(query, documents, mode);
          } catch (error) {
            results = fallbackSearch(query, documents);
          }
        } else {
          results = fallbackSearch(query, documents);
        }

        renderResults(resultsContainer, query, results);
      })
      .catch(() => {
        renderMessage(resultsContainer, "Search index could not be loaded.");
      });
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
    const query = parseQuery();
    ensureSearchInputSync(query);
    syncStickyOffsets();
    window.addEventListener("resize", syncStickyOffsets);
    window.addEventListener("hashchange", syncStickyOffsets);
    initSearchPage();
    initSinglePageChapterUi();
    syncStickyOffsets();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
