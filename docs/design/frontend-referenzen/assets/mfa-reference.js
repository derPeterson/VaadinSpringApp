(() => {
  // Nur synthetische Gestaltungsvorschau. Kein echter Authenticator, Kontozugriff oder Versand.
  const codes = Array.from({length:10},(_,index)=>`${String(index).padStart(8,'0')}-12345678-ABCDEF01-23456789`).join('\n');
  const symbol = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4"/></svg>';
  const shieldKeyhole = '<span class="mfa-shield-keyhole-icon" aria-hidden="true"></span>';
  const notify = text => window.PublicNotifications?.show(text,'normal','',4000);
  const submitPastedOtp = (root, input, clipboardText) => {
    if (/^[0-9]{6}$/.test(input.value) && clipboardText.replace(/[\s-]/g, '') === input.value) {
      root.closest('.vp-card').querySelector('[data-mfa-continue]').click();
    }
  };
  const numberIcon = number => '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + (number === 1 ? '<path d="M10 10l2-2v8"/>' : number === 2 ? '<path d="M10 8h3a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 0-1 1v3h4"/>' : '<path d="M10 9a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1"/>') + '<path d="M12 3c7.2 0 9 1.8 9 9s-1.8 9-9 9-9-1.8-9-9 1.8-9 9-9Z"/></svg>';
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
      const clipboardText = event.clipboardData.getData('text');
      const digits = clipboardText.replace(/[^0-9]/g,'');
      input.setRangeText(digits, input.selectionStart, input.selectionEnd, 'end');
      input.dispatchEvent(new Event('input', {bubbles:true}));
      submitPastedOtp(root, input, clipboardText);
    });
    ['focus','blur','click','keyup'].forEach(name => input.addEventListener(name,refreshSlots));
    refreshSlots();
  };
  const renderRecovery = (root, english, saved) => {
    const text = english ? {
      once:'Single use',onceHint:'Each code replaces one authenticator confirmation and works only once.',
      restore:'Restore access',restoreHint:'If you lose your app, choose “Restore authenticator” after signing in and use an unused recovery code.',
      storage:'Keep them safe',storageHint:'Save or print the codes. Keep them in a safe place separate from your device. They will not be displayed again later.',
      copy:'Copy all codes',pdf:'Download PDF',summary:'Your 10 recovery codes',saved:'I have saved my recovery codes securely.'
    } : {
      once:'Einmal verwendbar',onceHint:'Jeder Code ersetzt eine Authenticator-Bestätigung und gilt nur einmal.',
      restore:'Zugang wiederherstellen',restoreHint:'Bei Verlust Ihrer App wählen Sie nach der Anmeldung „Authenticator wiederherstellen“ und verwenden einen unbenutzten Ersatzcode.',
      storage:'Sicher aufbewahren',storageHint:'Speichern oder drucken Sie die Codes. Bewahren Sie sie an einem sicheren Ort getrennt von Ihrem Gerät auf. Sie werden später nicht erneut angezeigt.',
      copy:'Alle Codes kopieren',pdf:'PDF herunterladen',summary:'Ihre 10 Ersatzcodes',saved:'Ich habe meine Ersatzcodes sicher gespeichert.'
    };
    const copyIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2M16 4h2a2 2 0 0 1 2 2v4M21 14H11m4-4-4 4 4 4"/></svg>';
    const downloadIcon = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>';
    const summaryIcon = "<svg aria-hidden=\"true\" class=\"lucide lucide-key-round\" xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" > <path d=\"M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z\" /> <circle cx=\"16.5\" cy=\"7.5\" r=\".5\" fill=\"currentColor\" /> </svg>";
    const codeLabel = english ? 'Recovery code' : 'Ersatzcode';
    const copyLabel = english ? 'Copy' : 'Kopieren';
    const codeFields = codes.split('\n').map((code,index) => `<div class="mfa-recovery-code-field"><input readonly autocomplete="off" value="${code}" aria-label="${codeLabel} ${index+1}"><button type="button" data-mfa-copy-code aria-label="${copyLabel}: ${codeLabel} ${index+1}" data-tooltip="${copyLabel}">${copyIcon}</button></div>`).join('');
    const hint = (number,title,description) => `<div class="mfa-recovery-hint">${numberIcon(number)}<div><strong>${title}</strong><p class="portal-copy">${description}</p></div></div>`;
    root.innerHTML = `<div class="mfa-recovery-layout"><div class="mfa-recovery-explanation"><img class="mfa-recovery-art" src="assets/two-factor-vault.svg" alt="">${hint(1,text.once,text.onceHint)}${hint(2,text.restore,text.restoreHint)}${hint(3,text.storage,text.storageHint)}<div class="mfa-recovery-actions"><button type="button" class="portal-button" data-mfa-copy-codes>${copyIcon}<span>${text.copy}</span></button><button type="button" class="portal-button" data-mfa-download-demo>${downloadIcon}<span>${text.pdf}</span></button></div></div><div class="mfa-recovery-controls"><div class="mfa-recovery-codes-section"><div class="mfa-recovery-codes-header"><span class="mfa-recovery-heading">${summaryIcon}<span>${text.summary}</span></span><button type="button" class="mfa-recovery-toggle" data-mfa-toggle-codes aria-controls="mfa-recovery-code-list" aria-expanded="false"><span>Details</span><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg></button></div><details id="mfa-recovery-code-list" class="mfa-recovery-details"><summary>${text.summary}</summary><div class="mfa-recovery-code-fields">${codeFields}</div></details></div><div class="mfa-recovery-confirmation"><label class="mfa-recovery-saved"><input type="checkbox" role="switch" data-mfa-saved><span>${text.saved}</span></label></div></div></div>`;
    root.querySelectorAll('[data-mfa-copy-code]').forEach(button => button.addEventListener('click', async () => {
      try { await navigator.clipboard.writeText(button.previousElementSibling.value); notify(english ? 'The recovery code was copied.' : 'Der Ersatzcode wurde kopiert.'); }
      catch { notify(english ? 'Please select and copy the code.' : 'Bitte markieren und kopieren Sie den Code.'); }
    }));
    const details = root.querySelector('details');
    details.open = root.dataset.codesVisible !== 'false';
    const toggle = root.querySelector('[data-mfa-toggle-codes]');
    const refreshToggle = () => { root.dataset.codesVisible = String(details.open); toggle.setAttribute('aria-expanded', String(details.open)); toggle.querySelector('path').setAttribute('d', details.open ? 'm6 9 6 6 6-6' : 'm9 6 6 6-6 6'); };
    toggle.addEventListener('click', () => { details.open = !details.open; refreshToggle(); });
    details.addEventListener('toggle', refreshToggle);
    refreshToggle();
    root.querySelector('[data-mfa-saved]').checked = saved;
    root.querySelector('[data-mfa-copy-codes]').addEventListener('click', async () => {
      try { await navigator.clipboard.writeText(codes); notify(english ? 'The recovery codes were copied.' : 'Die Ersatzcodes wurden kopiert.'); }
      catch { notify(english ? 'Please select and copy the codes.' : 'Bitte markieren und kopieren Sie die Codes.'); }
    });
    root.querySelector('[data-mfa-download-demo]').addEventListener('click', () => notify(english ? 'Design preview: The PDF download is available in the Vaadin application.' : 'Gestaltungsvorschau: Der PDF-Download ist in der Vaadin-Anwendung verfügbar.'));
  };
  const helpMessages = [];
  const confirmationText = english => english ? {
      confirm:'Confirm sign-in',confirmSubtitle:'Use your authenticator app or an unused recovery code.',authenticator:'Authenticator code',recovery:'Recovery code',
      authHint:'Enter the six-digit code from your authenticator app:',recoveryHint:'Enter an unused recovery code to confirm your sign-in:',
      restore:'Restore authenticator',restorePrompt:'Use a recovery code to set up a new app?',restoreSubtitle:'Use a recovery code to set up a new app.',restoreHint:'Use an unused recovery code to set up a new authenticator. Your current app remains valid until you confirm the new one.',
      consumption:'The recovery code is only used up after the replacement succeeds.',
      helpTitle:'Recovery help',helpSubtitle:'No access to your authenticator or recovery codes?',helpPageHint:'Send a request to the administration.',noUnlock:'A request is not proof of identity. Without an authenticator or valid recovery code we cannot unlock the account.',
      empty:'No requests.',request:'Your request',placeholder:'Briefly describe what you need help with.',warningLabel:'Security notice:',warning:'Never enter passwords, authenticator secrets or recovery codes in messages.',
      existing:'Existing requests',ticketSubject:'Help accessing your authenticator',open:'Open',conversation:'Conversation',message:'message',messages:'messages',
      signIn:'Sign in',loginBack:'Sign in',continue:'Continue',create:'Create request',reply:'Add reply',refresh:'Refresh'
    } : {
      confirm:'Anmeldung bestätigen',confirmSubtitle:'Verwenden Sie Ihre Authenticator-App oder einen unbenutzten Ersatzcode.',authenticator:'Authenticator-Code',recovery:'Ersatzcode',
      authHint:'Geben Sie den sechsstelligen Code Ihrer Authenticator-App ein:',recoveryHint:'Geben Sie einen noch unbenutzten Ersatzcode ein, um Ihre Anmeldung zu bestätigen:',
      restore:'Authenticator wiederherstellen',restorePrompt:'Mit einem Ersatzcode eine neue App einrichten?',restoreSubtitle:'Mit einem Ersatzcode eine neue App einrichten.',restoreHint:'Verwenden Sie einen unbenutzten Ersatzcode, um einen neuen Authenticator einzurichten. Ihre bisherige App bleibt gültig, bis Sie die neue bestätigt haben.',
      consumption:'Der Ersatzcode wird erst nach dem erfolgreichen Wechsel verbraucht.',
      helpTitle:'Hilfe zur Wiederherstellung',helpSubtitle:'Kein Zugriff auf Authenticator und Ersatzcodes?',helpPageHint:'Stellen Sie eine Anfrage an die Administration.',noUnlock:'Eine Anfrage ersetzt keinen Identitätsnachweis. Ohne Authenticator oder gültigen Ersatzcode können wir das Konto nicht entsperren.',
      empty:'Keine Anfragen vorhanden.',request:'Ihre Anfrage',placeholder:'Beschreiben Sie Ihr Anliegen kurz.',warningLabel:'Sicherheitshinweis:',warning:'Geben Sie niemals Passwörter, Authenticator-Geheimschlüssel oder Ersatzcodes in Nachrichten ein.',
      existing:'Bestehende Anfragen',ticketSubject:'Hilfe beim Authenticator-Zugriff',open:'Offen',conversation:'Nachrichtenverlauf',message:'Nachricht',messages:'Nachrichten',
      signIn:'Anmelden',loginBack:'Anmeldung',continue:'Weiter',create:'Anfrage erstellen',reply:'Antwort hinzufügen',refresh:'Aktualisieren'
    };
  const renderConfirmationFlow = (root, phase, english) => {
    const text = confirmationText(english);
    const infoIcon = document.querySelector('#vp-symbols').content.querySelector('[data-symbol="info"]').innerHTML;
    const key = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="16" cy="8" r="5"/><path d="m12.5 11.5-9 9H2v-4l3-3h3v-3h3"/></svg>';
    const device = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/></svg>';
    const escape = value => String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
    const recoveryPlaceholder = 'XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX';
    const recoveryField = id => `
      <div class="mfa-recovery-field">
        <label class="mfa-code-label" for="${id}">${text.recovery}<span class="mfa-required" aria-hidden="true">•</span></label>
        <input id="${id}" class="mfa-recovery-input" maxlength="35" autocomplete="off" aria-required="true" placeholder="${recoveryPlaceholder}">
      </div>`;
    const recoveryMode = root.dataset.loginMethod === 'recovery';
    if (phase === 'confirm') {
      const alternativesOpen = root.querySelector('.mfa-login-alternatives')?.open;
      const helpTitle = english ? 'Sign-in problems and alternatives' : 'Zugangsprobleme und Alternativen';
      const question = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M9 9a3 3 0 1 1 5 2.2c-1.3 1-2 1.4-2 2.8m0 3h.01"/></svg>';
      const chevron = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>';
      const alternatives = `<details class="mfa-login-alternatives" ${alternativesOpen ? 'open' : ''}>
        <summary><span class="mfa-login-help-topic">${question}<span>${helpTitle}</span></span>
          <span class="mfa-login-help-toggle">Details${chevron}</span></summary>
        <div class="mfa-login-help-links">
          <div class="mfa-login-help-row"><span>${text.restorePrompt}</span><button class="mfa-quiet-action" type="button" data-mfa-go="recover">${text.restore}<span class="mfa-login-open-icon" aria-hidden="true"></span></button></div>
          <div class="mfa-login-help-row"><span>${text.helpSubtitle}</span><button class="mfa-quiet-action" type="button" data-mfa-go="help">${text.helpTitle}<span class="mfa-login-open-icon" aria-hidden="true"></span></button></div>
        </div>
      </details>`;
      root.innerHTML = `<div class="mfa-setup-tabs mfa-login-tabs" role="tablist" aria-label="${text.confirm}"><button type="button" role="tab" data-mfa-login-method="authenticator" aria-selected="${!recoveryMode}" tabindex="${recoveryMode?-1:0}">${device}<span>${text.authenticator}</span></button><button type="button" role="tab" data-mfa-login-method="recovery" aria-selected="${recoveryMode}" tabindex="${recoveryMode?0:-1}">${key}<span>${text.recovery}</span></button></div><p class="portal-copy">${recoveryMode?text.recoveryHint:text.authHint}</p>${recoveryMode?recoveryField('mfa-login-recovery-code'):`<div class="mfa-otp-field"><label class="mfa-otp-label" for="mfa-login-code">${text.authenticator} <span class="mfa-required">•</span></label><div class="mfa-otp"><div class="mfa-otp-slots" aria-hidden="true">${'<span>•</span>'.repeat(6)}</div><input id="mfa-login-code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" aria-required="true"></div></div>`}<div class="mfa-login-help">${alternatives}</div>`;
      const tabs = [...root.querySelectorAll('[data-mfa-login-method]')];
      tabs.forEach((tab, index) => {
        const select = () => { root.dataset.loginMethod = tab.dataset.mfaLoginMethod; renderPhase(root, 'confirm'); };
        tab.addEventListener('click', select);
        tab.addEventListener('keydown', event => {
          if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) { return; }
          event.preventDefault();
          const next = event.key === 'Home' ? 0 : event.key === 'End' ? 1 : 1 - index;
          root.dataset.loginMethod = tabs[next].dataset.mfaLoginMethod;
          renderPhase(root, 'confirm');
          root.querySelectorAll('[data-mfa-login-method]')[next].focus();
        });
      });
      const input = root.querySelector('#mfa-login-code');
      if (input) {
        const slots = [...root.querySelectorAll('.mfa-otp-slots > span')];
        const refresh = () => slots.forEach((slot,index) => { slot.textContent=input.value[index]||'•';slot.classList.toggle('filled',Boolean(input.value[index]));slot.classList.toggle('active',index===Math.min(input.selectionStart||0,5)); });
        input.addEventListener('input',()=>{input.value=input.value.replace(/[^0-9]/g,'').slice(0,6);refresh();});
        input.addEventListener('paste', event => {
          event.preventDefault();
          const clipboardText = event.clipboardData.getData('text');
          input.value = clipboardText.replace(/[^0-9]/g, '').slice(0, 6);
          input.dispatchEvent(new Event('input'));
          submitPastedOtp(root, input, clipboardText);
        });
        ['focus','click','keyup'].forEach(event=>input.addEventListener(event,refresh));refresh();
      }
    } else if (phase === 'recover') {
      root.innerHTML = `<p class="portal-copy">${text.restoreHint}</p>${recoveryField('mfa-restore-recovery-code')}<div class="mfa-flow-notice">${infoIcon}<span>${text.consumption}</span></div>`;
    } else {
      const date = new Intl.DateTimeFormat(english ? 'en-GB' : 'de-DE', {dateStyle:'short',timeStyle:'short'});
      const conversation = helpMessages.length ? `
        <section class="mfa-help-conversation">
          <div class="mfa-help-conversation-heading"><h3>${text.conversation} #1</h3>
            <span>${helpMessages.length} ${helpMessages.length === 1 ? text.message : text.messages}</span></div>
          <div class="mfa-help-thread" tabindex="0" aria-label="${text.conversation} #1">
            ${helpMessages.map(message => `<div class="mfa-help-message">
              <div class="mfa-help-message-metadata">
                <span class="mfa-help-message-avatar" role="img" tabindex="0" aria-label="John Doe"
                  data-tooltip="John Doe · john.doe@example.test">JD</span>
                <time datetime="${message.time.toISOString()}">${date.format(message.time)}</time>
              </div>
              <p class="mfa-flow-ticket">${escape(message.body)}</p>
            </div>`).join('')}
          </div>
        </section>` : '';
      const requestSummary = helpMessages.length ? `
        <div class="mfa-help-ticket-list">
          <div><div class="mfa-help-ticket-title"><span>#1</span> ${text.ticketSubject}</div>
            <time>${date.format(helpMessages.at(-1).time)}</time></div>
          <span class="mfa-help-ticket-status">${text.open}</span>
        </div>` : `<div class="mfa-help-empty"><span class="mfa-help-empty-icon" aria-hidden="true"></span>${text.empty}</div>`;
      const formLabel = helpMessages.length ? text.reply : text.request;
      const warningIcon = document.querySelector('#vp-symbols').content.querySelector('[data-symbol="error"]').innerHTML;
      root.innerHTML = `
        <div class="mfa-flow-notice">${infoIcon}<span>${text.noUnlock}</span></div>
        <section class="mfa-help-requests"><h3>${text.existing}</h3>${requestSummary}</section>
        ${conversation}
        <div class="mfa-help-editor">
          <div class="mfa-flow-notice mfa-help-security-notice">${warningIcon}<span><strong>${text.warningLabel}</strong> ${text.warning}</span></div>
          <div class="mfa-recovery-field">
            <label class="mfa-code-label" for="mfa-help-request">${formLabel}<span class="mfa-required" aria-hidden="true">•</span></label>
            <textarea id="mfa-help-request" rows="4" maxlength="2000" aria-required="true" placeholder="${text.placeholder}"></textarea>
          </div>
        </div>`;
      const draft = root.querySelector('textarea');draft.value=root.dataset.helpDraft||'';draft.addEventListener('input',()=>{root.dataset.helpDraft=draft.value;});
    }
    const card = root.closest('.vp-card');
    if (card) {
      const heading = card.querySelector('.vp-card-head');
      if (!heading.querySelector('.portal-reference-icon')) { heading.insertAdjacentHTML('afterbegin','<span class="portal-reference-icon"></span>'); }
      const headingIcons = {
        confirm: shieldKeyhole,
        recover: '<span class="mfa-restore-heading-icon" aria-hidden="true"></span>',
        help: '<span class="mfa-help-heading-icon" aria-hidden="true"></span>'
      };
      heading.querySelector('.portal-reference-icon').innerHTML = headingIcons[phase];
      heading.querySelector('h2').textContent = phase==='confirm'?text.confirm:phase==='recover'?text.restore:text.helpTitle;
      heading.querySelector('p').textContent = phase==='confirm'?text.confirmSubtitle:phase==='recover'?text.restoreSubtitle:text.helpSubtitle;
      const footer = card.querySelector('.vp-card-foot');
      footer.querySelector('[data-mfa-flow-back]')?.remove();
      footer.querySelector('[data-mfa-refresh]')?.remove();
      const cancel = footer.querySelector('a');cancel.hidden=phase==='help';
      const signInIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m10 17 5-5-5-5M15 12H3M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/></svg>';
      if (phase!=='confirm') {
        const back = document.createElement('button');
        back.type = 'button';
        back.className = 'portal-button';
        back.dataset.mfaFlowBack = phase;
        back.textContent = text.loginBack;
        back.insertAdjacentHTML('afterbegin', signInIcon);
        back.addEventListener('click', () => renderPhase(root, 'confirm'));
        footer.prepend(back);
      }
      if (phase==='help') {
        const reload = document.createElement('button');
        reload.type = 'button';
        reload.className = 'portal-button';
        reload.dataset.mfaRefresh = '';
        reload.textContent = text.refresh;
        reload.prepend(document.querySelector('#vp-symbols').content.querySelector('[data-symbol="refresh"] svg').cloneNode(true));
        reload.addEventListener('click', () => notify(english ? 'Design preview: Requests refreshed.' : 'Gestaltungsvorschau: Anfragen aktualisiert.'));
        footer.insertBefore(reload, footer.querySelector('[data-mfa-continue]'));
      }
      const actionLabels = {
        confirm: text.signIn,
        recover: text.continue,
        help: helpMessages.length ? text.reply : text.create
      };
      const submit = footer.querySelector('[data-mfa-continue]');
      submit.textContent = actionLabels[phase];
      if (phase === 'confirm') {
        submit.insertAdjacentHTML('afterbegin', signInIcon);
      } else if (phase === 'recover') {
        submit.insertAdjacentHTML('afterbegin', '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>');
      } else if (phase === 'help') {
        const icon = document.createElement('span');
        icon.className = helpMessages.length ? 'mfa-help-reply-icon' : 'mfa-help-create-icon';
        icon.setAttribute('aria-hidden', 'true');
        submit.prepend(icon);
      }
    }
    root.dataset.phase=phase;
  };
  const publicTwoFactor = document.querySelector('#public-two-factor');
  const publicTwoFactorTitle = publicTwoFactor?.dataset.title;
  const publicTwoFactorDescription = publicTwoFactor?.dataset.description;
  const renderPhase = (root, phase) => {
    const isPublic = root.hasAttribute('data-mfa-phase-content');
    if (isPublic) {
      root.closest('.vp-card').classList.toggle('mfa-introduction', phase === 'intro');
      root.closest('.vp-card').classList.toggle('mfa-setup', phase === 'qr');
      root.closest('.vp-card').classList.toggle('mfa-recovery', phase === 'codes');
      root.closest('.vp-card').classList.toggle('mfa-confirmation-flow', ['confirm','recover','help'].includes(phase));
      const english = document.querySelector('[data-menu-language="en"][aria-pressed="true"]');
      if (phase === 'help' || root.dataset.phase === 'help') {
        const text = confirmationText(Boolean(english));
        publicTwoFactor.dataset.title = phase === 'help' ? text.helpTitle : publicTwoFactorTitle;
        publicTwoFactor.dataset.description = phase === 'help' ? text.helpPageHint : publicTwoFactorDescription;
        if (!publicTwoFactor.hidden) {
          const title = document.querySelector('#portalTitle');
          title.firstChild.textContent = publicTwoFactor.dataset.title;
          document.querySelector('#portalDescription').textContent = publicTwoFactor.dataset.description;
        }
      }
      const action = document.querySelector('#public-two-factor [data-mfa-continue]');
      action.disabled = phase === 'codes';
      action.innerHTML = phase === 'intro'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z"/><path d="m9 12 2 2 4-4"/></svg><span>' + (english ? 'Set up' : 'Einrichten') + '</span>'
        : '<span>Bestätigen</span>';
      if (phase === 'qr') {
        action.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg><span>' + (english ? 'Enable authenticator' : 'Authenticator aktivieren') + '</span>';
      }
      const cancel = document.querySelector('#public-two-factor .vp-card-foot a');
      cancel.hidden=false;
      root.closest('.vp-card').querySelector('[data-mfa-flow-back]')?.remove();
      root.closest('.vp-card').querySelector('[data-mfa-refresh]')?.remove();
      cancel.href = '#public-login';
      cancel.hidden = phase === 'codes';
      const isSignInFlow = phase === 'confirm' || phase === 'recover';
      cancel.classList.toggle('vp-button', isSignInFlow);
      cancel.classList.toggle('danger', isSignInFlow || phase === 'intro' || phase === 'qr');
      cancel.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4m7 14 5-5-5-5m5 5H9"/></svg><span>' + (english ? 'Sign out' : 'Abmelden') + '</span>';
      if (phase === 'qr') {
        root.closest('.vp-card').querySelector('.vp-card-head').innerHTML = '<span class="portal-reference-icon">' + shieldKeyhole + '</span><div class="portal-reference-heading"><h2>' + (english ? 'Set up an authenticator app' : 'Authenticator-App einrichten') + '</h2><p>' + (english ? 'Protect your account with an authenticator app.' : 'Schützen Sie Ihr Konto mit einer Authenticator-App.') + '</p></div>';
        renderEnrollment(root, Boolean(english)); root.dataset.phase = phase; return;
      }
      const setupIcon = root.closest('.vp-card').querySelector('.mfa-shield-keyhole-icon');
      if (setupIcon) {
        setupIcon.parentElement.innerHTML = symbol;
      }
      if (phase === 'intro') {
        root.innerHTML = '<img class="two-factor-introduction-art" src="assets/two-factor-locks.svg" alt=""><h2 class="two-factor-introduction-title">' + (english ? 'Two-factor authentication required' : 'Zwei-Faktor-Authentifizierung erforderlich') + '</h2><p class="portal-copy">' + (english ? 'Your account requires additional protection. Connect an authenticator app now to continue using the application.' : 'Für Ihr Konto ist eine zusätzliche Absicherung erforderlich. Verbinden Sie jetzt eine Authenticator-App, um die Anwendung weiter zu nutzen.') + '</p>';
        root.dataset.phase = phase;
        return;
      }
      if (phase === 'codes') {
        if (root.dataset.phase !== 'codes') { root.dataset.codesVisible = 'true'; }
        const saved = root.dataset.phase === 'codes' && root.dataset.codesSaved === 'true';
        root.dataset.codesSaved = String(saved);
        root.closest('.vp-card').querySelector('.vp-card-head h2').textContent = english ? 'Keep your recovery codes safe' : 'Ersatzcodes sicher aufbewahren';
        root.closest('.vp-card').querySelector('.vp-card-head p').textContent = english ? 'Save your recovery codes for an emergency.' : 'Sichern Sie Ihre Ersatzcodes für den Notfall.';
        action.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg><span>' + (english ? 'Codes saved' : 'Codes gesichert') + '</span>';
        renderRecovery(root, Boolean(english), saved);
        root.querySelector('[data-mfa-saved]').addEventListener('change', event => {
          root.dataset.codesSaved = String(event.target.checked);
          action.disabled = !event.target.checked;
        });
        root.querySelector('[data-mfa-saved]').checked = saved;
        action.disabled = !saved;
        root.dataset.phase = phase;
        return;
      }
    }
    if (isPublic && ['confirm','recover','help'].includes(phase)) {
      renderConfirmationFlow(root,phase,Boolean(document.querySelector('[data-menu-language="en"][aria-pressed="true"]')));
      return;
    }
    const contents = {
      confirm: '<p class="portal-copy">Bestätigen Sie Ihre Anmeldung mit einem Code Ihrer Authenticator-App oder einem noch unbenutzten Ersatzcode.</p><label>Bestätigungscode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="recover">Authenticator wiederherstellen</button><button type="button" class="portal-button" data-mfa-go="help">Kein Zugriff auf Authenticator oder Ersatzcodes?</button></div>',
      recover: '<p class="portal-copy">Verwenden Sie einen unbenutzten Ersatzcode, um einen neuen Authenticator einzurichten. Ihr bisheriger Faktor bleibt bis zur Bestätigung der neuen App gültig. Der Ersatzcode wird erst beim erfolgreichen Wechsel verbraucht.</p><label>Ersatzcode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="confirm">Zur Code-Abfrage</button></div>',
      help: '<p class="portal-copy">Eine Anfrage ersetzt keinen Identitätsnachweis. Ohne Authenticator oder gültigen Ersatzcode können wir das Konto nicht entsperren.</p><label>Ihre Anfrage<textarea rows="4" maxlength="2000" placeholder="Keine Passwörter oder Ersatzcodes eingeben"></textarea></label>',
      setup: '<p class="portal-copy">Bestätigen Sie zuerst Ihre Identität, bevor Sie einen Authenticator einrichten.</p><label>Aktuelles Passwort •<input type="password" autocomplete="off" placeholder="Passwort eingeben"></label>',
      email: '<p class="portal-copy">Geben Sie den Einmalcode ein, den wir zur erstmaligen Einrichtung an Ihre bestätigte Kontaktadresse senden.</p><label>E-Mail-Code •<input inputmode="numeric" placeholder="Achtstelliger Code"></label>',
      qr: '<p class="portal-copy">Scannen Sie den QR-Code mit einer kompatiblen Authenticator-App. Sie können beispielsweise Google Authenticator, Microsoft Authenticator oder 2fast für Windows verwenden.</p><div class="mfa-qr-placeholder">QR-Code (Gestaltungsvorschau)</div><div class="mfa-phase-buttons"><a href="https://support.google.com/accounts/answer/1066447" target="_blank" rel="noopener noreferrer">Google Authenticator</a><a href="https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a" target="_blank" rel="noopener noreferrer">Microsoft Authenticator</a><a href="https://2fast-app.de/" target="_blank" rel="noopener noreferrer">2fast (Windows)</a></div><label>Erster Authenticator-Code •<input inputmode="numeric" placeholder="Sechsstelliger Code"></label>',
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
      const english = document.querySelector('[data-menu-language="en"][aria-pressed="true"]');
      if (publicPhase.dataset.phase === 'help') {
        const draft = publicPhase.dataset.helpDraft?.trim();
        if (!draft) {
          publicPhase.querySelector('textarea').setAttribute('aria-invalid', 'true');
          return;
        }
        helpMessages.push({body:draft,time:new Date()});
        publicPhase.dataset.helpDraft = '';
        renderPhase(publicPhase, 'help');
        notify(english ? 'Design preview: No request was sent.' : 'Gestaltungsvorschau: Es wurde keine Anfrage gesendet.');
        return;
      }
      const next={intro:'qr',setup:'qr',recover:'qr',email:'qr',qr:'codes',codes:'confirm',confirm:'confirm'};
      renderPhase(publicPhase,next[publicPhase.dataset.phase]);
      notify(english ? 'Design preview: No sign-in or account change was performed.' : 'Gestaltungsvorschau: Es wurde keine Anmeldung oder Kontoänderung ausgeführt.');
    });
    document.querySelector('#public-two-factor .vp-card-foot a').addEventListener('click', event => {
      if (publicPhase.dataset.phase === 'codes' && !publicPhase.querySelector('[data-mfa-saved]').checked) { event.preventDefault(); }
    });
    document.querySelectorAll('[data-menu-language]').forEach(button => button.addEventListener('click', () => {
      renderPhase(publicPhase, publicPhase.dataset.phase);
    }));
    renderPhase(publicPhase,'intro');
  }
  const portal = document.querySelector('#security-portal .portal-card-body');
  if(portal){
    const row=document.createElement('div');
    row.className='portal-row mfa-reference';
    row.innerHTML=`<span class="portal-icon">${shieldKeyhole}</span><div class="portal-row-copy"><h3>Zwei-Faktor-Authentifizierung <span class="external-connection-state connected gap-1"><span class="w-1.5 h-1.5 shrink-0 rounded-full bg-emerald-400" aria-hidden="true"></span>Eingerichtet</span></h3><p class="portal-copy">Schützen Sie Ihr Konto mit einer Authenticator-App und sichern Sie Ihre Ersatzcodes.</p></div><div class="mfa-actions"><button class="portal-button" type="button" data-mfa-manage="qr">Authenticator wechseln</button><button class="portal-button" type="button" data-mfa-manage="codes">Neue Ersatzcodes</button><button class="portal-button danger" type="button" data-mfa-manage="remove">Entfernen</button></div>`;
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
