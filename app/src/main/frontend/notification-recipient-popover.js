// Öffnet die vollständige Empfängerliste statt des nativen Menüs nur versteckter Avatare.
// Nur öffentliche DOM-Ereignisse und Light-DOM-Slots; kein Zugriff auf Vaadin-Interna.
const connections = new WeakMap();

function connect(group) {
  if (connections.has(group)) return;
  const open = event => {
    if (event.type === 'click' && event.button !== 0) return;
    if (event.type === 'keydown' && !['Enter', ' '].includes(event.key)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    if (!event.repeat) group.dispatchEvent(new CustomEvent('recipient-list-open'));
  };
  // Die ganze Reihe ist ein einzelner Tastatureinstieg; die Namen-Tooltips bleiben nativ.
  const updateAvatars = () => {
    for (const avatar of group.querySelectorAll(':scope > vaadin-avatar')) {
      avatar.setAttribute('tabindex', '-1');
      if (avatar.getAttribute('slot') === 'overflow') {
        avatar.setAttribute('role', 'presentation');
        avatar.removeAttribute('aria-haspopup');
        avatar.removeAttribute('aria-expanded');
      }
    }
  };
  const observer = new MutationObserver(updateAvatars);
  observer.observe(group, { childList: true });
  updateAvatars();
  group.addEventListener('click', open, true);
  group.addEventListener('keydown', open, true);
  connections.set(group, { open, observer });
}

function disconnect(group) {
  const connection = connections.get(group);
  if (!connection) return;
  group.removeEventListener('click', connection.open, true);
  group.removeEventListener('keydown', connection.open, true);
  connection.observer.disconnect();
  connections.delete(group);
}

window.CosmosRecipientPopover = { connect, disconnect };
