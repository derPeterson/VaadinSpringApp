(() => {
  const logos = document.getElementById('preview-logo-library');
  const sky = document.getElementById('preview-sky');
  window.addEventListener('message', event => {
    if (window.parent === window || event.source !== window.parent ||
        (event.origin !== location.origin && !(location.protocol === 'file:' && event.origin === 'null')) ||
        event.data?.type !== 'cosmos-appearance') { return; }
    const {logo, background} = event.data;
    const template = typeof logo === 'string' && /^[a-o]$/.test(logo)
      ? logos.content.querySelector(`[data-preview-logo="${logo}"]`) : null;
    if (template) {
      const previous = document.querySelector('header [data-service-logo]');
      if (previous && previous.dataset.serviceLogo !== logo) {
        previous.replaceWith(template.cloneNode(true));
      }
    }
    if (typeof background === 'string' && sky.dataset.allowed.split(',').includes(background)) {
      document.body.dataset.previewBackground = background;
    }
  });
})();
