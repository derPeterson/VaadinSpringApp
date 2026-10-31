document.querySelectorAll('[data-external-preview]').forEach(button => {
  button.addEventListener('click', () => {
    window.PublicNotifications.show(
      'Die Anmeldung mit ' + button.dataset.externalPreview + ' erfolgt in der Anwendung. Neue externe Konten benötigen kein Portalpasswort. Fehlende Namen und die Kontaktadresse werden anschließend ergänzt.',
      'normal', 'Anbieteranmeldung', -1);
  });
});
