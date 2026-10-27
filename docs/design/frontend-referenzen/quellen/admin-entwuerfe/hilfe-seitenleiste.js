(() => {
  const root=document.getElementById('admin-proposal');
  document.documentElement.classList.add('hc-overlay-page');
  const symbol=name=>document.getElementById('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  const drawer=document.createElement('dialog');
  drawer.id='configurationHelp';drawer.className='au-filter-drawer hc-drawer';
  drawer.setAttribute('aria-labelledby','hc-title');
  drawer.innerHTML=`<div class="au-drawer-head"><span class="portal-reference-icon">${symbol('info')}</span><div class="portal-reference-heading"><h2 id="hc-title" tabindex="-1" autofocus>Feldhilfe</h2><p>Hinweise zur Einstellung</p></div><button type="button" class="au-drawer-close" aria-label="Feldhilfe schließen">${symbol('close')}</button></div><div class="au-drawer-body" tabindex="0" role="region" aria-labelledby="hc-field-title"><p class="hc-group"></p><h3 id="hc-field-title"></h3><div class="hc-copy"></div></div><div class="au-drawer-footer hc-footer"><button type="button" class="portal-button" data-hc-close>${document.querySelector('[data-n-symbol="cancel" ]').innerHTML}Schließen</button></div>`;
  document.body.append(drawer);
  let activeField=null,initialOverflow='';
  function position(){
    const height=window.innerHeight;
    const header=document.querySelector('header').getBoundingClientRect();
    const footer=document.querySelector('footer').getBoundingClientRect();
    const visibleFooter=Math.max(0,Math.min(height,footer.bottom)-Math.max(0,footer.top));
    drawer.style.setProperty('--filter-drawer-top',`${Math.min(height,Math.max(0,header.bottom))}px`);
    drawer.style.setProperty('--filter-drawer-bottom',`${visibleFooter}px`);
  }
  function close(restoreFocus=true){
    if(!drawer.open){return;}
    drawer.close();
    document.documentElement.style.overflow=initialOverflow;
    if(activeField){
      activeField.trigger.setAttribute('aria-expanded','false');
      activeField.wrapper.removeAttribute('data-help-selected');
      if(restoreFocus&&activeField.trigger.isConnected){activeField.trigger.focus({preventScroll:true});}
    }
    activeField=null;
  }
  function helpContent(details,input){
    const content=document.createElement('div');
    const paragraphs=[...details.querySelectorAll('p')];
    const description=paragraphs[0].cloneNode(true);description.className='hc-description';
    content.append(description);
    if(input.dataset.configId==='COPYRIGHT_TEXT'){
      const placeholders=document.createElement('section');placeholders.className='hc-section';
      const title=document.createElement('h4');title.textContent='Erlaubte Platzhalter';
      const list=document.createElement('dl');list.className='hc-placeholders';
      for(const [token,meaning] of [['{year}','Aktuelles Kalenderjahr'],['{serviceName}','Wert aus Service Name']]){
        const row=document.createElement('div'),term=document.createElement('dt'),definition=document.createElement('dd'),code=document.createElement('code');
        code.textContent=token;term.append(code);definition.textContent=meaning;row.append(term,definition);list.append(row);
      }
      placeholders.append(title,list);content.append(placeholders);
    }
    const defaultParagraph=details.querySelector('[data-help-default]');
    if(defaultParagraph){
      const standard=document.createElement('div');standard.className='hc-default';
      const standardLabel=document.createElement('span'),standardValue=document.createElement('code');
      const defaultText=defaultParagraph.textContent,separator=defaultText.indexOf(':');
      standardLabel.textContent=defaultText.slice(0,separator);
      standardValue.textContent=defaultText.slice(separator+1).trim();standard.append(standardLabel,standardValue);content.append(standard);
    }
    const rules=document.createElement('section');rules.className='hc-section hc-rules';
    const rulesTitle=document.createElement('h4'),rulesText=details.querySelector('[data-help-rules-label]').textContent;
    rulesTitle.textContent=rulesText.endsWith(':')?rulesText.slice(0,-1):rulesText;
    rules.append(rulesTitle,details.querySelector('ul').cloneNode(true));content.append(rules);
    return content;
  }
  function open(field){
    if(activeField){activeField.trigger.setAttribute('aria-expanded','false');activeField.wrapper.removeAttribute('data-help-selected');}
    activeField=field;
    drawer.querySelector('.hc-group').textContent=root.querySelector('.ad-head h2').textContent;
    drawer.querySelector('#hc-field-title').textContent=field.label;
    drawer.querySelector('.hc-copy').replaceChildren(field.content.cloneNode(true));
    field.trigger.setAttribute('aria-expanded','true');field.wrapper.dataset.helpSelected='true';
    if(!drawer.open){
      initialOverflow=document.documentElement.style.overflow;
      position();
      document.documentElement.style.overflow='hidden';drawer.showModal();
    }
    position();drawer.querySelector('.au-drawer-body').scrollTop=0;
  }
  function install(){
    close(false);
    const panel=root.querySelector('.ad-config');
    if(!panel){return;}
    panel.querySelectorAll('.ad-field').forEach(wrapper=>{
      const details=wrapper.querySelector('details'),input=wrapper.querySelector('[data-config-id]'),label=wrapper.querySelector('label');
      if(!details||!input||!label){return;}
      const content=helpContent(details,input);
      const field={wrapper,input,label:label.textContent.replace('•','').trim(),content,trigger:null};
      const trigger=document.createElement('button');trigger.type='button';trigger.className='hc-trigger';
      const accessibleLabel=document.createElement('span');accessibleLabel.id='hc-label-'+input.id;accessibleLabel.className='hc-sr-only';accessibleLabel.textContent='Feldhilfe: '+field.label;
      trigger.innerHTML=symbol('info');trigger.append(accessibleLabel);trigger.setAttribute('aria-labelledby',accessibleLabel.id);
      trigger.setAttribute('aria-controls',drawer.id);trigger.setAttribute('aria-expanded','false');trigger.setAttribute('aria-haspopup','dialog');
      field.trigger=trigger;wrapper.classList.add('hc-field');details.remove();
      if(input.type==='checkbox'){
        const control=document.createElement('div');control.className='ad-toggle au-checkbox hc-toggle-control';
        wrapper.classList.remove('ad-toggle');wrapper.classList.add('hc-checkbox');
        control.append(label,input);wrapper.append(control);
      }
      wrapper.append(trigger);
      trigger.addEventListener('click',()=>{if(drawer.open&&activeField===field){close();}else{open(field);}});
    });
  }
  new MutationObserver(install).observe(root,{childList:true});
  drawer.querySelectorAll('.au-drawer-close,[data-hc-close]').forEach(button=>button.addEventListener('click',()=>close()));
  drawer.addEventListener('cancel',event=>{event.preventDefault();close();});
  drawer.addEventListener('click',event=>{
    if(event.target!==drawer){return;}
    const box=drawer.getBoundingClientRect();
    if(event.clientX<box.left||event.clientX>box.right||event.clientY<box.top||event.clientY>box.bottom){close();}
  });
  document.addEventListener('keydown',event=>{
    if(event.key==='Escape'&&drawer.open&&!event.defaultPrevented){event.preventDefault();close();}
  });
  document.addEventListener('click',event=>{
    if(drawer.open&&(event.target.closest('header')||event.target.closest('footer'))){close(false);}
  });
  window.addEventListener('resize',()=>{if(drawer.open){position();}});
  window.addEventListener('scroll',()=>{if(drawer.open){position();}},{passive:true});
  install();
  if(parent===window&&document.body.hasAttribute('data-config-help-preview')&&!root.querySelector('.ad-config')){document.querySelector('button[data-admin-route=settings]').click();}
})();
