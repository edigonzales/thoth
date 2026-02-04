(() => {
  const STORAGE_KEY = "thoth-theme";
  const LIGHT = "light";
  const DARK = "dark";

  function safeLocalStorageGet(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }

  function safeLocalStorageSet(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (error) {
      // ignore storage failures
    }
  }

  function preferredTheme() {
    if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
      return DARK;
    }
    return LIGHT;
  }

  function currentTheme() {
    return document.documentElement.dataset.theme || LIGHT;
  }

  function applyTheme(theme) {
    const stylesheet = document.getElementById("theme-style");
    if (!stylesheet) {
      return;
    }

    const nextTheme = theme === DARK ? DARK : LIGHT;
    const lightHref = stylesheet.getAttribute("data-light");
    const darkHref = stylesheet.getAttribute("data-dark");

    stylesheet.setAttribute("href", nextTheme === DARK ? darkHref : lightHref);
    document.documentElement.dataset.theme = nextTheme;

    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
      toggle.textContent = nextTheme === DARK ? "Light" : "Dark";
    }
  }

  function toggleTheme() {
    const next = currentTheme() === DARK ? LIGHT : DARK;
    applyTheme(next);
    safeLocalStorageSet(STORAGE_KEY, next);
  }

  document.addEventListener("DOMContentLoaded", () => {
    const stored = safeLocalStorageGet(STORAGE_KEY);
    applyTheme(stored || preferredTheme());

    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
      toggle.addEventListener("click", toggleTheme);
    }
  });
})();
