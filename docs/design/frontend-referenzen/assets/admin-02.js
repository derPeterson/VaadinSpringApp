(() => {
  let current = null;
  let timer = null;
  let returnFocus = null;
  const symbol = name => document.getElementById('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  const close = (focus = false) => {
    clearTimeout(timer);
    current?.remove();
    current = null;
    if (focus && returnFocus?.isConnected && returnFocus.getClientRects().length) { returnFocus.focus(); }
  };
  const show = (message, severity = 'normal', title = '', duration = 3000) => {
    close();
    returnFocus = document.activeElement;
    const box = document.createElement('aside');
    box.className = 'vp-toast';
    box.dataset.severity = severity;
    // Eine aktuelle Meldung pro Vorschau, ohne Fokusübernahme und ohne Layoutverschiebung.
    box.setAttribute('role', severity === 'error' ? 'alert' : 'status');
    box.setAttribute('aria-atomic', 'true');
    const badge = document.createElement('span');
    badge.className = 'vp-toast-icon';
    badge.innerHTML = symbol(severity === 'normal' ? 'info' : severity === 'warning' ? 'attention' : severity);
    const copy = document.createElement('div'); copy.className = 'vp-toast-copy';
    if (title) {
      const heading = document.createElement('p'); heading.className = 'vp-toast-title';
      heading.textContent = title; copy.append(heading);
    }
    const text = document.createElement('p'); text.className = 'vp-toast-message'; text.textContent = message;
    copy.append(text);
    const button = document.createElement('button'); button.type = 'button'; button.className = 'vp-toast-close';
    button.setAttribute('aria-label', 'Meldung schließen'); button.innerHTML = symbol('close');
    button.addEventListener('click', () => close(true));
    box.append(badge, copy, button); ([...document.querySelectorAll('dialog[open]')].at(-1) || document.body).append(box); current = box;
    const startTimer = () => {
      clearTimeout(timer);
      if (duration > 0) { timer = setTimeout(() => close(), duration); }
    };
    box.addEventListener('pointerenter', () => clearTimeout(timer));
    box.addEventListener('focusin', () => clearTimeout(timer));
    box.addEventListener('pointerleave', startTimer);
    box.addEventListener('focusout', event => { if (!box.contains(event.relatedTarget)) { startTimer(); } });
    startTimer();
  };
  document.addEventListener('keydown', event => {
    const openControl = document.querySelector('.au-options:not([hidden]),.reference-calendar:not([hidden])');
    if (event.key === 'Escape' && current && !openControl) { event.preventDefault(); close(current.contains(event.target)); }
  });
  window.PublicNotifications = {show, close};
})();
