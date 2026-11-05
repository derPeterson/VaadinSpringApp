(() => {
  const groups = new WeakMap();
  const sizing = ['width', 'min-width', 'max-width', 'flex'];
  const hasText = button => [...button.childNodes].some(node => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent.trim().length > 0;
    return node.nodeType === Node.ELEMENT_NODE
      && !node.matches('svg,img,vaadin-icon,[slot=prefix],[slot=suffix],[aria-hidden=true],[hidden]')
      && node.textContent.trim().length > 0;
  });

  function observe(root) {
    if (groups.has(root)) return;
    const originals = new Map();
    const expectedStyles = new WeakMap();
    let pending = false;
    let previousWidth = -1;
    const restore = button => {
      const values = originals.get(button);
      if (!values) return;
      for (const [name, value, priority] of values) {
        if (value) button.style.setProperty(name, value, priority);
        else button.style.removeProperty(name);
      }
    };
    const update = () => {
      pending = false;
      if (!root.isConnected || !root.getClientRects().length || root.hidden) return;
      // Nur die Aktionsleiste bekommt einen Inhaltsumbruch, keine Karten oder Icongruppen.
      if (root.matches('.cosmos-dialog-actions:not(.cosmos-notification-detail-actions),.portal-modal-foot')) {
        root.style.setProperty('display', 'flex', 'important');
        root.style.setProperty('flex-wrap', 'wrap', 'important');
      }
      const buttons = [...root.querySelectorAll('vaadin-button,button')];
      buttons.forEach(restore);
      const textButtons = buttons.filter(button => {
        const style = getComputedStyle(button);
        return hasText(button) && !button.hidden && button.getClientRects().length
          && style.display !== 'none' && style.visibility === 'visible';
      });
      for (const button of textButtons) {
        if (!originals.has(button)) {
          originals.set(button, sizing.map(name =>
            [name, button.style.getPropertyValue(name), button.style.getPropertyPriority(name)]));
        }
        // Innerhalb desselben Frames messen und wiederherstellen: keine Zwischenanzeige.
        button.style.setProperty('width', 'max-content', 'important');
        button.style.setProperty('min-width', '0', 'important');
        button.style.setProperty('max-width', 'none', 'important');
        button.style.setProperty('flex', 'none', 'important');
      }
      const width = Math.ceil(Math.max(0, ...textButtons.map(button => button.offsetWidth + 1)));
      for (const button of textButtons) {
        restore(button);
        button.style.setProperty('width', `${width}px`, 'important');
        button.style.setProperty('min-width', '0', 'important');
        button.style.setProperty('max-width', '100%', 'important');
        button.style.setProperty('flex', '0 0 auto', 'important');
      }
      for (const button of originals.keys()) {
        if (!root.contains(button)) originals.delete(button);
      }
      buttons.forEach(button => expectedStyles.set(button, button.getAttribute('style')));
      expectedStyles.set(root, root.getAttribute('style'));
    };
    const schedule = () => {
      if (!pending) {
        pending = true;
        requestAnimationFrame(update);
      }
    };
    const changes = new MutationObserver(records => {
      if (records.some(record => record.attributeName !== 'style'
          || expectedStyles.get(record.target) !== record.target.getAttribute('style'))) schedule();
    });
    changes.observe(root, {subtree:true, childList:true, characterData:true, attributes:true,
      attributeFilter:['hidden','class','theme','style']});
    const resize = new ResizeObserver(entries => {
      const width = entries[0].contentRect.width;
      if (width !== previousWidth) {
        previousWidth = width;
        schedule();
      }
    });
    resize.observe(root);
    groups.set(root, {changes, resize});
    document.fonts?.ready.then(schedule);
    root.addEventListener('load', schedule, true);
    schedule();
  }
  window.cosmosEqualButtonWidths = {observe};

  const initializeReference = () => {
    document.querySelectorAll('dialog .portal-modal-foot,dialog .n-actions,dialog .portal-card-foot,.external-connections-overview,.portal-reference .portal-home-actions')
      .forEach(observe);
  };
  if (document.querySelector('body.portal-reference,body.admin-reference,body.public-reference')) {
    initializeReference();
    new MutationObserver(initializeReference).observe(document.body, {childList:true, subtree:true});
  }
})();
