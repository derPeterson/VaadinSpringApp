(() => {
  const frame = document.getElementById('account-preview');
  const logo = document.getElementById('preview-logo');
  const background = document.getElementById('preview-background');
  const send = () => frame.contentWindow.postMessage({
    type: 'cosmos-appearance', logo: logo.value, background: background.value
  }, '*');
  logo.addEventListener('change', send);
  background.addEventListener('change', send);
  frame.addEventListener('load', send);
  const toolbar = document.querySelector('.preview-toolbar');
  new ResizeObserver(() => document.body.style.setProperty(
    '--preview-toolbar-height', `${toolbar.getBoundingClientRect().height}px`
  )).observe(toolbar);
})();
