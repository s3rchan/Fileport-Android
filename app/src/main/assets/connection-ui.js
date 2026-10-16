/* Android passively waits for signed Windows connection requests.
 * Connection approval and secure pairing are handled by discovery.js/pairing.js.
 * Do not show a reconnect modal or initiate connections from Android UI.
 */
(() => {
 'use strict';
 const setup=document.getElementById('androidFirstSetup');
 const hasTrustedDevices=()=>Boolean(window.FileportIdentity?.readTrusted()?.length);
 const render=()=>{if(setup)setup.classList.toggle('hidden',hasTrustedDevices());};
 window.addEventListener('fileport-trusted-updated',render);
 window.addEventListener('fileport-peer-connected',()=>{
  render();
  document.querySelector('[data-switch="flow"]')?.click();
 });
 render();
})();
