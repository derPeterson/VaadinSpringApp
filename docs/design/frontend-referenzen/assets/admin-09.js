(() => {
  const initialize = () => {
    const controls = new WeakMap();
    const menu = document.createElement('div');
    menu.id = 'ref-pagination-options';
    menu.className = 'ref-pagination-menu';
    menu.setAttribute('popover', 'auto');
    menu.setAttribute('role', 'listbox');
    document.body.append(menu);
    let active = null;

    const chevron = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"></path></svg>';
    const check = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 4 4 10-10"></path></svg>';

    function closeMenu(restoreFocus = false) {
      const previous = active;
      active = null;
      if (menu.matches(':popover-open')) {
        menu.hidePopover();
      }
      if (previous) {
        previous.trigger.setAttribute('aria-expanded', 'false');
        if (restoreFocus && previous.trigger.isConnected) {
          previous.trigger.focus({preventScroll:true});
        }
      }
    }

    function positionMenu() {
      if (!active || !menu.matches(':popover-open')) {
        return;
      }
      const bounds = active.trigger.getBoundingClientRect();
      const width = Math.min(Math.max(bounds.width, 96), window.innerWidth - 24);
      menu.style.width = `${width}px`;
      const height = menu.getBoundingClientRect().height;
      const below = bounds.bottom + 6;
      const above = bounds.top - height - 6;
      const top = below + height <= window.innerHeight - 12 || above < 12 ? below : above;
      menu.style.left = `${Math.max(12, Math.min(bounds.left, window.innerWidth - width - 12))}px`;
      menu.style.top = `${Math.max(12, Math.min(top, window.innerHeight - height - 12))}px`;
    }

    function updateControl(select, control) {
      const text = select.selectedOptions[0]?.textContent || '';
      if (control.value.textContent !== text) {
        control.value.textContent = text;
      }
      if (control.trigger.disabled !== select.disabled) {
        control.trigger.disabled = select.disabled;
      }
    }

    function openMenu(select, control, focus = 'selected') {
      if (select.disabled) {
        return;
      }
      closeMenu();
      active = {select, trigger:control.trigger};
      menu.replaceChildren();
      menu.setAttribute('aria-labelledby', control.label.id);
      for (const option of select.options) {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'ref-pagination-option';
        item.setAttribute('role', 'option');
        item.setAttribute('aria-selected', String(option.selected));
        item.tabIndex = -1;
        item.disabled = option.disabled;
        const label = document.createElement('span');
        label.textContent = option.textContent;
        item.append(label);
        item.insertAdjacentHTML('beforeend', check);
        item.addEventListener('click', () => {
          closeMenu();
          select.value = option.value;
          select.dispatchEvent(new Event('change', {bubbles:true}));
          updateControl(select, control);
          if (control.trigger.isConnected) {
            control.trigger.focus({preventScroll:true});
          }
        });
        menu.append(item);
      }
      control.trigger.setAttribute('aria-expanded', 'true');
      menu.showPopover();
      positionMenu();
      const items = [...menu.querySelectorAll('[role=option]:not(:disabled)')];
      const selected = items.find(item => item.getAttribute('aria-selected') === 'true');
      const target = focus === 'first' ? items[0] : focus === 'last' ? items.at(-1) : selected || items[0];
      target?.focus({preventScroll:true});
    }

    function enhance(select) {
      const known = controls.get(select);
      if (known) {
        updateControl(select, known);
        return;
      }
      const wrapper = select.closest('.c-page-select');
      const bar = wrapper.closest('.c-pagination-bar');
      const label = bar.querySelector('.c-page-size label');
      // Dynamisch geklonte Pagination enthält eventuell bereits einen ungebundenen Trigger.
      wrapper.querySelector('[data-ref-pagination-trigger]')?.remove();
      const trigger = document.createElement('button');
      trigger.type = 'button';
      trigger.id = `ref-page-trigger-${select.id}`;
      trigger.className = 'ref-pagination-trigger';
      trigger.setAttribute('data-ref-pagination-trigger', '');
      trigger.setAttribute('role', 'combobox');
      trigger.setAttribute('aria-haspopup', 'listbox');
      trigger.setAttribute('aria-controls', menu.id);
      trigger.setAttribute('aria-expanded', 'false');
      if (!label.id || label.id.startsWith('ref-page-label-')) {
        label.id = `ref-page-label-${select.id}`;
      }
      label.htmlFor = trigger.id;
      const value = document.createElement('span');
      value.id = `ref-page-value-${select.id}`;
      trigger.setAttribute('aria-labelledby', `${label.id} ${value.id}`);
      trigger.append(value);
      trigger.insertAdjacentHTML('beforeend', chevron);
      wrapper.setAttribute('data-ref-pagination', '');
      select.classList.add('ref-pagination-native');
      select.setAttribute('aria-hidden', 'true');
      select.tabIndex = -1;
      wrapper.append(trigger);
      const control = {trigger, value, label};
      controls.set(select, control);
      updateControl(select, control);
      let closeOnClick = false;
      trigger.addEventListener('pointerdown', () => {
        // Der native Außenklick schließt das Popover noch vor dem Click-Ereignis.
        closeOnClick = active?.select === select && menu.matches(':popover-open');
      });
      trigger.addEventListener('click', () => {
        if (closeOnClick || active?.select === select && menu.matches(':popover-open')) {
          closeMenu();
        } else {
          openMenu(select, control);
        }
        closeOnClick = false;
      });
      trigger.addEventListener('keydown', event => {
        if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
          event.preventDefault();
          openMenu(select, control, event.key === 'Home' ? 'first' : event.key === 'End' ? 'last' : 'selected');
        }
      });
      select.addEventListener('change', () => updateControl(select, control));
    }

    menu.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeMenu(true);
        return;
      }
      if (event.key === 'Tab') {
        closeMenu(true);
        return;
      }
      const items = [...menu.querySelectorAll('[role=option]:not(:disabled)')];
      const current = items.indexOf(document.activeElement);
      let next = -1;
      if (event.key === 'ArrowDown') {
        next = (current + 1) % items.length;
      } else if (event.key === 'ArrowUp') {
        next = (current - 1 + items.length) % items.length;
      } else if (event.key === 'Home') {
        next = 0;
      } else if (event.key === 'End') {
        next = items.length - 1;
      }
      if (next >= 0) {
        event.preventDefault();
        items[next]?.focus();
      }
    });
    menu.addEventListener('toggle', () => {
      if (!menu.matches(':popover-open') && active) {
        active.trigger.setAttribute('aria-expanded', 'false');
        active = null;
      }
    });
    const refresh = () => {
      if (active && (!active.select.isConnected || active.select.disabled || active.trigger.getClientRects().length === 0)) {
        closeMenu();
      }
      document.querySelectorAll('.c-page-select select').forEach(enhance);
    };
    new MutationObserver(refresh).observe(document.body, {childList:true, subtree:true, attributes:true, attributeFilter:['disabled','hidden']});
    window.addEventListener('resize', positionMenu);
    document.addEventListener('scroll', positionMenu, true);
    refresh();
  };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize, {once:true});
  } else {
    initialize();
  }
})();
