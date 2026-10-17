// Ergänzt nur kompakte Ziffernfolgen. Formatierung, AM/PM und übrige Eingaben bleiben nativ.
const installed = new WeakMap();

function install(picker) {
  const original = picker.i18n?.parseTime;
  if (typeof original !== 'function' || original === installed.get(picker)) return;
  const wrapped = text => {
    const digits = typeof text === 'string' ? text.trim() : '';
    // Vollständiger String aus ASCII-Ziffern, keine Gruppen oder Teiltreffer.
    if (/^[0-9]+$/.test(digits) && digits.length >= 3) {
      if (digits.length !== 3 && digits.length !== 4) return undefined;
      const hours = Number(digits.slice(0, -2));
      const minutes = Number(digits.slice(-2));
      if (hours > 23 || minutes > 59) return undefined;
      return { hours, minutes, seconds: 0, milliseconds: 0 };
    }
    return original(text);
  };
  installed.set(picker, wrapped);
  picker.i18n = { ...picker.i18n, parseTime: wrapped };
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.compactTimeInput = { install };
