(() => {
  function highlightCodeBlocks() {
    if (!window.hljs) {
      return;
    }

    if (typeof window.hljs.highlightAll === "function") {
      window.hljs.highlightAll();
      return;
    }

    if (typeof window.hljs.initHighlighting === "function") {
      window.hljs.initHighlighting();
      return;
    }

    if (typeof window.hljs.highlightBlock === "function") {
      document.querySelectorAll("pre code").forEach((block) => window.hljs.highlightBlock(block));
    }
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

    const date = document.createElement("div");
    date.className = "post-date";
    date.textContent = post.date;

    item.appendChild(title);
    item.appendChild(date);
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
      renderMessage(container, `No results for \"${query}\".`);
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
      queryElement.textContent = query ? `Results for \"${query}\"` : "Enter a query in the search field.";
    }

    if (!query) {
      renderMessage(container, "Enter a search term above.");
      return;
    }

    fetch("/assets/search-index.json")
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
      .catch(() => renderMessage(container, "Search index could not be loaded."));
  }

  document.addEventListener("DOMContentLoaded", () => {
    highlightCodeBlocks();
    initSearchPage();
  });
})();
