(() => {
 const STORAGE_KEY = 'yakin-flow-theme';
 const choices = ['system','light','dark'];
 let theme='system';
 try { const saved=localStorage.getItem(STORAGE_KEY); if(choices.includes(saved)) theme=saved; } catch(_) {}
 function apply(value) {
  theme=value;
  document.documentElement.dataset.theme=value;
  document.querySelectorAll('[data-theme-choice]').forEach(button => {
   const selected=button.dataset.themeChoice===value;
   button.setAttribute('aria-pressed',String(selected));
   button.classList.toggle('selected',selected);
  });
  try { localStorage.setItem(STORAGE_KEY,value); } catch(_) {}
  try { window.FileportNative?.setTheme?.(value); } catch(_) {}
 }
 document.querySelectorAll('[data-theme-choice]').forEach(button => button.addEventListener('click',() => apply(button.dataset.themeChoice)));
 apply(theme);
 // If the user follows the device theme, update the system bars as well.
 const media = window.matchMedia('(prefers-color-scheme: dark)');
 const systemChanged = () => { if (theme === 'system') apply('system'); };
 if (media.addEventListener) media.addEventListener('change', systemChanged);
 else if (media.addListener) media.addListener(systemChanged);
})();

(()=>{const icon=document.querySelector('.brand-wordmark');if(!icon)return;const update=()=>{const mode=document.documentElement.dataset.theme||'system';const dark=mode==='dark'||(mode==='system'&&matchMedia('(prefers-color-scheme: dark)').matches);icon.src=dark?'fileport-wordmark-dark.png':'fileport-wordmark.png';};new MutationObserver(update).observe(document.documentElement,{attributes:true,attributeFilter:['data-theme']});matchMedia('(prefers-color-scheme: dark)').addEventListener('change',update);update();})();
