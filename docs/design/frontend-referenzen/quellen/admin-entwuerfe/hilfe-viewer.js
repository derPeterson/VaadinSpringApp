(() => {
  const frame=document.getElementById('account-preview');
  const choice=document.getElementById('field-help-mode');
  const copy=document.getElementById('field-help-description');
  const descriptions={a:'A · Infoicon rechts am Feld. Klick/Tap öffnet ein Popover; Escape oder Außenklick schließt. Die Formularzeilen bleiben stehen.',b:'B · Hilfe am Label. Der vollständige Text klappt direkt unter dem Feld auf und bleibt während der Eingabe sichtbar.',c:'C · Gemeinsames Kontextpanel. Fokus oder Infoicon wählt ein Feld aus; auf dem Smartphone erscheint die Hilfe beim ausgewählten Feld.'};
  const send=()=>{copy.textContent=descriptions[choice.value];frame.contentWindow.postMessage({type:'cosmos-field-help',mode:choice.value},'*');};
  choice.addEventListener('change',send);frame.addEventListener('load',send);send();
})();
