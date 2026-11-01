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
    const height = Math.min(overlay.scrollHeight, innerHeight - topLimit - 12);
    const below = bounds.bottom + 6;
    const above = bounds.top - height - 6;
    overlay.style.top = `${below + height <= innerHeight - 12 ? below : above >= topLimit ? above : Math.max(topLimit, innerHeight - height - 12)}px`;
  };
  window.CosmosPreview = {positionOverlay};
})();

(() => {
  const byId = id => document.getElementById(id);
  const languageTrigger = byId('spracheTrigger');
  const languageList = byId('languageDropdown');
  const userTrigger = byId('userMenuTrigger');
  const userMenu = byId('userMenuPopover');
  const mobileTrigger = byId('mobileMenuBtn');
  const statusTrigger = byId('sidebarStatusTrigger');
  const statusPopover = byId('sidebarStatusPopover');
  const closeStatus = (focus = false) => { statusPopover.hidden = true; statusTrigger.setAttribute('aria-expanded','false'); if (focus) { statusTrigger.focus(); } };
  const navigation = byId('sidebarNav');
  const navigationDialog = byId('mobileNavigation');
  const navigationShade = byId('mobileNavigationShade');
  const navigationParent = navigation.parentNode;
  const navigationNext = navigation.nextSibling;
  const compactNavigation = window.matchMedia('(max-width:767px), (max-width:1023px) and (max-height:500px)');
  const initialOverflow = document.documentElement.style.overflow;
  const options = [...languageList.querySelectorAll('[role=option]')];
  let language = 'de';
  let activeLanguage = 0;
  let identityScenario = '';
  let feedbackTimer;

  // Nur lokale Vorschauzustände. Keine Dienste, Speicherung oder Netzaufrufe.
  const notify = message => {
    const feedback = byId('previewFeedback');
    feedback.textContent = message;
    feedback.hidden = false;
    clearTimeout(feedbackTimer);
    feedbackTimer = setTimeout(() => { feedback.hidden = true; }, 4200);
  };
  const setNavigation = (open, restoreFocus = false) => {
    closeStatus();
    if (open && !compactNavigation.matches) { return; }
    if (open) {
      navigationDialog.append(navigation);
      navigation.classList.remove('hidden');
      document.body.classList.add('navigation-open');
      document.documentElement.style.overflow = 'hidden';
      document.body.style.setProperty('--navigation-top',`${Math.max(0,document.querySelector('header').getBoundingClientRect().bottom)}px`);
      document.body.style.setProperty('--navigation-bottom',`${document.querySelector('footer').getBoundingClientRect().height}px`);
      document.querySelector('main').inert = true;
      navigationShade.hidden = false;
      if (!navigationDialog.open) { navigationDialog.show(); }
      mobileTrigger.focus();
    } else {
      if (navigationDialog.open) { navigationDialog.close(); }
      navigationParent.insertBefore(navigation,navigationNext);
      navigation.classList.add('hidden');
      navigationShade.hidden = true;
      document.querySelector('main').inert = false;
      document.body.classList.remove('navigation-open');
      document.documentElement.style.overflow = initialOverflow;
    }
    mobileTrigger.setAttribute('aria-expanded', String(open));
    mobileTrigger.setAttribute('aria-label', open ? 'Menü schließen' : 'Menü öffnen');
    if (restoreFocus) { mobileTrigger.focus(); }
  };
  navigationShade.addEventListener('click', () => setNavigation(false,true));
  navigationDialog.addEventListener('cancel', event => { event.preventDefault(); setNavigation(false,true); });
  navigationDialog.addEventListener('click', event => {
    if (event.target !== navigationDialog) { return; }
    const bounds = navigationDialog.getBoundingClientRect();
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) { setNavigation(false,true); }
  });
  const positionOverlay = window.CosmosPreview.positionOverlay;
  const markActiveLanguage = index => {
    activeLanguage = index;
    options.forEach((option, position) => { option.dataset.active = String(index === position); });
    languageTrigger.setAttribute('aria-activedescendant', options[index].id);
  };
  const closeLanguage = (restoreFocus = false) => {
    languageList.hidden = true;
    languageTrigger.setAttribute('aria-expanded', 'false');
    languageTrigger.removeAttribute('aria-activedescendant');
    if (restoreFocus) { languageTrigger.focus(); }
  };
  const closeUser = (restoreFocus = false) => {
    userMenu.hidden = true;
    userTrigger.setAttribute('aria-expanded', 'false');
    if (restoreFocus) { userTrigger.focus(); }
  };
  const openLanguage = () => {
    closeUser();
    closeStatus();
    languageList.hidden = false;
    languageTrigger.setAttribute('aria-expanded', 'true');
    markActiveLanguage(options.findIndex(option => option.dataset.language === language));
    positionOverlay(languageList, languageTrigger);
  };
  const chooseLanguage = value => {
    language = value;
    byId('selectedLanguageText').textContent = value === 'de' ? 'Deutsch' : 'Englisch';
    languageTrigger.dataset.value = value;
    options.forEach(option => option.setAttribute('aria-selected', String(option.dataset.language === value)));
  userMenu.querySelectorAll('[data-menu-language]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.menuLanguage === value)));
    closeLanguage();
    notify('Sprachwahl nur als Vorschau. Die Oberfläche bleibt in dieser Runde Deutsch.');
  };
  languageTrigger.addEventListener('click', () => { languageList.hidden ? openLanguage() : closeLanguage(); });
  languageTrigger.addEventListener('keydown', event => {
    if (event.key === 'Escape') { if (!languageList.hidden) { event.stopPropagation(); } closeLanguage(true); return; }
    if (event.key === 'Tab') { if (!languageList.hidden) { chooseLanguage(options[activeLanguage].dataset.language); } return; }
    if (['Enter', ' '].includes(event.key)) {
      event.preventDefault();
      if (languageList.hidden) { openLanguage(); } else { chooseLanguage(options[activeLanguage].dataset.language); }
      return;
    }
    if (['ArrowDown','ArrowUp','Home','End'].includes(event.key)) {
      event.preventDefault();
      const wasClosed = languageList.hidden;
      if (wasClosed) { openLanguage(); }
      if (event.key === 'Home') { markActiveLanguage(0); }
      else if (event.key === 'End') { markActiveLanguage(options.length - 1); }
      else if (!wasClosed) { markActiveLanguage(Math.max(0, Math.min(options.length - 1, activeLanguage + (event.key === 'ArrowDown' ? 1 : -1)))); }
      return;
    }
    // Genau ein D oder E (Groß-/Kleinschreibung egal) springt zur passenden Sprachoption.
    if (/^[de]$/i.test(event.key)) {
      event.preventDefault();
      if (languageList.hidden) { openLanguage(); }
      markActiveLanguage(event.key.toLowerCase() === 'd' ? 0 : 1);
    }
  });
  options.forEach((option, index) => {
    option.addEventListener('pointermove', () => markActiveLanguage(index));
    option.addEventListener('pointerdown', event => event.preventDefault());
    option.addEventListener('click', () => { chooseLanguage(option.dataset.language); languageTrigger.focus(); });
  });
  languageTrigger.addEventListener('blur', () => {
    if (!languageList.hidden) { chooseLanguage(options[activeLanguage].dataset.language); }
  });

  userTrigger.addEventListener('click', () => {
    if (navigationDialog.open) { setNavigation(false); }
    closeStatus();
    const open = userMenu.hidden;
    closeLanguage();
    userMenu.hidden = !open;
    userTrigger.setAttribute('aria-expanded', String(open));
    if (open) { positionOverlay(userMenu, userTrigger, 340); }
  });
  const chooseStatus = value => {
    const selected = userMenu.querySelector(`[data-status="${value}"]`);
    document.body.style.setProperty('--presence-color', selected.dataset.color);
    document.querySelectorAll('[data-presence-label]').forEach(label => { label.textContent = selected.dataset.label; });
    document.querySelectorAll('[data-presence-dot]').forEach(dot => { dot.setAttribute('aria-label', selected.dataset.label); });
    document.querySelectorAll('[data-status]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.status === value)));
    document.body.dataset.presence = value;
  };
statusTrigger.addEventListener('click', () => { const open = statusPopover.hidden; closeUser(); closeLanguage(); statusPopover.hidden = !open; statusTrigger.setAttribute('aria-expanded',String(open)); if (open) { positionOverlay(statusPopover,statusTrigger,320); statusPopover.querySelector('[aria-pressed=true]').focus(); } });
  document.querySelectorAll('[data-status]').forEach(button => button.addEventListener('click', () => { chooseStatus(button.dataset.status); if (statusPopover.contains(button)) { closeStatus(true); } }));
  userMenu.querySelectorAll('[data-menu-language]').forEach(button => button.addEventListener('click', () => chooseLanguage(button.dataset.menuLanguage)));
  document.addEventListener('click', event => { if (event.target.closest('#spracheTrigger,#userMenuTrigger,#sidebarStatusTrigger,#mobileMenuBtn')) { document.dispatchEvent(new Event('cosmos-close-calendar')); } });
  mobileTrigger.addEventListener('click', () => { closeUser(); closeLanguage(); setNavigation(mobileTrigger.getAttribute('aria-expanded') !== 'true'); });
  document.addEventListener('keydown', event => {
    if (event.key !== 'Escape') { return; }
    if (!statusPopover.hidden) { closeStatus(true); }
    else if (!languageList.hidden) { closeLanguage(true); }
    else if (!userMenu.hidden) { closeUser(true); }
    else if (mobileTrigger.getAttribute('aria-expanded') === 'true') { setNavigation(false, true); }
  });
  document.addEventListener('pointerdown', event => {
    if (!statusTrigger.contains(event.target) && !statusPopover.contains(event.target)) { closeStatus(); }
    if (!languageTrigger.contains(event.target) && !languageList.contains(event.target)) { closeLanguage(); }
    if (!userTrigger.contains(event.target) && !userMenu.contains(event.target)) { closeUser(); }
  });
  document.addEventListener('cosmos-close-overlays', () => { closeStatus(); closeUser(); closeLanguage(); });
  document.addEventListener('portal-close-overlays', () => { if (navigationDialog.open) { setNavigation(false); } closeStatus(); closeUser(); closeLanguage(); });
  document.addEventListener('portal-feedback', event => notify(event.detail));
  const reposition = () => {
    if (navigationDialog.open && !compactNavigation.matches) { setNavigation(false); }
    else if (navigationDialog.open) {
      document.body.style.setProperty('--navigation-top',`${Math.max(0,document.querySelector('header').getBoundingClientRect().bottom)}px`);
      document.body.style.setProperty('--navigation-bottom',`${document.querySelector('footer').getBoundingClientRect().height}px`);
    }
    if (!statusPopover.hidden) { positionOverlay(statusPopover,statusTrigger,320); }
    if (!languageList.hidden) { positionOverlay(languageList, languageTrigger); }
    if (!userMenu.hidden) { positionOverlay(userMenu, userTrigger, 340); }
  };
  window.addEventListener('resize', reposition);
  window.addEventListener('scroll', reposition, {passive:true,capture:true});
  const applyIdentity = scenario => {
    if (scenario === identityScenario) { return; }
    identityScenario = scenario;
    const long = scenario === 'long';
    const first = long ? 'Alexandra Maximiliane' : 'Design';
    const last = long ? 'von Morgenstern-Hohenfels-Winterberg' : 'Stitch';
    const email = long ? 'alexandra.maximiliane.von.morgenstern-hohenfels-winterberg@example.test' : 'design@stitch.de';
    const name = `${first} ${last}`;
    document.querySelectorAll('[data-identity-name]').forEach(node => { node.textContent = name; });
    document.querySelectorAll('[data-identity-email]').forEach(node => { node.textContent = email; });
    document.querySelectorAll('[data-initials]').forEach(node => { node.firstChild.textContent = long ? 'AM' : 'DS'; });
    byId('vorname').value = first;
    byId('nachname').value = last;
    byId('email').value = email;
    userTrigger.setAttribute('aria-label', `Benutzermenü für ${name} öffnen`);
    if (!userMenu.hidden) { reposition(); }
  };
  window.addEventListener('message', event => {
    if (event.source !== window.parent || event.data?.type !== 'cosmos-preview') { return; }
    if (['stitch','cosmos'].includes(event.data.background)) { document.body.dataset.background = event.data.background; }
    if (['short','long'].includes(event.data.identity)) { applyIdentity(event.data.identity); }
  });
  document.querySelectorAll('[data-demo-action]').forEach(button => button.addEventListener('click', event => {
    event.preventDefault();
    if (navigationDialog.open) { setNavigation(false,true); }
    notify(button.dataset.demoAction === 'save' ? 'Gestaltungsvorschau: Änderungen wurden nicht gespeichert.' : 'Diese Aktion folgt mit den weiteren Seiten und Dialogen der Portalvorschau.');
    if (userMenu.contains(button)) { closeUser(true); }
  }));
  document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => event.preventDefault()));
  applyIdentity('short');
  chooseStatus('available');
  document.body.dataset.background = 'stitch';
  if (document.body.dataset.initialOverlay === 'language') { openLanguage(); }
  if (document.body.dataset.initialOverlay === 'status') {
    userMenu.hidden = false;
    userTrigger.setAttribute('aria-expanded','true');
    positionOverlay(userMenu,userTrigger,340);
  }
})();

(() => {
  const byId = id => document.getElementById(id);
  const input = byId('geburtsdatum');
  const trigger = byId('calendarTrigger');
  const calendar = byId('calendarDropdown');
  const grid = byId('calendarGrid');
  const years = byId('calendarYears');
  const today = new Date();
  let visible = new Date(2006,9,1);
  let yearMode = false;
  let selected = new Date(2006,9,23);
  const sameDay = (left,right) => left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
  const close = (focus = false) => { calendar.hidden = true; trigger.setAttribute('aria-expanded','false'); if (focus) { trigger.focus(); } };
  const position = () => window.CosmosPreview.positionOverlay(calendar,input,320,'start');
  const choose = date => {
    selected = date;
    input.value = `${String(date.getDate()).padStart(2,'0')}.${String(date.getMonth()+1).padStart(2,'0')}.${date.getFullYear()}`;
    close(true);
  };
  const render = () => {
    const year = visible.getFullYear();
    const month = visible.getMonth();
    const startYear = year-((year-1)%12);
    byId('calendarMonth').textContent = yearMode ? `${startYear} – ${startYear+11}` : visible.toLocaleDateString('de-DE',{month:'long',year:'numeric'});
    byId('calendarPrevious').setAttribute('aria-label',yearMode ? 'Vorherige Jahre' : 'Vorheriger Monat');
    byId('calendarNext').setAttribute('aria-label',yearMode ? 'Nächste Jahre' : 'Nächster Monat');
    byId('calendarDays').hidden = yearMode;
    years.hidden = !yearMode;
    grid.replaceChildren();
    years.replaceChildren();
    if (yearMode) {
      for (let value=startYear;value<startYear+12;value++) {
        const button=document.createElement('button');
        button.type='button'; button.textContent=String(value); button.setAttribute('aria-pressed',String(value===year));
        button.addEventListener('click',()=>{visible=new Date(value,month,1);yearMode=false;render();byId('calendarHeading').focus();});
        years.append(button);
      }
    } else {
      const offset=(new Date(year,month,1).getDay()+6)%7;
      const count=new Date(year,month+1,0).getDate();
      const cells=Math.ceil((offset+count)/7)*7;
      for (let index=0;index<cells;index++) {
        const date=new Date(year,month,index-offset+1);
        const button=document.createElement('button'); button.type='button'; button.textContent=String(date.getDate());
        button.disabled=date.getMonth()!==month;
        button.setAttribute('aria-label',date.toLocaleDateString('de-DE',{day:'numeric',month:'long',year:'numeric'}));
        button.setAttribute('aria-pressed',String(sameDay(date,selected)));
        if (sameDay(date,today)) { button.setAttribute('aria-current','date'); }
        button.tabIndex=sameDay(date,selected) || ((selected.getMonth()!==month || selected.getFullYear()!==year) && date.getDate()===1) ? 0 : -1;
        button.dataset.day=String(date.getDate());
        button.addEventListener('click',()=>choose(date));
        grid.append(button);
      }
    }
    position();
  };
  const open = () => {
    document.dispatchEvent(new Event('cosmos-close-overlays'));
    // Genau Tag.Monat.Jahr aus dem sichtbaren DE-Beispielfeld auslesen, keine Fachvalidierung ersetzen.
    const match=/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/.exec(input.value);
    if (match) {
      const parsed=new Date(Number(match[3]),Number(match[2])-1,Number(match[1]));
      if (parsed.getDate()===Number(match[1]) && parsed.getMonth()===Number(match[2])-1) { selected=parsed; }
    }
    visible=new Date(selected.getFullYear(),selected.getMonth(),1); yearMode=false;
    calendar.hidden=false; trigger.setAttribute('aria-expanded','true'); render();
    const active=grid.querySelector('[aria-pressed=true]'); if (active) { active.focus(); }
  };
  trigger.addEventListener('click',()=>calendar.hidden ? open() : close());
  input.addEventListener('keydown',event=>{if(event.key==='ArrowDown'){event.preventDefault();open();}});
  byId('calendarHeading').addEventListener('click',()=>{yearMode=!yearMode;render();});
  byId('calendarPrevious').addEventListener('click',()=>{visible=yearMode ? new Date(visible.getFullYear()-12,visible.getMonth(),1) : new Date(visible.getFullYear(),visible.getMonth()-1,1);render();});
  byId('calendarNext').addEventListener('click',()=>{visible=yearMode ? new Date(visible.getFullYear()+12,visible.getMonth(),1) : new Date(visible.getFullYear(),visible.getMonth()+1,1);render();});
  byId('calendarToday').addEventListener('click',()=>choose(new Date(today.getFullYear(),today.getMonth(),today.getDate())));
  byId('calendarCancel').addEventListener('click',()=>close(true));
  grid.addEventListener('keydown',event=>{
    const steps={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7};
    const day=Number(event.target.dataset.day);
    if (!day || !(event.key in steps)) { return; }
    event.preventDefault();
    const next=new Date(visible.getFullYear(),visible.getMonth(),day+steps[event.key]);
    if(next.getMonth()!==visible.getMonth() || next.getFullYear()!==visible.getFullYear()){visible=new Date(next.getFullYear(),next.getMonth(),1);render();}
    const target=grid.querySelector(`button[data-day="${next.getDate()}"]:not([disabled])`);
    grid.querySelectorAll('button').forEach(button=>{button.tabIndex=button===target ? 0 : -1;}); if(target){target.focus();}
  });
  document.addEventListener('keydown',event=>{if(event.key==='Escape' && !calendar.hidden){event.preventDefault();close(true);}});
  document.addEventListener('pointerdown',event=>{if(!calendar.contains(event.target) && !trigger.contains(event.target)){close();}});
  document.addEventListener('cosmos-close-calendar',()=>close());
  window.addEventListener('resize',position); window.addEventListener('scroll',position,{capture:true,passive:true});
  if(document.body.dataset.initialOverlay==='calendar'){open();}
})();

(() => {
  const byId = id => document.getElementById(id);
  const bell = byId('portalBell');
  const routes = {
    home:['PORTAL','Startseite','Ihr persönlicher Einstieg in das Portal.'],
    notifications:['PORTAL','Benachrichtigungen','Alle Hinweise, Systemmeldungen und Aktualisierungen an einem Ort.'],
    profile:['MEIN KONTO','Persönliche Daten','Verwalten Sie Ihre persönlichen Angaben und Ihre bevorzugte Sprache.'],
    security:['MEIN KONTO','Sicherheit','Verwalten Sie Ihr Passwort und Ihre E-Mail-Adresse.'],
    sessions:['MEIN KONTO','Geräte & Sitzungen','Überprüfen Sie Ihre aktiven Sitzungen und gespeicherten Anmeldungen.']
  };
  let returnFocus = null;
  let avatarMode = 'initials';
  let avatarHasImage = false;
  let savedAvatar = false;
  let savedAvatarMode = 'initials';
  let savedAvatarCrop = {x:0, y:0, zoom:'1'};
  let cropX = 0;
  let cropY = 0;
  const notify = text => document.dispatchEvent(new CustomEvent('portal-feedback',{detail:text}));
  const closeOtherOverlays = () => { document.dispatchEvent(new Event('portal-close-overlays')); document.dispatchEvent(new Event('cosmos-close-calendar')); };
  const restoreFocus = () => {
    const target = returnFocus?.isConnected && returnFocus.getClientRects().length ? returnFocus : byId('mobileMenuBtn').getClientRects().length ? byId('mobileMenuBtn') : bell;
    target?.focus({preventScroll:true});
  };
  const closeDialog = dialog => { if(dialog.open){dialog.close();} };
  const switchRoute = (next,focus=true) => {
    if(!routes[next]){return;}
    closeOtherOverlays();
    document.querySelectorAll('.portal-modal[open]').forEach(closeDialog);
    document.querySelectorAll('[data-portal-page]').forEach(node=>{node.hidden=node.dataset.portalPage!==next;});
    const [group,title,description]=routes[next];
    byId('portalGroup').textContent=group;
    byId('portalTitle').replaceChildren(document.createTextNode(title));
    const dot=document.createElement('span');dot.textContent='.';dot.style.color='var(--accent)';byId('portalTitle').append(dot);
    byId('portalDescription').textContent=description;
    document.querySelectorAll('[data-portal-route]').forEach(button=>{if(button.dataset.portalRoute===next && button.closest('#sidebarNav')){button.setAttribute('aria-current','page');}else{button.removeAttribute('aria-current');}});
    window.scrollTo({top:0});
    if(focus){document.querySelector(`[data-portal-page="${next}"]`).focus({preventScroll:true});}
  };
  window.CosmosPreview.navigate = switchRoute;
  const setPasswordReveal = (button, visible) => {
    const input = byId(button.dataset.reveal);
    const label = input.closest('.portal-field').querySelector('label').textContent.replace('•', '').trim();
    input.type = visible ? 'text' : 'password';
    button.setAttribute('aria-pressed', String(visible));
    const action = `${label} ${visible ? 'verbergen' : 'anzeigen'}`;
    button.setAttribute('aria-label', action);
    button.dataset.tooltip = action;
  };
  const openDialog = kind => {
    const dialog=byId(`${kind}Dialog`);
    if(!dialog){return;}
    returnFocus=document.activeElement;
    closeOtherOverlays();
    document.querySelectorAll('.portal-modal[open]').forEach(closeDialog);
    if(kind==='avatar'){resetAvatarDraft();}
    dialog.showModal();
  };
  const selectAvatarMode = mode => {
    avatarMode=mode;
    document.querySelectorAll('[data-avatar-mode]').forEach(button=>{const active=button.dataset.avatarMode===mode;button.setAttribute('aria-pressed',String(active));button.classList.toggle('active',active);});
    byId('avatarImageControls').hidden=mode!=='image';
    byId('avatarPreview').replaceChildren();
    if(mode==='image' && avatarHasImage){byId('avatarPreview').append(byId('avatarCropFrame').querySelector('svg').cloneNode(true));}
    else{const initials=document.createElement('span');initials.dataset.avatarInitials='';initials.textContent=document.body.dataset.identityScenario==='long'?'AM':document.querySelector('[data-initials]').firstChild.textContent.trim();byId('avatarPreview').append(initials);}
  };
  const updateCrop = () => {
    const value=Number(byId('avatarZoom').value);
    byId('avatarZoomValue').textContent=`${Math.round(value*100)} %`;
    const transform=`translate(${cropX / 240 * 100}%,${cropY / 240 * 100}%) scale(${value})`;
    byId('avatarCropFrame').querySelector('svg').style.transform=transform;
    const previewImage=byId('avatarPreview').querySelector('svg');
    if(previewImage){previewImage.style.transform=transform;}
  };
  const resetAvatarDraft = () => {
    avatarHasImage=savedAvatar;cropX=savedAvatarCrop.x;cropY=savedAvatarCrop.y;byId('avatarZoom').value=savedAvatarCrop.zoom;updateCrop();
    byId('avatarCropControls').hidden=!avatarHasImage;byId('avatarDelete').hidden=!avatarHasImage;
    selectAvatarMode(savedAvatarMode);
  };
  document.querySelectorAll('.portal-modal').forEach(dialog=>{
    dialog.addEventListener('close',()=>{
      dialog.querySelectorAll('input').forEach(input=>{if(input.type!=='range'){input.value='';}});
      dialog.querySelectorAll('[data-reveal]').forEach(button=>{setPasswordReveal(button, false);});
      if(!document.querySelector('.portal-modal[open]')){restoreFocus();}
    });
    dialog.addEventListener('click',event=>{if(event.target===dialog){const bounds=dialog.getBoundingClientRect();if(event.clientX<bounds.left||event.clientX>bounds.right||event.clientY<bounds.top||event.clientY>bounds.bottom){closeDialog(dialog);}}});
  });
  document.addEventListener('click',event=>{
    const button=event.target.closest('button,a[data-portal-route]');if(!button){return;}
    if(button.dataset.portalRoute){event.preventDefault();switchRoute(button.dataset.portalRoute);return;}
    if(button.dataset.openDialog){openDialog(button.dataset.openDialog);return;}
    if(button.hasAttribute('data-close-dialog')){closeDialog(button.closest('dialog'));return;}
    if(button.dataset.portalAction){notify(button.dataset.portalAction==='refresh'?'Vorschau aktualisiert. Die Beispieldaten bleiben unverändert.':button.dataset.portalAction==='revoke'?'Gestaltungsvorschau: Keine Sitzung und keine gespeicherte Anmeldung wurde beendet.':button.dataset.portalAction==='logout'?'Gestaltungsvorschau: Sie wurden nicht abgemeldet.':'Diese Aktion wird hier nur als Gestaltungsvorschau gezeigt.');return;}
    if(button.dataset.submitDemo){
      if(button.dataset.submitDemo==='avatar'){
        savedAvatar=avatarHasImage;
        savedAvatarMode=avatarMode;
        savedAvatarCrop={x:cropX,y:cropY,zoom:byId('avatarZoom').value};
        document.querySelectorAll('.reference-avatar').forEach(avatar=>{
          avatar.querySelector('[data-demo-photo]')?.remove();
          if(savedAvatar && savedAvatarMode==='image'){
            const photo=byId('avatarCropFrame').querySelector('svg').cloneNode(true);
            const cropTransform=photo.style.transform;
            photo.dataset.demoPhoto='';
            photo.style.cssText='position:absolute;inset:0;width:100%;height:100%;border-radius:50%';
            photo.style.transform=cropTransform;
            avatar.insertBefore(photo,avatar.querySelector('[data-presence-dot]'));
          }
        });
      }
      closeDialog(button.closest('dialog'));
      notify(button.dataset.submitDemo==='email'?'Gestaltungsvorschau: Keine Bestätigungsmail wurde versendet und keine E-Mail-Adresse geändert.':button.dataset.submitDemo==='password'?'Gestaltungsvorschau: Kein Passwort wurde geändert und keine Sitzung beendet.':'Beispielbild nur in dieser Vorschau übernommen. Kein Bild wurde hochgeladen oder gespeichert.');return;
    }
    if(button.dataset.reveal){setPasswordReveal(button, byId(button.dataset.reveal).type === 'password');return;}
    if(button.dataset.avatarMode){selectAvatarMode(button.dataset.avatarMode);return;}
    if(button.dataset.avatarImage){
      const action=button.dataset.avatarImage;
      if(action==='choose'){avatarHasImage=true;byId('avatarCropControls').hidden=false;byId('avatarDelete').hidden=false;selectAvatarMode('image');notify('Gestaltungsvorschau: Ein synthetisches Beispielbild wurde geladen. Es wird keine Datei hochgeladen oder gespeichert.');}
      if(action==='cancel'){resetAvatarDraft();return;}
      if(action==='delete'){byId('avatar-delete-confirm').showModal();}
      if(action==='reset'||action==='choose'){cropX=0;cropY=0;byId('avatarZoom').value='1';updateCrop();}return;
    }
  });

  const dropZone = document.querySelector('[data-avatar-image="choose"]');
  dropZone.addEventListener('dragover', event => {
    event.preventDefault();
    dropZone.classList.add('avatar-dragover');
  });
  dropZone.addEventListener('dragleave', event => {
    if (!dropZone.contains(event.relatedTarget)) dropZone.classList.remove('avatar-dragover');
  });
  dropZone.addEventListener('drop', event => {
    event.preventDefault();
    dropZone.classList.remove('avatar-dragover');
    if (event.dataTransfer?.files.length) dropZone.click();
  });

  const deleteConfirmation = byId('avatar-delete-confirm');
  deleteConfirmation.querySelectorAll('[data-avatar-delete-close]').forEach(button => button.addEventListener('click', () => deleteConfirmation.close()));
  deleteConfirmation.querySelector('[data-avatar-delete-confirm]').addEventListener('click', () => {
    if (byId('avatarDialog').open) {
      avatarHasImage=false;byId('avatarCropControls').hidden=true;byId('avatarDelete').hidden=true;selectAvatarMode('initials');
    }
    deleteConfirmation.close();
  });
  deleteConfirmation.addEventListener('close', () => { if (byId('avatarDialog').open && !byId('avatarDelete').hidden) { byId('avatarDelete').focus(); } });
  byId('avatarDialog').addEventListener('close', () => deleteConfirmation.close());
  byId('avatarZoom').addEventListener('input',updateCrop);
  byId('avatarCropFrame').addEventListener('keydown',event=>{const steps={ArrowLeft:[-4,0],ArrowRight:[4,0],ArrowUp:[0,-4],ArrowDown:[0,4]};if(steps[event.key]){event.preventDefault();cropX=Math.max(-60,Math.min(60,cropX+steps[event.key][0]));cropY=Math.max(-60,Math.min(60,cropY+steps[event.key][1]));updateCrop();}});
  const entry = location.hash.slice(1);
  switchRoute(['home', 'security', 'sessions'].includes(entry) ? entry : 'profile', false);
})();

(() => {
  const byId = id => document.getElementById(id);
  const iconLibrary = byId('n-icons').content;
  const inbox = byId('n-inbox-rows');
  const unreadTemplate = inbox.querySelector('.inbox-card.unread').cloneNode(true);
  const readTemplate = inbox.querySelector('.inbox-card:not(.unread)').cloneNode(true);
  const preview = byId('n-preview');
  const bell = byId('portalBell');
  const detail = byId('n-detail');
  const confirm = byId('n-confirm');
  const titles = ['Willkommen im Benutzerportal', 'Passwort geändert', 'Wartung am Samstag', 'Neues aus dem Portal'];
  let messages = Array.from({length: 23}, (_, index) => ({
    id: index + 1,
    title: titles[index % titles.length],
    source: index % 4 === 1 ? 'System' : 'Administration',
    system: index % 4 === 1,
    read: index % 2 === 1,
    date: `${String(24 - Math.floor(index / 3)).padStart(2, '0')}.10.2026, ${String(18 - index % 3).padStart(2, '0')}:15 Uhr`,
    body: index % 4 === 1
      ? 'Das Passwort Ihres Benutzerkontos wurde geändert.\n\nFalls Sie diese Änderung nicht selbst vorgenommen haben, überprüfen Sie bitte Ihren Kontozugang und Ihre angemeldeten Geräte.'
      : 'Hier finden Sie einen Hinweis aus dem Benutzerportal.\n\nDiese Nachricht enthält ausschließlich erfundene Beispieldaten für die gemeinsame Designbeurteilung. Auch längere Texte bleiben im Dialog lesbar und können bei Bedarf gescrollt werden.\n\nDie Gestaltung soll wichtige Informationen und die verfügbaren Aktionen klar voneinander trennen.'
  }));
  let filter = 'all';
  let previewFilter = 'all';
  let page = 0;
  let pageSize = 10;
  let openedId = null;
  let fromPreview = false;
  let pending = null;
  let returnTarget = null;
  let feedbackTimeout = null;

  function icon(name) {
    const node = iconLibrary.querySelector(`[data-n-icon="${name}"]`).cloneNode(true);
    node.removeAttribute('data-n-icon');
    return node;
  }

  function fillSymbols(root = document) {
    root.querySelectorAll('[data-n-symbol]').forEach(node => node.replaceChildren(icon(node.dataset.nSymbol)));
  }

  function notify(message) {
    clearTimeout(feedbackTimeout);
    byId('n-feedback').textContent = message;
    byId('n-feedback').hidden = false;
    feedbackTimeout = setTimeout(() => { byId('n-feedback').hidden = true; }, 3500);
  }

  function matching(location) {
    const selected = location === 'preview' ? previewFilter : filter;
    const query = location === 'preview' ? '' : byId('n-search').value.trim().toLocaleLowerCase('de');
    return messages.filter(item => (selected === 'all' || (selected === 'read' ? item.read : !item.read))
      && (!query || `${item.title} ${item.body} ${item.source}`.toLocaleLowerCase('de').includes(query)));
  }

  function visible(location) {
    const items = matching(location);
    return location === 'preview' ? items.slice(0, 5) : items.slice(page * pageSize, (page + 1) * pageSize);
  }

  function iconButton(label, symbol, action, id, danger = false) {
    const node = document.createElement('button');
    node.type = 'button';
    node.className = `c-icon-button${danger ? ' c-danger' : ''}`;
    node.setAttribute('aria-label', label);
    node.dataset.tooltip = label;
    node.dataset.nAction = action;
    if (id !== undefined) { node.dataset.nId = String(id); }
    node.append(icon(symbol));
    return node;
  }

  function rowActions(item, target) {
    target.replaceChildren(iconButton(`Nachricht öffnen: ${item.title}`, 'open', 'open', item.id));
    if (!item.read) { target.append(iconButton(`Als gelesen markieren: ${item.title}`, 'read', 'read', item.id)); }
    target.append(iconButton(`Nachricht löschen: ${item.title}`, 'remove', 'delete', item.id, true));
  }

  function messageMetadata(item) {
    const fragment = document.createDocumentFragment();
    const source = document.createElement('span');
    source.className = `n-origin ${item.system ? 'system' : 'admin'}`;
    source.textContent = item.source;
    source.setAttribute('aria-label', `Herkunft: ${item.source}`);
    const time = document.createElement('span');
    time.className = 'n-row-time';
    time.textContent = item.date;
    const status = document.createElement('span');
    status.className = `n-read-state ${item.read ? 'read' : 'unread'}`;
    status.textContent = item.read ? 'Gelesen' : 'Ungelesen';
    fragment.append(source, time, status);
    return fragment;
  }

  function messageState(node, item) {
    node.classList.toggle('n-message-unread', !item.read);
    node.classList.toggle('n-message-read', item.read);
  }

  function filterLabel(node, label, count) {
    const text = document.createElement('span');
    text.textContent = label;
    const badge = document.createElement('span');
    badge.className = 'n-filter-count';
    badge.textContent = String(count);
    badge.setAttribute('aria-hidden', 'true');
    node.replaceChildren(text, badge);
    node.setAttribute('aria-label', `${label}: ${count} Nachrichten`);
  }

  function inboxRow(item) {
    const row = (item.read ? readTemplate : unreadTemplate).cloneNode(true);
    row.dataset.nMessage = String(item.id);
    row.querySelector('h3').textContent = item.title;
    row.querySelector('p').textContent = item.body.split('\n')[0];
    const metadata = row.firstElementChild.firstElementChild;
    metadata.className = 'n-message-meta';
    metadata.replaceChildren(messageMetadata(item));
    messageState(row, item);
    rowActions(item, row.firstElementChild.lastElementChild);
    return row;
  }

  function previewRow(item) {
    const row = document.createElement('article');
    row.className = `n-preview-row${item.read ? '' : ' unread'}`;
    row.dataset.nMessage = String(item.id);
    const title = document.createElement('h3');
    title.textContent = item.title;
    const body = document.createElement('p');
    body.textContent = item.body.split('\n')[0];
    const meta = document.createElement('div');
    meta.className = 'n-preview-meta n-message-meta';
    meta.append(messageMetadata(item));
    messageState(row, item);
    const actions = document.createElement('div');
    actions.className = 'n-preview-row-actions';
    rowActions(item, actions);
    row.append(title, body, meta, actions);
    row.addEventListener('click', event => {
      if (!event.target.closest('button')) { openDetail(item.id); }
    });
    return row;
  }

  function renderRows(container, items, renderer) {
    container.replaceChildren(...items.map(renderer));
    if (!items.length) {
      const empty = document.createElement('div');
      empty.className = 'n-empty';
      const title = document.createElement('h3');
      title.className = 'n-empty-title';
      title.textContent = container === inbox && byId('n-search').value.trim() ? 'Keine passenden Benachrichtigungen gefunden.' : 'Keine Benachrichtigungen vorhanden.';
      empty.append(icon('bell'), title);
      container.append(empty);
    }
  }

  function pageButton(label, target, symbol, disabled, current = false) {
    const node = iconButton(label, symbol || 'open', 'page');
    node.dataset.nPage = String(target);
    node.disabled = disabled;
    if (!symbol) { node.replaceChildren(document.createTextNode(String(target + 1))); delete node.dataset.tooltip; }
    if (current) { node.setAttribute('aria-current', 'page'); }
    return node;
  }

  function render() {
    const filtered = matching('inbox');
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    page = Math.max(0, Math.min(page, totalPages - 1));
    renderRows(inbox, visible('inbox'), inboxRow);
    renderRows(byId('n-preview-rows'), visible('preview'), previewRow);
    const unread = messages.filter(item => !item.read).length;
    document.querySelectorAll('.c-unread-badge').forEach(node => {
      node.dataset.count = String(unread);
      node.textContent = unread > 99 ? '99+' : String(unread);
    });
    bell.setAttribute('aria-label', `Benachrichtigungen: ${unread} ungelesen`);
    document.querySelector('[data-portal-route="notifications"]').setAttribute('aria-label', `Benachrichtigungen: ${unread} ungelesen`);
    document.querySelectorAll('[data-n-unread]').forEach(node => { node.textContent = String(unread); });
    byId('n-preview-unread-total').setAttribute('aria-label', `${unread} ungelesene Nachrichten insgesamt`);
    byId('n-preview-unread-total').dataset.tooltip = `${unread} ungelesene Nachrichten insgesamt`;
    document.querySelector('.c-counter dd').textContent = String(messages.length);
    document.querySelector('.c-counter-unread dd').textContent = String(unread);
    const filterLabels = {all: ['Alle', messages.length], unread: ['Ungelesen', unread], read: ['Gelesen', messages.length - unread]};
    document.querySelectorAll('[data-n-filter]').forEach(node => {
      const [label, count] = filterLabels[node.dataset.nFilter];
      filterLabel(node, label, count);
      node.setAttribute('aria-pressed', String(node.dataset.nFilter === filter));
      node.classList.toggle('c-filter-active', node.dataset.nFilter === filter);
    });
    document.querySelectorAll('[data-n-preview-filter]').forEach(node => {
      const [label, count] = filterLabels[node.dataset.nPreviewFilter];
      filterLabel(node, label, count);
      node.setAttribute('aria-pressed', String(node.dataset.nPreviewFilter === previewFilter));
    });
    for (const [action, location] of [['read-all', 'inbox'], ['preview-read-all', 'preview']]) {
      document.querySelector(`[data-n-action="${action}"]`).disabled = !visible(location).some(item => !item.read);
    }
    document.querySelector('[data-n-action="delete-read"]').disabled = !messages.some(item => item.read);
    document.querySelector('.c-page-range').textContent = filtered.length ? `${page * pageSize + 1}–${Math.min((page + 1) * pageSize, filtered.length)} von ${filtered.length}` : '0 von 0';
    const pages = [pageButton('Erste Seite', 0, 'first', page === 0), pageButton('Vorherige Seite', page - 1, 'previous', page === 0)];
    for (let index = 0; index < totalPages; index++) { pages.push(pageButton(`Seite ${index + 1}`, index, null, false, index === page)); }
    pages.push(pageButton('Nächste Seite', page + 1, 'next', page === totalPages - 1), pageButton('Letzte Seite', totalPages - 1, 'last', page === totalPages - 1));
    document.querySelector('.c-page-nav').replaceChildren(...pages);
    if (detail.open) { renderDetail(); }
    positionPreview();
  }

  function positionPreview() {
    if (preview.hidden) { return; }
    const bounds = bell.getBoundingClientRect();
    const width = Math.min(440, innerWidth - 24);
    const top = Math.max(12, bounds.bottom + 8);
    preview.style.width = `${width}px`;
    preview.style.left = `${Math.max(12, Math.min(bounds.right - width, innerWidth - width - 12))}px`;
    preview.style.top = `${top}px`;
    preview.style.maxHeight = `${Math.max(120, innerHeight - top - 12)}px`;
  }

  function closePreview(restore = true) {
    preview.hidden = true;
    bell.setAttribute('aria-expanded', 'false');
    if (restore) { bell.focus({preventScroll: true}); }
  }

  function openPreview() {
    document.dispatchEvent(new Event('portal-close-overlays'));
    document.dispatchEvent(new Event('cosmos-close-calendar'));
    preview.hidden = false;
    bell.setAttribute('aria-expanded', 'true');
    render();
    preview.querySelector('button').focus({preventScroll: true});
  }

  function captureReturnFocus() {
    const active = document.activeElement;
    returnTarget = {
      element: active,
      id: active?.dataset.nId,
      action: active?.dataset.nAction,
      location: active?.closest('#n-preview') ? 'preview' : 'inbox'
    };
  }

  function restoreFocus() {
    if (document.querySelector('.n-dialog[open]')) { return; }
    const scope = returnTarget?.location === 'preview' && !preview.hidden ? preview : inbox;
    const replacement = returnTarget?.id ? scope.querySelector(`[data-n-action="${returnTarget.action}"][data-n-id="${returnTarget.id}"]`)
      || scope.querySelector(`[data-n-action="open"][data-n-id="${returnTarget.id}"]`) : null;
    const target = replacement || (returnTarget?.element?.isConnected ? returnTarget.element : null);
    if (target?.getClientRects().length && !target.disabled) { target.focus({preventScroll: true}); }
    else { (scope.getClientRects().length ? scope.querySelector('button') || bell : bell).focus({preventScroll: true}); }
  }

  function renderDetail() {
    const item = messages.find(message => message.id === openedId);
    if (!item) { detail.close(); return; }
    byId('n-detail-subject').textContent = item.title;
    byId('n-detail-body').textContent = item.body;
    detail.querySelector('.n-detail-message').classList.toggle('read', item.read);
    messageState(detail.querySelector('.n-detail-message'), item);
    byId('n-detail-meta').classList.add('n-message-meta');
    byId('n-detail-meta').replaceChildren(messageMetadata(item));
    byId('n-detail-read').hidden = item.read;
    byId('n-detail-account').hidden = !item.system;
    byId('n-detail-history').hidden = !fromPreview;
  }

  function openDetail(id) {
    if (!messages.some(item => item.id === id)) { notify('Keine Benachrichtigung zur Vorschau vorhanden.'); return; }
    captureReturnFocus();
    openedId = id;
    fromPreview = !preview.hidden;
    renderDetail();
    detail.showModal();
  }

  function openConfirmation(kind, ids) {
    if (!ids.length) { notify('Keine passenden Nachrichten für diese Aktion vorhanden.'); return; }
    captureReturnFocus();
    const neutral = kind === 'read';
    const bulk = kind === 'delete-read';
    pending = {kind, ids: [...ids]};
    confirm.classList.toggle('danger', !neutral);
    byId('n-confirm-title').textContent = neutral ? 'Nachrichten als gelesen markieren?' : bulk ? 'Gelesene Benachrichtigungen löschen' : 'Benachrichtigung löschen';
    byId('n-confirm-description').textContent = neutral ? 'Möchten Sie die angezeigten ungelesenen Nachrichten als gelesen markieren?' : bulk ? 'Möchten Sie alle gelesenen Benachrichtigungen aus Ihrem Postfach löschen?' : 'Möchten Sie diese Benachrichtigung aus Ihrem Postfach löschen?';
    byId('n-confirm-explanation').textContent = bulk ? 'Die gelesenen Benachrichtigungen werden anschließend nicht mehr in Ihrer Nachrichtenliste aufgeführt.' : 'Der Eintrag wird anschließend nicht mehr in Ihrer Nachrichtenliste aufgeführt.';
    byId('n-confirm-explanation').hidden = neutral;
    // Beginn Nachrichtenkontext: dieselben Metadaten wie im Nachrichtendetail.
const contextText = document.createElement('div');
contextText.className = 'portal-confirm-text';
const contextTitle = document.createElement('strong');
const selectedMessage = kind === 'delete' ? messages.find(item => item.id === ids[0]) : null;
contextTitle.textContent = selectedMessage?.title || `${ids.length} ${ids.length === 1 ? 'Nachricht' : 'Nachrichten'} betroffen`;
contextText.append(contextTitle);
if (selectedMessage) {
  const metadata = document.createElement('div');
  metadata.className = 'n-detail-meta portal-confirm-metadata';
  metadata.append(messageMetadata(selectedMessage));
  contextText.append(metadata);
}
const contextIcon = document.createElement('span');
contextIcon.className = 'portal-confirm-object';
contextIcon.setAttribute('aria-hidden','true');
contextIcon.append(icon('bell'));
byId('n-confirm-context').replaceChildren(contextIcon,contextText);
// Ende Nachrichtenkontext.
    byId('n-confirm-hint').textContent = neutral ? 'Die Nachrichten bleiben in Ihrem Postfach erhalten.' : 'Es werden ausschließlich Nachrichten aus Ihrem persönlichen Postfach gelöscht.';
    byId('n-confirm-symbol').replaceChildren(icon(neutral ? 'read-all' : 'remove'));
    byId('n-confirm-label').textContent = neutral ? 'Als gelesen markieren' : 'Löschen';
    byId('n-confirm-submit').className = `n-button ${neutral ? 'n-primary' : 'n-danger'}`;
    byId('n-confirm-submit').querySelector('[data-n-symbol]').replaceChildren(icon(neutral ? 'read-all' : 'remove'));
    confirm.showModal();
  }

  function markRead(ids) {
    messages.forEach(item => { if (ids.includes(item.id)) { item.read = true; } });
    render();
  }

  function showState(state) {
    detail.close();
    confirm.close();
    closePreview(false);
    if (state === 'preview') { openPreview(); }
    if (state === 'detail') { openDetail(messages[0]?.id); }
    if (state === 'delete' && messages.length) { openConfirmation('delete', [messages[0].id]); }
    if (state === 'delete-read') { openConfirmation('delete-read', messages.filter(item => item.read).map(item => item.id)); }
    if (state === 'confirm-read') { openConfirmation('read', visible('inbox').filter(item => !item.read).map(item => item.id)); }
  }

  bell.setAttribute('aria-haspopup', 'dialog');
  bell.setAttribute('aria-controls', 'n-preview');
  bell.setAttribute('aria-expanded', 'false');
  bell.addEventListener('click', () => { if (preview.hidden) { openPreview(); } else { closePreview(); } });
  byId('n-search').addEventListener('input', () => { page = 0; render(); });
  byId('c-page-size').addEventListener('change', event => { pageSize = Number(event.target.value); page = 0; render(); });
  for (const dialog of [detail, confirm]) {
    dialog.addEventListener('close', restoreFocus);
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) { return; }
      const bounds = dialog.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) { dialog.close(); }
    });
  }
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !document.querySelector('.n-dialog[open]') && !preview.hidden) { event.preventDefault(); closePreview(); }
  });
  document.addEventListener('click', event => {
    if (!preview.hidden && !preview.contains(event.target) && !bell.contains(event.target) && !document.querySelector('.n-dialog[open]')) { closePreview(false); }
    const button = event.target.closest('button');
    if (!button) { return; }
    if (button.dataset.nClose) {
      if (button.dataset.nClose === 'preview') { closePreview(); }
      else { byId(`n-${button.dataset.nClose}`).close(); }
      return;
    }
    if (button.dataset.nFilter) { filter = button.dataset.nFilter; page = 0; render(); return; }
    if (button.dataset.nPreviewFilter) { previewFilter = button.dataset.nPreviewFilter; render(); return; }
    const action = button.dataset.nAction;
    const id = Number(button.dataset.nId);
    if (action === 'open') { openDetail(id); }
    if (action === 'read') { captureReturnFocus(); markRead([id]); restoreFocus(); }
    if (action === 'delete') { openConfirmation('delete', [id]); }
    if (action === 'detail-delete') { openConfirmation('delete', [openedId]); }
    if (action === 'delete-read') { openConfirmation('delete-read', messages.filter(item => item.read).map(item => item.id)); }
    if (action === 'refresh' || action === 'preview-refresh') { render(); notify('Demo: Ansicht aktualisiert.'); }
    if (action === 'read-all' || action === 'preview-read-all') { markRead(visible(action === 'read-all' ? 'inbox' : 'preview').filter(item => !item.read).map(item => item.id)); }
    if (action === 'detail-read') { markRead([openedId]); detail.querySelector('[data-n-close="detail"]').focus(); }
    if (action === 'confirm' && pending) {
      if (pending.kind === 'read') { markRead(pending.ids); }
      else {
        const closesDetail = detail.open && pending.ids.includes(openedId);
        messages = messages.filter(item => !pending.ids.includes(item.id));
        render();
        if (closesDetail) { detail.close(); }
      }
      pending = null;
      confirm.close();
      notify('Demo: Auswahl übernommen.');
    }
    if (action === 'history') { detail.close(); closePreview(false); if (window.CosmosPreview?.navigate) { window.CosmosPreview.navigate('notifications'); } byId('n-search').focus(); }
    if (action === 'account') { detail.close(); closePreview(false); if (window.CosmosPreview?.navigate) { window.CosmosPreview.navigate('security'); } else { notify('Demo: Dieser Zugang führt im Portal zu den Sicherheitseinstellungen.'); } }
    if (action === 'page') { page = Number(button.dataset.nPage); render(); document.querySelector('.c-page-nav [aria-current="page"]').focus({preventScroll: true}); }
  });
  document.addEventListener('portal-close-overlays', () => { closePreview(false); for (const dialog of [detail, confirm]) { if (dialog.open) { dialog.close(); } } });
  window.addEventListener('resize', positionPreview);
  window.addEventListener('scroll', positionPreview, {passive: true});
  window.addEventListener('message', event => {
    if (event.source === parent && event.origin === location.origin && event.data?.type === 'notification-design-state') { showState(event.data.state); }
  });
  fillSymbols();
  render();
})();

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



(() => {
  window.addEventListener('message', event => {
    if (event.source !== parent || event.data?.type !== 'cosmos-style-preview') { return; }
    if (event.origin !== location.origin && !(location.protocol === 'file:' && event.origin === 'null')) { return; }
    const {surface, icons} = event.data;
    if (!['original', 'panel', 'canvas'].includes(surface) || !['neutral', 'accent'].includes(icons)) { return; }
    if (surface === 'original') {
      delete document.body.dataset.consistencySurface;
      delete document.body.dataset.consistencyIcons;
      return;
    }
    document.body.dataset.consistencySurface = surface;
    document.body.dataset.consistencyIcons = icons;
  });
})();

// Beginn Profil-Geschlechtsauswahl.
(() => {
  const trigger = document.getElementById('profileGenderTrigger');
  const menu = document.getElementById('profileGenderDropdown');
  if (!trigger || !menu) { return; }
  const options = [...menu.querySelectorAll('[role=option]')];
  let active = 0;
  const close = (restore = false) => {
    menu.hidden = true;
    trigger.setAttribute('aria-expanded', 'false');
    trigger.removeAttribute('aria-activedescendant');
    if (restore) { trigger.focus({preventScroll:true}); }
  };
  const mark = index => {
    active = (index + options.length) % options.length;
    options.forEach((option, cursor) => { option.dataset.active = String(cursor === active); });
    trigger.setAttribute('aria-activedescendant', options[active].id);
  };
  const position = () => window.CosmosPreview.positionOverlay(menu, trigger);
  const open = () => {
    document.dispatchEvent(new Event('portal-close-overlays'));
    document.dispatchEvent(new Event('cosmos-close-calendar'));
    menu.hidden = false;
    trigger.setAttribute('aria-expanded', 'true');
    mark(Math.max(0, options.findIndex(option => option.getAttribute('aria-selected') === 'true')));
    position();
  };
  const choose = option => {
    options.forEach(entry => entry.setAttribute('aria-selected', String(entry === option)));
    document.getElementById('selectedProfileGenderText').textContent = option.dataset.gender || 'Geschlecht auswählen';
    document.getElementById('profileGender').value = option.dataset.gender;
    close(true);
  };
  trigger.addEventListener('click', () => menu.hidden ? open() : close());
  trigger.addEventListener('keydown', event => {
    if (!['ArrowDown','ArrowUp','Home','End','Enter',' ','Escape','Tab'].includes(event.key)) { return; }
    if (event.key === 'Tab') { close(); return; }
    if (event.key === 'Escape') { if (!menu.hidden) { event.preventDefault(); event.stopPropagation(); close(true); } return; }
    event.preventDefault();
    if (menu.hidden) { open(); return; }
    if (event.key === 'Enter' || event.key === ' ') { choose(options[active]); return; }
    mark(event.key === 'Home' ? 0 : event.key === 'End' ? options.length - 1 : active + (event.key === 'ArrowUp' ? -1 : 1));
  });
  menu.addEventListener('click', event => { const option = event.target.closest('[role=option]'); if (option) { choose(option); } });
  menu.addEventListener('pointermove', event => { const option = event.target.closest('[role=option]'); if (option) { mark(options.indexOf(option)); } });
  document.addEventListener('pointerdown', event => { if (!menu.contains(event.target) && !trigger.contains(event.target)) { close(); } });
  document.addEventListener('keydown', event => { if (event.key === 'Escape' && !menu.hidden) { close(true); } });
  document.addEventListener('portal-close-overlays', () => close());
  document.addEventListener('cosmos-close-overlays', () => close());
  window.addEventListener('resize', () => { if (!menu.hidden) { position(); } });
  window.addEventListener('scroll', () => { if (!menu.hidden) { position(); } }, {capture:true,passive:true});
})();
// Ende Profil-Geschlechtsauswahl.
