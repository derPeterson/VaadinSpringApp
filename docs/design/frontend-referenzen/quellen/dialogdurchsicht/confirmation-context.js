// Beginn Nachrichtenkontext: dieselben Metadaten wie im Nachrichtendetail.
const contextText = document.createElement('div');
contextText.className = 'portal-confirm-text';
const contextTitle = document.createElement('strong');
const selectedMessage = kind === 'delete' ? messages.find(item => item.id === ids[0]) : null;
contextTitle.textContent = selectedMessage?.title || `${ids.length} ${ids.length === 1 ? 'Nachricht' : 'Nachrichten'} betroffen`;
contextText.append(contextTitle);
if (selectedMessage) {
  const metadata = document.createElement('div');
  metadata.className = 'n-detail-meta portal-confirm-metadata';
  metadata.append(messageMetadata(selectedMessage));
  contextText.append(metadata);
}
const contextIcon = document.createElement('span');
contextIcon.className = 'portal-confirm-object';
contextIcon.setAttribute('aria-hidden','true');
contextIcon.append(icon('bell'));
byId('n-confirm-context').replaceChildren(contextIcon,contextText);
// Ende Nachrichtenkontext.
