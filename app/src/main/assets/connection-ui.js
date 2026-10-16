/* Setup and chat screens are driven by signed Windows connection requests. */
(() => {
 'use strict';
 const $=id=>document.getElementById(id);
 let online=false;
 const trusted=()=>window.FileportIdentity?.readTrusted()||[];
 function render(){
  const first=!trusted().length;
  $('androidFirstSetup').classList.toggle('hidden',!first);
  $('androidReconnect').classList.toggle('hidden',first||online);
 }
 window.addEventListener('fileport-trusted-updated',render);
 window.addEventListener('fileport-peer-connected',()=>{online=true;render();document.querySelector('[data-switch="flow"]')?.click();});
 window.addEventListener('fileport-peer-disconnected',()=>{online=false;render();});
 window.addEventListener('fileport-connection-error',()=>{online=false;render();});
 $('androidReconnectBtn').addEventListener('click',()=>{
   $('reconnectNearbyList').textContent='Open Fileport on your PC and choose Connect. The request will appear here.';
 });
 render();
})();
