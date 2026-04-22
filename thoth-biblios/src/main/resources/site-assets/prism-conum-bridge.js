(() => {
  if (typeof Prism === "undefined" || typeof document === "undefined") {
    return;
  }

  const MARKER_PREFIX = "THOTHCONUMMARKER";
  const MARKER_SUFFIX = "TOKEN";
  const conumMarkersByElement = new WeakMap();

  function isListingCodeElement(element) {
    return !!(
      element &&
      element.tagName === "CODE" &&
      element.parentElement &&
      element.parentElement.tagName === "PRE" &&
      element.closest(".listingblock")
    );
  }

  function isConumElement(node) {
    return !!(
      node &&
      node.nodeType === Node.ELEMENT_NODE &&
      node.tagName === "I" &&
      node.classList.contains("conum")
    );
  }

  function isConumFallbackElement(node, value) {
    if (!node || node.nodeType !== Node.ELEMENT_NODE || node.tagName !== "B") {
      return false;
    }
    const expected = "(" + value + ")";
    return (node.textContent || "").trim() === expected;
  }

  function toAlphabeticIndex(index) {
    let value = index;
    let result = "";
    do {
      result = String.fromCharCode(65 + (value % 26)) + result;
      value = Math.floor(value / 26) - 1;
    } while (value >= 0);
    return result;
  }

  function createMarker(index) {
    return MARKER_PREFIX + toAlphabeticIndex(index) + MARKER_SUFFIX;
  }

  function serializeNodeContent(node, markers) {
    if (!node) {
      return "";
    }
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent || "";
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return "";
    }

    let output = "";
    const children = node.childNodes;
    for (let i = 0; i < children.length; i += 1) {
      const child = children[i];
      if (isConumElement(child)) {
        const value = (child.getAttribute("data-value") || "").trim();
        if (value) {
          const marker = createMarker(markers.length);
          markers.push({ marker, value });
          output += marker;
          const nextSibling = children[i + 1];
          if (isConumFallbackElement(nextSibling, value)) {
            i += 1;
          }
          continue;
        }
      }
      output += serializeNodeContent(child, markers);
    }
    return output;
  }

  function escapeHtml(value) {
    return value
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function conumHtml(value) {
    const safeValue = escapeHtml(value);
    return `<i class="conum" data-value="${safeValue}" aria-hidden="true"></i><b>(${safeValue})</b>`;
  }

  Prism.hooks.add("before-sanity-check", (env) => {
    const codeElement = env.element;
    if (!isListingCodeElement(codeElement)) {
      return;
    }
    if (!codeElement.querySelector("i.conum[data-value]")) {
      return;
    }

    const markers = [];
    const rawCode = serializeNodeContent(codeElement, markers);
    if (markers.length === 0) {
      return;
    }

    conumMarkersByElement.set(codeElement, markers);
    env.code = rawCode;
  });

  Prism.hooks.add("complete", (env) => {
    const codeElement = env.element;
    const markers = conumMarkersByElement.get(codeElement);
    if (!markers || markers.length === 0) {
      return;
    }

    let html = codeElement.innerHTML;
    for (const marker of markers) {
      html = html.split(marker.marker).join(conumHtml(marker.value));
    }
    codeElement.innerHTML = html;
    conumMarkersByElement.delete(codeElement);
  });
})();
