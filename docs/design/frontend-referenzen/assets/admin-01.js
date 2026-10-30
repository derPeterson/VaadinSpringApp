(() => {
  // Gemeinsame Overlaygeometrie für alle lokalen Vorschau-Komponenten.
  const positionOverlay = (overlay, trigger, requestedWidth, align = requestedWidth ? 'end' : 'start') => {
    if (overlay.hidden) { return; }
    const bounds = trigger.getBoundingClientRect();
    const width = Math.min(requestedWidth || bounds.width, innerWidth - 24);
    const topLimit = Math.max(12, document.querySelector('header').getBoundingClientRect().bottom + 8);
    overlay.style.width = `${width}px`;
    overlay.style.maxHeight = `${Math.max(80, innerHeight - topLimit - 12)}px`;
    overlay.style.left = `${Math.max(12, Math.min(align === 'end' ? bounds.right - width : bounds.left, innerWidth - width - 12))}px`;
    overlay.style.right = 'auto';
    const height = Math.min(overlay.offsetHeight, innerHeight - topLimit - 12);
    const below = bounds.bottom + 6;
    const above = bounds.top - height - 6;
    overlay.style.top = `${below + height <= innerHeight - 12 ? below : above >= topLimit ? above : Math.max(topLimit, innerHeight - height - 12)}px`;
  };
  window.CosmosPreview = {positionOverlay};
})();