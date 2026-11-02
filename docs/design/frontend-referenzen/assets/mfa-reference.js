(() => {
  // Nur synthetische Gestaltungsvorschau. Kein echter Authenticator, Kontozugriff oder Versand.
  const codes = Array.from({length:10},(_,index)=>`${String(index).padStart(8,'0')}-12345678-ABCDEF01-23456789`).join('\n');
  const symbol = '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4"/></svg>';
  const notify = text => window.PublicNotifications?.show(text,'normal','',4000);
  const renderPhase = (root, phase) => {
    const contents = {
      confirm: '<p class="portal-copy">Bestätigen Sie Ihre Anmeldung mit einem Code Ihrer Authenticator-App oder einem noch unbenutzten Ersatzcode.</p><label>Bestätigungscode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="recover">Authenticator wiederherstellen</button><button type="button" class="portal-button" data-mfa-go="help">Kein Zugriff auf Authenticator oder Ersatzcodes?</button></div>',
      recover: '<p class="portal-copy">Verwenden Sie einen unbenutzten Ersatzcode, um einen neuen Authenticator einzurichten. Ihr bisheriger Faktor bleibt bis zur Bestätigung der neuen App gültig. Der Ersatzcode wird erst beim erfolgreichen Wechsel verbraucht.</p><label>Ersatzcode •<input autocomplete="off" placeholder="Code eingeben"></label><div class="mfa-phase-buttons"><button type="button" class="portal-button" data-mfa-go="confirm">Zur Code-Abfrage</button></div>',
      setup: '<p class="portal-copy">Bestätigen Sie zuerst Ihre Identität, bevor Sie einen Authenticator einrichten.</p><label>Aktuelles Passwort •<input type="password" autocomplete="off" placeholder="Passwort eingeben"></label>',
      email: '<p class="portal-copy">Geben Sie den Einmalcode ein, den wir zur erstmaligen Einrichtung an Ihre bestätigte Kontaktadresse senden.</p><label>E-Mail-Code •<input inputmode="numeric" placeholder="Achtstelliger Code"></label>',
      qr: '<p class="portal-copy">Scannen Sie den QR-Code mit einer kompatiblen Authenticator-App. Google Authenticator und Microsoft Authenticator sind zwei mögliche Apps.</p><div class="mfa-qr-placeholder">QR-Code (Gestaltungsvorschau)</div><label>Erster Authenticator-Code •<input inputmode="numeric" placeholder="Sechsstelliger Code"></label>',
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
      const next={setup:'qr',recover:'qr',email:'qr',qr:'codes',codes:'confirm',help:'help',confirm:'confirm'};
      renderPhase(publicPhase,next[publicPhase.dataset.phase]);
      notify('Gestaltungsvorschau: Es wurde keine Anmeldung oder Kontoänderung ausgeführt.');
    });
    renderPhase(publicPhase,'confirm');
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
