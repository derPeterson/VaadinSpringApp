(() => {
  const root=document.getElementById('admin-proposal');
  const symbol=name=>document.getElementById('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
  let mode='a', context=null;
  const compact=matchMedia('(max-width:1100px)');
  // Die Vergleichsseite konzentriert sich auf die acht Konfigurationsgruppen.
  document.querySelectorAll('button[data-admin-route]').forEach(button=>{
    if(button.dataset.adminRoute!=='settings'){button.disabled=true;}
  });
  const closePopovers=()=>root.querySelectorAll('.hp-popover:popover-open').forEach(node=>node.hidePopover());
  const positionPopover=(popover,trigger)=>{
    window.CosmosPreview.positionOverlay(popover,trigger,430,'end');
  };
  function inspect(field,focus=false){
    if(!context){return;}
    const inspector=context.panel.querySelector('.hp-inspector');
    if(!inspector){return;}
    inspector.replaceChildren(field.content.cloneNode(true));
    const heading=document.createElement('h3');heading.textContent=field.label;
    inspector.prepend(heading);inspector.setAttribute('aria-label','Feldhilfe: '+field.label);
    context.selected=field;
    context.panel.querySelectorAll('[data-hp-field]').forEach(button=>button.setAttribute('aria-pressed',String(button===field.trigger)));
    if(compact.matches){field.wrapper.after(inspector);}else{context.workspace.append(inspector);}
    if(focus){inspector.focus();}
  }
  function apply(){
    const panel=root.querySelector('.ad-config');
    if(!panel){context=null;return;}
    closePopovers();
    const grid=panel.querySelector('.ad-grid');
    if(context?.panel!==panel){context={panel,grid,original:grid.innerHTML,selected:null};}
    const values=new Map([...grid.querySelectorAll('[data-config-id]')].map(input=>[input.id,{value:input.value,checked:input.checked}]));
    const workspace=panel.querySelector('.hp-workspace');
    if(workspace){workspace.replaceWith(grid);}
    panel.querySelectorAll('.hp-popover,.hp-inspector').forEach(node=>node.remove());
    grid.innerHTML=context.original;
    grid.querySelectorAll('[data-config-id]').forEach(input=>{
      const value=values.get(input.id);if(!value){return;}
      if(input.type==='checkbox'){input.checked=value.checked;}else{input.value=value.value;}
    });
    panel.dataset.helpProposal=mode;context.selected=null;
    if(mode==='c'){
      context.workspace=document.createElement('div');context.workspace.className='hp-workspace';
      grid.before(context.workspace);context.workspace.append(grid);
      const inspector=document.createElement('aside');inspector.className='hp-inspector';inspector.tabIndex=-1;
      inspector.setAttribute('aria-live','polite');inspector.innerHTML='<h3>Feldhilfe</h3><p>Ein Feld auswählen, um seine Beschreibung, den Standardwert und die Eingaberegeln zu lesen.</p>';
      inspector.id='hp-inspector';context.workspace.append(inspector);
    }
    grid.querySelectorAll('.ad-field').forEach(wrapper=>{
      const details=wrapper.querySelector('details'),input=wrapper.querySelector('[data-config-id]'),label=wrapper.querySelector('label');
      if(!details||!input||!label){return;}
      const content=document.createElement('div');content.className='hp-help-copy';
      [...details.children].filter(node=>node.tagName!=='SUMMARY').forEach(node=>content.append(node.cloneNode(true)));
      const field={wrapper,input,label:label.textContent.replace('•','').trim(),content,trigger:null};
      const id='hp-help-'+input.id;label.id='hp-label-'+input.id;
      const trigger=document.createElement('button');trigger.type='button';trigger.className='hp-trigger';
      const triggerLabel=document.createElement('span');triggerLabel.id='hp-trigger-label-'+input.id;triggerLabel.className='sr-only';triggerLabel.textContent='Feldhilfe: '+field.label;
      trigger.setAttribute('aria-labelledby',triggerLabel.id);trigger.innerHTML=symbol('info');trigger.append(triggerLabel);field.trigger=trigger;
      details.remove();
      if(mode==='a'){
        wrapper.classList.add('hp-field-right');trigger.setAttribute('aria-haspopup','dialog');trigger.setAttribute('aria-controls',id);trigger.setAttribute('aria-expanded','false');
        const popover=document.createElement('section');popover.id=id;popover.className='hp-popover';popover.setAttribute('popover','auto');
        popover.setAttribute('role','dialog');popover.setAttribute('aria-labelledby',label.id);
        const heading=document.createElement('div');heading.className='hp-popover-head';
        const title=document.createElement('h3');title.textContent=field.label;
        const close=document.createElement('button');close.type='button';close.className='portal-button portal-icon-button';
        close.setAttribute('aria-label','Feldhilfe schließen');close.innerHTML=symbol('close');
        close.addEventListener('click',()=>{popover.hidePopover();trigger.focus();});
        heading.append(title,close);popover.append(heading,content);panel.append(popover);wrapper.append(trigger);
        trigger.addEventListener('click',()=>{
          if(popover.matches(':popover-open')){popover.hidePopover();return;}
          popover.showPopover({source:trigger});positionPopover(popover,trigger);
        });
        popover.addEventListener('toggle',()=>trigger.setAttribute('aria-expanded',String(popover.matches(':popover-open'))));
        const position=()=>{if(popover.matches(':popover-open')){positionPopover(popover,trigger);}};
        popover.addEventListener('beforetoggle',event=>{if(event.newState==='closed'){trigger.setAttribute('aria-expanded','false');}});
        field.position=position;field.popover=popover;
      }else if(mode==='b'){
        const row=document.createElement('div');row.className='hp-label-row';label.before(row);row.append(label,trigger);
        trigger.classList.add('hp-inline-trigger');trigger.insertAdjacentHTML('beforeend','<span>Hilfe</span>');
        content.id=id;content.hidden=true;content.setAttribute('role','region');content.setAttribute('aria-labelledby',label.id);
        trigger.setAttribute('aria-controls',id);trigger.setAttribute('aria-expanded','false');wrapper.append(content);
        trigger.addEventListener('click',()=>{content.hidden=!content.hidden;trigger.setAttribute('aria-expanded',String(!content.hidden));});
      }else{
        wrapper.classList.add('hp-field-right');trigger.dataset.hpField=input.id;trigger.setAttribute('aria-controls','hp-inspector');
        trigger.setAttribute('aria-pressed','false');wrapper.append(trigger);
        trigger.addEventListener('click',()=>inspect(field));input.addEventListener('focus',()=>inspect(field));
      }
      input.dataset.hpInput='true';wrapper.hpHelp=field;
    });
  }
  new MutationObserver(apply).observe(root,{childList:true});
  const reposition=()=>{
    if(!context?.panel.isConnected){return;}
    if(mode==='a'){context.grid.querySelectorAll('.ad-field').forEach(wrapper=>wrapper.hpHelp?.position?.());}
    if(mode==='c'&&context.selected){inspect(context.selected);}
  };
  window.addEventListener('resize',reposition);window.addEventListener('scroll',reposition,{capture:true,passive:true});
  document.addEventListener('keydown',event=>{
    if(event.key==='Escape'&&mode==='b'&&context){
      const open=[...context.panel.querySelectorAll('.hp-help-copy:not([hidden])')];
      if(open.length){event.preventDefault();open.forEach(node=>{node.hidden=true;const trigger=context.panel.querySelector(`[aria-controls="${node.id}"]`);trigger.setAttribute('aria-expanded','false');});}
    }
  });
  window.addEventListener('message',event=>{
    if(parent===window||event.source!==parent||(event.origin!==location.origin&&!(location.protocol==='file:'&&event.origin==='null'))||event.data?.type!=='cosmos-field-help'){return;}
    if(['a','b','c'].includes(event.data.mode)){mode=event.data.mode;apply();}
  });
  apply();
  if(parent===window&&!root.querySelector('.ad-config')){document.querySelector('button[data-admin-route=settings]').click();}
})();
