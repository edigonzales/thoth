(() => {
  let activeTrigger = null;

  function isActivationKey(event) {
    return event.key === "Enter" || event.key === " ";
  }

  function closeLightbox(dialog) {
    if (dialog.open) {
      dialog.close();
    }
  }

  function handleBackdropClick(dialog, event) {
    if (event.target !== dialog) {
      return;
    }

    const bounds = dialog.getBoundingClientRect();
    const clickedOutside =
      event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom;

    if (clickedOutside) {
      closeLightbox(dialog);
    }
  }

  function openLightbox(dialog, dialogImage, image) {
    const source = image.getAttribute("src") || image.currentSrc;
    if (!source) {
      return;
    }

    activeTrigger = image;
    dialogImage.setAttribute("src", source);
    dialogImage.setAttribute("alt", image.getAttribute("alt") || "");

    if (!dialog.open) {
      dialog.showModal();
    }
  }

  function bindImage(dialog, dialogImage, image) {
    if (image.closest("a")) {
      return;
    }

    image.classList.add("lightbox-trigger");
    image.setAttribute("tabindex", "0");
    image.setAttribute("role", "button");
    image.setAttribute("aria-haspopup", "dialog");

    image.addEventListener("click", () => {
      openLightbox(dialog, dialogImage, image);
    });

    image.addEventListener("keydown", (event) => {
      if (!isActivationKey(event)) {
        return;
      }

      event.preventDefault();
      openLightbox(dialog, dialogImage, image);
    });
  }

  function initImageLightbox() {
    const dialog = document.getElementById("image-lightbox");
    const dialogImage = document.getElementById("image-lightbox-image");
    const closeButton = document.getElementById("image-lightbox-close");

    if (!dialog || !dialogImage || !closeButton) {
      return;
    }

    if (typeof dialog.showModal !== "function") {
      return;
    }

    document.querySelectorAll(".post-content img[src]").forEach((image) => {
      bindImage(dialog, dialogImage, image);
    });

    closeButton.addEventListener("click", () => {
      closeLightbox(dialog);
    });

    dialog.addEventListener("click", (event) => {
      handleBackdropClick(dialog, event);
    });

    dialog.addEventListener("close", () => {
      dialogImage.removeAttribute("src");
      dialogImage.setAttribute("alt", "");

      if (activeTrigger && document.contains(activeTrigger)) {
        activeTrigger.focus();
      }
      activeTrigger = null;
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initImageLightbox);
  } else {
    initImageLightbox();
  }
})();
