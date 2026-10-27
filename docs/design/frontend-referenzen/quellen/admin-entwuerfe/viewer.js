(() => {
  const frame=document.getElementById('account-preview');
  const page=document.getElementById('ad-page');
  const variant=document.getElementById('ad-variant');
  const state=document.getElementById('ad-state');
  const requestedPage=location.hash?.slice(1);
  if([...page.options].some(option=>option.value===requestedPage)){page.value=requestedPage;}
  let latestRequest=0;
  const states={dashboard:['ready'],settings:['ready','error','unavailable'],mail:['ready','validation','success','error','maintenance','unavailable'],send:['ready','preview-valid','preview-invalidated','no-recipients','validation','success','error','empty','maintenance','unavailable','permission-denied'],users:['ready']};
  const restrict=()=>{
    const route=page.value.split(':')[0];
    [...state.options].forEach(option=>{option.disabled=!states[route].includes(option.value);option.hidden=option.disabled;});
    if(!states[route].includes(state.value)){state.value='ready';}
    if(route==='dashboard'||route==='settings'||route==='mail'){variant.value='a';}
    if(route==='send'){variant.value='b';}
    variant.disabled=true;
  };
  const send=()=>{
    restrict();const [route,group]=page.value.split(':');
    frame.contentWindow.postMessage({type:'cosmos-admin-proposal',requestId:++latestRequest,route,group,variant:variant.value,state:state.value},'*');
  };
  page.addEventListener('change',()=>{state.value='ready';send();});
  variant.addEventListener('change',send);state.addEventListener('change',send);frame.addEventListener('load',send);
  window.addEventListener('message',event=>{
    if(latestRequest===0){return;}
    const ownSandbox=event.origin==='null'&&frame.hasAttribute('sandbox');
    if(event.source!==frame.contentWindow||(event.origin!==location.origin&&!ownSandbox)){return;}
    if(event.data?.type!=='cosmos-admin-proposal-route'){return;}
    if(event.data.requestId!==null&&event.data.requestId!==latestRequest){return;}
    const value=event.data.route==='settings'?`settings:${event.data.group}`:event.data.route;
    if([...page.options].some(option=>option.value===value)){page.value=value;restrict();}
    if([...state.options].some(option=>!option.disabled&&option.value===event.data.state)){state.value=event.data.state;}
  });
})();
