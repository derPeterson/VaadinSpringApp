
(() => {
  const inventory = window.AdminDraftInventory;
  const root = document.getElementById('admin-proposal');
  const usersRegion = document.querySelector('.au-content');
  const configuration = inventory.configuration;
  const mail=inventory.mail.texts;
  const notifications=inventory.notifications;
  const notificationText=notifications.texts;
  const paginationReference=document.querySelector('.au-pagination .c-pagination-bar').cloneNode(true);
  const groups = {};
  for(const [group,values] of Object.entries(configuration.groups)){
    groups[group]=values.label;
    if(group==='SECURITY'){groups.EXTERNAL_LOGIN='Anmeldeanbieter';}
  }
  const groupIcons = {GENERAL:'settings',SECURITY:'shield',TOKENS:'key',USER_STATUS:'config-user',NOTIFICATIONS:'bell',EMAIL_SENDER:'mail',SMTP:'config-server',QUEUE:'config-list'};
  groupIcons.EXTERNAL_LOGIN='cloud-lock';
  const labels = {dashboard:'Dashboard',settings:configuration.title,mail:mail.title,send:notificationText.title,users:'Benutzerverwaltung'};
  const descriptions = {dashboard:'Ihr Einstieg in die Administration.',settings:configuration.description,mail:mail.description,send:notificationText.adminPageDescription,users:'Verwalten Sie Benutzerkonten, Rollen und Aktivierungsstatus.'};
  document.querySelector('[data-admin-route="mail"]>span').textContent=mail.title;
  const byId = id => document.getElementById(id);
  const escape = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  // Tabler cloud-lock (MIT). Original und Lizenz: assets/tabler/.
  const cloudLock = '<svg class="ad-tabler-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 18a3.5 3.5 0 0 0 0 -7h-1c.397 -1.768 -.285 -3.593 -1.788 -4.787c-1.503 -1.193 -3.6 -1.575 -5.5 -1s-3.315 2.019 -3.712 3.787c-2.199 -.088 -4.155 1.326 -4.666 3.373c-.512 2.047 .564 4.154 2.566 5.027"/><path d="M8 16a1 1 0 0 1 1 -1h6a1 1 0 0 1 1 1v3a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1l0 -3"/><path d="M10 15v-2a2 2 0 1 1 4 0v2"/></svg>';
  const icon = name => name==='cloud-lock' ? cloudLock : byId('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  const button = (action, text, symbol='check', primary=false) => `<button type="button" class="portal-button ${primary?'primary':''}" data-ad-action="${action}">${icon(symbol)}${text}</button>`;
  const notice = (text, tone='info') => `<div class="ad-notice" data-tone="${tone}" role="status">${icon(tone==='error'?'error':tone==='warning'?'warning':tone==='success'?'check':'info')}<span>${text}</span></div>`;
  const panel = (title, description, symbol, content, footer='', metadata='') => `<article class="ad-card"><div class="ad-head"><span class="portal-reference-icon">${icon(symbol)}</span><div><h2>${title}</h2><p>${description}</p></div>${metadata}</div><div class="ad-body">${content}</div>${footer?`<div class="ad-foot">${footer}</div>`:''}</article>`;
  const toast = (text, variant='normal') => window.PublicNotifications.show(text,variant,'',6000);
  const targets = [{name:'Alex Berger',email:'alex.berger@example.test'},{name:'Elena Sommer',email:'elena.sommer@example.test'},{name:'Mia König',email:'mia.koenig@example.test'},{name:'Noah Fischer',email:'noah.fischer@example.test'},{name:'Jonas Weber',email:'jonas.weber@example.test'},{name:'Lena Hoffmann',email:'lena.hoffmann@example.test'},{name:'Paul Schuster',email:'paul.schuster@example.test'},{name:'Emma Vogel',email:'emma.vogel@example.test'}];
  const values = {SERVICE_NAME:'Cosmos',COPYRIGHT_TEXT:'© {year} {serviceName}. Alle Rechte vorbehalten.',BASE_URL:'https://portal.example.test',REMEMBER_ME_DURATION:'1209600',REMEMBER_ME_SECRET_KEY:'',LOGIN_ATTEMPTS_LIMIT:'5',MAX_SESSIONS_PER_USER:'4',MAINTENANCE_MODE:false,VERIFICATION_TOKEN_VALID_DURATION:'PT24H',VERIFICATION_TOKEN_LIVE_DURATION:'PT48H',PASSWORD_RESET_TOKEN_VALID_DURATION:'PT1H',PASSWORD_RESET_TOKEN_LIVE_DURATION:'PT24H',USER_AUTO_ABSENT_TIMEOUT:'PT5M',USER_NOTIFICATIONS_INBOX_RETENTION_DAYS:'',USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS:'',EMAIL_FROM:'noreply@example.test',EMAIL_ADMIN:'admin@example.test',MAIL_HOST:'smtp.example.test',MAIL_PORT:'587',MAIL_USERNAME:'demo@example.test',MAIL_PASSWORD:'',MAIL_SMTP_AUTH:true,MAIL_SMTP_STARTTLS_ENABLE:true,MAIL_SMTP_SSL_ENABLE:false,MAIL_SMTP_SSL_TRUST:'',MAIL_DEBUG:false,EMAIL_QUEUE_POOL_SIZE:'2',EMAIL_QUEUE_CAPACITY:'100',EMAIL_QUEUE_MAX_RETRY:'3',EMAIL_QUEUE_SENT_LIVE_DURATION:'P7D'};
  let route='dashboard', group='GENERAL', variant='a', state='ready', historyPage=0;
  let previewValid=false, previewRevision=0, composedRevision=0;
  let historyPageSize=notifications.pageSizes[0];
  const history = Array.from({length:8},(_,index)=>({title:['Wartungsfenster am Wochenende','Willkommen im Benutzerportal','Aktualisierte Kontoeinstellungen','Hinweis zur Erreichbarkeit'][index%4],body:'Dies ist eine synthetische interne Nachricht für die Gestaltungsvorschau. Es wurde nichts versendet.',time:`${String(26-index).padStart(2,'0')}.10.2026 · 14:30`,recipients:targets.slice(0,index%2?3:8)}));
  const blocked = () => ['maintenance','unavailable','permission-denied'].includes(state);
  const stateNotice = () => state==='maintenance' ? notice('Der Wartungsmodus ist aktiv. Diese Aktion steht vorübergehend nicht zur Verfügung.','warning') : state==='unavailable' ? notice('Der aktuelle Systemzustand konnte nicht ermittelt werden. Die Aktion bleibt gesperrt.','warning') : state==='permission-denied' ? notice('Diese Ansicht steht für diese Beispielrolle nicht zur Verfügung. Der interne Versand setzt Superadmin voraus.','error') : '';

  const submenu = document.createElement('div');
  submenu.className='ad-subnav';submenu.id='ad-config-subnav';submenu.hidden=true;
  submenu.innerHTML=Object.entries(groups).map(([id,label])=>`<button type="button" data-ad-group="${id}">${icon(groupIcons[id])}<span>${label}</span></button>`).join('');
  const configButton=document.querySelector('[data-admin-route=settings]');
  configButton.after(submenu);configButton.setAttribute('aria-expanded','false');configButton.setAttribute('aria-controls',submenu.id);
  configButton.insertAdjacentHTML('beforeend',`<span class="ad-nav-chevron" aria-hidden="true">${icon('chevron')}</span>`);
  document.querySelectorAll('button[data-admin-route]').forEach(node=>{
    node.disabled=false;
    node.removeAttribute('aria-description');
  });
  document.querySelectorAll('[data-admin-route]').forEach(node=>node.addEventListener('click',event=>{
    if(node.dataset.adminRoute==='portal'){return;}
    if(node.dataset.adminRoute==='settings'){event.stopPropagation();}
    if(node.dataset.adminRoute==='settings'&&route==='settings'){
      submenu.hidden=!submenu.hidden;configButton.setAttribute('aria-expanded',String(!submenu.hidden));configButton.querySelector('.ad-nav-chevron').dataset.open=String(!submenu.hidden);return;
    }
    navigate(node.dataset.adminRoute,'ready');
  }));
  submenu.addEventListener('click',event=>{
    const node=event.target.closest('[data-ad-group]');if(!node){return;}
    group=node.dataset.adGroup;navigate('settings','ready');
    // Die bestehende mobile Navigation schließt nur ihre bekannten Routen.
    const dialog=byId('mobileNavigation');
    if(dialog.open){byId('mobileNavigationShade').click();}
  });
  const publish=(requestId)=>{
    if(parent!==window){parent.postMessage({type:'cosmos-admin-proposal-route',route,group,state,requestId},'*');}
  };
  function navigate(next, nextState='ready', requestId=null) {
    if(!Object.hasOwn(labels,next)){return;}
    document.querySelector('[data-close-filters]')?.click();
    window.PublicNotifications.close();
    route=next;state=nextState;
    if(route==='dashboard'||route==='settings'||route==='mail'){variant='a';}
    if(route==='send'){variant='b';}
    root.hidden=route==='users';usersRegion.hidden=route!=='users';
    document.querySelectorAll('button[data-admin-route]').forEach(node=>{
      if(node.dataset.adminRoute===route){node.setAttribute('aria-current','page');}else{node.removeAttribute('aria-current');}
    });
    submenu.hidden=route!=='settings';configButton.setAttribute('aria-expanded',String(!submenu.hidden));
    configButton.querySelector('.ad-nav-chevron').dataset.open=String(!submenu.hidden);
    submenu.querySelectorAll('button').forEach(node=>{
      if(node.dataset.adGroup===group){node.setAttribute('aria-current','page');}else{node.removeAttribute('aria-current');}
    });
    byId('portalTitle').innerHTML=escape(route==='settings'?configuration.title:labels[route])+'<span class="text-sky-400">.</span>';
    byId('portalGroup').textContent='ADMINISTRATION / '+labels[route].toLocaleUpperCase('de')+(route==='settings'?' / '+groups[group].toLocaleUpperCase('de'):'');
    byId('portalDescription').textContent=descriptions[route];
    if(route==='dashboard'){renderDashboard();}
    if(route==='settings'){renderConfig();}
    if(route==='mail'){renderMail();}
    if(route==='send'){renderCompose();}
    publish(requestId);
  }

  function renderDashboard() {
    const shortcuts = [
      ['go-users',labels.users,descriptions.users,'users'],
      ['go-settings',labels.settings,descriptions.settings,'settings'],
      ['go-mail',labels.mail,descriptions.mail,'mail'],
      ['go-send',labels.send,descriptions.send,'bell']
    ].map(([action,title,description,symbol]) => `
      <article class="ad-dashboard-shortcut" aria-label="${escape(title)}">
        <span class="portal-reference-icon" aria-hidden="true">${icon(symbol)}</span>
        <div class="ad-dashboard-shortcut-copy">
          <div class="ad-dashboard-shortcut-title"><h3>${escape(title)}</h3></div>
          <p>${escape(description)}</p>
        </div>
        <button type="button" class="portal-button" data-ad-action="${action}" aria-label="${escape(title)} öffnen">${icon('open-section')}Öffnen</button>
      </article>`).join('');
    root.innerHTML = `<div class="ad-dashboard ad-stack">
      <article class="ad-card ad-dashboard-hero" aria-labelledby="ad-dashboard-welcome">
        <div class="ad-dashboard-intro">
          <div class="ad-dashboard-copy"><p class="ad-dashboard-eyebrow">Ihr Administrationsbereich</p>
            <h2 id="ad-dashboard-welcome">Alles an einem Ort<span>.</span></h2>
            <p>Verwalten Sie Benutzerkonten und Berechtigungen, passen Sie zentrale Einstellungen an, prüfen Sie den E-Mail-Versand und erstellen Sie interne Benachrichtigungen.</p>
          </div>
          <div class="ad-dashboard-visual">${byId('ad-dashboard-visual').innerHTML}</div>
        </div>
        <div class="ad-config-save-hint ad-dashboard-hint">${icon('info')}<p>Hier entsteht künftig Ihr zentraler Einstieg in die Administration. Die vorhandenen Bereiche erreichen Sie über die Schnellzugriffe.</p></div>
      </article>
      <section class="ad-dashboard-access" aria-labelledby="ad-dashboard-shortcuts">
        <div class="ad-dashboard-access-heading"><h2 id="ad-dashboard-shortcuts">Schnellzugriff</h2><p>Direkt zu den vorhandenen Verwaltungsbereichen.</p></div>
        <div class="ad-grid">${shortcuts}</div>
      </section>
    </div>`;
  }

  function configField(field) {
    const id=`ad-config-${field.id}`, value=values[field.id];
    const required=field.required?'<span class="ad-required"> •</span>':'';
    const help=`<details><summary>${icon('info')}Hinweis</summary><p>${escape(field.tooltip)}</p><p data-help-default>${escape(field.defaultText)}</p><p data-help-rules-label>${escape(configuration.inputRules)}:</p><ul>${field.rules.map(rule=>`<li>${escape(rule)}</li>`).join('')}</ul></details>`;
    if(field.type==='boolean'){
      return `<div class="ad-field ad-toggle"><label for="${id}">${escape(field.label)}</label><input type="checkbox" role="switch" id="${id}" data-config-id="${field.id}" ${value?'checked':''} aria-label="${escape(field.label)}">${help}</div>`;
    }
    if(field.durationStorage){return window.AdminDuration.render(field,value,help);}
    const type=field.type==='integer'?'number':field.type==='password'?'password':field.type==='email'?'email':'text';
    const placeholder=field.placeholder;
    const attributes=type==='number'?` min="${field.id==='EMAIL_QUEUE_MAX_RETRY'?0:1}" step="1" ${field.id==='MAIL_PORT'?'max="65535"':''}`:'';
    let input=`<input id="${id}" data-config-id="${field.id}" type="${type}" value="${escape(value)}" placeholder="${escape(placeholder)}"${attributes} ${required?'required':''} ${field.type==='password'?'autocomplete="new-password"':''}>`;
    if(type==='password'){input=`<div class="ad-input-action">${input}<button type="button" data-ad-reveal="${id}" aria-label="Passwort anzeigen" aria-pressed="false">${icon('eye')}</button></div>`;}
    if(type==='number'){input=`<div class="ad-input-action ad-number-control">${input}<span class="ad-number-actions"><button type="button" data-ad-step="1" data-ad-input="${id}" aria-label="${escape(field.label)} erhöhen">${icon('chevron')}</button><button type="button" data-ad-step="-1" data-ad-input="${id}" aria-label="${escape(field.label)} verringern">${icon('chevron')}</button></span></div>`;}
    return `<div class="ad-field"><label for="${id}">${escape(field.label)}${required}</label>${input}${help}</div>`;
  }
  function renderConfig() {
    if(group==='EXTERNAL_LOGIN'){renderProviders();return;}
    const fields=inventory.fields.filter(field=>field.group===group);
    const count=(fields.length===1?configuration.countOne:configuration.countOther).replace('{0}',String(fields.length));
    const metadata=`<span class="ad-pill ad-config-count">${escape(count)}</span>`;
    const footer=`<div class="ad-config-save-hint">${icon('info')}<span>${escape(configuration.saveHint)}</span></div><div class="ad-config-actions">`+button('config-reload',escape(configuration.reload),'refresh')+button('config-save',escape(configuration.save),'check',true)+'</div>';
    root.innerHTML='<div class="ad-config" data-variant="a">'+panel(escape(groups[group]),escape(configuration.groups[group].description),groupIcons[group],`<form id="ad-config-form" novalidate><div class="ad-grid">${fields.map(configField).join('')}</div><div id="ad-config-feedback" class="ad-spacer" aria-live="polite"></div></form>`,footer,metadata)+'</div>';
    if(state==='error'){
      const first=root.querySelector('input');first.setAttribute('aria-invalid','true');
      byId('ad-config-feedback').innerHTML=notice('Beispiel eines ungültigen Werts. Bitte Format und Hinweise prüfen.','error');
    }
    if(state==='unavailable'){
      byId('ad-config-feedback').innerHTML=stateNotice();root.querySelectorAll('input,[data-ad-step],[data-duration-trigger],[data-ad-action=config-save]').forEach(node=>node.disabled=true);
    }
    root.querySelectorAll('[data-duration-amount]').forEach(input=>window.AdminDuration.update(input.closest('.ad-field')));
    syncNumberButtons();
  }
  function renderProviders() {
    const providers=[['google','Google'],['microsoft','Microsoft']];
    const numberOne="<!--\ntags: [one, number, digit, numeral, square, rounded, squircle, box, badge, count, step, numbered]\ncategory: Numbers\nversion: \"1.111\"\nunicode: \"f5c9\"\n-->\n<svg class=\"ad-provider-number\" aria-hidden=\"true\"\n  xmlns=\"http://www.w3.org/2000/svg\"\n  width=\"24\"\n  height=\"24\"\n  viewBox=\"0 0 24 24\"\n  fill=\"none\"\n  stroke=\"currentColor\"\n  stroke-width=\"2\"\n  stroke-linecap=\"round\"\n  stroke-linejoin=\"round\"\n>\n  <path d=\"M10 10l2 -2v8\" />\n  <path d=\"M12 3c7.2 0 9 1.8 9 9c0 7.2 -1.8 9 -9 9c-7.2 0 -9 -1.8 -9 -9c0 -7.2 1.8 -9 9 -9\" />\n</svg>\n";
    const numberTwo="<!--\ntags: [two, number, digit, numeral, square, rounded, squircle, box, badge, count, step, numbered]\ncategory: Numbers\nversion: \"1.111\"\nunicode: \"f5ca\"\n-->\n<svg class=\"ad-provider-number\" aria-hidden=\"true\"\n  xmlns=\"http://www.w3.org/2000/svg\"\n  width=\"24\"\n  height=\"24\"\n  viewBox=\"0 0 24 24\"\n  fill=\"none\"\n  stroke=\"currentColor\"\n  stroke-width=\"2\"\n  stroke-linecap=\"round\"\n  stroke-linejoin=\"round\"\n>\n  <path d=\"M10 8h3a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-2a1 1 0 0 0 -1 1v2a1 1 0 0 0 1 1h3\" />\n  <path d=\"M12 3c7.2 0 9 1.8 9 9c0 7.2 -1.8 9 -9 9c-7.2 0 -9 -1.8 -9 -9c0 -7.2 1.8 -9 9 -9\" />\n</svg>\n";
    const numberThree="<!--\ntags: [three, number, digit, numeral, square, rounded, squircle, box, badge, count, step, numbered]\ncategory: Numbers\nversion: \"1.111\"\nunicode: \"f5cb\"\n-->\n<svg class=\"ad-provider-number\" aria-hidden=\"true\"\n  xmlns=\"http://www.w3.org/2000/svg\"\n  width=\"24\"\n  height=\"24\"\n  viewBox=\"0 0 24 24\"\n  fill=\"none\"\n  stroke=\"currentColor\"\n  stroke-width=\"2\"\n  stroke-linecap=\"round\"\n  stroke-linejoin=\"round\"\n>\n  <path d=\"M10 9a1 1 0 0 1 1 -1h2a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-2h2a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1\" />\n  <path d=\"M12 3c7.2 0 9 1.8 9 9c0 7.2 -1.8 9 -9 9c-7.2 0 -9 -1.8 -9 -9c0 -7.2 1.8 -9 9 -9\" />\n</svg>\n";
    const externalLink="<!--\ntags: [external-link, open-in-new, new-tab, new-window, link, launch, redirect, outbound, arrow, url, external]\ncategory: System\nversion: \"1.0\"\nunicode: \"ea99\"\n-->\n<svg aria-hidden=\"true\"\n  xmlns=\"http://www.w3.org/2000/svg\"\n  width=\"24\"\n  height=\"24\"\n  viewBox=\"0 0 24 24\"\n  fill=\"none\"\n  stroke=\"currentColor\"\n  stroke-width=\"2\"\n  stroke-linecap=\"round\"\n  stroke-linejoin=\"round\"\n>\n  <path d=\"M12 6h-6a2 2 0 0 0 -2 2v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-6\" />\n  <path d=\"M11 13l9 -9\" />\n  <path d=\"M15 4h5v5\" />\n</svg>\n";
    const bot="<!-- @license lucide-static v1.63.0 - ISC -->\n<svg\n  class=\"lucide lucide-bot ad-provider-bot\" aria-hidden=\"true\"\n  xmlns=\"http://www.w3.org/2000/svg\"\n  width=\"24\"\n  height=\"24\"\n  viewBox=\"0 0 24 24\"\n  fill=\"none\"\n  stroke=\"currentColor\"\n  stroke-width=\"2\"\n  stroke-linecap=\"round\"\n  stroke-linejoin=\"round\"\n>\n  <path d=\"M12 8V4H8\" />\n  <rect width=\"16\" height=\"12\" x=\"4\" y=\"8\" rx=\"2\" />\n  <path d=\"M2 14h2\" />\n  <path d=\"M20 14h2\" />\n  <path d=\"M15 13v2\" />\n  <path d=\"M9 13v2\" />\n</svg>\n";
    const logo=provider=>`<img src="assets/provider-${provider}.${provider==='google'?'png':'svg'}" alt="" aria-hidden="true">`;
    const statuses=providers.map(([provider,name])=>{
      const active=provider==='google';
      const hint=active?`${name} ist für neue Anmeldungen freigegeben. Die Zugangsdaten werden beim Anmeldeversuch geprüft.`:`${name} ist für neue Anmeldungen deaktiviert. Bestehende Sitzungen bleiben erhalten.`;
      return `<span class="ad-provider-status" data-state="${active?'active':'inactive'}" tabindex="0" aria-label="${name}: ${active?'Aktiv':'Inaktiv'}" data-ref-tooltip="${escape(hint)}">${logo(provider)}<span>${active?'Aktiv':'Inaktiv'}</span></span>`;
    }).join('');
    const tabs=providers.map(([provider,name],index)=>`<button type="button" role="tab" id="ad-provider-tab-${provider}" data-provider-tab="${provider}" aria-controls="ad-provider-${provider}" aria-selected="${index===0}" tabindex="${index===0?0:-1}">${logo(provider)}<span>${name}</span></button>`).join('');
    const fieldHelp=(provider,field)=>{
      const descriptions={
        google:{client:'Kopieren Sie die Client-ID Ihrer Webanwendung aus der Google-Verwaltung. Die Client-ID ist nicht das Client-Secret.',secret:'Kopieren Sie das Client-Secret derselben Webanwendung aus der Google-Verwaltung. Ein neu eingegebenes Geheimnis ersetzt beim Speichern das bisherige. Bereits gespeicherte Geheimnisse werden nicht angezeigt.'},
        microsoft:{client:'Kopieren Sie die Anwendungs-ID (Client-ID) aus der App-Registrierung in Microsoft Entra. Verwenden Sie weder die Objekt-ID noch die Verzeichnis-ID.',secret:'Kopieren Sie unter Zertifikate und Geheimnisse den Wert des geheimen Clientschlüssels, nicht dessen ID. Der Wert ist nur direkt nach dem Erstellen sichtbar. Ein neu eingegebenes Geheimnis ersetzt beim Speichern das bisherige.'}
      };
      const rules={client:'Zum Aktivieren erforderlich. Maximal 512 Zeichen. Keine Steuerzeichen.',secret:'Zum Aktivieren erforderlich, falls noch kein Geheimnis gespeichert ist. Leer lassen, um das gespeicherte Geheimnis beizubehalten. Maximal 2048 Zeichen. Keine Steuerzeichen.',enabled:'Eine Aktivierung erfordert eine Client-ID und ein Client-Secret. Änderungen werden erst mit Einstellungen speichern übernommen.'};
      const description=field==='enabled'?'Erlaubt nach dem Speichern neue Anmeldungen über diesen Anbieter. Beim Ausschalten bleiben bestehende Sitzungen erhalten. Die Zugangsdaten werden erst beim nächsten Anmeldeversuch geprüft.':descriptions[provider][field];
      return `<details><summary>Feldhilfe</summary><p>${description}</p><span data-help-rules-label>Eingaberegeln</span><ul><li>${rules[field]}</li></ul></details>`;
    };
    const panels=providers.map(([provider,name],index)=>{
      const google=provider==='google';
      const guide=google?'Erstellen Sie in der Google-Verwaltung eine Anwendung vom Typ Webanwendung und hinterlegen Sie die folgende Rücksprungadresse.':'Registrieren Sie in Microsoft Entra eine Anwendung für private sowie Firmen- und Schulkonten. Hinterlegen Sie die folgende Rücksprungadresse als Web-Adresse.';
      const consoleUrl=google?'https://console.cloud.google.com/auth/clients':'https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade';
      const redirect=`https://portal.example.test/login/oauth2/code/${provider}`;
      return `<section id="ad-provider-${provider}" class="ad-provider-content" role="tabpanel" aria-labelledby="ad-provider-tab-${provider}" ${index?'hidden':''}>
        <form id="ad-provider-form-${provider}" data-provider-form="${provider}" class="ad-provider-fields" novalidate>
          <div class="ad-provider-assistant-intro"><span class="ad-provider-assistant-kicker">${bot}Einrichtungsassistent</span><h3>${name}-Anmeldung einrichten</h3><p>Richten Sie den Anbieter ein, übernehmen Sie die Zugangsdaten und geben Sie die Anmeldung frei.</p></div>
          <section class="ad-provider-step"><span class="ad-provider-step-label">${numberOne}Anbieter einrichten</span><div class="ad-provider-step-body"><div class="ad-provider-console-guide"><p>${guide}</p><a class="ad-provider-console-link" href="${consoleUrl}" target="_blank" rel="noopener noreferrer">${externalLink}<span>Anbieter-Verwaltung öffnen</span></a></div>
          <div class="ad-provider-redirect"><div class="ad-field"><label for="ad-provider-${provider}-redirect">Rücksprungadresse beim Anbieter hinterlegen</label><input id="ad-provider-${provider}-redirect" value="${redirect}" readonly></div><button type="button" class="portal-button" data-provider-copy="${provider}">Kopieren</button></div></div></section>
          <section class="ad-provider-step"><span class="ad-provider-step-label">${numberTwo}Zugangsdaten hinterlegen</span><div class="ad-provider-credentials"><div class="ad-field"><label for="ad-provider-${provider}-client">Client-ID<span class="ad-required"> •</span></label><input data-provider-help="${provider}" id="ad-provider-${provider}-client" value="comparison-${provider}" placeholder="Client-ID des Anbieters eingeben" maxlength="512">${fieldHelp(provider,'client')}</div>
          <div class="ad-field"><label for="ad-provider-${provider}-secret">Client-Secret<span class="ad-required"> •</span></label><div class="ad-input-action"><input data-provider-help="${provider}" data-secret-stored="true" id="ad-provider-${provider}-secret" type="password" placeholder="Bereits hinterlegt" autocomplete="new-password" maxlength="2048"><button type="button" data-ad-reveal="ad-provider-${provider}-secret" aria-label="Passwort anzeigen" aria-pressed="false">${icon('eye')}</button></div><small>Leer lassen, um das hinterlegte Geheimnis beizubehalten.</small>${fieldHelp(provider,'secret')}</div></div></section>
          <section class="ad-provider-step"><span class="ad-provider-step-label">${numberThree}Anmeldung aktivieren</span><div class="ad-field ad-toggle"><label for="ad-provider-${provider}-enabled">Anmeldung erlauben</label><input data-provider-help="${provider}" type="checkbox" id="ad-provider-${provider}-enabled" ${google?'checked':''}>${fieldHelp(provider,'enabled')}</div></section>
        </form><div class="ad-foot"><div class="ad-config-save-hint">${icon('info')}<span>Nach dem Speichern gilt die Freigabe für neue Anmeldungen. Bestehende Sitzungen bleiben erhalten. Zugangsdaten werden beim nächsten Anmeldeversuch geprüft. Bei einer Deaktivierung benötigen betroffene Benutzer einen anderen Anmeldeweg.</span></div>
        <div class="ad-config-actions"><button type="button" class="portal-button" data-provider-reload="${provider}">${icon('refresh')}Werte neu laden</button><button type="button" class="portal-button primary" data-provider-save="${provider}">${icon('check')}Einstellungen speichern</button></div></div>
      </section>`;
    }).join('');
    root.innerHTML=`<div class="ad-config ad-providers"><article class="ad-card"><div class="ad-head ad-provider-panel-header"><span class="portal-reference-icon" aria-hidden="true">${icon('cloud-lock')}</span><div><h2>Anmeldeanbieter</h2><p>Richten Sie Google und Microsoft als zusätzliche Anmeldemöglichkeiten ein.<br>Die Anmeldung erfolgt über OpenID Connect auf Basis von OAuth 2.0.</p></div><div class="ad-provider-statuses">${statuses}</div></div><div class="ad-provider-tabs" role="tablist" aria-label="Anmeldeanbieter">${tabs}</div>${panels}</article></div>`;
    root.querySelectorAll('[data-provider-form]').forEach(updateProviderAvailability);
  }
  function updateProviderAvailability(form){
    const provider=form.dataset.providerForm;
    const client=form.querySelector(`#ad-provider-${provider}-client`),secret=form.querySelector(`#ad-provider-${provider}-secret`),enabled=form.querySelector(`#ad-provider-${provider}-enabled`);
    const complete=client.value.trim()!==''&&(secret.value.trim()!==''||secret.dataset.secretStored==='true');
    enabled.disabled=!complete&&!enabled.checked;
  }
  root.addEventListener('input',event=>{
    const form=event.target.closest('[data-provider-form]');
    if(form){updateProviderAvailability(form);}
  });
  function selectProvider(provider, focus=false) {
    root.querySelectorAll('[data-provider-tab]').forEach(tab=>{
      const selected=tab.dataset.providerTab===provider;
      tab.setAttribute('aria-selected',String(selected));
      tab.tabIndex=selected?0:-1;
      byId(tab.getAttribute('aria-controls')).hidden=!selected;
      if(selected&&focus){tab.focus();}
    });
  }
  root.addEventListener('click',event=>{
    const tab=event.target.closest('[data-provider-tab]');
    if(tab){selectProvider(tab.dataset.providerTab);return;}
    const copy=event.target.closest('[data-provider-copy]');
    if(copy){
      const field=byId(`ad-provider-${copy.dataset.providerCopy}-redirect`);
      field.select();
      toast('Gestaltungsvorschau: Die Beispieladresse ist zum Kopieren markiert.');
    }
    const reload=event.target.closest('[data-provider-reload]');
    if(reload){const form=byId(`ad-provider-form-${reload.dataset.providerReload}`);form.reset();updateProviderAvailability(form);toast('Beispielwerte neu geladen.');}
    if(event.target.closest('[data-provider-save]')){toast('Gestaltungsvorschau: Es wurden keine Anbietereinstellungen gespeichert.');}
  });
  root.addEventListener('keydown',event=>{
    const tab=event.target.closest('[data-provider-tab]');
    if(!tab||!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)){return;}
    const tabs=[...root.querySelectorAll('[data-provider-tab]')];
    const index=tabs.indexOf(tab);
    const next=event.key==='Home'?0:event.key==='End'?tabs.length-1:(index+(event.key==='ArrowRight'?1:-1)+tabs.length)%tabs.length;
    event.preventDefault();selectProvider(tabs[next].dataset.providerTab,true);
  });
  function syncNumberButtons(){
    root.querySelectorAll('.ad-number-control').forEach(control=>{
      const input=control.querySelector('input'),value=input.valueAsNumber;
      control.querySelectorAll('[data-ad-step]').forEach(button=>{
        const increasing=button.dataset.adStep==='1',limit=increasing?input.max:input.min;
        const atLimit=limit!==''&&Number.isFinite(value)&&(increasing?value>=Number(limit):value<=Number(limit));
        button.disabled=input.disabled||input.readOnly||atLimit;
      });
    });
  }
  function saveConfig() {
    const inputs=[...root.querySelectorAll('[data-config-id]')];
    const invalid=root.querySelector('[data-duration-amount]:invalid')||inputs.find(input=>!input.checkValidity());
    if(invalid){invalid.setAttribute('aria-invalid','true');invalid.focus();byId('ad-config-feedback').innerHTML=notice('Bitte den markierten Wert prüfen.','error');return;}
    inputs.forEach(input=>{values[input.dataset.configId]=input.type==='checkbox'?input.checked:input.value;input.removeAttribute('aria-invalid');});
    // Dies ist nur der lokale Entwurf; vollständige Servicevalidierung bleibt Aufgabe der Anwendung.
    byId('ad-config-feedback').innerHTML=notice('Beispielwerte dieses Bereichs lokal übernommen. Es wurde keine Anwendungskonfiguration gespeichert.','success');
  }

  function renderMail() {
    const fields=[['recipient','ad-mail-to','email','jonas.weber@example.test'],['subject','ad-mail-subject','text',mail.defaultSubject],['body','ad-mail-body','textarea',mail.defaultBody]].map(([key,id,type,value])=>{
      const attributes=`id="${id}" data-mail-field="${key}" required placeholder="${escape(inventory.mail.placeholders[key])}"`;
      const input=type==='textarea'?`<textarea ${attributes} aria-describedby="ad-mail-body-hint">${escape(value)}</textarea>`:`<input ${attributes} type="${type}" value="${escape(value)}">`;
      const hint=key==='body'?`<small id="ad-mail-body-hint">${escape(mail.bodyHint)}</small>`:'';
      return `<div class="ad-field"><label for="${id}">${escape(mail[key])}<span class="ad-required"> •</span></label>${input}${hint}</div>`;
    }).join('');
    const status=inventory.mail.status[state]||inventory.mail.status.ready;
    const metadata=`<span class="ad-pill ad-mail-badge" aria-label="${escape(mail.cardBadge+': '+status)}"><i class="ad-dot" style="--dot:${state==='maintenance'?'#fbbf24':state==='unavailable'?'#64748b':'#34d399'}"></i>${escape(mail.cardBadge)}</span>`;
    const footer=`<div class="ad-config-save-hint ad-mail-hint">${icon('info')}<div><p>${escape(mail.sendHint)}</p><button type="button" class="ad-mail-settings-link" data-ad-action="go-smtp">${icon('open-section')}${escape(configuration.groups.SMTP.label)}</button></div></div><div class="ad-config-actions">${button('mail-send',escape(mail.send),'send',true)}</div>`;
    root.innerHTML='<div class="ad-mail" data-variant="a">'+panel(escape(mail.cardTitle),escape(mail.cardDescription),'mail',`<form id="ad-mail-form" class="ad-stack" novalidate>${fields}</form>`,footer,metadata)+'</div>';
    if(blocked()){root.querySelectorAll('input,textarea,[data-ad-action=mail-send]').forEach(node=>node.disabled=true);}
    if(state==='error'){mailNotification('Simulation: '+mail.error,'error');}
    if(state==='validation'){byId('ad-mail-to').value='';byId('ad-mail-to').setAttribute('aria-invalid','true');mailNotification(mail.validationRecipient,'error');}
    if(state==='success'){mailSuccess();}
    byId('ad-mail-form').addEventListener('submit',event=>{event.preventDefault();sendMail();});
    byId('ad-mail-form').addEventListener('keydown',event=>{
      if(event.key==='Enter'&&['ad-mail-to','ad-mail-subject'].includes(event.target.id)){event.preventDefault();sendMail();}
    });
  }
  function mailNotification(message,severity){
    window.PublicNotifications.show(message,severity,inventory.mail.notificationTitles[severity],severity==='error'?-1:undefined);
  }
  function mailSuccess(){mailNotification('Simulation: '+mail.success,'success');}
  function sendMail(){
    if(blocked()){return;}
    const fields=[['ad-mail-to',mail.validationRecipient],['ad-mail-subject',mail.validationSubject],['ad-mail-body',mail.validationBody]];
    fields.forEach(([id])=>byId(id).removeAttribute('aria-invalid'));
    const invalid=fields.find(([id])=>!byId(id).value.trim()||!byId(id).validity.valid);
    if(invalid){const [id,message]=invalid;byId(id).setAttribute('aria-invalid','true');byId(id).focus();mailNotification(message,'error');return;}
    mailSuccess();
  }

  function notificationFormat(value, count) {
    const template=typeof value==='object' ? value[count===1?'one':'other'] : value;
    return template.replaceAll('{0}',String(count));
  }
  function notificationFeedback(message, severity='error', title=notifications.notificationTitles[severity]) {
    window.PublicNotifications.show(message,severity,title,severity==='error'?-1:undefined);
  }
  function recipientSelection() {
    if(state==='no-recipients'){return [];}
    if(byId('ad-all').checked){return targets;}
    return [...root.querySelectorAll('[data-recipient]:checked')].map(node=>targets[Number(node.dataset.recipient)]);
  }
  function positionRecipientPopover() {
    const popover=byId('ad-recipient-options');
    if(!popover?.matches(':popover-open')){return;}
    const bounds=byId('ad-recipient-toggle').getBoundingClientRect();
    const width=Math.min(bounds.width,window.innerWidth-16);
    const below=window.innerHeight-bounds.bottom-12;
    const above=bounds.top-12;
    const height=Math.min(280,Math.max(below,above));
    popover.style.width=width+'px';
    popover.style.maxHeight=Math.max(80,height)+'px';
    popover.style.left=Math.max(8,Math.min(bounds.left,window.innerWidth-width-8))+'px';
    popover.style.top=(below>=180||below>=above?bounds.bottom+6:Math.max(8,bounds.top-height-6))+'px';
  }
  function refreshRecipientField() {
    const all=byId('ad-all').checked;
    byId('ad-recipient-summary').hidden=!all;
    byId('ad-recipient-picker').hidden=all;
    byId('ad-recipient-label').htmlFor=all?'ad-recipient-summary':'ad-recipient-toggle';
    byId('ad-recipient-summary').value=notificationFormat(notificationText.everyonePlaceholder,state==='no-recipients'?0:targets.length);
    const selected=[...root.querySelectorAll('[data-recipient]:checked')];
    const count=selected.length;
    byId('ad-recipient-choice-label').textContent=notificationFormat(notificationText.selectedUsers,count);
    byId('ad-recipient-toggle').classList.toggle('ad-recipient-placeholder',count===0);
    if(all&&byId('ad-recipient-options').matches(':popover-open')){byId('ad-recipient-options').hidePopover();}
  }
  function renderCompose() {
    previewValid=false;previewRevision=0;composedRevision=0;
    if(state==='permission-denied'){
      root.innerHTML=panel(escape(notificationText.title),'Superadmin','shield',notice(escape(notificationText.accessDenied),'error'));return;
    }
    const recipientChoices=(state==='no-recipients'?[]:targets).map((target,index)=>`<label class="au-checkbox ad-recipient-choice"><input type="checkbox" data-recipient="${index}"><span><strong>${escape(target.name)}</strong><small>${escape(target.email)}</small></span></label>`).join('')||`<div class="ad-empty">${escape(notificationText.noRecipients)}</div>`;
    const form=`<form id="ad-compose-form" class="ad-stack" novalidate>
      <label class="au-checkbox ad-notification-choice"><input id="ad-all" type="checkbox" checked aria-describedby="ad-everyone-hint"><span><strong>${escape(notificationText.everyone)}</strong><small id="ad-everyone-hint" aria-hidden="true">${escape(notificationText.everyoneHint)}</small></span></label>
      <div class="ad-field"><label id="ad-recipient-label" for="ad-recipient-summary">${escape(notificationText.recipients)}<span class="ad-required"> •</span></label>
        <input id="ad-recipient-summary" type="text" readonly disabled>
        <div id="ad-recipient-picker" hidden>
          <button id="ad-recipient-toggle" type="button" class="ad-recipient-trigger" popovertarget="ad-recipient-options" aria-labelledby="ad-recipient-label ad-recipient-choice-label" aria-haspopup="dialog" aria-expanded="false" aria-controls="ad-recipient-options"><span id="ad-recipient-choice-label"></span>${icon('chevron')}</button>
        </div>
        <div id="ad-recipient-options" popover="auto" role="dialog" aria-label="${escape(notificationText.selectRecipients)}" class="ad-recipient-popover">${recipientChoices}</div>
      </div>
      <div class="ad-field"><label for="ad-notification-title">${escape(notificationText.subject)}<span class="ad-required"> •</span></label><input id="ad-notification-title" maxlength="${notifications.titleLimit}" required placeholder="${escape(notificationText.subject)}" value="Wartungsfenster am Wochenende"><small id="ad-title-count" class="ad-count" aria-hidden="true"></small></div>
      <div class="ad-field"><label for="ad-notification-body">${escape(notificationText.body)}<span class="ad-required"> •</span></label><textarea id="ad-notification-body" maxlength="${notifications.bodyLimit}" required placeholder="${escape(notificationText.body)}">Am Samstag steht das Portal vorübergehend nicht zur Verfügung. Vielen Dank für Ihr Verständnis.</textarea><small id="ad-body-count" class="ad-count" aria-hidden="true"></small></div>
    </form>`;
    const badge=`<span class="ad-pill ad-delivery-badge" data-state="${blocked()?state:'ready'}" role="status" aria-label="${escape(notificationText.internal+': '+notifications.status[blocked()?state:'ready'])}">${escape(notificationText.internal)}</span>`;
    const composeFooter=`<div class="ad-config-save-hint">${icon('info')}<p>${escape(notificationText.deliveryInfo)}</p></div><div class="ad-config-save-hint">${icon('info')}<p>Prüfen Sie die Vorschau nach Änderungen erneut.</p></div><div class="ad-config-actions">${button('notification-preview',escape(notificationText.preview),'eye')}</div>`;
    const previewFooter=`<div class="ad-config-save-hint">${icon('info')}<p>Vor dem Senden die Empfänger prüfen.</p></div><div class="ad-config-actions">${button('notification-send',escape(notificationText.send),'send',true)}</div>`;
    const historyFooter=historyPaginationFooter();
    root.innerHTML=`<div class="ad-notifications ad-stack"><div class="ad-compose" data-variant="b"><div class="ad-compose-panel">${panel(escape(notificationText.composeTitle),'Mitteilung erstellen, Vorschau prüfen und innerhalb der Anwendung senden.','bell',form,composeFooter,badge)}</div><div class="ad-preview-column">${panel('Vorschau','Nach jeder Änderung erneut prüfen.','eye','<div id="ad-message-preview"></div>',previewFooter)}</div></div><div class="ad-notification-history">${panel(escape(notificationText.sentHistory),escape(notificationText.historyDescription),'bell','<div id="ad-history" class="ad-history"></div>',historyFooter,'<span id="ad-history-count" class="ad-pill ad-config-count"></span>')}</div></div>`;
    const composeForm=byId('ad-compose-form');
    composeForm.addEventListener('input',event=>{
      if(['ad-notification-title','ad-notification-body'].includes(event.target.id)){invalidatePreview();}
    });
    byId('ad-all').addEventListener('change',()=>{refreshRecipientField();invalidatePreview();});
    root.querySelectorAll('[data-recipient]').forEach(node=>node.addEventListener('change',()=>{refreshRecipientField();invalidatePreview();}));
    byId('ad-recipient-options').addEventListener('toggle',event=>{
      byId('ad-recipient-toggle').setAttribute('aria-expanded',String(event.newState==='open'));
      if(event.newState==='open'){positionRecipientPopover();}
    });
    byId('ad-history-size').addEventListener('change',()=>{historyPageSize=Number(byId('ad-history-size').value);historyPage=0;renderHistory();});
    refreshRecipientField();updateCounts();clearMessagePreview();renderHistory();
    if(blocked()){
      composeForm.querySelectorAll('input,textarea,button').forEach(node=>node.disabled=true);
      root.querySelector('[data-ad-action=notification-preview]').disabled=true;
      notificationFeedback(state==='maintenance'?notificationText.deliveryPaused:notificationText.unavailable);
    }
    setSendState();
    if(state==='validation'){
      byId('ad-notification-title').value='';createPreview();
    }
    if(['preview-valid','preview-invalidated','success'].includes(state)){
      createPreview();
      if(state==='preview-invalidated'){byId('ad-notification-title').value+=' · aktualisiert';invalidatePreview();}
      if(state==='success'){sendNotification();}
    }
    if(state==='error'){markRecipientError(notificationText.recipientsChanged);notificationFeedback(notificationText.recipientsChanged);}
  }
  function clearMessagePreview() {
    byId('ad-message-preview').removeAttribute('data-created');
    byId('ad-message-preview').innerHTML=`<div class="ad-notification-preview-empty">${icon('eye')}<p>${escape(notificationText.preview)}</p></div>`;
  }
  function updateCounts() {
    byId('ad-title-count').textContent=`${byId('ad-notification-title').value.length} / ${notifications.titleLimit}`;
    byId('ad-body-count').textContent=`${byId('ad-notification-body').value.length} / ${notifications.bodyLimit}`;
  }
  function clearNotificationValidation() {
    root.querySelectorAll('.ad-compose [aria-invalid=true]').forEach(node=>{node.removeAttribute('aria-invalid');node.removeAttribute('title');});
  }
  function markRecipientError(message=notificationText.noRecipients) {
    const field=byId(byId('ad-all').checked?'ad-recipient-summary':'ad-recipient-toggle');
    field.setAttribute('aria-invalid','true');field.setAttribute('title',message);
  }
  function setSendState(){root.querySelector('[data-ad-action=notification-send]').disabled=!previewValid||blocked();}
  function invalidatePreview() {
    composedRevision++;updateCounts();clearNotificationValidation();
    previewValid=false;setSendState();
    const status=byId('ad-preview-status');
    if(status){status.innerHTML=icon('warning')+escape(notificationText.previewNeedsReview);status.dataset.tone='warning';}
  }
  function createPreview() {
    if(blocked()){return;}
    clearNotificationValidation();
    previewValid=false;setSendState();
    const title=byId('ad-notification-title').value.trim();
    const body=byId('ad-notification-body').value.trim();
    const recipients=recipientSelection();
    const messages=[];
    for(const [id,value,limit,error] of [['ad-notification-title',title,notifications.titleLimit,notificationText.invalidSubject],['ad-notification-body',body,notifications.bodyLimit,notificationText.invalidBody]]){
      if(!value||value.length>limit){byId(id).setAttribute('aria-invalid','true');byId(id).setAttribute('title',error);messages.push(error);}
    }
    if(!recipients.length){markRecipientError();messages.push(notificationText.noRecipients);}
    if(messages.length){
      const invalid=root.querySelector('.ad-compose [aria-invalid=true]:not(:disabled)');
      invalid?.focus();
      notificationFeedback(notificationText.previewFailedMessage+' '+messages.join(' '),'error',notificationText.previewFailedTitle);return;
    }
    const avatars=recipients.slice(0,3).map(target=>`<span class="au-avatar" role="img" aria-label="${escape(target.name)}">${escape(target.name.split(' ').map(part=>part[0]).join(''))}</span>`).join('');
    const more=recipients.length>3?`<span class="au-avatar ad-preview-more">+${recipients.length-3}</span>`:'';
    const recipientToggle=`<button type="button" class="ad-preview-recipient-toggle" data-ad-preview-details aria-expanded="false" aria-controls="ad-preview-recipient-list"><span data-detail-label>${escape(notificationText.recipientDetails)}</span>${icon('chevron')}</button>`;
    const recipientDetails=`<div id="ad-preview-recipient-list" class="ad-preview-recipient-details ad-recipient-list" hidden>${recipients.map(target=>`<div><strong>${escape(target.name)}</strong><span>${escape(target.email)}</span></div>`).join('')}</div>`;
    byId('ad-message-preview').innerHTML=`<div class="ad-preview-review-row"><div class="ad-history-label">${icon('history-message')}<span>${escape(notificationText.contentPreview)}</span></div><div id="ad-preview-status" class="ad-preview-status" data-tone="success" role="status">${icon('check')}${escape(notificationText.previewUpdated)}</div></div><div class="ad-preview ad-preview-content"><small class="ad-preview-meta"><span class="ad-preview-source">${escape(notificationText.sourceAdministration)}</span><span>27.10.2026 · 08:30</span></small><h3>${escape(title)}</h3><p>${escape(body)}</p></div><div class="ad-preview-audience"><span class="ad-label">${icon('users')}<span>${escape(notificationText.recipients)}</span></span><div class="ad-preview-targets">${avatars}${more}${recipientToggle}</div>${recipientDetails}</div>`;
    byId('ad-message-preview').dataset.created='true';
    previewValid=true;previewRevision=composedRevision;setSendState();
  }
  function sendNotification() {
    if(blocked()||!previewValid||previewRevision!==composedRevision){return;}
    if(state==='empty'){history.length=0;state='ready';publish(null);}
    history.unshift({title:byId('ad-notification-title').value.trim(),body:byId('ad-notification-body').value.trim(),time:'27.10.2026 · 08:30',recipients:recipientSelection()});
    byId('ad-notification-title').value='';byId('ad-notification-body').value='';
    previewValid=false;setSendState();clearNotificationValidation();clearMessagePreview();updateCounts();historyPage=0;
    renderHistory();
    notificationFeedback('Simulation: '+notificationText.sent,'success');
  }
  function historyPaginationFooter() {
    const footer=paginationReference.cloneNode(true);
    const select=footer.querySelector('.c-page-select select');
    select.id='ad-history-size';
    footer.querySelector('label').htmlFor=select.id;
    select.innerHTML=notifications.pageSizes.map(size=>`<option value="${size}" ${size===historyPageSize?'selected':''}>${size}</option>`).join('');
    footer.querySelector('.c-page-range').id='ad-history-total';
    const navigation=footer.querySelector('.c-page-nav');
    navigation.id='ad-history-pages';
    navigation.setAttribute('aria-label','Seiten der Benachrichtigungen');
    navigation.innerHTML='';
    return footer.outerHTML;
  }
  function pagination(total,page) {
    const pages=Math.ceil(total/historyPageSize);
    const referenceButtons=[...paginationReference.querySelectorAll('.c-page-nav button')];
    const arrow=(next,reference,disabled)=>{
      const button=reference.cloneNode(true);
      button.dataset.adHistoryPage=String(next);
      button.disabled=disabled;
      button.removeAttribute('aria-current');
      return button.outerHTML;
    };
    const numbered=Array.from({length:pages},(_,next)=>`<button type="button" class="c-icon-button" data-ad-history-page="${next}" aria-label="${escape(notificationFormat(notifications.pagination.Page,next+1))}" ${next===page?'aria-current="page"':''}>${next+1}</button>`).join('');
    return arrow(0,referenceButtons[0],page===0)+arrow(page-1,referenceButtons[1],page===0)+numbered+arrow(page+1,referenceButtons.at(-2),page===pages-1)+arrow(pages-1,referenceButtons.at(-1),page===pages-1);
  }
  function renderHistory() {
    const visible=state==='empty'?[]:history;
    historyPage=Math.min(historyPage,Math.max(0,Math.ceil(visible.length/historyPageSize)-1));
    byId('ad-history-count').textContent=notificationFormat(notificationText.historyCount,visible.length);
    const first=visible.length?historyPage*historyPageSize+1:0;
    const last=Math.min((historyPage+1)*historyPageSize,visible.length);
    byId('ad-history-total').textContent=`${first}–${last} von ${visible.length}`;
    byId('ad-history').innerHTML=visible.length?visible.slice(historyPage*historyPageSize,(historyPage+1)*historyPageSize).map(entry=>{
      const metadata=entry.time+' · '+notificationFormat(notificationText.historyRecipients,entry.recipients.length);
      return `<details class="ad-sent-entry" data-ad-history-entry><summary><span class="ad-sent-icon" aria-hidden="true">${icon('bell')}</span><span class="ad-sent-heading"><strong>${escape(entry.title)}</strong><small>${escape(metadata)}</small></span><span class="ad-sent-status">${icon('check')}${escape(notificationText.sentStatus)}</span><span class="portal-button ad-sent-detail-toggle"><span>Details</span>${icon('chevron')}</span></summary><div class="ad-history-body"><div class="ad-history-message"><div class="ad-history-label">${icon('history-message')}<span>Nachrichteninhalt</span></div><p>${escape(entry.body)}</p><div class="ad-history-note" data-ad-visual-example><span>Zustellung: Internes Postfach</span><span>Unveränderliche Versandprotokollierung</span></div></div></div></details>`;
    }).join(''):`<div class="ad-empty">${escape(notificationText.empty)}</div>`;
    byId('ad-history-pages').innerHTML=visible.length?pagination(visible.length,historyPage):'';
  }
  root.addEventListener('click',event=>{
    const toggle=event.target.closest('button[data-ad-preview-details]');
    if(!toggle){return;}
    const list=byId(toggle.getAttribute('aria-controls'));
    list.hidden=!list.hidden;
    toggle.setAttribute('aria-expanded',String(!list.hidden));
  });
  window.addEventListener('resize',()=>{if(route==='send'){positionRecipientPopover();}});
  root.addEventListener('submit',event=>event.preventDefault());
  function closeDuration(){
    root.querySelectorAll('.ad-duration-options').forEach(menu=>menu.hidden=true);
    root.querySelectorAll('[data-duration-trigger]').forEach(trigger=>trigger.setAttribute('aria-expanded','false'));
  }
  root.addEventListener('input',event=>{if(event.target.matches('[data-duration-amount]')){window.AdminDuration.update(event.target.closest('.ad-field'));syncNumberButtons();}});
  root.addEventListener('keydown',event=>{
    const option=event.target.closest('[data-duration-unit]'),trigger=event.target.closest('[data-duration-trigger]');
    if(event.key==='Escape'&&(option||trigger)){const active=event.target.closest('.ad-field').querySelector('[data-duration-trigger]');closeDuration();active.focus();event.preventDefault();}
    if(option&&['ArrowDown','ArrowUp','Home','End'].includes(event.key)){
      const options=[...option.closest('.ad-duration-options').querySelectorAll('[data-duration-unit]')],index=options.indexOf(option);
      const next=event.key==='Home'?0:event.key==='End'?options.length-1:(index+(event.key==='ArrowDown'?1:-1)+options.length)%options.length;
      options[next].focus();event.preventDefault();
    }
    if(event.target.matches('[data-duration-amount]')&&['ArrowUp','ArrowDown'].includes(event.key)){event.preventDefault();window.AdminDuration.step(event.target,event.key==='ArrowUp'?1:-1);}
    if(event.key==='Tab'){closeDuration();}
  });
  document.addEventListener('click',event=>{if(!event.target.closest('.ad-duration-unit')){closeDuration();}});
  root.addEventListener('input',syncNumberButtons);
  root.addEventListener('click',event=>{
    const unitTrigger=event.target.closest('[data-duration-trigger]');
    if(unitTrigger){
      const menu=unitTrigger.parentNode.querySelector('.ad-duration-options'),opening=menu.hidden;closeDuration();
      menu.hidden=!opening;unitTrigger.setAttribute('aria-expanded',String(opening));
      if(opening){menu.querySelector('[aria-selected=true]').focus();}return;
    }
    const unitOption=event.target.closest('[data-duration-unit]');
    if(unitOption){
      const wrapper=unitOption.closest('.ad-field');
      wrapper.querySelectorAll('[data-duration-unit]').forEach(option=>option.setAttribute('aria-selected',String(option===unitOption)));
      wrapper.querySelector('[data-duration-trigger] span').textContent=unitOption.firstChild.textContent;
      closeDuration();window.AdminDuration.update(wrapper);syncNumberButtons();wrapper.querySelector('[data-duration-trigger]').focus();return;
    }
    const step=event.target.closest('[data-ad-step]');
    if(step){
      const input=byId(step.dataset.adInput);
      if(input.disabled||input.readOnly){return;}
      if(input.matches('[data-duration-amount]')){window.AdminDuration.step(input,Number(step.dataset.adStep));input.focus({preventScroll:true});return;}
      input.stepUp(Number(step.dataset.adStep));
      input.dispatchEvent(new Event('input',{bubbles:true}));
      input.dispatchEvent(new Event('change',{bubbles:true}));
      input.focus({preventScroll:true});return;
    }
    const reveal=event.target.closest('[data-ad-reveal]');
    if(reveal){const input=byId(reveal.dataset.adReveal);input.type=input.type==='password'?'text':'password';reveal.setAttribute('aria-pressed',String(input.type==='text'));reveal.setAttribute('aria-label',input.type==='text'?'Passwort verbergen':'Passwort anzeigen');return;}
    const historyButton=event.target.closest('[data-ad-history-page]');
    if(historyButton){historyPage=Number(historyButton.dataset.adHistoryPage);renderHistory();byId('ad-history-pages').querySelector('[aria-current=page]')?.focus();return;}
    const action=event.target.closest('[data-ad-action]')?.dataset.adAction;
    if(!action){return;}
    if(action==='go-users'){navigate('users');}
    if(action==='go-send'){navigate('send');}
    if(action==='go-settings'){navigate('settings');}
    if(action==='go-mail'){navigate('mail');}
    if(['go-users','go-send','go-settings','go-mail'].includes(action)){
      byId('portalTitle').setAttribute('tabindex','-1');byId('portalTitle').focus();return;
    }
    if(action==='go-smtp'){group='SMTP';navigate('settings');}
    if(action==='config-reload'){renderConfig();toast('Gesicherte Beispielwerte dieses Bereichs neu geladen.');}
    if(action==='config-save'){saveConfig();}
    if(action==='mail-send'){sendMail();}
    if(action==='notification-preview'){createPreview();}
    if(action==='notification-send'){sendNotification();}
  });
  window.addEventListener('message',event=>{
    if(parent===window||event.source!==parent||(event.origin!==location.origin&&!(location.protocol==='file:'&&event.origin==='null'))||event.data?.type!=='cosmos-admin-proposal'){return;}
    if(['a','b'].includes(event.data.variant)){variant=event.data.variant;}
    if(Object.hasOwn(groups,event.data.group)){group=event.data.group;}
    const states=['ready','empty','maintenance','unavailable','error','validation','success','permission-denied','preview-valid','preview-invalidated','no-recipients'];
    navigate(event.data.route,states.includes(event.data.state)?event.data.state:'ready',Number.isSafeInteger(event.data.requestId)?event.data.requestId:null);
  });
  navigate('dashboard','ready',0);
})();
