(() => {
 const tabs=[...document.querySelectorAll('[data-tab]')];
 const buttons=[...document.querySelectorAll('[data-switch]')];
 const menu=document.getElementById('mobileNav');
 const toggle=document.getElementById('navToggle');
 const backdrop=document.getElementById('menuBackdrop');
 function close(){menu.classList.remove('open');toggle.setAttribute('aria-expanded','false');backdrop.hidden=true;}
 function show(name){
  tabs.forEach(el=>{el.classList.toggle('tab-visible',el.dataset.tab===name);el.scrollTop=0;});
  buttons.forEach(btn=>btn.classList.toggle('active',btn.dataset.switch===name));
  close();
 }
 function flip(){const next=!menu.classList.contains('open');menu.classList.toggle('open',next);toggle.setAttribute('aria-expanded',String(next));backdrop.hidden=!next;}
 buttons.forEach(btn=>btn.addEventListener('click',()=>show(btn.dataset.switch)));
 toggle.addEventListener('click',flip);
 backdrop.addEventListener('click',close);
 document.addEventListener('keydown',e=>{if(e.key==='Escape')close();});
 show('flow');
})();