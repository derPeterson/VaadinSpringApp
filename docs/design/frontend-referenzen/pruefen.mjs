// Offline-Prüfung der gesicherten Exporte, ohne Anwendung oder Browser zu starten.
import assert from 'node:assert/strict';
import {readFileSync, existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import vm from 'node:vm';

const files = ['index.html', 'portal.html', 'public.html', 'visitors.html', 'admin.html', 'admin-users.html'];
const scriptsByFile = new Map();
let scriptCount = 0;
for (const file of files) {
  const path = new URL(file, import.meta.url);
  const source = readFileSync(path, 'utf8');
  assert(!source.includes('\uFFFD'), `${file}: beschädigte Textkodierung`);
  const scriptPattern = /<script\b([^>]*)>([\s\S]*?)<\/script\s*>/gi;
  const scripts = [...source.matchAll(scriptPattern)];
  scriptsByFile.set(file, scripts.map(match => match[2]));
  const csp = source.match(/<meta\b[^>]*http-equiv="Content-Security-Policy"[^>]*content="([^"]+)"/i)?.[1];
  assert(csp?.includes("default-src 'none'") && csp.includes("form-action 'none'"), `${file}: CSP fehlt`);
  for (const [, attributes, code] of scripts) {
    const nonce = attributes.match(/\bnonce="([^"]+)"/)?.[1];
    assert(nonce && csp.includes(`'nonce-${nonce}'`), `${file}: falscher Script-Nonce`);
    assert(!/\bsrc\s*=/.test(attributes), `${file}: externes Script`);
    new vm.Script(code, {filename: `${file}:script-${++scriptCount}`});
  }
  const markup = source.replace(scriptPattern, '').replace(/<style\b[^>]*>[\s\S]*?<\/style\s*>/gi, '');
  const ids = [...markup.matchAll(/\sid\s*=\s*(["'])(.*?)\1/gi)].map(match => match[2]);
  const uniqueIds = new Set(ids);
  assert.equal(uniqueIds.size, ids.length, `${file}: doppelte IDs`);
  for (const [, , references] of markup.matchAll(/\saria-(?:labelledby|describedby|controls)\s*=\s*(["'])(.*?)\1/gi)) {
    for (const ref of references.split(/\s+/).filter(Boolean)) {
      assert(uniqueIds.has(ref), `${file}: fehlendes ARIA-Ziel ${ref}`);
    }
  }
  for (const [, ref] of markup.matchAll(/url\(#([^)]*)\)/g)) {
    assert(uniqueIds.has(ref), `${file}: fehlendes SVG-Ziel ${ref}`);
  }
  assert(!/\son[a-z]+\s*=/i.test(markup), `${file}: Inline-Eventhandler`);
  assert(!/<link\b[^>]*rel="stylesheet"/i.test(markup), `${file}: externes CSS`);
  if (file === 'portal.html' || file === 'public.html') {
    const languages = [...markup.matchAll(/<button\b[^>]*data-menu-language="(de|en)"[^>]*>/g)];
    assert.deepEqual(languages.map(match => match[1]), ['de','en'], `${file}: Sprachumschalter fehlt`);
    assert.equal(languages.filter(match => match[0].includes('aria-pressed="true"')).length, 1);
    for (const [button] of markup.matchAll(/<button\b[\s\S]*?<\/button>/gi)) {
      const label = button.replace(/<[^>]*>/g, '').trim();
      if (label !== 'Abbrechen') { continue; }
      assert(!/<circle\b/i.test(button), `${file}: Abbrechen muss ein schlichtes X verwenden`);
      assert(!button.includes('data-n-symbol="close"'), `${file}: Abbrechen darf nicht das Kopf-Schließen-Symbol verwenden`);
    }
    if (file === 'portal.html') {
      const genderLabel = markup.match(/<label\b[^>]*id="profileGenderLabel"[^>]*>[\s\S]*?<\/label>/i)?.[0];
      assert(genderLabel?.includes('text-sky-400') && genderLabel.includes('•'), 'Pflichtpunkt bei Geschlecht fehlt');
      assert(/<button\b[^>]*id="profileGenderTrigger"[^>]*aria-required="true"/i.test(markup), 'Geschlecht ist nicht als Pflichtauswahl ausgezeichnet');
      assert(markup.includes('data-n-icon="cancel"'), 'Eigenes Abbrechen-Symbol für dynamische Referenzdialoge fehlt');
      assert(/<dialog\b[^>]*id="n-confirm"[^>]*class="[^"]*portal-confirm-dialog/.test(markup), 'Gemeinsamer Bestätigungsbaustein fehlt');
      assert(source.includes('id="portal-confirmation-style"'), 'Gemeinsame Bestätigungsgestaltung fehlt');
    }
    if (file === 'public.html') {
      const header = markup.match(/<header\b[\s\S]*?<\/header>/i)?.[0];
      assert(header?.includes('id="public-language-switch"'), 'Sprachumschalter fehlt im öffentlichen Header');
      assert(!markup.includes('id="languageDropdown"') && !markup.includes('id="spracheTrigger"'), 'Öffentliche Sprachauswahl darf kein Popup sein');
    }
  }
  // Nur Referenzanker, eingebettete Ressourcen und Dateien desselben Ordners zulassen.
  for (const [, , value] of markup.matchAll(/\s(?:src|href|xlink:href)\s*=\s*(["'])(.*?)\1/gi)) {
    if (value.startsWith('#') || value.startsWith('data:')) { continue; }
    const target = new URL(value, path);
    assert(target.protocol === 'file:' && new URL('.', target).href === new URL('.', path).href, `${file}: fremde Ressource ${value}`);
    assert(existsSync(fileURLToPath(target)), `${file}: fehlende Datei ${value}`);
  }
}

const adminSource = readFileSync(new URL('admin.html', import.meta.url), 'utf8');
assert(adminSource.includes('data-admin-reference="users"'), 'Freigegebene Benutzerverwaltung fehlt');
assert(!adminSource.includes('window.AdminComparison'), 'Alte Vergleichssteuerung in der festen Referenz');
assert(!adminSource.includes('id="activeFilters"'), 'Entfernte Filterzeile wieder vorhanden');
// Den ausgelieferten Filter gegen unabhängige Beispieldaten und Ergebnismengen prüfen.
const visibleUsersCode = scriptsByFile.get('admin.html').find(code => code.includes('const visibleUsers = () =>'))
  ?.match(/  const visibleUsers = \(\) => \{[\s\S]*?\n  \};/)?.[0];
assert(visibleUsersCode, 'Benutzerfilter fehlt');
const filterUsers = [
  {id:1,first:'Nora',last:'Beispiel',email:'nora@example.test',enabled:true,status:'Verfügbar',roles:['Benutzer'],activity:10},
  {id:2,first:'Luca',last:'Beispiel',email:'luca@example.test',enabled:true,status:'Beschäftigt',roles:['Administrator'],activity:20},
  {id:3,first:'Mila',last:'Beispiel',email:'mila@example.test',enabled:false,status:'Abwesend',roles:['Benutzer','Gast'],activity:25},
  {id:4,first:'Aron',last:'Beispiel',email:'aron@example.test',enabled:true,status:'Offline',roles:['Superadmin'],activity:26}
];
const filterCases = [
  [{},[4,2,3,1]],
  [{enabled:'Ja'},[4,2,1]],
  [{enabled:'Nein'},[3]],
  [{status:'Verfügbar,Beschäftigt'},[2,1]],
  [{role:'Administrator,Gast'},[2,3]],
  [{enabled:'Ja',status:'Verfügbar,Beschäftigt',role:'Benutzer,Gast'},[1]],
  [{query:'NORA@'},[1]],
  [{query:'nicht vorhanden'},[]],
  [{cutoff:new Date(2026,9,25,14,30).getTime()},[4,3]],
  [{status:'Verfügbar,Beschäftigt,Abwesend,Offline'},[4,2,3,1]],
  [{role:'Superadmin,Administrator,Benutzer,Gast'},[4,2,3,1]]
];
for (const [filters,expected] of filterCases) {
  const actual = new vm.Script(visibleUsersCode + '\nvisibleUsers().map(user => user.id);').runInNewContext({
    users:filterUsers,filters,sort:{key:'name',direction:1}
  });
  assert.deepEqual(Array.from(actual),expected,`Benutzerfilter ${JSON.stringify(filters)}`);
}
console.log(`${filterCases.length} Benutzerfilterfälle für Suche, Mehrfachauswahl, UND/ODER und Datumsgrenze bestanden.`);
for (const [id, type, count] of [['filterEnabled','radio',3],['filterStatus','checkbox',4],['filterRole','checkbox',4]]) {
  const field = adminSource.match(new RegExp(`<fieldset\\b[^>]*id="${id}"[^>]*>[\\s\\S]*?<\\/fieldset>`, 'i'))?.[0];
  assert(field, `${id}: Auswahlgruppe fehlt`);
  assert.equal([...field.matchAll(new RegExp(`<input\\b[^>]*type="${type}"`, 'gi'))].length, count, `${id}: falsche Auswahlwerte`);
}
const portalEntry = scriptsByFile.get('portal.html').find(code => code.includes('portal-entry-route'));
assert(portalEntry, 'Einstieg aus der Benutzerverwaltung fehlt');
for (const route of ['home','notifications','profile','security','sessions','unbekannt']) {
  let selected = null;
  new vm.Script(portalEntry).runInNewContext({
    location: {hash: `#${route}`},
    document: {querySelector: selector => ({click() { selected = selector; }})}
  });
  assert.equal(selected, route === 'unbekannt' ? null : `[data-portal-route=${route}]`);
}

const routingScript = scriptsByFile.get('public.html').find(code => code.includes('cosmos-public-state'));
assert(routingScript, 'Besucher-Routing nicht gefunden');

function checkRouting(protocol) {
  const listeners = new Map();
  const messages = [];
  const hashEvents = [];
  const documentListeners = new Map();
  let notification = null;
  let hash = '';
  const origin = protocol === 'file:' ? 'null' : 'http://127.0.0.1:60825';
  const parent = {postMessage: message => messages.push(message)};
  const element = () => ({
    dataset: {}, hidden: false, disabled: false, children: [],
    setAttribute() {}, removeAttribute() {}, focus() {},
    append(...nodes) { this.children.push(...nodes); },
    replaceChildren(...nodes) { this.children = nodes; }
  });
  const ids = new Map();
  const byId = id => {
    if (!ids.has(id)) { ids.set(id, element()); }
    return ids.get(id);
  };
  const routes = ['home','login','register','forgot','reset','verification','email-confirm','recovery','maintenance'];
  const sections = routes.map(route => {
    const section = byId(`public-${route}`);
    section.dataset = {publicPage: route, title: route, description: route};
    section.querySelector = selector => selector === '[data-revoke="all"]' ? byId('bulk') : byId(`${route}:${selector}`);
    return section;
  });
  byId('vp-sessions').querySelectorAll = () => [element(),element(),element()];
  byId('vp-sessions').querySelector = () => element();
  const document = {
    getElementById: byId,
    querySelectorAll: selector => selector === '[data-public-page]' ? sections : [],
    createElement: element,
    createTextNode: text => ({textContent: text}),
    addEventListener(type, callback) { documentListeners.set(type, callback); },
    dispatchEvent() {}
  };
  const location = {
    origin, protocol,
    get hash() { return hash; },
    set hash(value) {
      const next = value.startsWith('#') ? value : `#${value}`;
      if (next !== hash) { hash = next; hashEvents.push(next); }
    }
  };
  const window = {
    parent, scrollTo() {},
    PublicControls: {closeAll() {}},
    PublicNotifications: {
      close() { notification = null; },
      show(text, severity, title) { notification = {text,severity,title}; }
    },
    addEventListener(type, callback) {
      if (!listeners.has(type)) { listeners.set(type, []); }
      listeners.get(type).push(callback);
    }
  };
  const context = vm.createContext({document, window, location, parent, Event: class {}});
  new vm.Script(routingScript).runInContext(context);
  const send = (source, senderOrigin, route, state) => {
    for (const callback of listeners.get('message')) {
      callback({source,origin:senderOrigin,data:{type:'cosmos-public-state',route,state}});
    }
  };
  const flushHashEvents = () => {
    while (hashEvents.length) {
      hashEvents.shift();
      for (const callback of listeners.get('hashchange')) { callback(); }
    }
  };
  // Das nachlaufende Hash-Ereignis darf den gewählten Fehlerzustand nicht löschen.
  send(parent, origin, 'login', 'invalid');
  assert.equal(notification?.severity, 'error');
  flushHashEvents();
  assert.equal(notification?.severity, 'error', `${protocol}: Hash-Ereignis setzte Fehler zurück`);
  assert.equal(messages.at(-1).state, 'invalid');
  // Gleicher Absender allein reicht nicht: fremde Origins bleiben ausgeschlossen.
  send(parent, 'https://untrusted.invalid', 'forgot', 'ready');
  send({}, origin, 'forgot', 'ready');
  assert.equal(byId('public-login').hidden, false);
  assert.equal(notification?.severity, 'error');
  // Eine echte Hash-Navigation muss weiterhin Seite und Meldung aktualisieren.
  location.hash = '#public-forgot';
  flushHashEvents();
  assert.equal(byId('public-forgot').hidden, false);
  assert.equal(byId('public-login').hidden, true);
  assert.equal(notification, null);

  byId('vp-symbols').content = {querySelector: () => ({innerHTML: '<svg></svg>'})};
  for (const route of ['register','reset']) {
    send(parent, origin, route, 'ready');
    flushHashEvents();
    const fields = [byId(`${route}-password`), byId(`${route}-password-confirm`)];
    fields.forEach(field => { field.type = 'text'; field.value = 'Synthetischer-Entwurf!'; });
    byId('register-email').value = 'beispiel@example.test';
    byId('register-email-confirm').value = 'beispiel@example.test';
    byId('register-birth').value = '23.10.2000';
    const form = {
      dataset: {demoForm: route},
      querySelectorAll: selector => {
        if (selector === 'input[type=password]') { return fields.filter(field => field.type === 'password'); }
        if (selector === '.portal-password-wrap input') { return fields; }
        return [];
      }
    };
    const button = {
      dataset: {},
      hasAttribute: name => name === 'data-form-submit',
      closest: selector => selector === 'form' ? form : button
    };
    // Auch über die Augenaktion sichtbare Entwürfe müssen nach Erfolg leer sein.
    documentListeners.get('click')({target: button});
    assert(fields.every(field => field.value === ''), `${route}: sichtbarer Passwortentwurf blieb erhalten`);
    assert.equal(byId('register-email').value, 'beispiel@example.test');
  }
}

checkRouting('http:');
checkRouting('file:');
// Der echte Reset-Handler darf nur Eingaben leeren, keine übernommenen Filter/Seite ändern.
const resetCode = adminSource.match(/byId\('clearFilters'\)\.addEventListener\('click',\(\) => \{([\s\S]*?)\n  \}\);/)?.[1];
assert(resetCode, 'Filter-Reset-Handler fehlt');
const resetFields = new Map(['filterEnabled','filterStatus','filterRole','activityCalendarInput','filterTime','userSearch'].map(id => [id,{value:'Entwurf',dataset:{value:'ausgewählt'},disabled:false}]));
const appliedFilters = {query:'Elena',enabled:'true',role:'Benutzer'};
let resetRenderCount = 0;
const resetContext = vm.createContext({
  filters:appliedFilters, page:2, timeTrigger:{disabled:false},
  byId:id => resetFields.get(id),
  setCombo:(id,value) => { resetFields.get(id).dataset.value=value; },
  closeTime() {}, render() { resetRenderCount++; }
});
new vm.Script(resetCode).runInContext(resetContext);
assert.equal(resetContext.filters, appliedFilters, 'Zurücksetzen ersetzte übernommene Filter');
assert.deepEqual(appliedFilters, {query:'Elena',enabled:'true',role:'Benutzer'});
assert.equal(resetContext.page,2, 'Zurücksetzen änderte die Tabellenseite');
assert.equal(resetRenderCount,0, 'Zurücksetzen renderte die Tabelle');
assert(['filterEnabled','filterStatus','filterRole'].every(id => resetFields.get(id).dataset.value === ''));
assert(['activityCalendarInput','filterTime','userSearch'].every(id => resetFields.get(id).value === ''));
assert(resetFields.get('filterTime').disabled && resetContext.timeTrigger.disabled);
console.log('Filter zurücksetzen leert nur den Entwurf; übernommene Kriterien und Tabellenseite bleiben erhalten.');
// Den echten Positionierer mit festen Viewportgrenzen prüfen, ohne den Drawer zu starten.
const drawerController = readFileSync(new URL('quellen/benutzerverwaltung/drawer.js', import.meta.url), 'utf8');
const positionCode = drawerController.match(/  const position = \(\) => \{[\s\S]*?\n  \};/)?.[0];
assert(positionCode, 'Drawer-Positionierer fehlt');
const drawerCases = [
  [720,65,1000,1041,0],
  [720,65,679,720,41],
  [720,65,705,746,15],
  [720,65,-61,-20,0],
  [844,65,1200,1241,0],
  [844,65,803,844,41],
  [390,65,500,541,0],
  [390,65,375,416,15],
  [720,-5,1000,1041,0]
];
for (const [height,headerBottom,footerTop,footerBottom,expectedBottom] of drawerCases) {
  const properties = new Map();
  new vm.Script(positionCode + '\nposition();').runInNewContext({
    window: {innerHeight:height},
    document: {querySelector: selector => ({getBoundingClientRect: () => selector === 'header' ? {bottom:headerBottom} : {top:footerTop,bottom:footerBottom}})},
    drawer: {style: {setProperty: (name,value) => properties.set(name,value)}}
  });
  assert.equal(properties.get('--filter-drawer-bottom'), `${expectedBottom}px`, `Footer ${footerTop}–${footerBottom} bei ${height}px`);
  assert.equal(properties.get('--filter-drawer-top'), `${Math.max(0,headerBottom)}px`);
}
console.log(`${drawerCases.length} Drawer-Geometriefälle für unsichtbare, sichtbare und teilweise sichtbare Footer bestanden.`);
console.log(`${files.length} HTML-Dateien und ${scriptCount} Scripts geprüft; Routing-, Absender- und Passwortregressionen für HTTP und lokale Dateien bestanden.`);
