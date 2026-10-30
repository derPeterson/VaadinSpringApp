(() => {
  const fields=window.AdminDraftInventory.fields;
  const nano=1000000000n,scale=1000000000000000000n;
  const units=[[1,'Sekunden','Sekunde'],[60,'Minuten','Minute'],[3600,'Stunden','Stunde'],[86400,'Tage','Tag']];
  const escape=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  function decimal(value){
    // Vorzeichen und höchstens 18 Nachkommastellen exakt als Festkommazahl lesen.
    const match=/^([+-]?)(\d+)(?:[.,](\d{0,18}))?$/.exec(String(value));
    if(!match){throw new Error('number');}
    return (match[1]==='-'?-1n:1n)*(BigInt(match[2])*scale+BigInt((match[3]||'').padEnd(18,'0')));
  }
  function plain(value,denominator){
    const sign=value<0n?'-':'',number=value<0n?-value:value;
    const fraction=(number%denominator).toString().padStart(denominator.toString().length-1,'0').replace(/0+$/,'');
    return sign+(number/denominator).toString()+(fraction?'.'+fraction:'');
  }
  function localized(value){
    const [whole,fraction]=value.split('.'),sign=whole==='-0'?'-':'';
    return sign+new Intl.NumberFormat('de-DE').format(BigInt(whole))+(fraction?','+fraction:'');
  }
  function decode(field,value){
    if(field.durationStorage!=='duration'){return BigInt(value)*nano*(field.durationStorage==='days'?86400n:1n);}
    // Die HTML-Beispielwerte verwenden ISO-Zeitspannen mit Tagen und Stunden/Minuten/Sekunden.
    const match=/^(-)?P(?:(-?\d+)D)?(?:T(?:(-?\d+)H)?(?:(-?\d+)M)?(?:(-?\d+(?:\.\d+)?)S)?)?$/.exec(value);
    if(!match){throw new Error('duration');}
    const seconds=(decimal(match[2]||'0')*86400n+decimal(match[3]||'0')*3600n+decimal(match[4]||'0')*60n+decimal(match[5]||'0'))*nano/scale;
    return match[1]?-seconds:seconds;
  }
  function snapshot(field,value,factor){
    const empty=value==='';
    if(empty&&field.durationStorage==='days'){return {valid:true,current:'Unbegrenzt',equivalent:'Unbegrenzt',stored:''};}
    if(empty&&field.durationStorage==='seconds'){const seconds=decode(field,field.durationDefault),unit=[86400,3600,60,1].find(factor=>seconds%(BigInt(factor)*nano)===0n);return snapshot(field,plain(seconds/BigInt(unit),nano),String(unit));}
    try{
      if(empty){throw new Error('required');}
      const number=decimal(value),multiplied=number*BigInt(factor)*nano;
      if(multiplied%scale!==0n){throw new Error('precision');}
      const seconds=multiplied/scale;
      if(field.durationStorage!=='duration'&&number%scale!==0n){throw new Error('integer');}
      if(field.durationStorage==='seconds'&&(seconds<1n*nano||seconds>2147483647n*nano)){throw new Error('range');}
      if(field.durationStorage==='days'&&(number<scale||number>2147483647n*scale)){throw new Error('range');}
      if(field.durationStorage==='duration'){
        if(seconds< -9223372036854775808n*nano||seconds>9223372036854775807n*nano+999999999n){throw new Error('range');}
        if(field.durationMinimumSeconds!==null&&seconds<decimal(field.durationMinimumSeconds)*nano/scale){throw new Error('minimum');}
      }
      const choice=units.find(unit=>unit[0]===Number(factor));
      return {valid:true,current:localized(plain(number,scale))+' '+choice[number===scale?2:1],equivalent:localized(plain(seconds,nano))+' '+(seconds===nano?'Sekunde':'Sekunden'),stored:field.durationStorage==='duration'?'PT'+plain(seconds,nano)+'S':field.durationStorage==='days'?plain(number,scale):(seconds/nano).toString()};
    }catch{return {valid:false,current:'Bitte Eingabe prüfen.',equivalent:'Bitte Eingabe prüfen.',stored:'invalid'};}
  }
  function inspect(wrapper){
    const hidden=wrapper.querySelector('[data-config-id]'),field=fields.find(field=>field.id===hidden.dataset.configId);
    const amount=wrapper.querySelector('[data-duration-amount]'),factor=wrapper.querySelector('[data-duration-unit][aria-selected=true]').dataset.durationUnit;
    const result=snapshot(field,amount.value,factor);
    if(amount.validity.badInput){return {valid:false,current:'Bitte Eingabe prüfen.',equivalent:'Bitte Eingabe prüfen.',stored:'invalid'};}
    return result;
  }
  function update(wrapper){
    const hidden=wrapper.querySelector('[data-config-id]'),field=fields.find(field=>field.id===hidden.dataset.configId);
    const amount=wrapper.querySelector('[data-duration-amount]'),factor=Number(wrapper.querySelector('[data-duration-unit][aria-selected=true]').dataset.durationUnit);
    if(field.durationStorage!=='duration'){amount.max=String(field.durationStorage==='days'?2147483647:Math.floor(2147483647/factor));}
    const result=inspect(wrapper);hidden.value=result.stored;
    amount.setCustomValidity(result.valid?'':'Bitte Zahl, Einheit und Mindestdauer prüfen.');
    amount.removeAttribute('aria-invalid');
  }
  function step(input,direction){
    try{input.value=plain((input.value===''?0n:decimal(input.value))+BigInt(direction)*scale,scale);}catch{return;}
    input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));
  }
  function render(field,value,help){
    const id='ad-config-'+field.id,amountId=id+'-amount',empty=value==='';
    const seconds=empty?0n:decode(field,String(value));
    const factor=field.durationStorage==='days'?86400:seconds!==0n?[86400,3600,60,1].find(unit=>seconds%(BigInt(unit)*nano)===0n)||1:1;
    const number=empty?'':plain(seconds/BigInt(factor),nano);
    const options=field.durationStorage==='days'?units.filter(unit=>unit[0]===86400):units;
    const icon=name=>document.getElementById('vp-symbols').content.querySelector(`[data-symbol="${name}"]`).innerHTML;
    const minimum=field.durationStorage==='duration'?'':' min="1"',required=field.required?' required':'';
    return `<div class="ad-field"><label for="${amountId}">${escape(field.label)}${field.required?'<span class="ad-required"> •</span>':''}</label><input type="hidden" id="${id}" data-config-id="${field.id}" value="${escape(value)}"><div class="ad-duration-control"><div class="ad-input-action ad-number-control"><input id="${amountId}" data-duration-amount type="number"${minimum}${required} step="${field.durationStorage==='duration'?'any':'1'}" value="${number}" placeholder="${field.durationStorage==='days'?'Unbegrenzt':''}"><span class="ad-number-actions"><button type="button" data-ad-step="1" data-ad-input="${amountId}" aria-label="Dauer erhöhen">${icon('chevron')}</button><button type="button" data-ad-step="-1" data-ad-input="${amountId}" aria-label="Dauer verringern">${icon('chevron')}</button></span></div><div class="ad-duration-unit"><button type="button" class="ad-duration-select" data-duration-trigger aria-label="Zeiteinheit" aria-haspopup="listbox" aria-expanded="false"${options.length===1?' disabled':''}><span>${units.find(unit=>unit[0]===factor)[1]}</span>${options.length>1?icon('chevron'):''}</button><div class="ad-duration-options" role="listbox" aria-label="Zeiteinheit" hidden>${options.map(([unit,label])=>`<button type="button" role="option" data-duration-unit="${unit}" aria-selected="${unit===factor}">${label}<span aria-hidden="true">✓</span></button>`).join('')}</div></div></div>${help}</div>`;
  }
  window.AdminDuration={render,inspect,update,step};
})();
