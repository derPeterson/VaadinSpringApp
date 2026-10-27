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
    document.getElementById('selectedProfileGenderText').textContent = option.dataset.gender;
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
