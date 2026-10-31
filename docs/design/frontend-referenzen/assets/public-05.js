(() => {
  const byId = id => document.getElementById(id);
  const allowed = ['home','login','register','forgot','reset','verification','email-confirm','recovery','maintenance'];
  let page = 'home';
  const notify = (text, severity='normal', title='', duration=3000) => window.PublicNotifications.show(text,severity,title,duration);
  const icon = name => byId('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  const outcome = (title, text, role='warning', target='login', label='Anmelden', action=null, kind='primary') => {
    const section = byId(`public-${page}`);
    const box = section.querySelector('.vp-outcome');
    box.className = `vp-outcome ${role}`;
    box.replaceChildren();
    const card = document.createElement('article'); card.className='vp-card portal-card';
    const header=document.createElement('div'); header.className='vp-card-head portal-card-head';
    const badge=document.createElement('span'); badge.className='vp-category portal-reference-icon'; badge.innerHTML=icon(role==='warning'||role==='invalid'?'attention':role);
    const heading=document.createElement('h2'); heading.textContent=title;
    const headingGroup=document.createElement('div'); headingGroup.className='portal-reference-heading';headingGroup.append(heading);
    header.append(badge,headingGroup);
    const body=document.createElement('div'); body.className='vp-card-body portal-card-body';
    const description=document.createElement('p'); description.textContent=text; body.append(description);
    const footer=document.createElement('div'); footer.className='vp-card-foot portal-card-foot';
    const link=document.createElement(action?'button':'a'); link.className=`vp-button portal-button ${kind}`;
    link.innerHTML=icon(action==='resend'?'mail':target==='home'?'home':target==='forgot'?'mail':'login');
    const actionText=document.createElement('span');actionText.textContent=label;link.append(actionText);
    if(action){link.type='button';link.dataset.demo=action;}else{link.href=`#public-${target}`;}
    footer.append(link); card.append(header,body,footer); box.append(card); box.hidden=false;
    section.querySelector('.vp-normal').hidden=true;
    return {body,footer};
  };
  const updateRecovery = () => {
    const count=byId('vp-sessions').querySelectorAll('[data-active-session]').length;
    byId('vp-session-count').textContent=`${count} aktive ${count===1?'Sitzung':'Sitzungen'}`;
    byId('vp-continue').disabled=count>=3;
    byId('public-recovery').querySelector('[data-revoke="all"]').disabled=!byId('vp-sessions').querySelector('[data-session]');
  };
  const showState = state => {
    window.PublicNotifications.close();
    parent.postMessage({type:'cosmos-public-outcome',page,state},location.protocol==='file:'?'*':location.origin);
    const section=byId(`public-${page}`);
    section.querySelector('.vp-outcome').hidden=true;
    section.querySelector('.vp-normal').hidden=false;
    if(state==='ready'){
      if(page==='verification'){outcome('Konto aktiviert','Ihr Benutzerkonto wurde erfolgreich aktiviert. Sie können sich jetzt anmelden.','success','home','Startseite');}
      return;
    }
    if(state==='success'){
      if(page==='forgot'){notify('Wenn ein geeignetes Konto vorhanden ist, erhalten Sie einen Link per E-Mail. Falls keine E-Mail eintrifft, prüfen Sie den Spamordner oder versuchen Sie es später erneut.','success','Anfrage entgegengenommen',-1);}
      else if(page==='register'){outcome('Registrierung erfolgreich','Bitte prüfen Sie Ihr E-Mail-Postfach und bestätigen Sie Ihre E-Mail-Adresse, um Ihr Benutzerkonto zu aktivieren.','success');}
      else if(page==='reset'){outcome('Passwort zurückgesetzt','Sie können sich jetzt mit Ihrem neuen Passwort anmelden.','success');}
      else if(page==='email-confirm'){outcome('E-Mail-Adresse geändert','Ihre E-Mail-Adresse wurde erfolgreich geändert. Bitte melden Sie sich mit der neuen Adresse an. Eine Sicherheitsbenachrichtigung geht an die bisherige und die neue Adresse.','success');}
      else if(page==='verification'){outcome('Konto aktiviert','Ihr Benutzerkonto wurde erfolgreich aktiviert. Sie können sich jetzt anmelden.','success','login','Anmelden');}
      else{outcome('Beispielzustand','Diese Ansicht ist nur eine Gestaltungsvorschau. Es wurde keine Anmeldung oder Kontenaktion ausgeführt.','success');}
    }else if(state==='unavailable'){
      if(page==='verification'){outcome('Aktivierung nicht bestätigt','Der Vorgang konnte momentan nicht verarbeitet werden. Versuchen Sie es später erneut.','error','home','Startseite');}
      const failures={login:'Die Anmeldung ist vorübergehend nicht möglich. Bitte versuchen Sie es später erneut.',register:'Die Registrierung ist derzeit nicht verfügbar. Bitte versuchen Sie es später erneut.',reset:'Das Zurücksetzen ist derzeit nicht verfügbar. Bitte versuchen Sie es später erneut.',verification:'Die Aktivierung ist derzeit nicht möglich. Bitte versuchen Sie es später erneut.','email-confirm':'Die Bestätigung ist derzeit nicht möglich. Ihre bisherige Adresse bleibt gültig. Bitte versuchen Sie es später erneut.'};
      notify(failures[page] || 'Der Vorgang konnte momentan nicht verarbeitet werden. Bitte versuchen Sie es später erneut.','error','Technisch nicht verfügbar',-1);
    }else if(page==='login' || page==='register'){
      notify(page==='login'?'Die E-Mail-Adresse oder das Passwort ist falsch. Bitte prüfen Sie Ihre Zugangsdaten.':'Bitte prüfen Sie Ihre Angaben. Die Registrierung konnte nicht abgeschlossen werden.','error','Vorgang nicht abgeschlossen',-1);
    }else if(page==='verification' && state==='invalid'){
      const {body,footer}=outcome('Aktivierungslink ungültig','Dieser Aktivierungslink ist ungültig.','invalid','home','Startseite',null,'secondary');
      const guidance=document.createElement('p');guidance.textContent='Fordern Sie einen neuen Link zur Aktivierung Ihres Benutzerkontos an.';body.append(guidance);
      const form=document.createElement('form');form.id='public-form-verification';form.className='vp-verification-request';form.dataset.demoForm='verification';
      const field=byId('forgot-email').closest('.vp-field').cloneNode(true);
      const input=field.querySelector('input');input.id='verification-email';input.name='verification-email';input.value='';field.querySelector('label').htmlFor=input.id;
      const button=document.createElement('button');button.type='button';button.dataset.formSubmit='';button.dataset.formOwner=form.id;button.className='vp-button portal-button primary';button.innerHTML=icon('mail')+'<span>Aktivierungslink anfordern</span>';
      form.append(field);body.append(form);
      const actions=document.createElement('div');actions.className='vp-actions vp-actions-paired';
      actions.append(...footer.children,button);footer.append(actions);
    }else if(page==='verification' && state==='expired'){
      const {footer}=outcome('Aktivierungslink abgelaufen','Fordern Sie eine neue Aktivierungsnachricht an, um die Registrierung fortzusetzen.','warning','home','Erneut senden','resend');
      const home=document.createElement('a');home.className='vp-button secondary portal-button';home.href='#public-home';home.innerHTML=icon('home')+'<span>Startseite</span>';
      const actions=document.createElement('div');actions.className='vp-actions vp-actions-paired';actions.append(home,...footer.children);footer.append(actions);
    }else if(page==='email-confirm'){
      const messages={expired:['Bestätigungslink abgelaufen','Dieser Link ist abgelaufen. Ihre E-Mail-Adresse wurde nicht geändert. Fordern Sie in Ihrem Konto einen neuen Link an.'],conflict:['Änderung nicht möglich','Diese E-Mail-Adresse kann nicht mehr verwendet werden. Ihre bisherige Adresse bleibt gültig. Fordern Sie eine Änderung mit einer anderen Adresse an.'],invalid:['Bestätigungslink ungültig','Dieser Link ist ungültig oder wurde bereits verwendet. Fordern Sie bei Bedarf in Ihrem Konto einen neuen Link an.']};
      const [title,text]=messages[state]||messages.invalid;outcome(title,text,state==='expired'?'warning':'invalid');
    }else if(page==='recovery'){
      outcome('Anmeldeschritt abgelaufen','Bitte melden Sie sich erneut an, um Ihre Zugänge zu prüfen.','warning');
    }else if(page==='reset'){
      const expired=state==='expired';
      outcome(expired?'Passwortlink abgelaufen':'Passwortlink ungültig',expired?'Dieser Passwortlink ist abgelaufen. Fordern Sie einen neuen Link zum Zurücksetzen Ihres Passworts an.':'Dieser Passwortlink ist ungültig oder wurde bereits verwendet. Fordern Sie einen neuen Link zum Zurücksetzen Ihres Passworts an.',expired?'warning':'invalid','forgot','Neuen Link anfordern');
    }else{
      outcome('Beispielhinweis','Dieser Zustand führt hier keine echte Aktion aus. Sie können zur Ausgangsansicht zurückkehren.','warning','home','Startseite');
    }
  };
  const navigate = (next, focus=true) => {
    page=allowed.includes(next)?next:'home';
    document.querySelectorAll('[data-public-page]').forEach(section=>{section.hidden=section.dataset.publicPage!==page;});
    document.querySelectorAll('.vp-header nav>a').forEach(link=>{if(link.hash===`#public-${page}`){link.setAttribute('aria-current','page');}else{link.removeAttribute('aria-current');}});
    const section=byId(`public-${page}`);
    byId('portalTitle').replaceChildren(document.createTextNode(section.dataset.title));
    const dot=document.createElement('span');dot.className='text-sky-400';dot.textContent='.';byId('portalTitle').append(dot);
    byId('portalDescription').textContent=section.dataset.description;
    document.querySelectorAll('[data-reveal]').forEach(button=>{const input=byId(button.dataset.reveal);input.value='';input.type='password';});
    document.querySelectorAll('[data-reveal]').forEach(button=>{button.setAttribute('aria-pressed','false');button.setAttribute('aria-label',`${byId(button.dataset.reveal).closest('.vp-field').querySelector('label').textContent.replace('•','').trim()} anzeigen`);});
    window.PublicControls.closeAll();document.dispatchEvent(new Event('cosmos-close-calendar'));
    showState('ready'); updateRecovery(); window.scrollTo({top:0});
    if(focus){byId(`public-${page}`).focus({preventScroll:true});}
    parent.postMessage({type:'cosmos-public-route',page},location.protocol==='file:'?'*':location.origin);
  };
  window.addEventListener('hashchange',()=>{
    const nextRoute=location.hash.replace('#public-','');
    if(nextRoute!==page){navigate(nextRoute);}
  });
  window.addEventListener('message',event=>{
    if(event.source!==parent || (event.origin!==location.origin && !(location.protocol==='file:' && event.origin==='null')) || event.data?.type!=='cosmos-public-state'){return;}
    const {route,state}=event.data;
    if(allowed.includes(route)){location.hash=`public-${route}`;navigate(route,false);}
    if(['ready','success','invalid','expired','conflict','unavailable'].includes(state)){showState(state);}
  });
  document.addEventListener('submit',event=>{
    event.preventDefault();
    const button=event.target.id?document.querySelector(`[data-form-owner="${event.target.id}"]`):event.target.querySelector('[data-form-submit]');
    button?.click();
  });
  document.addEventListener('keydown',event=>{
    const form=event.target.closest('form[data-demo-form]');
    if(event.key==='Enter' && form && event.target.tagName==='INPUT' && !['date','checkbox'].includes(event.target.type)){
      event.preventDefault();
      const button=form.id?document.querySelector(`[data-form-owner="${form.id}"]`):form.querySelector('[data-form-submit]');
      button?.click();
    }
  });
  document.addEventListener('click',event=>{
    const button=event.target.closest('button');if(!button){return;}
    if(button.dataset.reveal){const input=byId(button.dataset.reveal),visible=input.type==='password';input.type=visible?'text':'password';button.setAttribute('aria-pressed',String(visible));button.setAttribute('aria-label',`${input.closest('.vp-field').querySelector('label').textContent.replace('•','').trim()} ${visible?'verbergen':'anzeigen'}`);return;}
    if(button.hasAttribute('data-form-submit')){
      const form=button.closest('form') || byId(button.dataset.formOwner);
      const invalid=[...form.querySelectorAll('input,select')].find(input=>!input.checkValidity());
      if(invalid){
        const control=invalid.id==='register-gender'?byId('genderTrigger'):invalid;
        control.setAttribute('aria-invalid','true');control.focus();
        notify('Bitte füllen Sie die Pflichtfelder vollständig und im passenden Format aus.','warning','Eingaben prüfen',-1);return;
      }
      form.querySelectorAll('[aria-invalid]').forEach(control=>control.removeAttribute('aria-invalid'));
      if(form.dataset.demoForm==='register' && byId('register-birth').value.trim()){
        // Sichtbares deutsches Datum prüfen, ohne neue fachliche Altersregel zu erfinden.
        const match=/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/.exec(byId('register-birth').value);
        const date=match?new Date(Number(match[3]),Number(match[2])-1,Number(match[1])):null;
        if(!match || date.getDate()!==Number(match[1]) || date.getMonth()!==Number(match[2])-1 || date.getFullYear()!==Number(match[3])){
          byId('register-birth').focus();notify('Bitte geben Sie ein gültiges Datum im Format TT.MM.JJJJ ein.','warning','Geburtsdatum prüfen',-1);return;
        }
      }
      if(form.dataset.demoForm==='forgot'){showState('success');}
      else if(form.dataset.demoForm==='login'){notify('Gestaltungsvorschau: Keine Anmeldung ausgeführt. Den Sitzungsgrenzenfall finden Sie in der Seitenauswahl außerhalb der Oberfläche.');}
      else if(form.dataset.demoForm==='register'){
        if(byId('register-email').value!==byId('register-email-confirm').value || byId('register-password').value!==byId('register-password-confirm').value){notify('Bitte prüfen Sie die Bestätigungsfelder für E-Mail und Passwort.','warning','Eingaben prüfen',-1);return;}
        showState('success');form.querySelectorAll('.portal-password-wrap input').forEach(input=>{input.value='';});
      }
      else if(form.dataset.demoForm==='reset'){
        if(byId('reset-password').value!==byId('reset-password-confirm').value){notify('Die beiden Passwortentwürfe stimmen nicht überein.','warning','Eingaben prüfen',-1);return;}showState('success');form.querySelectorAll('.portal-password-wrap input').forEach(input=>{input.value='';});
      }else{notify('Gestaltungsvorschau: Keine E-Mail versendet.');}return;
    }
    if(button.dataset.revoke){if(button.dataset.revoke==='all'){byId('vp-sessions').replaceChildren();}else{button.closest('[data-session]').remove();}updateRecovery();notify('Nur synthetische Beispielzeilen entfernt. Keine Sitzung beendet.');return;}
    if(button.dataset.demo==='email-confirm'){showState('success');return;}
    if(button.dataset.demo){notify(button.dataset.demo==='resend'?'Gestaltungsvorschau: Keine Aktivierungsmail versendet.':button.dataset.demo==='continue'?'Gestaltungsvorschau: Keine Anmeldung ausgeführt.':'Beispielansicht aktualisiert. Keine Daten abgefragt.');return;}
  });
  navigate(location.hash.replace('#public-',''),false);
})();
