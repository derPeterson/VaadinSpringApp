(() => {
  const byId = id => document.getElementById(id);
  const symbol = name => byId('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  const escape = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const notify = (text, severity = 'normal', title = 'Gestaltungsvorschau') => window.PublicNotifications.show(text, severity, title, severity === 'error' ? 0 : 4000);
  let users = structuredClone(window.AdminFixtures);
  let filters = {};
  let page = 0;
  let sort = {key:'name', direction:1};
  let editedId = null;
  let deletedId = null;
  let editorFocus = null;
  let deleteFocus = null;
  const editor = byId('userEditor');
  const deletion = byId('deleteUserDialog');
  const menu = byId('userMenuPopover');
  const statusIcons = {'Verfügbar':'success','Abwesend':'clock','Beschäftigt':'minus','Offline':'close'};
  const statusColors = {'Verfügbar':'green','Abwesend':'amber','Beschäftigt':'red','Offline':''};
  const statusHex = {'Verfügbar':'#34d399','Abwesend':'#fbbf24','Beschäftigt':'#f87171','Offline':'#94a3b8'};
  const badge = (text, tone = '', icon = '') => `<span class="au-badge ${tone}">${icon ? symbol(icon) : ''}${escape(text)}</span>`;
  const action = (label, icon, attributes = '', danger = false) => `<button type="button" class="portal-button portal-icon-button ${danger ? 'danger' : ''}" aria-label="${escape(label)}" data-tooltip="${escape(label)}" ${attributes}>${symbol(icon)}</button>`;
  const pageTemplates = [...byId('pageNavigation').children].map(button => button.cloneNode(true));
  const pageButton = (label, target, templateIndex, disabled = false, current = false) => {
    const button = pageTemplates[templateIndex].cloneNode(true);
    button.setAttribute('aria-label', label);
    button.dataset.page = String(target);
    button.disabled = disabled;
    button.removeAttribute('aria-current');
    if (templateIndex === 2) {
      button.textContent = String(target + 1);
      delete button.dataset.tooltip;
    } else { button.dataset.tooltip = label; }
    if (current) { button.setAttribute('aria-current', 'page'); }
    return button;
  };

  // Dieselbe Listbox-Interaktion wie in der öffentlichen Referenz, für Adminfelder instanziiert.
  const lists = [...document.querySelectorAll('.au-combo')].map(trigger => ({trigger,menu:byId(trigger.getAttribute('aria-controls')), index:0}));
  const options = list => [...list.menu.querySelectorAll('[role=option]')];
  const closeList = (list, focus = false) => {
    list.menu.hidden = true; list.trigger.setAttribute('aria-expanded','false'); list.trigger.removeAttribute('aria-activedescendant');
    if (focus) { list.trigger.focus(); }
  };
  const closeLists = () => lists.forEach(list => closeList(list));
  const setCombo = (id, value) => {
    const field = byId(id);
    if (field.classList.contains('au-filter-choice')) {
      field.dataset.value = value;
      field.querySelectorAll('input').forEach(input => { input.checked = input.type === 'radio' ? input.value === value : value.split(',').includes(input.value); });
      return;
    }

    const list = lists.find(item => item.trigger.id === id);
    list.trigger.dataset.value = value;
    list.trigger.querySelector('span').textContent = value || (list.trigger.getAttribute('aria-required') === 'true' ? (list.trigger.dataset.refPlaceholder || 'Bitte auswählen') : 'Alle');
    options(list).forEach(option => option.setAttribute('aria-selected',String(option.dataset.value === value)));
  };
  const activate = (list, index) => {
    const entries = options(list); list.index = (index + entries.length) % entries.length;
    entries.forEach((option,cursor) => option.dataset.active = String(cursor === list.index));
    list.trigger.setAttribute('aria-activedescendant',entries[list.index].id);
    entries[list.index].scrollIntoView({block:'nearest'});
  };
  const openList = list => {
    document.dispatchEvent(new Event('cosmos-close-calendar')); closeLists();
    list.menu.hidden = false; list.trigger.setAttribute('aria-expanded','true');
    window.CosmosPreview.positionOverlay(list.menu,list.trigger);
    activate(list,Math.max(0,options(list).findIndex(option => option.getAttribute('aria-selected') === 'true')));
  };
  const choose = (list, option) => {
    setCombo(list.trigger.id,option.dataset.value); list.trigger.removeAttribute('aria-invalid'); closeList(list,true);
  };
  lists.forEach(list => {
    list.trigger.addEventListener('click',() => list.menu.hidden ? openList(list) : closeList(list));
    list.trigger.addEventListener('keydown',event => {
      if (!['ArrowDown','ArrowUp','Home','End','Enter',' ','Escape','Tab'].includes(event.key)) { return; }
      if (event.key === 'Tab') { closeList(list); return; }
      if (event.key === 'Escape') { if (!list.menu.hidden) { event.preventDefault();event.stopPropagation();closeList(list,true); } return; }
      event.preventDefault();
      if (list.menu.hidden) { openList(list); return; }
      if (event.key === 'Enter' || event.key === ' ') { choose(list,options(list)[list.index]); return; }
      activate(list,event.key === 'Home' ? 0 : event.key === 'End' ? options(list).length - 1 : list.index + (event.key === 'ArrowUp' ? -1 : 1));
    });
    list.menu.addEventListener('click',event => { const option = event.target.closest('[role=option]'); if (option) { choose(list,option); } });
  });
  document.addEventListener('pointerdown',event => lists.forEach(list => { if (!list.menu.contains(event.target) && !list.trigger.contains(event.target)) { closeList(list); } }));
  document.addEventListener('cosmos-close-overlays',closeLists);
  const repositionLists = () => lists.forEach(list => { if (!list.menu.hidden) { window.CosmosPreview.positionOverlay(list.menu,list.trigger); } });
  window.addEventListener('resize',repositionLists); window.addEventListener('scroll',repositionLists,{capture:true,passive:true});

  const parseDate = text => {
    // Exakt deutsches Datum TT.MM.JJJJ, ohne normalisierte Überläufe wie 31.02.
    const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(text);
    if (!match) { return null; }
    const date = new Date(Number(match[3]),Number(match[2]) - 1,Number(match[1]));
    return date.getFullYear() === Number(match[3]) && date.getMonth() === Number(match[2]) - 1 && date.getDate() === Number(match[1]) ? date : null;
  };
  const visibleUsers = () => {
    const query = (filters.query || '').trim().toLocaleLowerCase('de');
    const entries = users.filter(user => (!query || `${user.first} ${user.last} ${user.email}`.toLocaleLowerCase('de').includes(query)) && (!filters.enabled || user.enabled === (filters.enabled === 'Ja')) && (!filters.status || filters.status.split(',').includes(user.status)) && (!filters.role || filters.role.split(',').some(role => user.roles.includes(role))) && (!filters.cutoff || new Date(2026,9,user.activity,14,30).getTime() >= filters.cutoff));
    return entries.sort((left,right) => {
      const value = user => sort.key === 'name' ? `${user.first} ${user.last}` : sort.key === 'roles' ? user.roles.join(', ') : user[sort.key];
      const a = value(left); const b = value(right);
      return (typeof a === 'string' ? a.localeCompare(b,'de') : Number(a) - Number(b)) * sort.direction;
    });
  };
  const render = () => {
    const entries = visibleUsers(); const size = Number(byId('pageSize').value); const pages = Math.max(1,Math.ceil(entries.length/size));
    page = Math.max(0,Math.min(page,pages - 1));
    byId('accountCount').textContent = String(users.length);
    byId('pageSummary').textContent = entries.length ? `${page * size + 1}–${Math.min((page + 1) * size, entries.length)} von ${entries.length}` : '0 von 0';
    byId('emptyUsers').hidden = entries.length > 0;
    byId('userRows').innerHTML = entries.slice(page * size,(page + 1) * size).map(user => `<tr><td><div class="au-identity"><span class="au-avatar">${escape(user.first[0] + user.last[0])}</span><span class="au-name">${escape(user.first + ' ' + user.last)}${user.current ? ' <span class="au-self-badge">Dein Benutzer</span>' : ''}</span></div></td><td class="au-email"><span class="au-email-value" tabindex="0" data-tooltip="${escape(user.email)}">${escape(user.email)}</span></td><td>${badge(user.enabled ? 'Ja' : 'Nein',user.enabled ? 'green' : 'red')}</td><td>${badge(user.status,statusColors[user.status],statusIcons[user.status])}</td><td><div class="au-role-list">${user.roles.map(role => badge(role,'role')).join('')}</div></td><td class="au-activity-cell"><time datetime="2026-10-${String(user.activity).padStart(2,'0')}T14:30:00+01:00" title="${user.activity}.10.2026, 14:30 Uhr (Europe/Berlin)">${user.activity === 26 ? 'vor 15 Minuten' : `vor ${26 - user.activity} ${26 - user.activity === 1 ? 'Tag' : 'Tagen'}`}</time></td><td><div class="au-row-actions">${action(user.protected ? 'Geschütztes Konto: Bearbeiten nicht erlaubt' : `Benutzer bearbeiten: ${user.first} ${user.last}`,'edit',`data-edit="${user.id}" ${user.protected ? 'disabled' : ''}`)}${action(user.current ? 'Eigenes Konto kann nicht gelöscht werden' : user.protected ? 'Geschütztes Konto: Löschen nicht erlaubt' : `Benutzer löschen: ${user.first} ${user.last}`,'trash',`data-delete="${user.id}" ${user.current || user.protected ? 'disabled' : ''}`,true)}</div></td></tr>`).join('');
    const navigation = [pageButton('Erste Seite',0,0,page === 0),pageButton('Vorherige Seite',page - 1,1,page === 0)];
    for (let index = 0; index < pages; index++) { navigation.push(pageButton(`Seite ${index + 1}`,index,2,false,index === page)); }
    navigation.push(pageButton('Nächste Seite',page + 1,5,page === pages - 1),pageButton('Letzte Seite',pages - 1,6,page === pages - 1));
    byId('pageNavigation').replaceChildren(...navigation);
    const active = [['query','Suche',filters.query],['enabled','Aktiv',filters.enabled],['status','Status',filters.status],['role','Rolle',filters.role],['cutoff','Letzte Aktivität ab',filters.dateLabel]].filter(item => item[2]);
    byId('filterCount').textContent = `${active.length} aktiv`;
    document.dispatchEvent(new CustomEvent('admin-filter-rendered',{detail:active.length}));
  };

  byId('filterToggle').addEventListener('click',() => document.dispatchEvent(new Event('admin-filter-toggle')));
  byId('filterTime').disabled = true;
  byId('filterTimeTrigger').disabled = true;
  byId('filterTime').placeholder = 'HH:MM';
  byId('activityCalendarInput').addEventListener('input',() => { byId('filterTime').disabled = !parseDate(byId('activityCalendarInput').value); byId('filterTimeTrigger').disabled=byId('filterTime').disabled; if (byId('filterTime').disabled) { byId('filterTime').value = ''; } });
  const timeInput=byId('filterTime'),timeTrigger=byId('filterTimeTrigger'),timeMenu=byId('filterTimeMenu');
  let timeIndex=-1;
  const timeOptions=[...timeMenu.querySelectorAll('[role=option]')];
  const closeTime=(focus=false)=>{timeMenu.hidden=true;timeInput.setAttribute('aria-expanded','false');timeTrigger.setAttribute('aria-expanded','false');timeInput.removeAttribute('aria-activedescendant');if(focus){timeInput.focus();}};
  const activateTime=index=>{timeIndex=(index+timeOptions.length)%timeOptions.length;timeOptions.forEach((option,cursor)=>option.dataset.active=String(cursor===timeIndex));timeInput.setAttribute('aria-activedescendant',timeOptions[timeIndex].id);timeOptions[timeIndex].scrollIntoView({block:'nearest'});};
  const openTime=()=>{if(timeInput.disabled){return;}closeLists();document.dispatchEvent(new Event('cosmos-close-calendar'));timeMenu.hidden=false;timeInput.setAttribute('aria-expanded','true');timeTrigger.setAttribute('aria-expanded','true');timeIndex=timeOptions.findIndex(option=>option.dataset.value===timeInput.value);timeOptions.forEach(option=>option.setAttribute('aria-selected',String(option.dataset.value===timeInput.value)));window.CosmosPreview.positionOverlay(timeMenu,timeInput);activateTime(timeIndex<0?58:timeIndex);timeInput.focus();};
  const chooseTime=option=>{timeInput.value=option.dataset.value;closeTime(true);};
  timeTrigger.addEventListener('click',()=>timeMenu.hidden?openTime():closeTime(true));
  timeInput.addEventListener('keydown',event=>{if(event.key==='Escape'&&!timeMenu.hidden){event.preventDefault();closeTime(true);return;}if(event.key==='Tab'){closeTime();return;}if(!['ArrowDown','ArrowUp','Home','End','Enter'].includes(event.key)){return;}if(event.key==='Enter'&&timeMenu.hidden){return;}event.preventDefault();if(timeMenu.hidden){openTime();return;}if(event.key==='Enter'){chooseTime(timeOptions[timeIndex]);return;}activateTime(event.key==='Home'?0:event.key==='End'?timeOptions.length-1:timeIndex+(event.key==='ArrowUp'?-1:1));});
  timeInput.addEventListener('input',()=>closeTime());
  timeMenu.addEventListener('click',event=>{const option=event.target.closest('[role=option]');if(option){chooseTime(option);}});
  document.addEventListener('pointerdown',event=>{if(!timeMenu.contains(event.target)&&!timeInput.contains(event.target)&&!timeTrigger.contains(event.target)){closeTime();}});
  document.addEventListener('cosmos-close-overlays',()=>closeTime());
  document.addEventListener('cosmos-close-calendar',()=>closeTime());
  const repositionTime=()=>{if(!timeMenu.hidden){window.CosmosPreview.positionOverlay(timeMenu,timeInput);}};
  window.addEventListener('resize',repositionTime);window.addEventListener('scroll',repositionTime,{capture:true,passive:true});
  byId('applyFilters').addEventListener('click',() => {
    const dateText = byId('activityCalendarInput').value.trim(); const timeText = byId('filterTime').value.trim(); const date = parseDate(dateText);
    // Optional ist eine Uhrzeit von 00:00 bis 23:59; erst mit gültigem Datum verfügbar.
    if (dateText && !date || timeText && !/^([01]\d|2[0-3]):[0-5]\d$/.test(timeText)) { notify('Bitte Datum und Uhrzeit überprüfen.','error','Filter prüfen'); return; }
    if (date && timeText) { date.setHours(Number(timeText.slice(0,2)),Number(timeText.slice(3))); }
    filters = {query:byId('userSearch').value.trim(),enabled:byId('filterEnabled').dataset.value,status:byId('filterStatus').dataset.value,role:byId('filterRole').dataset.value,cutoff:date?.getTime(),dateLabel:dateText + (timeText ? ' ' + timeText : '')}; page = 0; render(); document.dispatchEvent(new Event('admin-filters-applied'));
  });
  byId('clearFilters').addEventListener('click',() => {
    ['filterEnabled','filterStatus','filterRole'].forEach(id => setCombo(id,'')); byId('activityCalendarInput').value = ''; byId('filterTime').value = ''; byId('filterTime').disabled = true; timeTrigger.disabled=true; closeTime(); byId('userSearch').value = '';
  });
  byId('refreshUsers').addEventListener('click',() => { page = 0; render(); notify('Die synthetische Benutzerliste wurde neu angezeigt.'); });
  byId('pageSize').addEventListener('change',() => { page = 0; render(); });
  byId('pageNavigation').addEventListener('click',event => { const target = event.target.closest('[data-page]'); if (target && !target.disabled) { page = Number(target.dataset.page); render(); byId('pageNavigation').querySelector('[aria-current=page]').focus({preventScroll:true}); } });
  document.querySelectorAll('[data-sort]').forEach(button => button.addEventListener('click',() => {
    sort = {key:button.dataset.sort,direction:sort.key === button.dataset.sort ? -sort.direction : 1}; page = 0;
    document.querySelectorAll('th[aria-sort]').forEach(head => head.setAttribute('aria-sort',head.contains(button) ? sort.direction === 1 ? 'ascending' : 'descending' : 'none')); render();
  }));

  const updatePasswordHints = () => {
    const present = byId('editorPassword').value.length > 0;
    byId('passwordHint').textContent = present || editedId === null ? 'Mindestens 8 Zeichen, ein Großbuchstabe und ein Sonderzeichen.' : 'Leer lassen, um das bisherige Passwort beizubehalten. Für ein neues Passwort gilt: Mindestens 8 Zeichen, ein Großbuchstabe und ein Sonderzeichen.';
    byId('confirmPasswordHint').textContent = present ? 'Passwort zur Bestätigung erneut eingeben.' : editedId === null ? 'Wird aktiv, sobald ein Passwort eingegeben ist.' : 'Wird aktiv, sobald ein neues Passwort eingegeben ist.';
  };
  const roleTrigger = byId('editorRoles');
  const roleOptions = byId('editorRolesOptions');
  const refreshRoleSelection = () => {
    const count = editor.querySelectorAll('[name=editorRole]:checked').length;
    byId('editorRolesSummary').textContent = `${count} ${count === 1 ? 'Rolle' : 'Rollen'} ausgew\u00e4hlt`;
    roleTrigger.placeholder = byId('editorRolesSummary').textContent;
    roleTrigger.classList.toggle('has-selection', count > 0);
    roleOptions.querySelectorAll('[data-editor-role]').forEach(option => {
      option.setAttribute('aria-selected', String(option.querySelector('input').checked));
    });
  };
  const hideRoleOptions = () => {
    if (roleOptions.matches(':popover-open')) { roleOptions.hidePopover(); }
    roleTrigger.setAttribute('aria-expanded', 'false');
    roleTrigger.value = '';
    roleOptions.querySelectorAll('[data-editor-role]').forEach(option => option.hidden = false);
  };
  const showRoleOptions = () => {
    if (roleOptions.matches(':popover-open')) { return; }
    const rect = roleTrigger.getBoundingClientRect();
    const height = Math.min(196, window.innerHeight - 24);
    roleOptions.style.width = `${rect.width}px`;
    roleOptions.style.left = `${Math.min(rect.left, window.innerWidth - rect.width - 10)}px`;
    roleOptions.style.top = `${rect.bottom + height + 8 <= window.innerHeight ? rect.bottom + 4 : Math.max(10,rect.top - height - 4)}px`;
    roleOptions.style.maxHeight = `${height}px`;
    roleOptions.showPopover();
    roleTrigger.setAttribute('aria-expanded', 'true');
  };
  roleTrigger.addEventListener('click', showRoleOptions);
  roleTrigger.addEventListener('input', () => {
    showRoleOptions();
    const search = roleTrigger.value.trim().toLocaleLowerCase('de');
    roleOptions.querySelectorAll('[data-editor-role]').forEach(option => {
      option.hidden = !option.dataset.editorRole.toLocaleLowerCase('de').includes(search);
    });
  });
  roleTrigger.addEventListener('keydown', event => {
    if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); hideRoleOptions(); }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault(); showRoleOptions();
      const options = [...roleOptions.querySelectorAll('button:not(:disabled):not([hidden])')];
      (event.key === 'ArrowDown' ? options[0] : options.at(-1))?.focus();
    }
    if (event.key === 'Enter') { event.preventDefault(); roleOptions.querySelector('button:not(:disabled):not([hidden])')?.click(); }
  });
  roleOptions.addEventListener('click', event => {
    const option = event.target.closest('[data-editor-role]');
    if (!option || option.disabled) { return; }
    const input = option.querySelector('input');
    input.checked = !input.checked;
    roleTrigger.value = '';
    roleOptions.querySelectorAll('[data-editor-role]').forEach(item => item.hidden = false);
    refreshRoleSelection();
  });
  roleOptions.addEventListener('keydown', event => {
    const options = [...roleOptions.querySelectorAll('button:not(:disabled):not([hidden])')];
    const index = options.indexOf(document.activeElement);
    let next;
    if (event.key === 'ArrowDown') { next = (index + 1) % options.length; }
    if (event.key === 'ArrowUp') { next = (index - 1 + options.length) % options.length; }
    if (event.key === 'Home') { next = 0; }
    if (event.key === 'End') { next = options.length - 1; }
    if (next !== undefined) { event.preventDefault(); options[next].focus(); }
    if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); hideRoleOptions(); roleTrigger.focus(); }
    if (event.key === 'Tab') { hideRoleOptions(); roleTrigger.focus(); }
  });
  roleOptions.addEventListener('toggle', () => {
    const open = roleOptions.matches(':popover-open');
    roleTrigger.setAttribute('aria-expanded', String(open));
    if (!open) { roleTrigger.value = ''; roleOptions.querySelectorAll('[data-editor-role]').forEach(option => option.hidden = false); }
  });
  window.addEventListener('resize', hideRoleOptions);
  byId('editorForm').addEventListener('scroll', hideRoleOptions);
  const closeEditor = () => { hideRoleOptions(); closeLists(); document.dispatchEvent(new Event('cosmos-close-calendar')); editor.close(); byId('editorPassword').value = ''; byId('editorConfirm').value = ''; editor.querySelectorAll('.vp-toast').forEach(toast=>toast.remove()); editorFocus?.focus(); };
  const openEditor = id => {
    const user = users.find(item => item.id === id); if (user?.protected) { return; }
    editorFocus = document.activeElement; editedId = user?.id ?? null; closeLists();
    byId('editorTitle').textContent = user ? 'Benutzer bearbeiten' : 'Benutzer anlegen';
    for (const [field,key] of [['editorFirst','first'],['editorLast','last'],['editorEmail','email'],['birthCalendarInput','birth']]) { byId(field).value = user?.[key] || ''; byId(field).removeAttribute('aria-invalid'); }
    byId('editorPassword').value = ''; byId('editorConfirm').value = ''; byId('editorConfirm').disabled = true;
    editor.querySelectorAll('[data-reveal]').forEach(button => { byId(button.dataset.reveal).type = 'password'; button.setAttribute('aria-pressed','false'); });
    byId('editorPassword').required = !user; byId('editorConfirm').required = false;
    editor.querySelector('label[for=editorPassword] .au-required').hidden = !!user;
    editor.querySelector('label[for=editorConfirm] .au-required').hidden = true;
    editor.querySelector('[data-reveal=editorConfirm]').disabled = true;
    byId('editorPassword').placeholder = user ? "Unverändert lassen" : "Passwort eingeben";
    updatePasswordHints();
    byId('editorEnabled').checked = user ? user.enabled : false;
    byId('editorSecurity').textContent = user ? 'Änderungen an Rollen und Aktivierung werden vor dem Speichern geprüft.' : 'Neue Benutzer werden sofort angelegt. Passwort, Rolle und Pflichtfelder sind erforderlich.';
    setCombo('editorGender',user?.gender || '');
    editor.querySelectorAll('[name=editorRole]').forEach(box => box.checked = user ? user.roles.includes(box.value) : false);
    refreshRoleSelection();
    byId('editorDelete').hidden = !user; byId('editorDelete').disabled = !!user?.current;
    editor.querySelectorAll('[aria-invalid=true]').forEach(control => control.removeAttribute('aria-invalid'));
    editor.showModal(); byId('editorFirst').focus();
  };
  byId('addUser').addEventListener('click',() => openEditor(null));
  editor.querySelectorAll('[data-close-editor]').forEach(button => button.addEventListener('click',closeEditor));
  byId('editorPassword').addEventListener('input',() => { const present = byId('editorPassword').value.length > 0; byId('editorConfirm').disabled = !present; byId('editorConfirm').required = present; editor.querySelector('label[for=editorConfirm] .au-required').hidden = !present; editor.querySelector('[data-reveal=editorConfirm]').disabled = !present; if (!present) { byId('editorConfirm').value = ''; } updatePasswordHints(); });
  editor.querySelectorAll('[data-reveal]').forEach(button => button.addEventListener('click',() => {
    const input = byId(button.dataset.reveal); const show = input.type === 'password'; input.type = show ? 'text' : 'password'; button.setAttribute('aria-pressed',String(show)); button.setAttribute('aria-label',`${input.id === 'editorConfirm' ? 'Passwortbestätigung' : 'Passwort'} ${show ? 'verbergen' : 'anzeigen'}`);
  }));
  byId('editorForm').addEventListener('submit',event => { event.preventDefault(); byId('saveUser').click(); });
  byId('saveUser').addEventListener('click',() => {
    const roles = [...editor.querySelectorAll('[name=editorRole]:checked')].map(box => box.value);
    const password = byId('editorPassword').value;
    const invalid = [...byId('editorForm').querySelectorAll('input[required]')].filter(input => !input.checkValidity() || !input.value.trim());
    if (!byId('editorGender').dataset.value) { invalid.push(byId('editorGender')); }
    const now = new Date(); const today = new Date(now.getFullYear(),now.getMonth(),now.getDate());
    const birth = parseDate(byId('birthCalendarInput').value); if (!birth || birth >= today) { invalid.push(byId('birthCalendarInput')); }
    // Die vorhandene zentrale Passwortregel wird vom Builder übernommen; keine neue Regel erfinden.
    if (password && !new RegExp(window.AdminPreviewRules.password).test(password.trim())) { invalid.push(byId('editorPassword')); }
    if (password && password !== byId('editorConfirm').value) { invalid.push(byId('editorConfirm')); }
    editor.querySelectorAll('[aria-invalid=true]').forEach(control => control.removeAttribute('aria-invalid'));
    invalid.forEach(control => control.setAttribute('aria-invalid','true'));
    if (invalid.length || !roles.length) { notify('Bitte prüfen Sie die markierten Pflichtfelder, die Passwortbestätigung und mindestens eine Rolle.','error','Eingaben prüfen'); invalid[0]?.focus(); return; }
    const old = users.find(user => user.id === editedId);
    if (old?.current && (!byId('editorEnabled').checked || !roles.includes('Administrator'))) { notify('Sie können Ihr eigenes Konto hier nicht deaktivieren oder sich die Administratorrolle entziehen.','error','Änderung nicht möglich'); return; }
    const email = byId('editorEmail').value.trim();
    if (users.some(user => user.id !== editedId && user.email.toLowerCase() === email.toLowerCase())) { notify('Diese E-Mail-Adresse wird bereits verwendet.','error','Eingaben prüfen'); byId('editorEmail').setAttribute('aria-invalid','true'); byId('editorEmail').focus(); return; }
    const next = {...old,id:editedId || Math.max(...users.map(user => user.id)) + 1,first:byId('editorFirst').value.trim(),last:byId('editorLast').value.trim(),email,enabled:byId('editorEnabled').checked,roles,gender:byId('editorGender').dataset.value,birth:byId('birthCalendarInput').value,status:old?.status || 'Offline',activity:old?.activity || 26};
    if (old) { users = users.map(user => user.id === old.id ? next : user); } else { users.push(next); }
    closeEditor(); byId('editorPassword').value = ''; byId('editorConfirm').value = ''; render(); notify('Die Änderung wurde nur in dieser Vorschau simuliert.','success',old ? 'Benutzer aktualisiert' : 'Benutzer angelegt');
  });
  const closeDelete = () => { deletion.close(); deleteFocus?.focus(); };
  const openDelete = id => {
    const user = users.find(item => item.id === id); if (!user || user.current || user.protected) { return; }
    deletedId = id; deleteFocus = document.activeElement;
    byId('deleteIdentity').innerHTML = `<span class="portal-confirm-object portal-confirm-avatar" aria-hidden="true">${escape(user.first[0] + user.last[0])}</span><div class="portal-confirm-text"><strong>${escape(user.first + ' ' + user.last)}</strong><p class="au-help">${escape(user.email)}</p></div>`; deletion.showModal(); deletion.querySelector('[data-close-delete][autofocus]').focus();
  };
  deletion.querySelectorAll('[data-close-delete]').forEach(button => button.addEventListener('click',closeDelete));
  byId('editorDelete').addEventListener('click',() => openDelete(editedId));
  byId('confirmDelete').addEventListener('click',() => {
    const user = users.find(item => item.id === deletedId); if (!user || user.current || user.protected) { return; }
    users = users.filter(item => item.id !== deletedId); closeDelete(); if (editor.open) { closeEditor(); } render(); byId('addUser').focus(); notify('Das Beispielkonto wurde nur aus dieser Vorschau entfernt.','success','Benutzer gelöscht');
  });
  byId('userRows').addEventListener('click',event => {
    const edit = event.target.closest('[data-edit]'); const remove = event.target.closest('[data-delete]');
    if (edit && !edit.disabled) { openEditor(Number(edit.dataset.edit)); }
    if (remove && !remove.disabled) { openDelete(Number(remove.dataset.delete)); }
  });
  [editor,deletion].forEach(dialog => {
    dialog.addEventListener('cancel',event => {
      event.preventDefault();
      if ([...document.querySelectorAll('.au-options,.reference-calendar')].some(overlay => !overlay.hidden)) { closeLists(); document.dispatchEvent(new Event('cosmos-close-calendar')); return; }
      dialog === editor ? closeEditor() : closeDelete();
    });
  });
  const mobile = byId('mobileMenuBtn'); const sidebar = byId('sidebarNav');
  // Derselbe Inhalts-Drawer und dieselbe Kompaktgrenze wie in der Home-Referenz.
  const navigationDialog = byId('mobileNavigation');
  const navigationShade = byId('mobileNavigationShade');
  const navigationParent = sidebar.parentNode;
  const navigationNext = sidebar.nextSibling;
  const compactNavigation = matchMedia('(max-width:767px), (max-width:1023px) and (max-height:500px)');
  const initialOverflow = document.documentElement.style.overflow;
  const positionNavigation = () => {
    document.body.style.setProperty('--navigation-top',`${Math.max(0,document.querySelector('header').getBoundingClientRect().bottom)}px`);
    document.body.style.setProperty('--navigation-bottom',`${document.querySelector('footer').getBoundingClientRect().height}px`);
  };
  const setNavigation = (open, restoreFocus = false) => {
    if (open && !compactNavigation.matches) { return; }
    if (open) {
      closeLists(); closeTime(); closeMenu(); document.dispatchEvent(new Event('cosmos-close-calendar'));
      navigationDialog.append(sidebar);
      document.body.classList.add('navigation-open');
      document.documentElement.style.overflow = 'hidden';
      positionNavigation();
      document.querySelector('main').inert = true;
      navigationShade.hidden = false;
      if (!navigationDialog.open) { navigationDialog.show(); }
      mobile.focus();
    } else {
      if (navigationDialog.open) { navigationDialog.close(); }
      navigationParent.insertBefore(sidebar,navigationNext);
      navigationShade.hidden = true;
      document.querySelector('main').inert = false;
      document.body.classList.remove('navigation-open');
      document.documentElement.style.overflow = initialOverflow;
    }
    mobile.setAttribute('aria-expanded',String(open));
    mobile.setAttribute('aria-label',open ? 'Menü schließen' : 'Menü öffnen');
    mobile.dataset.tooltip = mobile.getAttribute('aria-label');
    if (restoreFocus) { mobile.focus(); }
  };
  mobile.addEventListener('click',() => setNavigation(!navigationDialog.open));
  navigationShade.addEventListener('click',() => setNavigation(false,true));
  navigationDialog.addEventListener('cancel',event => { event.preventDefault(); setNavigation(false,true); });
  sidebar.addEventListener('click',event => { const item=event.target.closest('[data-admin-route]');if(item&&!item.disabled){setNavigation(false,compactNavigation.matches);} });
  document.addEventListener('keydown',event => { if(event.key==='Escape'&&navigationDialog.open){event.preventDefault();setNavigation(false,true);} });
  const repositionNavigation = () => { if(navigationDialog.open&&!compactNavigation.matches){setNavigation(false);}else if(navigationDialog.open){positionNavigation();} };
  window.addEventListener('resize',repositionNavigation);
  window.addEventListener('scroll',repositionNavigation,{passive:true,capture:true});
  document.querySelectorAll('[data-portal-route]').forEach(button => button.addEventListener('click',() => { location.href = 'portal.html?rev=' + window.AdminPortalRevision + '#' + button.dataset.portalRoute; }));
  byId('portalBell').addEventListener('click',() => { location.href = 'portal.html?rev=' + window.AdminPortalRevision + '#notifications'; });
  const menuTrigger = byId('userMenuTrigger');
  const closeMenu = (focus = false) => { menu.hidden = true; menuTrigger.setAttribute('aria-expanded','false'); if (focus) { menuTrigger.focus(); } };
  menuTrigger.addEventListener('click',() => { if(navigationDialog.open){setNavigation(false);} closeLists(); const opened = menu.hidden; menu.hidden = !opened; menuTrigger.setAttribute('aria-expanded',String(opened)); if (opened) { window.CosmosPreview.positionOverlay(menu,menuTrigger,320,'end'); } });
  menu.querySelectorAll('[data-menu-language]').forEach(button => button.addEventListener('click',() => { menu.querySelectorAll('[data-menu-language]').forEach(entry => entry.setAttribute('aria-pressed',String(entry === button))); }));
  menu.querySelectorAll('[data-status]').forEach(button => button.addEventListener('click',() => { menu.querySelectorAll('[data-status]').forEach(entry => entry.setAttribute('aria-pressed',String(entry === button))); document.body.style.setProperty('--presence-color',button.dataset.color); document.body.dataset.presence=button.dataset.status; document.querySelectorAll('[data-presence-dot]').forEach(dot => dot.setAttribute('aria-label',button.dataset.label)); const current = users.find(user=>user.current); if(current){current.status = button.dataset.label;render();} closeMenu(true); }));
  menu.querySelectorAll('[data-portal-action]').forEach(button => button.addEventListener('click',() => { closeMenu(true); notify('Diese Aktion wird in der Admin-Designvorschau nicht ausgeführt.'); }));
  document.addEventListener('pointerdown',event => { if (!menu.contains(event.target) && !menuTrigger.contains(event.target)) { closeMenu(); } });
  document.addEventListener('keydown',event => { if (event.key === 'Escape' && !menu.hidden) { event.preventDefault(); closeMenu(true); } });
  window.addEventListener('resize',() => { if (!menu.hidden) { window.CosmosPreview.positionOverlay(menu,menuTrigger,320,'end'); } });
  render();
})();
