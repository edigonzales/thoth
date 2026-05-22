(() => {
  const COPY_ICON = `
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-copy" viewBox="0 0 16 16" aria-hidden="true">
  <path fill-rule="evenodd" d="M4 2a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V2zm2-1a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H6z"/>
  <path d="M2 5a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1v-1h1v1a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h1v1H2z"/>
</svg>`;
  const CHECK_ICON = `
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-check" viewBox="0 0 16 16" aria-hidden="true">
  <path d="M10.97 4.97a.75.75 0 0 1 1.07 1.05l-3.992 4.99a.75.75 0 0 1-1.08.02L4.324 8.384a.75.75 0 1 1 1.06-1.06l2.094 2.093 3.473-4.425z"/>
</svg>`;
  const COPY_LABEL = "Copy code block content";
  const COPIED_LABEL = "Copied code block content";
  const COPY_FAILED_LABEL = "Copy failed";
  const RESET_DELAY_MS = 1800;

  function fallbackCopy(text) {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.setAttribute("readonly", "");
    textarea.style.position = "fixed";
    textarea.style.top = "-9999px";
    document.body.appendChild(textarea);
    textarea.select();
    textarea.setSelectionRange(0, text.length);

    let ok = false;
    try {
      ok = document.execCommand("copy");
    } catch (error) {
      ok = false;
    }

    document.body.removeChild(textarea);
    return ok;
  }

  async function copyToClipboard(text) {
    if (navigator.clipboard && typeof navigator.clipboard.writeText === "function") {
      try {
        await navigator.clipboard.writeText(text);
        return true;
      } catch (error) {
        return fallbackCopy(text);
      }
    }

    return fallbackCopy(text);
  }

  function setIconState(button, iconMarkup, title) {
    button.innerHTML = iconMarkup;
    button.setAttribute("aria-label", title);
    button.setAttribute("title", title);
  }

  function setTemporarySuccessState(button) {
    setIconState(button, CHECK_ICON, COPIED_LABEL);
    window.setTimeout(() => {
      setIconState(button, COPY_ICON, COPY_LABEL);
    }, RESET_DELAY_MS);
  }

  function createCopyButton() {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "code-copy-button";
    setIconState(button, COPY_ICON, COPY_LABEL);
    return button;
  }

  function ensureCodeBlockShell(pre) {
    const parent = pre.parentElement;
    if (!parent) {
      return null;
    }

    if (parent.classList.contains("code-block-shell")) {
      return parent;
    }

    const shell = document.createElement("div");
    shell.className = "code-block-shell";
    parent.insertBefore(shell, pre);
    shell.appendChild(pre);
    return shell;
  }

  function attachCopyButton(pre) {
    const code = pre.querySelector("code");
    if (!code) {
      return;
    }

    const shell = ensureCodeBlockShell(pre);
    if (!shell || shell.querySelector(".code-copy-button")) {
      return;
    }

    const button = createCopyButton();
    button.addEventListener("click", async () => {
      const ok = await copyToClipboard(code.textContent || "");
      if (ok) {
        setTemporarySuccessState(button);
      } else {
        setIconState(button, COPY_ICON, COPY_FAILED_LABEL);
        window.setTimeout(() => {
          setIconState(button, COPY_ICON, COPY_LABEL);
        }, RESET_DELAY_MS);
      }
    });

    shell.appendChild(button);
  }

  function initCopyButtons() {
    document.querySelectorAll('pre[class*="language-"]').forEach(attachCopyButton);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initCopyButtons);
  } else {
    initCopyButtons();
  }
})();
