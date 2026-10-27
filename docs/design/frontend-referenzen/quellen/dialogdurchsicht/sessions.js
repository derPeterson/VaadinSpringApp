(() => {
  const panel = document.querySelector('[data-portal-page="sessions"]');
  const dialog = document.getElementById('session-confirm');
  const byId = suffix => document.getElementById(`session-confirm${suffix}`);
  let entries = [...panel.querySelectorAll('[data-session-id]')].filter(node => node.matches('article'));
  let pending = [];
  let returnFocus = null;
  const feedback = text => document.dispatchEvent(new CustomEvent('portal-feedback', {detail: text}));

  function contextCard(row) {
    const card = document.createElement('div');
    card.className = 'portal-confirm-context';
    const device = document.createElement('span');
    device.className = 'portal-confirm-object';
    device.setAttribute('aria-hidden', 'true');
    device.append(row.querySelector('.session-device svg').cloneNode(true));
    const copy = document.createElement('div');
    copy.className = 'portal-confirm-text';
    const title = document.createElement('strong');
    title.textContent = row.querySelector('h3').textContent;
    copy.append(title, row.querySelector('.portal-copy').cloneNode(true), row.querySelector('.session-badges').cloneNode(true));
    card.append(device, copy);
    return card;
  }

  function render() {
    const active = entries.filter(row => row.dataset.sessionState !== 'saved').length;
    panel.querySelector('[data-session-active-count]').textContent = String(active);
    panel.querySelector('[data-session-active-caption]').textContent = active === 1 ? 'aktive Sitzung' : 'aktive Sitzungen';
    panel.querySelector('[data-session-action="revoke-all"]').disabled = !entries.some(row => row.dataset.sessionState !== 'current');
  }

  function openConfirmation(button) {
    returnFocus = button;
    const all = button.dataset.sessionAction === 'revoke-all';
    pending = entries.filter(row => row.dataset.sessionState !== 'current' && (all || row.dataset.sessionId === button.dataset.sessionId));
    if (!pending.length) { return; }
    document.dispatchEvent(new Event('portal-close-overlays'));
    document.dispatchEvent(new Event('cosmos-close-calendar'));
    const saved = pending.filter(row => row.dataset.sessionState === 'saved').length;
    const active = pending.length - saved;
    const savedOnly = !all && saved === 1;
    byId('-title').textContent = all ? 'Alle anderen abmelden' : savedOnly ? 'Gespeicherte Anmeldung widerrufen' : 'Gerät abmelden';
    byId('-description').textContent = all
      ? 'Möchten Sie die anderen aktiven Sitzungen beenden und gespeicherte Anmeldungen widerrufen?'
      : savedOnly ? 'Auf diesem Gerät ist derzeit keine Sitzung aktiv. Möchten Sie die gespeicherte Anmeldung widerrufen?'
        : 'Möchten Sie die Sitzung auf diesem Gerät beenden?';
    byId('-context').className = 'portal-confirm-context-list';
    byId('-context').replaceChildren(...pending.map(contextCard));
    byId('-subtitle').textContent = all
      ? `${active} ${active === 1 ? 'aktive Sitzung' : 'aktive Sitzungen'} und ${saved} ${saved === 1 ? 'gespeicherte Anmeldung' : 'gespeicherte Anmeldungen'}`
      : 'Bitte prüfen Sie Ihre Auswahl.';
    byId('-hint').textContent = savedOnly
      ? 'Bei der nächsten Nutzung ist dort eine neue Anmeldung erforderlich. Ihre aktuelle Sitzung bleibt erhalten.'
      : 'Ihre aktuelle Sitzung bleibt angemeldet. Auf den betroffenen Geräten ist danach eine neue Anmeldung erforderlich.';
    byId('-label').textContent = all ? 'Alle anderen abmelden' : savedOnly ? 'Widerrufen' : 'Abmelden';
    dialog.showModal();
  }

  dialog.addEventListener('close', () => {
    pending = [];
    const fallback = panel.querySelector('[data-session-action="refresh"]');
    const target = returnFocus?.isConnected && !returnFocus.disabled && returnFocus.getClientRects().length ? returnFocus : fallback;
    if (target.getClientRects().length) { target.focus({preventScroll: true}); }
  });
  dialog.addEventListener('click', event => {
    if (event.target !== dialog) { return; }
    const bounds = dialog.getBoundingClientRect();
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) { dialog.close(); }
  });
  document.addEventListener('portal-close-overlays', () => { if (dialog.open) { dialog.close(); } });
  document.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) { return; }
    if (button.hasAttribute('data-session-close')) { dialog.close(); return; }
    const action = button.dataset.sessionAction;
    if (action === 'refresh') { render(); feedback('Gestaltungsvorschau: Liste aktualisiert. Die Beispieldaten bleiben erhalten.'); }
    if (action === 'revoke' || action === 'revoke-all') { openConfirmation(button); }
    if (action === 'confirm' && dialog.open) {
      const removed = new Set(pending);
      pending.forEach(row => { row.remove(); });
      entries = entries.filter(row => !removed.has(row));
      render();
      dialog.close();
      feedback('Gestaltungsvorschau: Auswahl nur in den Beispieldaten übernommen. Keine echte Sitzung wurde beendet.');
    }
  });
  render();
})();
