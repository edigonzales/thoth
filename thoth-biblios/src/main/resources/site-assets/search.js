(() => {
  const SEARCH_INDEX_URL = "/search-index.json";

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

  function initSinglePageChapterUi() {
    if (!document.body || document.body.dataset.singlePageMode !== "true") {
      return;
    }

    const chapterCurrent = document.getElementById("chapter-breadcrumb-current");
    const navLinks = Array.from(document.querySelectorAll(".sidebar-nav .nav-link[data-chapter-id]"));
    if (!chapterCurrent || navLinks.length === 0) {
      return;
    }

    const chaptersById = new Map();
    for (const link of navLinks) {
      const chapterId = (link.dataset.chapterId || "").trim();
      const chapterTitle = (link.dataset.chapterTitle || "").trim();
      if (!chapterId || !chapterTitle) {
        continue;
      }
      if (!chaptersById.has(chapterId)) {
        chaptersById.set(chapterId, chapterTitle);
      }
    }

    if (chaptersById.size === 0) {
      return;
    }

    const initialChapterId = (document.body.dataset.initialChapterId || "").trim();

    function setActiveChapter(chapterId) {
      for (const link of navLinks) {
        if ((link.dataset.chapterId || "").trim() === chapterId) {
          link.classList.add("active");
        } else {
          link.classList.remove("active");
        }
      }
    }

    function applyChapterFromLocation() {
      let chapterId = decodeHashChapterId();
      if (!chapterId || !chaptersById.has(chapterId)) {
        chapterId = chaptersById.has(initialChapterId) ? initialChapterId : (chaptersById.keys().next().value || "");
      }
      if (!chapterId || !chaptersById.has(chapterId)) {
        return;
      }

      chapterCurrent.textContent = chaptersById.get(chapterId) || chapterCurrent.textContent;
      setActiveChapter(chapterId);
    }

    window.addEventListener("hashchange", applyChapterFromLocation);
    applyChapterFromLocation();
  }

  function init() {
    const query = parseQuery();
    ensureSearchInputSync(query);
    initSearchPage();
    initSinglePageChapterUi();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
