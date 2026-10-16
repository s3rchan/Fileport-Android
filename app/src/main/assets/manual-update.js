// Fileport Android v2.9: user-initiated GitHub Releases and offline APK updates.
(() => {
 'use strict';
 const messages = {
  'Checking GitHub Releases...':'GitHub sürümleri kontrol ediliyor...',
  'You already have the latest version.':'Zaten en güncel sürümü kullanıyorsunuz.',
  'An update operation is already running.':'Bir güncelleme işlemi devam ediyor.',
  'Check for an update first.':'Önce güncellemeleri kontrol edin.',
  'Download an update first.':'Önce güncellemeyi indirin.',
  'Android will ask you to confirm installation.':'Android, yükleme için onayınızı isteyecek.',
  'APK verified. Android will request installation approval.':'APK doğrulandı. Android yükleme onayı isteyecek.',
  'Select a Fileport APK from your files.':'Dosyalarınızdan bir Fileport APK seçin.',
  'Android bridge unavailable':'Android bağlantı arayüzü kullanılamıyor.',
  'Allow Fileport to install apps, then tap Install downloaded APK.':'Fileport uygulamasına yükleme izni verin, ardından İndirilen APK\'yı yükle düğmesine basın.'
 };
 const translate = value => {
  const tr = window.FileportI18n?.language === 'tr';
  if (!tr) return value;
  if (messages[value]) return messages[value];
  if (value.startsWith('Update check failed: ')) return 'Güncelleme kontrolü başarısız: ' + value.slice(21);
  if (value.startsWith('Update failed: ')) return 'Güncelleme başarısız: ' + value.slice(15);
  if (value.startsWith('Update rejected: ')) return 'Güncelleme reddedildi: ' + value.slice(17);
  if (value.startsWith('Installer error: ')) return 'Yükleyici hatası: ' + value.slice(17);
  if (value.startsWith('Downloading Fileport v')) return 'Fileport indiriliyor: v' + value.slice(22);
  return value;
 };
 const status = message => {
  const el=document.getElementById('apkUpdateStatus');
  if(el){el.dataset.message=message;el.textContent=translate(message);}
 };
 window.fileportUpdateStatus=status;
 window.fileportGithubUpdateAvailable=version=>{
  document.getElementById('downloadGithubUpdate').hidden=false;
  document.getElementById('installGithubUpdate').hidden=true;
  status(window.FileportI18n?.language==='tr'
   ? 'Yeni sürüm bulundu: v'+version+'. İndirmek için düğmeye basın.'
   : 'New version available: v'+version+'. Tap Download and install.');
 };
 window.fileportGithubUpdateUnavailable=message=>{
  document.getElementById('downloadGithubUpdate').hidden=true;
  document.getElementById('installGithubUpdate').hidden=true;
  status(message);
 };
 window.fileportGithubUpdateDownloaded=version=>{
  document.getElementById('downloadGithubUpdate').hidden=true;
  document.getElementById('installGithubUpdate').hidden=false;
  status(window.FileportI18n?.language==='tr'
   ? 'Fileport v'+version+' indirildi. Android yükleme onayı isteyecek.'
   : 'Fileport v'+version+' downloaded. Android will ask for installation approval.');
 };
 window.addEventListener('DOMContentLoaded',()=>{
  const native=window.FileportNative;
  document.getElementById('selectUpdateApk')?.addEventListener('click',()=>{
   if(!native?.selectUpdateApk)return status('Android bridge unavailable');
   status('Select a Fileport APK from your files.');
   native.selectUpdateApk();
  });
  document.getElementById('checkGithubUpdate')?.addEventListener('click',()=>{
   if(!native?.checkGithubUpdate)return status('Android bridge unavailable');
   document.getElementById('downloadGithubUpdate').hidden=true;
   document.getElementById('installGithubUpdate').hidden=true;
   native.checkGithubUpdate();
  });
  document.getElementById('downloadGithubUpdate')?.addEventListener('click',()=>{
   if(!native?.downloadGithubUpdate)return status('Android bridge unavailable');
   native.downloadGithubUpdate();
  });
  document.getElementById('installGithubUpdate')?.addEventListener('click',()=>{
   if(!native?.installGithubUpdate)return status('Android bridge unavailable');
   native.installGithubUpdate();
  });
  document.getElementById('languageSelect')?.addEventListener('change',()=>{
   const el=document.getElementById('apkUpdateStatus');
   if(el?.dataset.message)el.textContent=translate(el.dataset.message);
  });
 });
})();
