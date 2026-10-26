// Offline-Prüfung der gesicherten Exporte, ohne Anwendung oder Browser zu starten.
import assert from 'node:assert/strict';
import {readFileSync, existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import vm from 'node:vm';

const files = ['index.html', 'portal.html', 'public.html', 'visitors.html'];
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
  assert(!/\son[a-z]+\s*=/i.test(markup), `${file}: Inline-Eventhandler`);
  assert(!/<link\b[^>]*rel="stylesheet"/i.test(markup), `${file}: externes CSS`);
  if (file === 'portal.html' || file === 'public.html') {
    const languages = [...markup.matchAll(/<button\b[^>]*data-menu-language="(de|en)"[^>]*>/g)];
    assert.deepEqual(languages.map(match => match[1]), ['de','en'], `${file}: Sprachumschalter fehlt`);
    assert.equal(languages.filter(match => match[0].includes('aria-pressed="true"')).length, 1);
  }
  // Nur Referenzanker, eingebettete Ressourcen und Dateien desselben Ordners zulassen.
  for (const [, , value] of markup.matchAll(/\s(?:src|href|xlink:href)\s*=\s*(["'])(.*?)\1/gi)) {
    if (value.startsWith('#') || value.startsWith('data:')) { continue; }
    const target = new URL(value, path);
    assert(target.protocol === 'file:' && new URL('.', target).href === new URL('.', path).href, `${file}: fremde Ressource ${value}`);
    assert(existsSync(fileURLToPath(target)), `${file}: fehlende Datei ${value}`);
  }
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
console.log(`${files.length} HTML-Dateien und ${scriptCount} Scripts geprüft; Routing-, Absender- und Passwortregressionen für HTTP und lokale Dateien bestanden.`);
