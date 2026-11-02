(() => {
  // Nur synthetische Gestaltungsvorschau. Kein echter Authenticator, Kontozugriff oder Versand.
  const codes = Array.from({length:10},(_,index)=>`${String(index).padStart(8,'0')}-12345678-ABCDEF01-23456789`).join('\n');
  const symbol = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4"/></svg>';
  const notify = text => window.PublicNotifications?.show(text,'normal','',4000);
  const numberIcon = number => '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + (number === 1 ? '<path d="M10 10l2-2v8"/>' : '<path d="M10 8h3a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 0-1 1v3h4"/>') + '<path d="M12 3c7.2 0 9 1.8 9 9s-1.8 9-9 9-9-1.8-9-9 1.8-9 9-9Z"/></svg>';
  const renderEnrollment = (root, english) => {
    const selectedMethod = root.dataset.setupMethod || 'qr';
    const text = english ? {
      qr:'QR code',manual:'Key',manualHint:'Enter this key in your authenticator app and select a time-based code (TOTP).',codeHint:'Enter the current six-digit code shown in your authenticator app:',
      stepOne:'Prepare your authenticator app',stepTwo:'Confirm the code',apps:'You can use a compatible authenticator app, for example:',compatibility:'You can also use any other app that supports TOTP according to RFC 6238.',
      scan:'Scan this QR code with your authenticator app.',code:'Authenticator code',manualLabel:'Key for manual setup',copy:'Copy',copied:'The setup key was copied.',copyFailed:'Please select and copy the key in the field.'
    } : {
      qr:'QR-Code',manual:'Schlüssel',manualHint:'Tragen Sie diesen Schlüssel in Ihrer Authenticator-App ein und wählen Sie dort einen zeitbasierten Code (TOTP).',codeHint:'Geben Sie den aktuell angezeigten sechsstelligen Code Ihrer Authenticator-App ein:',
      stepOne:'Authenticator-App vorbereiten',stepTwo:'Code bestätigen',apps:'Sie können eine kompatible Authenticator-App verwenden, beispielsweise:',compatibility:'Sie können auch eine andere App verwenden, die TOTP nach RFC 6238 unterstützt.',
      scan:'Scannen Sie diesen QR-Code mit Ihrer Authenticator-App.',code:'Authenticator-Code',manualLabel:'Schlüssel für manuelle Einrichtung',copy:'Kopieren',copied:'Der Einrichtungsschlüssel wurde kopiert.',copyFailed:'Bitte markieren und kopieren Sie den Schlüssel im Feld.'
    };
    const externalIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6M11 13 20 4m-5 0h5v5"/></svg>';
    const qrIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="5" height="5" x="3" y="3" rx="1"/><rect width="5" height="5" x="16" y="3" rx="1"/><rect width="5" height="5" x="3" y="16" rx="1"/><path d="M21 16h-3a2 2 0 0 0-2 2v3M21 21v.01M12 7v3a2 2 0 0 1-2 2H7M3 12h.01M12 3h.01M12 16v.01M16 12h1M21 12v.01M12 21v-1"/></svg>';
    const keyIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"/><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"/></svg>';
    const infoIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-9v1"/></svg>';
    const appLink = (url, caption) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${externalIcon}<span>${caption}</span></a>`;
    root.innerHTML = `<div class="mfa-setup-layout">
      <div class="mfa-setup-method">
        <div class="mfa-setup-tabs" role="tablist" aria-label="${english ? 'Setup method' : 'Einrichtungsmethode'}">
          <button type="button" role="tab" id="mfa-qr-tab" aria-selected="true" aria-controls="mfa-qr-pane" data-mfa-method="qr">${qrIcon}<span>${text.qr}</span></button>
          <button type="button" role="tab" id="mfa-manual-tab" aria-selected="false" tabindex="-1" aria-controls="mfa-manual-pane" data-mfa-method="manual">${keyIcon}<span>${text.manual}</span></button>
        </div>
        <div id="mfa-qr-pane" role="tabpanel" aria-labelledby="mfa-qr-tab" class="mfa-setup-qr">
          <div class="mfa-qr-placeholder">QR-Code<br>${english ? 'Design preview' : 'Gestaltungsvorschau'}</div>
          <p class="portal-copy mfa-qr-hint">${text.scan}</p>
        </div>
        <div id="mfa-manual-pane" role="tabpanel" aria-labelledby="mfa-manual-tab" class="mfa-setup-manual" hidden>
          <p class="portal-copy mfa-manual-hint">${text.manualHint}</p>
          <div class="mfa-manual-control">
            <label class="mfa-manual-label" for="mfa-manual-key"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"/><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"/></svg>${text.manualLabel}</label>
            <div class="mfa-manual-field">
              <textarea id="mfa-manual-key" rows="2" readonly autocomplete="off" spellcheck="false">DEMO KEYX ONLY NOXX&#10;REAL SECR ETXX XXXX</textarea>
              <button type="button" data-mfa-copy-key aria-label="${text.copy}" title="${text.copy}"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2M16 4h2a2 2 0 0 1 2 2v4M21 14H11m4-4-4 4 4 4"/></svg></button>
            </div>
          </div>
        </div>
      </div>
      <div class="mfa-setup-instructions">
        <section class="mfa-setup-step">
          <h3 class="mfa-step-label">${numberIcon(1)}<span>${text.stepOne}</span></h3>
          <p class="portal-copy">${text.apps}</p>
          <div class="mfa-app-links">${appLink('https://support.google.com/accounts/answer/1066447', 'Google Authenticator')}${appLink('https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a', 'Microsoft Authenticator')}${appLink('https://2fast-app.de/', '2fast (Windows)')}</div>
          <div class="mfa-app-compatibility">${infoIcon}<span>${text.compatibility}</span></div>
        </section>
        <section class="mfa-setup-step">
          <h3 class="mfa-step-label">${numberIcon(2)}<span>${text.stepTwo}</span></h3>
          <p class="portal-copy">${text.codeHint}</p>
          <label class="mfa-otp-label" for="mfa-setup-code">${text.code}<span class="mfa-required">•</span></label>
          <div class="mfa-otp"><div class="mfa-otp-slots" aria-hidden="true">${'<span>•</span>'.repeat(6)}</div><input id="mfa-setup-code" type="text" inputmode="numeric" autocomplete="one-time-code" maxlength="6" aria-required="true" data-mfa-otp-input></div>
        </section>
      </div>
    </div>`;
    root.querySelector('[data-mfa-copy-key]').addEventListener('click', async () => {
      try {
        // Gruppierungsleerzeichen und Zeilenumbruch gehören nicht zum Schlüssel.
        const key = root.querySelector('#mfa-manual-key').value.replaceAll(' ', '').replaceAll('\n', '');
        await navigator.clipboard.writeText(key);
        notify(text.copied);
      } catch {
        notify(text.copyFailed);
      }
    });
    const tabs = [...root.querySelectorAll('[data-mfa-method]')];
    const selectMethod = tab => {
      root.dataset.setupMethod = tab.dataset.mfaMethod;
      tabs.forEach(item => { const selected = item === tab; item.setAttribute('aria-selected', String(selected)); item.tabIndex = selected ? 0 : -1; });
      root.querySelector('#mfa-qr-pane').hidden = tab.dataset.mfaMethod !== 'qr';
      root.querySelector('#mfa-manual-pane').hidden = tab.dataset.mfaMethod !== 'manual';
    };
    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => selectMethod(tab));
      tab.addEventListener('keydown', event => {
        if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) { return; }
        event.preventDefault();
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? 1 : 1 - index;
        selectMethod(tabs[next]); tabs[next].focus();
      });
    });
    selectMethod(tabs.find(tab => tab.dataset.mfaMethod === selectedMethod));
    const input = root.querySelector('[data-mfa-otp-input]');
    const slots = [...root.querySelectorAll('.mfa-otp-slots > span')];
    const refreshSlots = () => slots.forEach((slot,index) => {
      slot.textContent = input.value[index] || '•';
      slot.classList.toggle('filled', Boolean(input.value[index]));
      slot.classList.toggle('active', index === Math.min(input.selectionStart || 0, 5));
    });
    input.addEventListener('input', () => { input.value = input.value.replace(/[^0-9]/g,'').slice(0,6); refreshSlots(); });
    input.addEventListener('paste', event => {
      event.preventDefault();
      const digits = event.clipboardData.getData('text').replace(/[^0-9]/g,'');
      input.setRangeText(digits, input.selectionStart, input.selectionEnd, 'end');
      input.dispatchEvent(new Event('input', {bubbles:true}));
    });
    ['focus','blur','click','keyup'].forEach(name => input.addEventListener(name,refreshSlots));
    refreshSlots();
  };
  const renderPhase = (root, phase) => {
    const isPublic = root.hasAttribute('data-mfa-phase-content');
    if (isPublic) {
      root.closest('.vp-card').classList.toggle('mfa-introduction', phase === 'intro');
      root.closest('.vp-card').classList.toggle('mfa-setup', phase === 'qr');
      const english = document.querySelector('[data-menu-language="en"][aria-pressed="true"]');
      const action = document.querySelector('#public-two-factor [data-mfa-continue]');
      action.disabled = phase === 'codes';
      action.innerHTML = phase === 'intro'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z"/><path d="m9 12 2 2 4-4"/></svg><span>' + (english ? 'Set up' : 'Einrichten') + '</span>'
        : '<span>Bestätigen</span>';
      if (phase === 'qr') {
        action.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg><span>' + (english ? 'Enable authenticator' : 'Authenticator aktivieren') + '</span>';
      }
      const cancel = document.querySelector('#public-two-factor .vp-card-foot a');
      cancel.href = '#public-login';
      cancel.setAttribute('aria-disabled', String(phase === 'codes'));
      cancel.classList.toggle('danger', phase === 'intro' || phase === 'qr');
      cancel.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4m7 14 5-5-5-5m5 5H9"/></svg><span>' + (english ? 'Sign out' : 'Abmelden') + '</span>';
      if (phase === 'qr') {
        root.closest('.vp-card').querySelector('.vp-card-head').innerHTML = '<span class="portal-reference-icon">' + symbol + '</span><div class="portal-reference-heading"><h2>' + (english ? 'Set up an authenticator app' : 'Authenticator-App einrichten') + '</h2><p>' + (english ? 'Protect your account with an authenticator app.' : 'Schützen Sie Ihr Konto mit einer Authenticator-App.') + '</p></div>';
        renderEnrollment(root, Boolean(english)); root.dataset.phase = phase; return;
      }
      if (phase === 'intro') {
        root.innerHTML = '<img class="two-factor-introduction-art" src="assets/two-factor-locks.svg" alt=""><h2 class="two-factor-introduction-title">' + (english ? 'Two-factor authentication required' : 'Zwei-Faktor-Authentifizierung erforderlich') + '</h2><p class="portal-copy">' + (english ? 'Your account requires additional protection. Connect an authenticator app now to continue using the application.' : 'Für Ihr Konto ist eine zusätzliche Absicherung erforderlich. Verbinden Sie jetzt eine Authenticator-App, um die Anwendung weiter zu nutzen.') + '</p>';
        root.dataset.phase = phase;
        return;
      }
      if (phase === 'codes') {
        const saved = root.dataset.phase === 'codes' && root.dataset.codesSaved === 'true';
        root.dataset.codesSaved = String(saved);
        root.closest('.vp-card').querySelector('.vp-card-head h2').textContent = english ? 'Keep your recovery codes safe' : 'Ersatzcodes sicher aufbewahren';
        action.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg><span>' + (english ? 'Codes saved — continue' : 'Codes gesichert und weiter') + '</span>';
        cancel.href = 'portal.html#security';
        cancel.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m6 6 12 12M6 18 18 6"/></svg><span>' + (english ? 'Back to account' : 'Zurück zum Konto') + '</span>';
        root.innerHTML = `<p class="portal-copy">${english ? 'Save or print these recovery codes. Each code works only once. Keep them in a safe place separate from your device.' : 'Speichern oder drucken Sie diese Ersatzcodes. Jeder Code gilt nur einmal. Bewahren Sie sie getrennt von Ihrem Gerät sicher auf.'}</p><pre class="mfa-code-values">${codes}</pre><p class="portal-copy">${english ? 'The application provides a protected PDF download with its service logo.' : 'Die Anwendung bietet einen geschützten PDF-Download mit Service-Logo.'}</p><div class="vp-options"><label><input type="checkbox" role="switch" data-mfa-saved> ${english ? 'I have saved my recovery codes securely.' : 'Ich habe meine Ersatzcodes sicher gespeichert.'}</label></div>`;
        root.querySelector('[data-mfa-saved]').addEventListener('change', event => {
          root.dataset.codesSaved = String(event.target.checked);
          action.disabled = !event.target.checked;
          cancel.setAttribute('aria-disabled', String(!event.target.checked));
        });
        root.querySelector('[data-mfa-saved]').checked = saved;
        action.disabled = !saved;
        cancel.setAttribute('aria-disabled', String(!saved));
        root.dataset.phase = phase;
        return;
      }
    }
    const contents = {
      confirm: '<p class="portal-copy">Bestätigen Sie Ihre Anmeldung mit einem Code Ihrer Authenticator-App oder einem noch unbenutzten Ersatzcode.</p><label>Bestätigungscode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="recover">Authenticator wiederherstellen</button><button type="button" class="portal-button" data-mfa-go="help">Kein Zugriff auf Authenticator oder Ersatzcodes?</button></div>',
      recover: '<p class="portal-copy">Verwenden Sie einen unbenutzten Ersatzcode, um einen neuen Authenticator einzurichten. Ihr bisheriger Faktor bleibt bis zur Bestätigung der neuen App gültig. Der Ersatzcode wird erst beim erfolgreichen Wechsel verbraucht.</p><label>Ersatzcode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="confirm">Zur Code-Abfrage</button></div>',
      setup: '<p class="portal-copy">Bestätigen Sie zuerst Ihre Identität, bevor Sie einen Authenticator einrichten.</p><label>Aktuelles Passwort •<input type="password" autocomplete="off" placeholder="Passwort eingeben"></label>',
      email: '<p class="portal-copy">Geben Sie den Einmalcode ein, den wir zur erstmaligen Einrichtung an Ihre bestätigte Kontaktadresse senden.</p><label>E-Mail-Code •<input inputmode="numeric" placeholder="Achtstelliger Code"></label>',
      qr: '<p class="portal-copy">Scannen Sie den QR-Code mit einer kompatiblen Authenticator-App. Sie können beispielsweise Google Authenticator, Microsoft Authenticator oder 2fast für Windows verwenden.</p><div class="mfa-qr-placeholder">QR-Code (Gestaltungsvorschau)</div><div class="mfa-phase-buttons"><a href="https://support.google.com/accounts/answer/1066447" target="_blank" rel="noopener noreferrer">Google Authenticator</a><a href="https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a" target="_blank" rel="noopener noreferrer">Microsoft Authenticator</a><a href="https://2fast-app.de/" target="_blank" rel="noopener noreferrer">2fast (Windows)</a></div><label>Erster Authenticator-Code •<input inputmode="numeric" placeholder="Sechsstelliger Code"></label>',
      codes: `<p class="portal-copy">Speichern oder drucken Sie diese Ersatzcodes. Jeder Code gilt nur einmal. Neue Codes widerrufen alle alten.</p><pre class="mfa-code-values">${codes}</pre><p class="portal-copy">Die echte Anwendung bietet hier einen geschützten PDF-Download mit Logo und Erklärung.</p>`,
      help: '<p class="portal-copy">Eine Anfrage ersetzt keinen Identitätsnachweis. Ohne Authenticator oder gültigen Ersatzcode können wir das Konto nicht entsperren.</p><label>Ihre Anfrage<textarea rows="4" maxlength="2000" placeholder="Keine Passwörter oder Ersatzcodes eingeben"></textarea></label>'
    };
    root.innerHTML=contents[phase]||contents.confirm;
    root.dataset.phase=phase;
  };
  const publicPhase = document.querySelector('#public-two-factor [data-mfa-phase-content]');
  if(publicPhase){
    const recovery=document.createElement('button');
    recovery.type='button'; recovery.className='portal-button'; recovery.dataset.mfaPhase='recover'; recovery.textContent='Wiederherstellung';
    document.querySelector('#public-two-factor .mfa-phase-buttons').append(recovery);
    document.querySelectorAll('#public-two-factor [data-mfa-phase]').forEach(button=>button.addEventListener('click',()=>renderPhase(publicPhase,button.dataset.mfaPhase)));
    publicPhase.addEventListener('click',event=>{const control=event.target.closest('[data-mfa-go]');if(control){renderPhase(publicPhase,control.dataset.mfaGo);}});
    document.querySelector('#public-two-factor [data-mfa-continue]').addEventListener('click',()=>{
      if (publicPhase.dataset.phase === 'codes' && !publicPhase.querySelector('[data-mfa-saved]').checked) { return; }
      const next={intro:'qr',setup:'qr',recover:'qr',email:'qr',qr:'codes',codes:'confirm',help:'help',confirm:'confirm'};
      renderPhase(publicPhase,next[publicPhase.dataset.phase]);
      notify('Gestaltungsvorschau: Es wurde keine Anmeldung oder Kontoänderung ausgeführt.');
    });
    document.querySelector('#public-two-factor .vp-card-foot a').addEventListener('click', event => {
      if (publicPhase.dataset.phase === 'codes' && !publicPhase.querySelector('[data-mfa-saved]').checked) { event.preventDefault(); }
    });
    document.querySelectorAll('[data-menu-language]').forEach(button => button.addEventListener('click', () => {
      if (['intro','qr','codes'].includes(publicPhase.dataset.phase)) { renderPhase(publicPhase, publicPhase.dataset.phase); }
    }));
    renderPhase(publicPhase,'intro');
  }
  const portal = document.querySelector('#security-portal .portal-card-body');
  if(portal){
    const row=document.createElement('div');
    row.className='portal-row mfa-reference';
    row.innerHTML=`<span class="portal-icon">${symbol}</span><div class="portal-row-copy"><h3>Zwei-Faktor-Authentifizierung <span class="external-connection-state connected">Eingerichtet</span></h3><p class="portal-copy">Schützen Sie Ihr Konto mit einer Authenticator-App und sichern Sie Ihre Ersatzcodes.</p></div><div class="mfa-actions"><button class="portal-button" type="button" data-mfa-manage="qr">Authenticator wechseln</button><button class="portal-button" type="button" data-mfa-manage="codes">Neue Ersatzcodes</button><button class="portal-button danger" type="button" data-mfa-manage="remove">Entfernen</button></div>`;
    portal.append(row);
    const dialog=document.createElement('dialog');
    dialog.className='portal-modal portal-security-dialog mfa-reference';
    dialog.innerHTML=`<div class="portal-modal-head"><span class="portal-reference-icon">${symbol}</span><div><h2>Identität bestätigen</h2><p class="portal-copy">Bestätigen Sie diese Aktion mit Ihrem zweiten Faktor.</p></div></div><div class="portal-modal-body mfa-form" data-mfa-body></div><div class="portal-modal-foot"><button class="portal-button" type="button" data-mfa-close>Abbrechen</button><button class="portal-button primary" type="button" data-mfa-submit>Bestätigen</button></div>`;
    document.body.append(dialog);
    let action;
    row.querySelectorAll('[data-mfa-manage]').forEach(button=>button.addEventListener('click',()=>{
      action=button.dataset.mfaManage;
      renderPhase(dialog.querySelector('[data-mfa-body]'),'confirm');
      dialog.showModal();
    }));
    dialog.querySelector('[data-mfa-close]').addEventListener('click',()=>dialog.close());
    dialog.querySelector('[data-mfa-submit]').addEventListener('click',()=>{
      if(action==='remove'){notify('Gestaltungsvorschau: Der Authenticator bleibt unverändert.');dialog.close();return;}
      renderPhase(dialog.querySelector('[data-mfa-body]'),action);
    });
  }
})();
