import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

function createFixture() {
  const window = {};
  const observers = [];
  class Observer {
    constructor(update) { this.update = update; observers.push(this); }
    observe() { this.active = true; }
    disconnect() { this.active = false; }
  }
  vm.runInNewContext(readFileSync(new URL('../../main/frontend/notification-recipient-popover.js', import.meta.url), 'utf8'),
    { window, WeakMap, MutationObserver: Observer, CustomEvent });
  const listeners = new Map();
  const events = [];
  const avatars = [];
  const group = {
    querySelectorAll: () => avatars,
    addEventListener(type, listener, capture) { assert.equal(capture, true); listeners.set(type, listener); },
    removeEventListener(type, listener, capture) {
      assert.equal(listeners.get(type), listener); assert.equal(capture, true); listeners.delete(type);
    },
    dispatchEvent(event) { events.push(event.type); }
  };
  const send = (type, details = {}) => {
    const event = { type, button: 0, ...details, prevented: false, stopped: false,
      preventDefault() { this.prevented = true; }, stopImmediatePropagation() { this.stopped = true; } };
    listeners.get(type)(event);
    return event;
  };
  return { ...window.CosmosRecipientPopover, group, send, events, listeners, observers, avatars };
}

test('captures clicks anywhere in the avatar row before its native overflow handler', () => {
  const fixture = createFixture();
  fixture.connect(fixture.group);
  for (const target of ['first-avatar', 'overflow', 'gap']) {
    const event = fixture.send('click', { target });
    assert.equal(event.prevented, true);
    assert.equal(event.stopped, true);
  }
  assert.deepEqual(fixture.events, Array(3).fill('recipient-list-open'));
  assert.equal(fixture.send('click', { button: 2 }).stopped, false);
  assert.equal(fixture.events.length, 3);
});

test('Enter and Space open once; other keys and held keys do not request extra openings', () => {
  const fixture = createFixture();
  fixture.connect(fixture.group);
  for (const key of ['Enter', ' ']) {
    assert.equal(fixture.send('keydown', { key }).prevented, true);
    assert.equal(fixture.send('keydown', { key, repeat: true }).prevented, true);
  }
  assert.equal(fixture.send('keydown', { key: 'Tab' }).prevented, false);
  assert.deepEqual(fixture.events, ['recipient-list-open', 'recipient-list-open']);
});

test('installation is idempotent, updates light-DOM avatars and disconnects cleanly', () => {
  const fixture = createFixture();
  fixture.connect(fixture.group);
  fixture.connect(fixture.group);
  assert.equal(fixture.observers.length, 1);
  const attributes = new Map([['slot', 'overflow'], ['aria-haspopup', 'menu'], ['aria-expanded', 'false']]);
  fixture.avatars.push({
    getAttribute: name => attributes.get(name),
    setAttribute: (name, value) => attributes.set(name, value),
    removeAttribute: name => attributes.delete(name)
  });
  fixture.observers[0].update();
  assert.equal(attributes.get('tabindex'), '-1');
  assert.equal(attributes.get('role'), 'presentation');
  assert.equal(attributes.has('aria-haspopup'), false);
  fixture.disconnect(fixture.group);
  fixture.disconnect(fixture.group);
  assert.equal(fixture.listeners.size, 0);
  assert.equal(fixture.observers[0].active, false);
  fixture.connect(fixture.group);
  assert.equal(fixture.observers.length, 2);
});
