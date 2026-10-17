import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const window = {};
vm.runInNewContext(readFileSync(new URL('../../main/frontend/compact-time-input.js', import.meta.url), 'utf8'), { window, WeakMap });
const { install } = window.Vaadin.Flow.compactTimeInput;
const plain = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value));

test('compact times use the last two digits as minutes and retain the 24 hour range', () => {
  const picker = { value: '10:15', step: 900, i18n: { parseTime: () => 'native', formatTime: time => `${time.hours}:${time.minutes}` } };
  const format = picker.i18n.formatTime;
  install(picker);
  for (const [input, hours, minutes] of [['930',9,30],['1430',14,30],['0000',0,0],['2359',23,59],['123',1,23],[' 945 ',9,45]]) {
    assert.deepEqual(plain(picker.i18n.parseTime(input)), { hours, minutes, seconds:0, milliseconds:0 });
  }
  assert.equal(picker.value, '10:15');
  assert.equal(picker.step, 900);
  assert.equal(picker.i18n.formatTime, format);
});

test('invalid compact times do not fall back to the lenient native parser', () => {
  const picker = { i18n: { parseTime: () => { throw Error('unexpected fallback'); } } };
  install(picker);
  for (const input of ['2400','2360','999','12345','999999']) assert.equal(picker.i18n.parseTime(input), undefined);
});

test('separated times, AM/PM, single hours and empty input keep native parsing', () => {
  const calls = [];
  const picker = { i18n: { parseTime: text => { calls.push(text); return 'native'; } } };
  install(picker);
  for (const input of ['09:30','2:30 PM','9','12','']) assert.equal(picker.i18n.parseTime(input), 'native');
  assert.deepEqual(calls, ['09:30','2:30 PM','9','12','']);
});

test('installation is idempotent and can wrap a new parser after a locale update', () => {
  const picker = { i18n: { parseTime: () => 'de', formatTime: () => '09:30' } };
  install(picker);
  const first = picker.i18n.parseTime;
  install(picker);
  assert.equal(picker.i18n.parseTime, first);
  picker.i18n = { parseTime: () => 'en', formatTime: () => '9:30 AM' };
  install(picker);
  assert.equal(picker.i18n.parseTime('9:30 AM'), 'en');
  assert.equal(picker.i18n.formatTime({hours:9,minutes:30}), '9:30 AM');
  assert.equal(picker.i18n.parseTime('930').hours, 9);
});
