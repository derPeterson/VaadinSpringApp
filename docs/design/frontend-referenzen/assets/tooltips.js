(() => {
  if (document.getElementById('reference-tooltip')) { return; }
  const tooltip = document.createElement('div');
  tooltip.id = 'reference-tooltip';
  tooltip.className = 'ref-tooltip';
  tooltip.setAttribute('role', 'tooltip');
  tooltip.setAttribute('popover', 'manual');
  tooltip.hidden = true;
  document.body.append(tooltip);
  let active = null;
  let pending = null;
  let timer = null;
  const prepare = node => {
    if (node.hasAttribute('title')) {
      node.dataset.refTooltip = node.getAttribute('title');
      node.removeAttribute('title');
    }
  };
  const install = root => {
    if (root.nodeType !== Node.ELEMENT_NODE && root !== document) { return; }
    if (root.matches?.('[title]')) { prepare(root); }
    root.querySelectorAll('[title]').forEach(prepare);
  };
  const target = event => {
    const node = event.target.closest('[data-ref-tooltip],[data-tooltip],button[aria-label],a[aria-label]');
    if (!node || node.disabled || node.closest('[hidden]')) { return null; }
    const text = node.dataset.refTooltip || node.dataset.tooltip || node.getAttribute('aria-label');
    return text && (node.hasAttribute('data-ref-tooltip') || node.hasAttribute('data-tooltip') || text.trim() !== node.textContent.trim()) ? node : null;
  };
  const hide = () => {
    clearTimeout(timer);
    pending = null;
    if (active) {
      const ids = (active.getAttribute('aria-describedby') || '').split(/\s+/).filter(id => id && id !== tooltip.id);
      if (ids.length) { active.setAttribute('aria-describedby', ids.join(' ')); }
      else { active.removeAttribute('aria-describedby'); }
    }
    active = null;
    if (tooltip.matches(':popover-open')) { tooltip.hidePopover(); }
    tooltip.hidden = true;
  };
  const show = node => {
    hide();
    if (!node.isConnected) { return; }
    active = node;
    tooltip.textContent = node.dataset.refTooltip || node.dataset.tooltip || node.getAttribute('aria-label');
    const descriptions = (node.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
    node.setAttribute('aria-describedby', [...new Set([...descriptions, tooltip.id])].join(' '));
    tooltip.hidden = false;
    tooltip.showPopover();
    const trigger = node.getBoundingClientRect(), bounds = tooltip.getBoundingClientRect();
    const left = Math.max(16, Math.min(innerWidth - bounds.width - 16, trigger.left + (trigger.width - bounds.width) / 2));
    const above = trigger.top - bounds.height - 8;
    const top = above >= 8 ? above : Math.min(innerHeight - bounds.height - 8, trigger.bottom + 8);
    tooltip.style.left = `${left}px`;
    tooltip.style.top = `${Math.max(8, top)}px`;
  };
  document.addEventListener('pointerover', event => {
    if (event.pointerType === 'touch') { return; }
    const node = target(event);
    if (!node || node.contains(event.relatedTarget)) { return; }
    hide(); pending = node; timer = setTimeout(() => show(node), 500);
  });
  document.addEventListener('pointerout', event => {
    const node = active || pending;
    if (node?.contains(event.target) && !node.contains(event.relatedTarget)) { hide(); }
  });
  document.addEventListener('focusin', event => { const node = target(event); if (node) { show(node); } });
  document.addEventListener('focusout', hide);
  document.addEventListener('pointerdown', hide);
  document.addEventListener('keydown', event => { if (event.key === 'Escape') { hide(); } });
  document.addEventListener('scroll', hide, true);
  window.addEventListener('resize', hide);
  new MutationObserver(records => {
    for (const record of records) {
      if (record.type === 'attributes') { prepare(record.target); }
      else { record.addedNodes.forEach(install); }
    }
    if (active && !active.isConnected) { hide(); }
  }).observe(document.body, {subtree: true, childList: true, attributes: true, attributeFilter: ['title']});
  install(document);
})();
