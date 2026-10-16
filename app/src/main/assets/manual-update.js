// Offline Android APK installer entry point.
window.fileportUpdateStatus=message=>{const el=document.getElementById('apkUpdateStatus');if(el)el.textContent=message;};
window.addEventListener('DOMContentLoaded',()=>{
 document.getElementById('selectUpdateApk')?.addEventListener('click',()=>{
  if(!window.FileportNative?.selectUpdateApk)return window.fileportUpdateStatus('Android bridge unavailable');
  window.fileportUpdateStatus('Select a Fileport APK from your files.');
  window.FileportNative.selectUpdateApk();
 });
});
