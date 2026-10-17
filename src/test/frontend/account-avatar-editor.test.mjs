import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

// Offline-Vertrag der Upload-/Crop-Steuerung, keine Browser- oder Renderingprüfung.
class FakeElement extends EventTarget {
  constructor() { super(); this.attributes = new Map(); this.clientWidth = 96; this.value = '1'; this.files = []; }
  setAttribute(name, value) { this.attributes.set(name, String(value)); if (this.constructor.observedAttributes?.includes(name)) this.attributeChangedCallback(); }
  getAttribute(name) { return this.attributes.get(name) ?? null; }
  querySelector(selector) { const name = selector.match(/data-avatar="([^"]+)"/)[1]; return this.elements[name]; }
  getContext() { return { drawImage() {} }; }
  setPointerCapture() {}
}
const registered = new Map();
globalThis.HTMLElement = FakeElement;
globalThis.customElements = { get: name => registered.get(name), define: (name, ctor) => registered.set(name, ctor) };
globalThis.MutationObserver = class { observe() {} disconnect() {} };
globalThis.ResizeObserver = class { observe() {} disconnect() {} };
globalThis.window = { devicePixelRatio: 1 };
let revoke = [];
let serial = 0;
const images = [];
globalThis.Image = class {
  constructor() { this.naturalWidth = 400; this.naturalHeight = 200; images.push(this); }
  set src(value) { this.source = value; queueMicrotask(() => this.onload?.()); }
};
const nativeURL = globalThis.URL;
nativeURL.createObjectURL = () => `blob:avatar-${++serial}`;
nativeURL.revokeObjectURL = value => revoke.push(value);
vm.runInThisContext(readFileSync(new URL('../../main/frontend/account-avatar-editor.js', import.meta.url), 'utf8'));
function editor() {
  const element = new (registered.get('account-avatar-editor'))();
  element.elements = Object.fromEntries(['upload','filename','error','frame','crop','preview','zoom','percent','reset'].map(name => [name,new FakeElement()]));
  element.elements.frame.clientWidth = 240;
  element.elements.crop.parentElement = element.elements.frame;
  element.elements.preview.parentElement = {clientWidth:96};
  element.setAttribute('data-draft-id','first-draft');
  element.setAttribute('data-mode','INITIALS');
  element.setAttribute('data-error-image','image-error');
  element.isConnected = true;
  element.connectedCallback();
  return element;
}
const settle = () => new Promise(resolve => setImmediate(resolve));
const file = () => new File([Uint8Array.of(137,80,78,71,13,10,26,10,0)], 'picture.png', {type:'image/png'});
function dispatch(element,type,detail) { element.dispatchEvent(new CustomEvent(type,{detail})); }
async function upload(element, id) {
  const selected = file(); const headers = {};
  const xhr = {setRequestHeader:(key,value) => {headers[key]=value;},getResponseHeader:()=>id,abort(){}};
  dispatch(element.elements.upload,'upload-request',{file:selected,xhr});
  assert.equal(headers['X-Avatar-Draft'],'first-draft');
  assert.equal(element.getCrop().loading,true);
  dispatch(element.elements.upload,'upload-success',{file:selected,xhr});
  await settle();return {selected,xhr};
}
test('upload binds crop to the server image revision and clamps movements', async () => {
  const element=editor();await upload(element,'image-one');
  assert.equal(element.getCrop().imageId,'image-one');assert.equal(element.getCrop().local,true);
  element._x=10; element._y=10;element._clamp();
  assert.equal(element.getCrop().x,0.5);assert.equal(element.getCrop().y,0);
  element.elements.zoom.value='2';dispatch(element.elements.zoom,'input',{});
  assert.equal(element.getCrop().zoom,2);
  element.resetDraft();element.disconnectedCallback();
});
test('late upload completion after cancel cannot replace the next dialog draft', async () => {
  const element=editor();const selected=file();let aborted=0;
  const xhr={setRequestHeader(){},getResponseHeader:()=> 'stale-image',abort(){aborted++;}};
  dispatch(element.elements.upload,'upload-request',{file:selected,xhr});
  element.resetDraft();element.setAttribute('data-draft-id','next-draft');
  dispatch(element.elements.upload,'upload-success',{file:selected,xhr});await settle();
  assert.equal(aborted,1);assert.equal(element.getCrop().imageId,null);assert.equal(element.getCrop().local,false);
  element.disconnectedCallback();
});
test('a failed replacement retains the last accepted crop and releases blob URLs on close', async () => {
  const element=editor();await upload(element,'accepted-image');revoke=[];
  dispatch(element.elements.upload,'upload-error',{});
  assert.equal(element.getCrop().imageId,'accepted-image');
  assert.equal(element.elements.error.textContent,'image-error');
  element.resetDraft();assert.equal(revoke.length,1);assert.equal(element.getCrop().imageId,null);
  element.disconnectedCallback();
});
test('saved image can be displayed without submitting it as a new upload', async () => {
  const element=editor();element.loadSaved('/private-saved-image');await settle();
  assert.equal(element.getAttribute('data-has-image'),'true');assert.equal(element.getCrop().local,false);
  assert.equal(element.elements.zoom.disabled,true);assert.equal(element.getCrop().imageId,null);
  element.resetDraft();element.disconnectedCallback();
});
