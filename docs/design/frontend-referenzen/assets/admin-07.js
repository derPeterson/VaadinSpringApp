(() => {
  const byId = id => document.getElementById(id);
  const input = byId('birthCalendarInput');
  const trigger = byId('birthCalendarTrigger');
  const birthCalendar = byId('birthCalendarDropdown');
  const grid = byId('birthCalendarGrid');
  const years = byId('birthCalendarYears');
  const today = new Date();
  let visible = new Date(1994,4,12);
  let yearMode = false;
  let selected = new Date(1994,4,12);
  const sameDay = (left,right) => left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
  const close = (focus = false) => { birthCalendar.hidden = true; trigger.setAttribute('aria-expanded','false'); if (focus) { trigger.focus(); } };
  const position = () => window.CosmosPreview.positionOverlay(birthCalendar,input,320,'start');
  const choose = date => {
    selected = date;
    input.value = `${String(date.getDate()).padStart(2,'0')}.${String(date.getMonth()+1).padStart(2,'0')}.${date.getFullYear()}`;
    input.dispatchEvent(new Event('input', {bubbles:true})); close(true);
  };
  const render = () => {
    const year = visible.getFullYear();
    const month = visible.getMonth();
    const startYear = year-((year-1)%12);
    byId('birthCalendarMonth').textContent = yearMode ? `${startYear} – ${startYear+11}` : visible.toLocaleDateString('de-DE',{month:'long',year:'numeric'});
    byId('birthCalendarPrevious').setAttribute('aria-label',yearMode ? 'Vorherige Jahre' : 'Vorheriger Monat');
    byId('birthCalendarNext').setAttribute('aria-label',yearMode ? 'Nächste Jahre' : 'Nächster Monat');
    byId('birthCalendarDays').hidden = yearMode;
    years.hidden = !yearMode;
    grid.replaceChildren();
    years.replaceChildren();
    if (yearMode) {
      for (let value=startYear;value<startYear+12;value++) {
        const button=document.createElement('button');
        button.type='button'; button.textContent=String(value); button.setAttribute('aria-pressed',String(value===year));
        button.addEventListener('click',()=>{visible=new Date(value,month,1);yearMode=false;render();byId('birthCalendarHeading').focus();});
        years.append(button);
      }
    } else {
      const offset=(new Date(year,month,1).getDay()+6)%7;
      const count=new Date(year,month+1,0).getDate();
      const cells=Math.ceil((offset+count)/7)*7;
      for (let index=0;index<cells;index++) {
        const date=new Date(year,month,index-offset+1);
        const button=document.createElement('button'); button.type='button'; button.textContent=String(date.getDate());
        button.disabled=date.getMonth()!==month;
        button.setAttribute('aria-label',date.toLocaleDateString('de-DE',{day:'numeric',month:'long',year:'numeric'}));
        button.setAttribute('aria-pressed',String(sameDay(date,selected)));
        if (sameDay(date,today)) { button.setAttribute('aria-current','date'); }
        button.tabIndex=sameDay(date,selected) || ((selected.getMonth()!==month || selected.getFullYear()!==year) && date.getDate()===1) ? 0 : -1;
        button.dataset.day=String(date.getDate());
        button.addEventListener('click',()=>choose(date));
        grid.append(button);
      }
    }
    position();
  };
  const open = () => {
    document.dispatchEvent(new Event('cosmos-close-overlays')); document.dispatchEvent(new Event('cosmos-close-calendar'));
    // Genau Tag.Monat.Jahr aus dem sichtbaren DE-Beispielfeld auslesen, keine Fachvalidierung ersetzen.
    const match=/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/.exec(input.value);
    if (match) {
      const parsed=new Date(Number(match[3]),Number(match[2])-1,Number(match[1]));
      if (parsed.getDate()===Number(match[1]) && parsed.getMonth()===Number(match[2])-1) { selected=parsed; }
    }
    visible=new Date(selected.getFullYear(),selected.getMonth(),1); yearMode=false;
    birthCalendar.hidden=false; trigger.setAttribute('aria-expanded','true'); render();
    const active=grid.querySelector('[aria-pressed=true]'); if (active) { active.focus(); }
  };
  trigger.addEventListener('click',()=>birthCalendar.hidden ? open() : close());
  input.addEventListener('keydown',event=>{if(event.key==='ArrowDown'){event.preventDefault();open();}});
  byId('birthCalendarHeading').addEventListener('click',()=>{yearMode=!yearMode;render();});
  byId('birthCalendarPrevious').addEventListener('click',()=>{visible=yearMode ? new Date(visible.getFullYear()-12,visible.getMonth(),1) : new Date(visible.getFullYear(),visible.getMonth()-1,1);render();});
  byId('birthCalendarNext').addEventListener('click',()=>{visible=yearMode ? new Date(visible.getFullYear()+12,visible.getMonth(),1) : new Date(visible.getFullYear(),visible.getMonth()+1,1);render();});
  byId('birthCalendarToday').addEventListener('click',()=>choose(new Date(today.getFullYear(),today.getMonth(),today.getDate())));
  byId('birthCalendarCancel').addEventListener('click',()=>close(true));
  grid.addEventListener('keydown',event=>{
    const steps={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7};
    const day=Number(event.target.dataset.day);
    if (!day || !(event.key in steps)) { return; }
    event.preventDefault();
    const next=new Date(visible.getFullYear(),visible.getMonth(),day+steps[event.key]);
    if(next.getMonth()!==visible.getMonth() || next.getFullYear()!==visible.getFullYear()){visible=new Date(next.getFullYear(),next.getMonth(),1);render();}
    const target=grid.querySelector(`button[data-day="${next.getDate()}"]:not([disabled])`);
    grid.querySelectorAll('button').forEach(button=>{button.tabIndex=button===target ? 0 : -1;}); if(target){target.focus();}
  });
  document.addEventListener('keydown',event=>{if(event.key==='Escape' && !birthCalendar.hidden){event.preventDefault();close(true);}});
  document.addEventListener('pointerdown',event=>{if(!birthCalendar.contains(event.target) && !trigger.contains(event.target)){close();}});
  document.addEventListener('cosmos-close-calendar',()=>close());
  window.addEventListener('resize',position); window.addEventListener('scroll',position,{capture:true,passive:true});
  if(document.body.dataset.initialOverlay==='birthCalendar'){open();}
})();
