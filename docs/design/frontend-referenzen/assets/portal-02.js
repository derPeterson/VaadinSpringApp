
// portal-entry-route: Ziel aus der Benutzerverwaltung beim Einstieg anzeigen.
(() => {
  const route = location.hash.slice(1);
  if (['home','notifications','profile','security','sessions'].includes(route)) {
    document.querySelector('[data-portal-route='+route+']')?.click();
  }
})();
