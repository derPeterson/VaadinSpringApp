/* Der native Download beginnt erst nach erfolgreicher Prüfung der Serverantwort. */
(() => {
  window.cosmosRecoveryCodeDownload = async button => {
    if (button.hasAttribute('aria-busy')) return null;
    button.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(button.getAttribute('data-download-url'), { credentials: 'same-origin', cache: 'no-store' });
      if (!response.ok || !response.headers.get('content-type')?.toLowerCase().startsWith('application/pdf')) {
        return false;
      }
      const pdf = await response.blob();
      if (await pdf.slice(0, 5).text() !== '%PDF-') return false;
      // Nur den bereinigten ASCII-Dateinamen des Servers übernehmen, niemals Pfade.
      const disposition = response.headers.get('content-disposition') || '';
      const fileName = /(?:^|;)\s*filename="([A-Za-z0-9_-]+\.pdf)"(?:;|$)/i.exec(disposition)?.[1];
      const url = URL.createObjectURL(pdf);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = fileName || 'recovery-codes.pdf';
      window.document.body.append(link);
      try {
        link.click();
      } finally {
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
      return true;
    } catch {
      return false;
    } finally {
      button.removeAttribute('aria-busy');
    }
  };
})();
