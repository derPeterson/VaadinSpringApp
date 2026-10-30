(() => {
  const drawer = document.getElementById('filterPanel');
  const trigger = document.getElementById('filterToggle');
  const fields = ['userSearch','filterEnabled','filterStatus','filterRole','activityCalendarInput','filterTime'];
  let initialFields = null;
  let initialOverflow = '';
  const readFields = () => Object.fromEntries(fields.map(id => {
    const node = document.getElementById(id);
    return [id,node.classList.contains('au-filter-choice') ? node.dataset.value : node.value];
  }));
  const closeOverlays = () => {
    document.dispatchEvent(new Event('cosmos-close-overlays'));
    document.dispatchEvent(new Event('cosmos-close-calendar'));
  };
  const restoreFields = () => {
    if (!initialFields) { return; }
    fields.forEach(id => {
      const node = document.getElementById(id), value = initialFields[id];
      if (node.classList.contains('au-filter-choice')) {
        node.dataset.value = value;
        node.querySelectorAll('input').forEach(input => { input.checked = input.type === 'radio' ? input.value === value : value.split(',').includes(input.value); });
      } else { node.value = value; }
    });
    document.getElementById('activityCalendarInput').dispatchEvent(new Event('input',{bubbles:true}));
  };
  const count = document.getElementById('filterCount');
  let appliedCount = Number.parseInt(count.textContent,10) || 0;
  const draftCount = () => {
    const values = readFields();
    const restricted = id => {
      const selected = values[id].split(',').filter(Boolean).length;
      return selected > 0;
    };
    return Number(Boolean(values.userSearch.trim())) + Number(Boolean(values.filterEnabled))
      + Number(restricted('filterStatus')) + Number(restricted('filterRole'))
      + Number(Boolean(values.activityCalendarInput.trim()));
  };
  const updateCount = () => {
    count.textContent = `${drawer.open ? draftCount() : appliedCount} aktiv`;
    const badge = document.getElementById('drawerAppliedCount');
    badge.hidden = appliedCount === 0; badge.textContent = String(appliedCount);
    trigger.setAttribute('aria-label',appliedCount ? `Filter: ${appliedCount} aktiv` : 'Filter');
  };
  document.addEventListener('admin-filter-rendered',event => {
    appliedCount = event.detail; updateCount();
    if (drawer.open) { position(); }
  });
  drawer.querySelectorAll('.au-filter-choice').forEach(group => {
    group.addEventListener('change',() => {
      group.dataset.value = [...group.querySelectorAll('input:checked')].map(input => input.value).join(',');
      updateCount();
    });
  });
  drawer.addEventListener('input',updateCount);
  const position = () => {
    const viewportHeight = window.innerHeight;
    const header = document.querySelector('header').getBoundingClientRect();
    const footer = document.querySelector('footer').getBoundingClientRect();
    // Nur tatsächlich sichtbare Footerfläche freihalten, nicht die Höhe unterhalb des Viewports.
    const visibleFooter = Math.max(0,Math.min(viewportHeight,footer.bottom)-Math.max(0,footer.top));
    drawer.style.setProperty('--filter-drawer-top',`${Math.min(viewportHeight,Math.max(0,header.bottom))}px`);
    drawer.style.setProperty('--filter-drawer-bottom',`${visibleFooter}px`);
  };
  const close = (cancel = true) => {
    if (!drawer.open) { return; }
    closeOverlays();
    if (cancel) { restoreFields(); }
    drawer.close(); document.documentElement.style.overflow = initialOverflow;
    updateCount();
    trigger.setAttribute('aria-expanded','false'); trigger.focus({preventScroll:true});
  };
  const open = () => {
    closeOverlays(); initialFields = readFields();
    initialOverflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden'; position();
    drawer.showModal(); trigger.setAttribute('aria-expanded','true');
    drawer.querySelector('.au-drawer-body').scrollTop = 0;
    document.getElementById('userSearch').focus({preventScroll:true});
    updateCount();
  };
  document.addEventListener('admin-filter-toggle',() => drawer.open ? close() : open());
  drawer.querySelectorAll('[data-close-filters]').forEach(button => button.addEventListener('click',() => close()));
  document.addEventListener('admin-filters-applied',() => close(false));
  document.getElementById('clearFilters').addEventListener('click',() => {
    document.getElementById('activityCalendarInput').dispatchEvent(new Event('input',{bubbles:true}));
    updateCount();
  });
  drawer.addEventListener('cancel',event => {
    event.preventDefault();
    const overlay = [...drawer.querySelectorAll('.au-options,.reference-calendar')].find(node => !node.hidden);
    if (overlay) { closeOverlays(); return; }
    close();
  });
  drawer.addEventListener('keydown',event => {
    if (event.key === 'Enter' && event.target.id === 'userSearch') {
      event.preventDefault(); document.getElementById('applyFilters').click(); return;
    }
    if (event.key === 'Escape' && event.target.id === 'userSearch') {
      event.preventDefault(); event.stopPropagation(); close(); return;
    }
    if (event.key !== 'Tab' || event.defaultPrevented) { return; }
    const controls = [...drawer.querySelectorAll('button,input,select,a[href],[tabindex]')].filter(node => !node.disabled && node.tabIndex >= 0 && node.getClientRects().length);
    const first = controls[0], last = controls.at(-1), active = document.activeElement;
    if (event.shiftKey && active === first || !event.shiftKey && active === last) {
      event.preventDefault(); (event.shiftKey ? last : first).focus({preventScroll:true});
    }
  });
  drawer.addEventListener('click',event => {
    if (event.target !== drawer) { return; }
    const box = drawer.getBoundingClientRect();
    if (event.clientX<box.left||event.clientX>box.right||event.clientY<box.top||event.clientY>box.bottom) { close(); }
  });
  updateCount();
  window.addEventListener('resize',() => { if (drawer.open) { position(); } });
  open();
})();
