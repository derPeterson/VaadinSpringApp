(() => {
  const units = { seconds: { label: 'Sekunden', one: 'Sekunde', factor: 1 }, minutes: { label: 'Minuten', one: 'Minute', factor: 60 }, hours: { label: 'Stunden', one: 'Stunde', factor: 3600 }, days: { label: 'Tage', one: 'Tag', factor: 86400 } };
  const input = document.getElementById('duration-value');
  const trigger = document.getElementById('unit-trigger');
  const menu = document.getElementById('unit-options');
  const options = [...menu.querySelectorAll('[role=option]')];
  const error = document.getElementById('duration-error');
  const human = document.getElementById('human-value');
  const secondsText = document.getElementById('seconds-value');
  const increase = document.getElementById('increase');
  const decrease = document.getElementById('decrease');
  const format = new Intl.NumberFormat('de-DE');
  let unit = 'days';
  function update() {
    const current = units[unit], empty = input.value === '', amount = input.valueAsNumber;
    const seconds = empty ? 1209600 : amount * current.factor;
    const valid = empty || (Number.isSafeInteger(amount) && amount > 0 && Number.isSafeInteger(seconds) && seconds <= 2147483647);
    input.max = String(Math.floor(2147483647 / current.factor));
    input.placeholder = String(1209600 / current.factor);
    input.setAttribute('aria-invalid', String(!valid));
    error.hidden = valid;
    error.textContent = valid ? '' : `Bitte eine ganze Zahl zwischen 1 und ${format.format(Number(input.max))} eingeben.`;
    human.textContent = valid ? empty ? 'Standardwert · 14 Tage' : `${format.format(amount)} ${amount === 1 ? current.one : current.label}` : 'Bitte Eingabe prüfen';
    secondsText.textContent = valid ? `${format.format(seconds)} ${seconds === 1 ? 'Sekunde' : 'Sekunden'}` : '—';
    increase.disabled = !empty && (!valid || amount >= Number(input.max));
    decrease.disabled = !empty && (!valid || amount <= 1);
  }
  function close(restore = false) {
    menu.hidden = true; trigger.setAttribute('aria-expanded', 'false');
    if (restore) trigger.focus();
  }
  function open() {
    menu.hidden = false; trigger.setAttribute('aria-expanded', 'true');
    options.find(option => option.dataset.unit === unit).focus();
  }
  trigger.addEventListener('click', () => menu.hidden ? open() : close(true));
  trigger.addEventListener('keydown', event => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') { event.preventDefault(); open(); }
  });
  options.forEach(option => option.addEventListener('click', () => {
    unit = option.dataset.unit;
    document.getElementById('unit-name').textContent = units[unit].label;
    options.forEach(item => item.setAttribute('aria-selected', String(item === option)));
    close(true); update();
  }));
  menu.addEventListener('keydown', event => {
    const index = options.indexOf(document.activeElement);
    let next;
    if (event.key === 'ArrowDown') next = (index + 1) % options.length;
    if (event.key === 'ArrowUp') next = (index - 1 + options.length) % options.length;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = options.length - 1;
    if (next !== undefined) { event.preventDefault(); options[next].focus(); }
    if (event.key === 'Escape') { event.preventDefault(); close(true); }
    if (event.key === 'Tab') close();
  });
  document.addEventListener('click', event => { if (!event.target.closest('.unit-control')) close(); });
  input.addEventListener('input', update);
  increase.addEventListener('click', () => { input.stepUp(); update(); });
  decrease.addEventListener('click', () => { if (input.value === '') input.value = '1'; else input.stepDown(); update(); });
  document.getElementById('reset').addEventListener('click', () => {
    unit = 'days'; input.value = '14'; document.getElementById('unit-name').textContent = units.days.label;
    options.forEach(item => item.setAttribute('aria-selected', String(item.dataset.unit === unit)));
    close(); update();
  });
  const help=document.getElementById('duration-help');
  document.getElementById('help-open').addEventListener('click',()=>{close();update();help.showModal();});
  document.querySelectorAll('[data-help-close]').forEach(button=>button.addEventListener('click',()=>help.close()));
  update();
})();
