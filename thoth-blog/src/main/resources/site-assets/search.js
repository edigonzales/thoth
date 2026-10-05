(() => {
  const messages = document.body.dataset;
  function text(key, query = "") {
    return (messages[key] || "").replace("{query}", query);
  }

  function initLanguageSwitcher() {
    const select = document.getElementById("language-switch");
    if (!select) return;
    select.addEventListener("change", () => {
      const target = new URL(select.value, window.location.href);
      const query = new URLSearchParams(window.location.search).get("q");
      if (target.pathname.endsWith("/search.html") && query) target.searchParams.set("q", query);
      window.location.href = target.href;
    });
  }

  function ensureSearchInputSync(query) {
    const input = document.getElementById("search-input");
    if (input && query) {
      input.value = query;
    }
  }

  function parseQuery() {
    const params = new URLSearchParams(window.location.search);
    return (params.get("q") || "").trim();
  }

  function createResultElement(post) {
    const item = document.createElement("li");

    const title = document.createElement("a");
    title.className = "post-title";
    title.href = post.url;
    title.textContent = post.title;
    if (post.language) title.lang = post.language;

    const date = document.createElement("div");
    date.className = "post-date";
    date.textContent = post.date;

    item.appendChild(title);
    item.appendChild(date);
    if (post.language && post.language !== document.documentElement.lang) {
      const notice = document.createElement("span");
      notice.className = "language-notice";
      notice.textContent = `${text("onlyAvailable")} ${post.languageName || post.language}`;
      item.appendChild(notice);
    }
    return item;
  }

  function renderMessage(container, message) {
    container.innerHTML = "";
    const p = document.createElement("p");
    p.className = "teaser";
    p.textContent = message;
    container.appendChild(p);
  }

  function renderResults(container, query, results) {
    container.innerHTML = "";

    if (results.length === 0) {
      renderMessage(container, text("noResults", query));
      return;
    }

    const list = document.createElement("ul");
    for (const post of results) {
      list.appendChild(createResultElement(post));
    }
    container.appendChild(list);
  }

  function fallbackSearch(query, documents) {
    const normalized = query.toLowerCase();
    return documents.filter((doc) => {
      const blob = `${doc.title} ${doc.tags} ${doc.teaser} ${doc.body}`.toLowerCase();
      return blob.includes(normalized);
    });
  }

  function lunrSearch(query, documents) {
    const docsByUrl = new Map();
    documents.forEach((doc) => docsByUrl.set(doc.url, doc));

    const index = window.lunr(function () {
      if (messages.multilingual === "true" || document.documentElement.lang.startsWith("de")) {
        this.pipeline.remove(window.lunr.stemmer, window.lunr.stopWordFilter);
        this.searchPipeline.remove(window.lunr.stemmer);
      }
      this.ref("url");
      this.field("title");
      this.field("tags");
      this.field("teaser");
      this.field("body");
      documents.forEach((doc) => this.add(doc));
    });

    let results = [];
    try {
      results = index.search(query);
    } catch (error) {
      const wildcardQuery = query
        .split(/\s+/)
        .filter(Boolean)
        .map((token) => `${token}*`)
        .join(" ");
      if (wildcardQuery) {
        try {
          results = index.search(wildcardQuery);
        } catch (ignored) {
          results = [];
        }
      }
    }

    return results
      .map((entry) => docsByUrl.get(entry.ref))
      .filter(Boolean);
  }

  function initSearchPage() {
    const container = document.getElementById("search-results");
    if (!container) {
      return;
    }

    const query = parseQuery();
    ensureSearchInputSync(query);

    const queryElement = document.getElementById("search-query");
    if (queryElement) {
      queryElement.textContent = query ? text("resultsFor", query) : text("enterQuery");
    }

    if (!query) {
      renderMessage(container, text("enterSearchTerm"));
      return;
    }

    fetch(messages.searchIndexUrl || "/assets/search-index.json")
      .then((response) => {
        if (!response.ok) {
          throw new Error("search-index fetch failed");
        }
        return response.json();
      })
      .then((documents) => {
        const results = (window.lunr && typeof window.lunr === "function")
          ? lunrSearch(query, documents)
          : fallbackSearch(query, documents);
        renderResults(container, query, results);
      })
      .catch(() => renderMessage(container, text("searchFailed")));
  }

  function initPage() {
    initLanguageSwitcher();
    initSearchPage();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initPage);
  } else {
    initPage();
  }

})();
