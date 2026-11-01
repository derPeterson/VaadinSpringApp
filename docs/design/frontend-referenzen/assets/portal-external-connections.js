(() => {
  // Ausschließlich synthetische Gestaltungsvorschau, keine Anmeldung oder Kontoänderung.
  const tabs = [...document.querySelectorAll('.account-security-tabs [role=tab]')];
  const selectTab = tab => {
    tabs.forEach(item => {
      const selected = item === tab;
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
      document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
    });
  };
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => selectTab(tab));
    tab.addEventListener('keydown', event => {
      let next;
      if (event.key === 'ArrowRight') next = tabs[(index + 1) % tabs.length];
      if (event.key === 'ArrowLeft') next = tabs[(index + tabs.length - 1) % tabs.length];
      if (event.key === 'Home') next = tabs[0];
      if (event.key === 'End') next = tabs[tabs.length - 1];
      if (next) {
        event.preventDefault();
        tabs.forEach(item => { item.tabIndex = item === next ? 0 : -1; });
        next.focus();
      }
    });
  });
  const dialog = document.getElementById('externalIdentityDialog');
  const password = document.getElementById('externalProofPassword');
  const submit = document.getElementById('externalProofContinue');
  let pending;
  let returning;
  let completed;

  const open = (action, trigger, provider) => {
    pending = {action, provider};
    returning = trigger;
    password.value = '';
    password.removeAttribute('aria-invalid');
    const brand = provider === 'google' ? 'Google' : 'Microsoft';
    const title = provider ? `${brand} ${action === 'unlink' ? 'trennen' : 'verbinden'}`
      : action === 'password' ? 'Portalpasswort einrichten oder ändern' : 'E-Mail-Adresse ändern';
    document.getElementById('externalIdentityTitle').textContent = title;
    const symbol = dialog.querySelector('.portal-modal-head > .portal-icon');
    if (!symbol.dataset.original) symbol.dataset.original = symbol.innerHTML;
    if (provider) {
      symbol.replaceChildren(Object.assign(document.createElement('img'), {
        src: `assets/provider-${provider}${provider === 'google' ? '.png' : '.svg'}`,
        alt: '', width: 20, height: 20
      }));
    } else {
      symbol.innerHTML = symbol.dataset.original;
    }
    dialog.querySelector('.external-proof-ready').hidden = true;
    dialog.querySelector('[data-external-password-field]').hidden = false;
    dialog.querySelector('.external-proof-separator').hidden = false;
    dialog.querySelector('.external-proof-alternatives').hidden = false;
    dialog.querySelector('[data-external-warning]').hidden = action !== 'unlink';
    submit.querySelector('span').textContent = provider ? title : 'Weiter';
    submit.classList.toggle('danger', action === 'unlink');
    submit.classList.toggle('primary', action !== 'unlink');
    submit.querySelector('[data-proof-icon=confirm]').toggleAttribute('hidden', action === 'unlink');
    submit.querySelector('[data-proof-icon=unlink]').toggleAttribute('hidden', action !== 'unlink');
    dialog.showModal();
  };

  document.addEventListener('click', event => {
    const account = event.target.closest('[data-open-dialog="password"], [data-open-dialog="email"]');
    if (account) {
      const action = account.dataset.openDialog;
      if (completed === action) {
        completed = null;
        return;
      }
      event.preventDefault();
      event.stopImmediatePropagation();
      open(action, account);
      return;
    }
    const provider = event.target.closest('[data-external-provider]');
    if (provider) {
      const linked = provider.closest('[data-external-row]').dataset.connected === 'true';
      open(linked ? 'unlink' : 'link', provider, provider.dataset.externalProvider);
    }
  }, true);

  dialog.querySelector('[data-external-proof]').addEventListener('click', () => {
    dialog.querySelector('.external-proof-ready').hidden = false;
    dialog.querySelector('[data-external-password-field]').hidden = true;
    dialog.querySelector('.external-proof-separator').hidden = true;
    dialog.querySelector('.external-proof-alternatives').hidden = true;
  });
  submit.addEventListener('click', () => {
    const proofReady = !dialog.querySelector('.external-proof-ready').hidden;
    if (!proofReady && !password.value) {
      password.setAttribute('aria-invalid', 'true');
      document.dispatchEvent(new CustomEvent('portal-feedback', {
        detail: 'Bitte füllen Sie alle erforderlichen Felder aus.'
      }));
      return;
    }
    const action = pending;
    const trigger = returning;
    dialog.close();
    if (action.action === 'password' || action.action === 'email') {
      completed = action.action;
      trigger.click();
      const current = document.getElementById(action.action === 'password' ? 'currentPassword' : 'emailPassword');
      current.closest('.portal-field').hidden = true;
      return;
    }
    const row = document.querySelector(`[data-external-row="${action.provider}"]`);
    const linked = action.action === 'link';
    row.dataset.connected = String(linked);
    const state = row.querySelector('.external-connection-state');
    state.textContent = linked ? 'Verbunden' : 'Nicht verbunden';
    state.classList.toggle('connected', linked);
    row.querySelector('[data-external-hint]').textContent = 'Gestaltungsvorschau: Keine echte Verbindung oder Sitzung wurde geändert.';
    const button = row.querySelector('[data-external-provider]');
    button.classList.toggle('danger', linked);
    button.querySelector('span').textContent = `${action.provider === 'google' ? 'Google' : 'Microsoft'} ${linked ? 'trennen' : 'verbinden'}`;
    button.querySelector('[data-external-icon=link]').toggleAttribute('hidden', linked);
    button.querySelector('[data-external-icon=unlink]').toggleAttribute('hidden', !linked);
  });
  dialog.addEventListener('close', () => {
    password.value = '';
    pending = null;
    returning?.focus();
  });
})();
