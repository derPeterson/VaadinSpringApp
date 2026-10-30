(() => {
  const byId = id => document.getElementById(id);
  const languageSwitch = byId('public-language-switch');
  const languageButtons = [...languageSwitch.querySelectorAll('[data-menu-language]')];
  const lists = [
    {trigger: byId('genderTrigger'), menu: byId('genderDropdown'), selected: byId('selectedGenderText'), attribute: 'gender'}
  ];
  const options = list => [...list.menu.querySelectorAll('[role=option]')];
  const close = (list, focus = false) => {
    list.menu.hidden = true; list.trigger.setAttribute('aria-expanded', 'false');
    list.trigger.removeAttribute('aria-activedescendant');
    if (focus) { list.trigger.focus(); }
  };
  const closeAll = () => lists.forEach(list => close(list));
  const position = list => window.CosmosPreview.positionOverlay(list.menu, list.trigger);
  const activate = (list, index) => {
    const entries = options(list); list.index = (index + entries.length) % entries.length;
    entries.forEach((option, cursor) => { option.dataset.active = String(cursor === list.index); });
    list.trigger.setAttribute('aria-activedescendant', entries[list.index].id);
    entries[list.index].scrollIntoView({block: 'nearest'});
  };
  const open = list => {
    document.dispatchEvent(new Event('cosmos-close-calendar')); closeAll();
    list.menu.hidden = false; list.trigger.setAttribute('aria-expanded', 'true'); position(list);
    const index = options(list).findIndex(option => option.getAttribute('aria-selected') === 'true');
    activate(list, Math.max(0, index));
  };
  const choose = (list, option) => {
    const value = option.dataset[list.attribute];
    options(list).forEach(entry => entry.setAttribute('aria-selected', String(entry === option)));
    list.selected.textContent = value;
    byId('register-gender').value = value;
    list.trigger.dataset.empty = 'false';
    list.trigger.removeAttribute('aria-invalid');
    close(list, true);
  };
  lists.forEach(list => {
    list.trigger.addEventListener('click', () => list.menu.hidden ? open(list) : close(list));
    list.trigger.addEventListener('keydown', event => {
      if (!['ArrowDown','ArrowUp','Home','End','Enter',' ','Escape','Tab'].includes(event.key)) { return; }
      if (event.key === 'Tab') { close(list); return; }
      if (event.key === 'Escape') { if (!list.menu.hidden) { event.preventDefault(); close(list, true); } return; }
      event.preventDefault();
      if (list.menu.hidden) { open(list); return; }
      if (event.key === 'Enter' || event.key === ' ') { choose(list, options(list)[list.index]); return; }
      activate(list, event.key === 'Home' ? 0 : event.key === 'End' ? options(list).length - 1 : list.index + (event.key === 'ArrowUp' ? -1 : 1));
    });
    list.menu.addEventListener('click', event => { const option = event.target.closest('[role=option]'); if (option) { choose(list, option); } });
    list.menu.addEventListener('pointermove', event => { const option = event.target.closest('[role=option]'); if (option) { activate(list, options(list).indexOf(option)); } });
  });
  languageButtons.forEach(button => button.addEventListener('click', () => {
    languageButtons.forEach(entry => entry.setAttribute('aria-pressed', String(entry === button)));
  }));
  languageSwitch.addEventListener('keydown', event => {
    if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) { return; }
    event.preventDefault();
    const current = languageButtons.indexOf(document.activeElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? languageButtons.length - 1 : (current + (event.key === 'ArrowLeft' ? -1 : 1) + languageButtons.length) % languageButtons.length;
    languageButtons[next].focus();
  });
  document.addEventListener('pointerdown', event => lists.forEach(list => {
    if (!list.menu.contains(event.target) && !list.trigger.contains(event.target)) { close(list); }
  }));
  document.addEventListener('cosmos-close-overlays', closeAll);
  const reposition = () => {
    lists.forEach(list => { if (!list.menu.hidden) { position(list); } });
  };
  window.addEventListener('resize', reposition);
  window.addEventListener('scroll', reposition, {capture:true,passive:true});
  window.PublicControls = {closeAll};
})();
