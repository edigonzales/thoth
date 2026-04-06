(() => {
  const STORAGE_KEY = "thoth-theme";
  const LIGHT = "light";
  const DARK = "dark";
  const SYSTEM = "system";
  const mediaQuery = window.matchMedia ? window.matchMedia("(prefers-color-scheme: dark)") : null;

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
    if (mediaQuery && mediaQuery.matches) {
      return DARK;
    }
    return LIGHT;
  }

  function normalizeMode(mode) {
    if (mode === LIGHT || mode === DARK || mode === SYSTEM) {
      return mode;
    }
    return SYSTEM;
  }

  function currentMode() {
    return document.documentElement.dataset.themeMode || SYSTEM;
  }

  function resolvedTheme(mode) {
    if (mode === LIGHT || mode === DARK) {
      return mode;
    }
    return preferredTheme();
  }

  function applyTheme(mode) {
    const stylesheet = document.getElementById("theme-style");
    if (!stylesheet) {
      return;
    }

    const normalizedMode = normalizeMode(mode);
    const nextTheme = resolvedTheme(normalizedMode);
    const lightHref = stylesheet.getAttribute("data-light");
    const darkHref = stylesheet.getAttribute("data-dark");

    stylesheet.setAttribute("href", nextTheme === DARK ? darkHref : lightHref);
    document.documentElement.dataset.theme = nextTheme;
    document.documentElement.dataset.themeMode = normalizedMode;

    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
      toggle.setAttribute("title", `${normalizedMode} mode`);
      toggle.setAttribute(
        "aria-label",
        `Switch between dark and light mode (currently ${normalizedMode} mode)`
      );
    }
  }

  function toggleTheme() {
    const mode = currentMode();
    const next = mode === SYSTEM ? LIGHT : mode === LIGHT ? DARK : SYSTEM;
    applyTheme(next);
    safeLocalStorageSet(STORAGE_KEY, next);
  }

  document.addEventListener("DOMContentLoaded", () => {
    const stored = normalizeMode(safeLocalStorageGet(STORAGE_KEY));
    applyTheme(stored);

    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
      toggle.addEventListener("click", toggleTheme);
    }
  });

  if (mediaQuery) {
    const handler = () => {
      if (currentMode() === SYSTEM) {
        applyTheme(SYSTEM);
      }
    };
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener("change", handler);
    } else if (mediaQuery.addListener) {
      mediaQuery.addListener(handler);
    }
  }
})();
