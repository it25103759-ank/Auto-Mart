
/* AutoMart remaining profile fixes: budget, listings, reviews, security, danger, search */
(function(){
  'use strict';

  const PROFILE_KEY = 'automartCurrentProfileV2';
  const USER_KEY = 'automartUserProfileRealV4';
  const BUYER_KEY = 'automartBuyerProfileRealV1';
  const SETTINGS_KEY = 'automartSettingsProfileV1';
  const SHARED_KEY = 'automartSharedMarketplaceDataV2';
  const LISTINGS_KEY = 'automartProfileListingsV3';
  const REVIEWS_KEY = 'automartProfileReviewsV2';
  const REVIEW_SORT_KEY = 'automartProfileReviewSortV1';
  const SUB_KEY = 'automartDealerProSubscriptionV2';
  const SESSIONS_KEY = 'automartActiveSessionsV1';
  const PASSWORD_KEY = 'automartLocalPasswordV1';
  const NOTIF_KEY = 'automartLiveNotificationsV1';
  const MSG_KEY = 'automartLiveMessagesV1';

  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function toast(msg,type){if(window.AutoMartToast) window.AutoMartToast(msg,type||'success'); else console.log(msg);}
  function popup(title,msg,ok,cb,icon){if(window.AutoMartConfirm) return window.AutoMartConfirm(title,msg,ok,cb,icon||'✓'); if(cb) cb();}
  function money(n){n=Number(String(n||0).replace(/[^\d]/g,'')); return n ? n.toLocaleString('en-US') : '';}
  function parseMoney(v){return Number(String(v||'').replace(/[^\d]/g,''))||0;}
  function mLabel(n){n=Number(n)||0; return n>=1000000 ? Math.round(n/1000000)+'M' : Math.round(n/1000)+'K';}

  const LISTING_MEDIA_PREFIX = 'automartListingImage_';
  const LISTING_PLACEHOLDER = "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='900' height='520' viewBox='0 0 900 520'><defs><linearGradient id='g' x1='0' x2='1' y1='0' y2='1'><stop offset='0' stop-color='#07100d'/><stop offset='1' stop-color='#1f2b1b'/></linearGradient><radialGradient id='r' cx='50%' cy='45%' r='60%'><stop offset='0' stop-color='#c8ff3e' stop-opacity='.20'/><stop offset='1' stop-color='#c8ff3e' stop-opacity='0'/></radialGradient></defs><rect width='900' height='520' rx='32' fill='url(#g)'/><rect width='900' height='520' fill='url(#r)'/><path d='M170 330h560l-48-86c-12-22-35-36-60-36H340c-26 0-50 14-63 37l-36 65h-42c-20 0-36 16-36 36v20h70a64 64 0 0 1 126 0h236a64 64 0 0 1 126 0h42v-16c0-22-18-40-40-40h-34' fill='none' stroke='#c8ff3e' stroke-width='12' stroke-linecap='round' stroke-linejoin='round' opacity='.75'/><circle cx='296' cy='366' r='34' fill='none' stroke='#c9a84c' stroke-width='12'/><circle cx='658' cy='366' r='34' fill='none' stroke='#c9a84c' stroke-width='12'/><text x='450' y='135' text-anchor='middle' font-family='Arial, sans-serif' font-size='52' font-weight='800' fill='#c8ff3e'>AutoMart</text><text x='450' y='174' text-anchor='middle' font-family='Arial, sans-serif' font-size='20' fill='#9cad9b'>Vehicle image pending</text></svg>`);
  function listingImageKey(id){ return LISTING_MEDIA_PREFIX + String(id||Date.now()).replace(/[^a-zA-Z0-9_-]/g,'_'); }
  function getListingImage(l){
    if(!l) return LISTING_PLACEHOLDER;
    if(l.imageRef){
      const img = localStorage.getItem(l.imageRef);
      if(img) return img;
    }
    if(l.image && /^data:image|^https?:|^\//.test(l.image)) return l.image;
    return LISTING_PLACEHOLDER;
  }
  function migrateListingImages(list){
    let changed=false;
    list.forEach(l=>{
      if(l.image && String(l.image).startsWith('data:image')){
        const key = listingImageKey(l.id);
        try { localStorage.setItem(key, l.image); l.imageRef=key; l.image=''; changed=true; } catch(e) { console.warn('listing image migration skipped', e); }
      }
    });
    if(changed) write(LISTINGS_KEY,list);
    return list;
  }
  function compressListingFile(file){
    return new Promise((resolve,reject)=>{
      if(!file) return resolve('');
      if(!file.type.startsWith('image/')) return reject(new Error('Choose a valid vehicle image.'));
      const reader=new FileReader();
      reader.onerror=()=>reject(new Error('Image upload failed.'));
      reader.onload=()=>{
        const img=new Image();
        img.onerror=()=>reject(new Error('Image preview failed.'));
        img.onload=()=>{
          const maxW=1100,maxH=680,quality=.72;
          let w=img.width,h=img.height;
          const ratio=Math.min(maxW/w,maxH/h,1);
          w=Math.max(1,Math.round(w*ratio)); h=Math.max(1,Math.round(h*ratio));
          const canvas=document.createElement('canvas'); canvas.width=w; canvas.height=h;
          canvas.getContext('2d').drawImage(img,0,0,w,h);
          resolve(canvas.toDataURL('image/jpeg',quality));
        };
        img.src=reader.result;
      };
      reader.readAsDataURL(file);
    });
  }
  function saveListingImage(id,data){
    if(!data) return '';
    const key=listingImageKey(id);
    try { localStorage.setItem(key,data); return key; }
    catch(e){ toast('Image is too large. Please choose a smaller image.','error'); return ''; }
  }

  function getProfile(){return window.AutoMartProfileStore?.getCurrentUserProfile ? window.AutoMartProfileStore.getCurrentUserProfile() : Object.assign({},read(PROFILE_KEY,{}),read(USER_KEY,{}),read(SETTINGS_KEY,{}));}
  function updateProfile(p){
    if(window.AutoMartProfileStore?.updateCurrentUserProfile) return window.AutoMartProfileStore.updateCurrentUserProfile(p);
    [PROFILE_KEY,USER_KEY,BUYER_KEY,SETTINGS_KEY].forEach(k=>write(k,Object.assign(read(k,{}),p)));
    const shared=read(SHARED_KEY,{});
    shared.activeProfile=Object.assign(shared.activeProfile||{},p);
    shared.profile=Object.assign(shared.profile||{},p);
    shared.buyerProfile=Object.assign(shared.buyerProfile||{},p);
    shared.updatedAt=Date.now();
    write(SHARED_KEY,shared);
    return true;
  }
  function cleanText(el){return String(el?.textContent||'').replace(/\s+/g,' ').trim();}
  function byText(root, selector, text){
    return [...(root||document).querySelectorAll(selector)].find(el=>cleanText(el).toLowerCase().includes(text.toLowerCase()));
  }
  function fieldByLabel(labelText){
    const wanted=String(labelText).toLowerCase();
    const label=[...document.querySelectorAll('.form-label,label')].find(l=>cleanText(l).toLowerCase().includes(wanted));
    return label ? (label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea')) : null;
  }
  function panel(id){return document.getElementById(id);}
  function currentRole(){return (document.getElementById('globalProfileRole')?.textContent || '').toLowerCase();}
  function canList(){const r=currentRole(); return r.includes('seller') || r.includes('admin') || !r.includes('buyer');}

  /* SEARCH BUG FIX */
  function fixSearchInputs(){
    ['automartSearchQuery','autoMartSearchQuery','searchQuery','headerSearch','lastSearch','automartProfileSearch'].forEach(k=>{try{localStorage.removeItem(k);sessionStorage.removeItem(k);}catch{}});
    document.querySelectorAll('input[type="search"],input[placeholder*="Search"]').forEach(input=>{
      if(/trusted automart seller|fully inspected vehicles|thankyou/i.test(input.value) || input.placeholder.toLowerCase().includes('search')){
        input.value='';
      }
      if(input.placeholder.toLowerCase().includes('search')) input.placeholder='Search cars...';
      if(!input.dataset.automartSearchClean){
        input.dataset.automartSearchClean='1';
        input.addEventListener('input',()=>{ if(/trusted automart seller|fully inspected vehicles|thankyou/i.test(input.value)) input.value=''; });
      }
    });
  }

  /* BUDGET RANGE */
  function budgetInputs(){
    const min=fieldByLabel('Minimum Budget');
    const max=fieldByLabel('Maximum Budget');
    const range=document.querySelector('#panel-marketplace .range-wrap input[type="range"], .range-wrap input[type="range"]');
    const label=document.getElementById('budgetVal') || document.querySelector('.range-val');
    return {min,max,range,label};
  }
  function setBudgetUI(minVal,maxVal,saveNow){
    const {min,max,range,label}=budgetInputs();
    minVal=Math.max(0,Number(minVal)||0);
    maxVal=Math.max(0,Number(maxVal)||0);
    if(min && document.activeElement!==min) min.value=money(minVal);
    if(max && document.activeElement!==max) max.value=money(maxVal);
    if(range) range.value=String(maxVal || range.value || 15000000);
    if(label) label.textContent='LKR '+mLabel(minVal)+' — '+mLabel(maxVal);
    if(saveNow){
      const p=getProfile();
      const prefs=Object.assign({},p.marketplacePreferences||p.preferences||{},{minimumBudget:minVal,maximumBudget:maxVal,budgetRange:'LKR '+mLabel(minVal)+' — '+mLabel(maxVal)});
      updateProfile(Object.assign({},p,{marketplacePreferences:prefs,preferences:prefs,profileFields:Object.assign({},p.profileFields||{},{minimum_budget:String(minVal),maximum_budget:String(maxVal)})}));
    }
  }
  function validateBudget(showError){
    const {min,max}=budgetInputs();
    if(!min||!max) return true;
    const minVal=parseMoney(min.value), maxVal=parseMoney(max.value);
    if(minVal<=0 || maxVal<=0 || minVal>maxVal){
      if(showError) toast('Invalid budget range. Minimum cannot be higher than Maximum.', 'error');
      min.style.borderColor='rgba(255,79,110,.75)';
      max.style.borderColor='rgba(255,79,110,.75)';
      return false;
    }
    min.style.borderColor='rgba(53,255,154,.55)';
    max.style.borderColor='rgba(53,255,154,.55)';
    return true;
  }
  function wireBudget(){
    const {min,max,range}=budgetInputs();
    const p=getProfile(), prefs=p.marketplacePreferences||p.preferences||{};
    const minSaved=prefs.minimumBudget || p.profileFields?.minimum_budget || parseMoney(min?.value)||1000000;
    const maxSaved=prefs.maximumBudget || p.profileFields?.maximum_budget || parseMoney(max?.value)||15000000;
    setBudgetUI(minSaved,maxSaved,false);
    [min,max].filter(Boolean).forEach(input=>{
      if(input.dataset.automartBudget) return;
      input.dataset.automartBudget='1';
      input.addEventListener('input',()=>{
        input.value=money(parseMoney(input.value));
        if(validateBudget(false)) setBudgetUI(parseMoney(min.value), parseMoney(max.value), true);
      });
      input.addEventListener('blur',()=>validateBudget(true));
    });
    if(range && !range.dataset.automartBudget){
      range.dataset.automartBudget='1';
      range.addEventListener('input',()=>{
        const minVal=parseMoney(min?.value)||1000000;
        const maxVal=Number(range.value)||15000000;
        if(maxVal<minVal){toast('Maximum cannot be lower than minimum.', 'error'); range.value=String(minVal); return;}
        setBudgetUI(minVal,maxVal,true);
      });
    }
  }

  /* MARKETPLACE RESET */
  const defaultPrefs = {
    bodyTypes:['SUV','Sedan'],
    minimumBudget:1000000,
    maximumBudget:15000000,
    budgetRange:'LKR 1M — 15M',
    preferredFuelType:'All Types',
    transmission:'Any',
    preferredMake:'',
    maxMileage:'',
    preferredContactMethod:'WhatsApp first',
    inspectionCity:'Colombo',
    toggles:{
      'Instant WhatsApp enquiries':true,
      'Auto-boost new listings':false,
      'Trusted Buyer Badge':true,
      'Premium Auction Offers':false
    }
  };
  function applyDefaultPrefsToUI(){
    document.querySelectorAll('#panel-marketplace .pref-chip').forEach(chip=>{
      const label=chip.querySelector('.pref-chip-label')?.textContent?.trim();
      chip.classList.toggle('selected', defaultPrefs.bodyTypes.includes(label));
    });
    setBudgetUI(defaultPrefs.minimumBudget,defaultPrefs.maximumBudget,false);
    const setters=[
      ['Preferred Fuel Type',defaultPrefs.preferredFuelType],['Transmission',defaultPrefs.transmission],
      ['Preferred Make',defaultPrefs.preferredMake],['Max Mileage',defaultPrefs.maxMileage],
      ['Preferred Contact Method',defaultPrefs.preferredContactMethod],['Inspection City',defaultPrefs.inspectionCity]
    ];
    setters.forEach(([label,value])=>{const f=fieldByLabel(label); if(f) f.value=value;});
    document.querySelectorAll('#panel-marketplace .toggle').forEach(t=>{
      const row=t.closest('.toggle-row'); const name=row?.querySelector('.t-name')?.textContent?.trim();
      if(name && Object.prototype.hasOwnProperty.call(defaultPrefs.toggles,name)) t.classList.toggle('on',!!defaultPrefs.toggles[name]);
    });
  }
  function resetPrefs(){
    popup('Confirm Changes','Are you sure you want to reset these preferences to default?','Confirm',()=>{
      const p=getProfile();
      applyDefaultPrefsToUI();
      updateProfile(Object.assign({},p,{marketplacePreferences:defaultPrefs,preferences:defaultPrefs,profileFields:Object.assign({},p.profileFields||{},{minimum_budget:String(defaultPrefs.minimumBudget),maximum_budget:String(defaultPrefs.maximumBudget),preferred_fuel_type:defaultPrefs.preferredFuelType,transmission:defaultPrefs.transmission,preferred_make:defaultPrefs.preferredMake,max_mileage:defaultPrefs.maxMileage,inspection_city:defaultPrefs.inspectionCity})}));
      toast('Marketplace preferences reset successfully');
    },'↺');
  }

  /* LISTINGS */
  function defaultListings(){
    return [
      {id:'l1',model:'Toyota Aqua G 2019',location:'Colombo',mileage:'42,000 km',transmission:'Auto',fuel:'Hybrid',price:'LKR 5,800,000',views:847,enquiries:12,status:'active',image:'',boosted:false,createdAt:new Date(Date.now()-2*86400000).toISOString()},
      {id:'l2',model:'BMW 318i F30 2017',location:'Colombo',mileage:'72,000 km',transmission:'Auto',fuel:'Petrol',price:'LKR 12,500,000',views:1204,enquiries:28,status:'active',image:'',boosted:false,createdAt:new Date(Date.now()-5*86400000).toISOString()},
      {id:'l3',model:'Honda Vezel RS 2018',location:'Kandy',mileage:'55,000 km',transmission:'Auto',fuel:'Hybrid',price:'LKR 10,200,000',views:0,enquiries:0,status:'pending',image:'',boosted:false,createdAt:new Date(Date.now()-86400000).toISOString()}
    ];
  }
  function listings(){let list=read(LISTINGS_KEY,null); if(!Array.isArray(list)){list=defaultListings(); write(LISTINGS_KEY,list);} return migrateListingImages(list);}
  function saveListings(list){write(LISTINGS_KEY,list); const shared=read(SHARED_KEY,{}); shared.listings=list; shared.updatedAt=Date.now(); write(SHARED_KEY,shared); updateListingCount(list); renderListings(false);}
  function updateListingCount(list){
    const root=panel('panel-listings'); if(!root) return;
    const summary=root.querySelector('div[style*="font-size:13px"]');
    if(summary) summary.textContent=`${list.filter(x=>x.status==='active').length} active listings · ${list.filter(x=>x.sold).length||47} sold · ${list.filter(x=>x.status==='pending').length} pending`;
    document.querySelectorAll('.stat-value').forEach(el=>{if(cleanText(el)==='12') el.textContent=String(list.length);});
  }
  function listingCard(l){
    const statusClass=l.status==='active'?'ok':l.status==='pending'?'warn':'danger';
    const imgSrc=getListingImage(l);
    return `<div class="listing-card" data-listing-id="${esc(l.id)}"><div class="listing-thumb automart-real-listing-thumb" style="background-image:url('${imgSrc}');background-size:cover;background-position:center"><div class="listing-status"><span class="pill ${statusClass}"><span class="pill-dot"></span>${esc((l.status||'pending').replace(/^./,c=>c.toUpperCase()))}</span>${l.boosted?'<span class="pill ok" style="margin-left:4px">Boosted</span>':''}</div></div><div class="listing-info"><div class="listing-name">${esc(l.model||'Vehicle Listing')}</div><div class="listing-meta">${esc(l.location||'Sri Lanka')} · ${esc(l.mileage||'0 km')} · ${esc(l.transmission||'Auto')} · ${esc(l.fuel||'Petrol')}</div><div class="listing-price">${esc(l.price||'LKR 0')}</div><div style="font-size:10px;color:var(--t3);margin-top:3px">${Number(l.views||0).toLocaleString()} views · ${Number(l.enquiries||0)} enquiries · ${l.status==='pending'?'Awaiting admin approval':'Listed '+new Date(l.createdAt||Date.now()).toLocaleDateString()}</div><div class="listing-actions"><button class="btn btn-outline btn-sm" data-listing-action="edit">Edit</button><button class="btn btn-outline btn-sm" data-listing-action="boost">${l.boosted?'Boosted':'Boost'}</button><button class="btn btn-danger btn-sm" data-listing-action="remove">Remove</button></div></div></div>`;
  }
  function renderListings(showAll){
    const root=panel('panel-listings'); if(!root) return;
    const grid=root.querySelector('.listing-grid'); if(!grid) return;
    const list=listings();
    grid.innerHTML=(showAll?list:list.slice(0,6)).map(listingCard).join('');
    updateListingCount(list);
    const viewBtn=byText(root,'button','View All');
    if(viewBtn) viewBtn.textContent=showAll?'Show Less Listings ↑':'View All '+list.length+' Listings →';
  }
  function imageInputToData(input){
    const file=input.files&&input.files[0];
    return compressListingFile(file);
  }
  function openListingModal(existing){
    if(!canList()) return toast('Buyer accounts cannot add listings.', 'error');
    document.getElementById('automartListingModal')?.remove();
    const l=existing||{};
    const modal=document.createElement('div');
    modal.className='automart-plan-overlay';
    modal.id='automartListingModal';
    const imgSrc=getListingImage(l);
    modal.innerHTML=`<div class="automart-plan-card"><div class="automart-plan-head"><div><h3>${existing?'Edit Listing':'Add New Listing'}</h3><p>${existing?'Update listing details and save in real time.':'Use the Sell Car page to add a styled pending listing.'}</p></div><button type="button" class="automart-plan-close">✕</button></div>
      <div class="automart-payment-fields">
        <input id="lmModel" placeholder="Brand / Model" value="${esc(l.model||'')}">
        <input id="lmYear" placeholder="Year" inputmode="numeric" value="${esc(l.year||'')}">
        <input id="lmPrice" placeholder="Price in LKR" value="${esc(l.price||'')}">
        <input id="lmMileage" placeholder="Mileage" value="${esc(l.mileage||'')}">
        <input id="lmFuel" placeholder="Fuel Type" value="${esc(l.fuel||'Hybrid')}">
        <input id="lmTrans" placeholder="Transmission" value="${esc(l.transmission||'Auto')}">
        <input id="lmLocation" placeholder="Location" value="${esc(l.location||'')}">
        <input id="lmImage" type="file" accept="image/*">
      </div>
      <textarea id="lmDesc" placeholder="Description" style="width:100%;min-height:90px;margin-top:10px;border-radius:14px;background:#101a20;color:#fff;border:1px solid rgba(255,255,255,.12);padding:12px">${esc(l.description||'')}</textarea>
      <div id="lmPreview" style="height:190px;border:1px dashed rgba(200,255,62,.25);border-radius:16px;margin:12px 0;background:url('${imgSrc}') center/cover;display:grid;place-items:center;color:#9cad9b">${imgSrc===LISTING_PLACEHOLDER?'Clean AutoMart placeholder':''}</div>
      <div class="automart-plan-actions" style="justify-content:flex-end"><button class="automart-plan-btn" data-close>Cancel</button><button class="automart-plan-btn primary" data-save>${existing?'Save Listing':'Open Sell Car Page'}</button></div></div>`;
    document.body.appendChild(modal);
    modal.querySelector('.automart-plan-close').onclick=()=>modal.remove();
    modal.querySelector('[data-close]').onclick=()=>modal.remove();
    modal.onclick=e=>{if(e.target===modal) modal.remove();};
    modal.querySelector('#lmImage').onchange=async()=>{try{const data=await imageInputToData(modal.querySelector('#lmImage')); if(data){const p=modal.querySelector('#lmPreview'); p.textContent=''; p.style.background=`url('${data}') center/cover`; modal.dataset.image=data;}}catch(e){toast(e.message,'error');}};
    modal.querySelector('[data-save]').onclick=()=>{
      if(!existing){ location.href='/sell'; return; }
      popup('Confirm Changes','Do you want to save this listing?','Confirm',()=>{
        const model=modal.querySelector('#lmModel').value.trim();
        const price=modal.querySelector('#lmPrice').value.trim();
        if(!model||!price) return toast('Vehicle model and price are required.','error');
        const list=listings();
        const idx=list.findIndex(x=>x.id===existing.id);
        const item=Object.assign({},existing,{
          model, year:modal.querySelector('#lmYear').value.trim(),
          price, mileage:modal.querySelector('#lmMileage').value.trim(),
          fuel:modal.querySelector('#lmFuel').value.trim(),
          transmission:modal.querySelector('#lmTrans').value.trim(),
          location:modal.querySelector('#lmLocation').value.trim(),
          description:modal.querySelector('#lmDesc').value.trim(),
          imageRef: modal.dataset.image ? saveListingImage(existing.id, modal.dataset.image) : existing.imageRef,
          image: '',
          updatedAt:new Date().toISOString()
        });
        if(idx>=0) list[idx]=item;
        saveListings(list);
        modal.remove();
        toast('Listing saved successfully');
      },'🚗');
    };
  }
  function listingAction(action,id){
    const list=listings(), item=list.find(x=>x.id===id);
    if(!item) return toast('Listing not found.','error');
    if(action==='edit') return openListingModal(item);
    if(action==='boost') {
      if(item.boosted) return toast('This listing is already boosted.','info');
      return popup('Confirm Changes','Do you want to boost this listing?','Confirm',()=>{
        item.boosted=true; item.boostedAt=new Date().toISOString(); item.views=Number(item.views||0)+50;
        saveListings(list); toast('Listing boosted successfully');
      },'🚀');
    }
    if(action==='remove') return popup('Confirm Changes','Are you sure you want to remove this listing?','Confirm',()=>{
      if(item.imageRef) try{localStorage.removeItem(item.imageRef);}catch(e){}
      saveListings(list.filter(x=>x.id!==id)); toast('Listing removed successfully');
    },'🗑️');
  }

  /* REVIEWS */
  function seedReviewsFromDom(){
    const existing=read(REVIEWS_KEY,null); if(Array.isArray(existing)) return existing;
    const root=panel('panel-reviews'); 
    const cards=[...(root?root.querySelectorAll('.review-card,.review-item,[class*="review"]'):[])].filter(el=>/★|review|verified/i.test(el.textContent));
    let list=cards.slice(0,6).map((el,i)=>({
      id:'r'+i, html:el.outerHTML, rating:(el.textContent.match(/([1-5])\.?\d*\s*★/)?.[1] || (el.textContent.match(/★★★★★/)?5:4)),
      date:new Date(Date.now()-i*86400000).toISOString()
    }));
    if(!list.length) list=[
      {id:'r1',rating:5,date:new Date().toISOString(),html:'<div class="form-card review-card"><div class="form-card-body"><b>Excellent seller</b><p>Fast response and clean vehicle.</p><div class="stars">★★★★★</div></div></div>'},
      {id:'r2',rating:4,date:new Date(Date.now()-86400000*3).toISOString(),html:'<div class="form-card review-card"><div class="form-card-body"><b>Good communication</b><p>Helpful and trusted.</p><div class="stars">★★★★☆</div></div></div>'}
    ];
    write(REVIEWS_KEY,list);
    return list;
  }
  function reviewContainer(){
    const root=panel('panel-reviews'); if(!root) return null;
    let c=root.querySelector('[data-review-list]');
    if(!c){
      const selects=root.querySelector('select')?.closest('.form-card') || root.querySelector('.form-card:last-child') || root;
      c=document.createElement('div'); c.dataset.reviewList='1'; c.style.display='grid'; c.style.gap='12px'; c.style.marginTop='14px';
      root.appendChild(c);
    }
    return c;
  }
  function renderReviews(){
    const c=reviewContainer(); if(!c) return;
    let list=seedReviewsFromDom().slice();
    const sort=localStorage.getItem(REVIEW_SORT_KEY)||'newest';
    if(sort==='highest') list.sort((a,b)=>Number(b.rating)-Number(a.rating));
    else if(sort==='lowest') list.sort((a,b)=>Number(a.rating)-Number(b.rating));
    else list.sort((a,b)=>new Date(b.date)-new Date(a.date));
    c.innerHTML=list.map(r=>r.html).join('');
    const root=panel('panel-reviews');
    root?.querySelectorAll('select').forEach(s=>{
      const text=[...s.options].map(o=>o.textContent.toLowerCase()).join(' ');
      if(/newest|rating|highest|lowest/.test(text)){
        [...s.options].forEach(o=>{o.selected=(sort==='newest'&&/newest/i.test(o.textContent))||(sort==='highest'&&/highest/i.test(o.textContent))||(sort==='lowest'&&/lowest/i.test(o.textContent));});
      }
    });
  }
  function wireReviewSort(){
    const root=panel('panel-reviews'); if(!root) return;
    renderReviews();
    root.querySelectorAll('select').forEach(s=>{
      if(s.dataset.automartReviewSort) return;
      const text=[...s.options].map(o=>o.textContent.toLowerCase()).join(' ');
      if(!/newest|rating|highest|lowest/.test(text)) return;
      s.dataset.automartReviewSort='1';
      s.addEventListener('change',()=>{
        const t=s.options[s.selectedIndex]?.textContent.toLowerCase()||'';
        const sort=t.includes('highest')?'highest':t.includes('lowest')?'lowest':'newest';
        localStorage.setItem(REVIEW_SORT_KEY,sort);
        renderReviews();
      });
    });
  }

  /* PLAN */
  function showPremiumLoading(cb){
    document.getElementById('automartPremiumLoader')?.remove();
    const el=document.createElement('div');
    el.id='automartPremiumLoader';
    el.className='automart-plan-overlay';
    el.innerHTML='<div class="automart-plan-card" style="max-width:420px;text-align:center"><div style="font-size:44px;margin:8px">⭐</div><h3>Loading Dealer Pro...</h3><p style="color:#9cad9b">Preparing secure subscription manager</p><div class="automart-loading-line"></div></div>';
    document.body.appendChild(el);
    setTimeout(()=>{el.remove(); cb&&cb();},650);
  }
  function subscription(){return Object.assign({plan:'Dealer Pro',status:'active',renewalDate:'15 June 2026',price:'LKR 3,990 / month',paymentMethod:'',paymentRef:''},read(SUB_KEY,{}));}
  function saveSub(s){write(SUB_KEY,Object.assign(s,{updatedAt:new Date().toISOString()})); toast('Subscription updated successfully');}
  function openPlan(){
    if(typeof window.openPlanModal==='function') return showPremiumLoading(()=>window.openPlanModal());
    showPremiumLoading(()=>{
      document.getElementById('automartPlanModal')?.remove();
      const sub=subscription();
      let selected=sub.plan||'Dealer Pro', method='Card';
      const modal=document.createElement('div');
      modal.id='automartPlanModal'; modal.className='automart-plan-overlay';
      modal.innerHTML=`<div class="automart-plan-card"><div class="automart-plan-head"><div><h3>Dealer Pro Plan Manager</h3><p>Current plan: ${esc(sub.plan)} · ${esc(sub.price)}</p></div><button class="automart-plan-close">✕</button></div><div class="automart-plan-current"><div><strong>${esc(sub.status==='cancelled'?'No active plan':sub.plan)}</strong><span>${esc(sub.status==='cancelled'?'Subscription cancelled':'Renews '+sub.renewalDate+' · '+sub.price)}</span></div><div class="automart-plan-actions"><button class="automart-plan-btn" data-select="Dealer Pro">Change Plan</button><button class="automart-plan-btn primary" data-select="Premium">Upgrade Plan</button><button class="automart-plan-btn danger" data-cancel>Cancel Subscription</button></div></div><div class="automart-payment-box"><b>Payment Methods</b><div class="automart-payment-methods"><button class="automart-plan-btn active" data-method="Card">Card Payment</button><button class="automart-plan-btn" data-method="PayPal">PayPal</button></div><div class="automart-payment-fields" id="cardFields"><input id="cardName" placeholder="Cardholder Name"><input id="cardNumber" placeholder="Card Number" maxlength="19"><input id="cardExpiry" placeholder="MM/YY" maxlength="5"><input id="cardCvv" placeholder="CVV" maxlength="4"></div><div class="automart-payment-fields" id="paypalFields" style="display:none"><input id="paypalEmail" placeholder="PayPal Email"></div><div class="automart-plan-actions" style="justify-content:flex-end;margin-top:14px"><button class="automart-plan-btn primary" data-subscribe>Subscribe / Upgrade</button></div></div></div>`;
      document.body.appendChild(modal);
      modal.querySelector('.automart-plan-close').onclick=()=>modal.remove();
      modal.onclick=e=>{if(e.target===modal) modal.remove();};
      modal.querySelectorAll('[data-method]').forEach(b=>b.onclick=()=>{method=b.dataset.method; modal.querySelectorAll('[data-method]').forEach(x=>x.classList.toggle('active',x===b)); modal.querySelector('#cardFields').style.display=method==='Card'?'grid':'none'; modal.querySelector('#paypalFields').style.display=method==='PayPal'?'grid':'none';});
      modal.querySelectorAll('[data-select]').forEach(b=>b.onclick=()=>{selected=b.dataset.select; toast(selected+' selected. Enter payment details.','info');});
      modal.querySelector('[data-cancel]').onclick=()=>popup('Confirm Changes','Are you sure you want to cancel your Dealer Pro subscription?','Confirm',()=>{saveSub(Object.assign(subscription(),{status:'cancelled',plan:'No Active Plan'})); modal.remove();},'🗑️');
      modal.querySelector('[data-subscribe]').onclick=()=>{
        let ok=false, ref='';
        if(method==='Card'){const n=modal.querySelector('#cardNumber').value.replace(/\s/g,''), name=modal.querySelector('#cardName').value.trim(), ex=modal.querySelector('#cardExpiry').value.trim(), cvv=modal.querySelector('#cardCvv').value.trim(); ok=!!name&&/^\d{12,19}$/.test(n)&&/^(0[1-9]|1[0-2])\/\d{2}$/.test(ex)&&/^\d{3,4}$/.test(cvv); ref='Card ending '+n.slice(-4);}
        else {const em=modal.querySelector('#paypalEmail').value.trim().toLowerCase(); ok=/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(em); ref=em;}
        if(!ok) return toast('Enter valid '+method+' payment details.','error');
        popup('Confirm Changes','Do you want to subscribe/upgrade to '+selected+'?','Confirm',()=>{saveSub({plan:selected,status:'active',renewalDate:'15 June 2026',price:selected==='Premium'?'LKR 6,990 / month':'LKR 3,990 / month',paymentMethod:method,paymentRef:ref}); modal.remove();},'💳');
      };
    });
  }


  function appendLocalListingsToInventory(){
    if(!location.pathname.includes('/inventory')) return;
    const grid=document.querySelector('.grid.cards');
    if(!grid || grid.dataset.automartLocalListingsAppended) return;
    grid.dataset.automartLocalListingsAppended='1';
    const list=listings().filter(l=>l.status!=='removed');
    if(!list.length) return;
    const html=list.map(l=>{
      const img=getListingImage(l);
      return `<article class="card vehicle-card automart-local-inventory-card" data-listing-id="${esc(l.id)}">
        <div class="card-image" style="height:190px;border-radius:18px;background:url('${img}') center/cover;margin-bottom:14px"></div>
        <p class="eyebrow">${esc((l.status||'pending').toUpperCase())}</p>
        <h3>${esc(l.model||'Vehicle Listing')}</h3>
        <p class="price">${esc(l.price||'LKR 0')}</p>
        <p>${esc(l.year||'')} · ${esc(l.mileage||'0 km')} · ${esc(l.fuel||'Fuel')} · ${esc(l.transmission||'Transmission')}</p>
        <div class="actions"><a class="btn btn-primary" href="/profile">View Seller Listing</a></div>
      </article>`;
    }).join('');
    grid.insertAdjacentHTML('afterbegin', html);
  }

  /* SECURITY */
  function openPasswordModal(){
    document.getElementById('automartPasswordModal')?.remove();
    const m=document.createElement('div'); m.className='automart-plan-overlay'; m.id='automartPasswordModal';
    m.innerHTML='<div class="automart-plan-card" style="max-width:540px"><div class="automart-plan-head"><div><h3>Change Password</h3><p>Use uppercase, lowercase and numbers for a safer password.</p></div><button class="automart-plan-close">✕</button></div><div class="automart-payment-fields" style="grid-template-columns:1fr"><div style="display:flex;gap:8px"><input id="curPass" type="password" placeholder="Current Password" style="flex:1"><button class="automart-plan-btn" data-toggle-pass type="button">Show</button></div><div style="display:flex;gap:8px"><input id="newPass" type="password" placeholder="New Password" style="flex:1"><button class="automart-plan-btn" data-toggle-pass type="button">Show</button></div><div style="display:flex;gap:8px"><input id="conPass" type="password" placeholder="Confirm New Password" style="flex:1"><button class="automart-plan-btn" data-toggle-pass type="button">Show</button></div></div><div style="color:#9cad9b;font-size:12px;margin-top:10px">Password must be at least 8 characters and include uppercase, lowercase and a number.</div><div class="automart-plan-actions" style="justify-content:flex-end;margin-top:14px"><button class="automart-plan-btn" data-close>Cancel</button><button class="automart-plan-btn primary" data-save>Save Password</button></div></div>';
    document.body.appendChild(m);
    m.querySelector('.automart-plan-close').onclick=()=>m.remove();
    m.querySelector('[data-close]').onclick=()=>m.remove();
    m.querySelectorAll('[data-toggle-pass]').forEach(btn=>btn.onclick=()=>{const input=btn.parentElement.querySelector('input'); input.type=input.type==='password'?'text':'password'; btn.textContent=input.type==='password'?'Show':'Hide';});
    m.querySelector('[data-save]').onclick=()=>{
      const stored=localStorage.getItem(PASSWORD_KEY)||'';
      const cur=m.querySelector('#curPass').value, np=m.querySelector('#newPass').value, cp=m.querySelector('#conPass').value;
      if(stored && cur!==stored) return toast('Current password is wrong.','error');
      if(!np || np.length<8) return toast('New password must be at least 8 characters.','error');
      if(!/[A-Z]/.test(np) || !/[a-z]/.test(np) || !/\d/.test(np)) return toast('New password must include uppercase, lowercase and a number.','error');
      if(np!==cp) return toast('New passwords do not match.','error');
      popup('Confirm Changes','Do you want to change your password?','Confirm',()=>{localStorage.setItem(PASSWORD_KEY,np); m.remove(); toast('Password changed successfully');},'🔐');
    };
  }
  function toggle2FA(btn){
    const p=getProfile(); const enabled=!(p.securityPreferences?.twoFactorEnabled);
    popup('Confirm Changes',(enabled?'Enable':'Disable')+' Two-Factor Authentication?','Confirm',()=>{
      const securityPreferences=Object.assign({},p.securityPreferences||{},{twoFactorEnabled:enabled});
      updateProfile(Object.assign({},p,{securityPreferences}));
      if(btn) btn.textContent=enabled?'Disable 2FA':'Enable 2FA';
      const item=btn?.closest('.sec-item');
      const pill=item?.querySelector('.pill');
      if(pill){pill.classList.toggle('ok',enabled); pill.classList.toggle('warn',!enabled); pill.innerHTML='<span class="pill-dot"></span>'+(enabled?'Enabled':'Disabled');}
      const sub=item?.querySelector('.sec-sub'); if(sub) sub.textContent=enabled?'Enabled — your account is protected':'Not enabled — strongly recommended for seller accounts';
      toast(enabled?'Two-Factor Authentication enabled':'Two-Factor Authentication disabled');
    },'🛡️');
  }
  function signOutOtherDevices(){
    popup('Confirm Changes','Are you sure you want to sign out all other devices?','Confirm',()=>{
      const current={id:'current',device:navigator.userAgent.slice(0,40),current:true,updatedAt:new Date().toISOString()};
      write(SESSIONS_KEY,[current]);
      document.querySelectorAll('#panel-security .sec-item').forEach(item=>{if(/Safari|Chrome|iPhone|Android|Windows|Mac/i.test(item.textContent)&&!item.textContent.includes('Current')) item.remove();});
      toast('All other devices signed out');
    },'🚪');
  }

  /* DANGER */
  function allUserData(){
    return {profile:getProfile(), listings:listings(), reviews:read(REVIEWS_KEY,[]), requests:read('automartRequestsV1',[]), preferences:getProfile().marketplacePreferences||{}, notifications:read(NOTIF_KEY,[]), messages:read(MSG_KEY,[]), subscription:read(SUB_KEY,{})};
  }
  function downloadMyData(){
    popup('Confirm Changes','Do you want to download your AutoMart data?','Confirm',()=>{
      const blob=new Blob([JSON.stringify(allUserData(),null,2)],{type:'application/json'});
      const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='automart-my-data.json'; document.body.appendChild(a); a.click(); setTimeout(()=>{URL.revokeObjectURL(a.href); a.remove();},0); toast('Data export downloaded');
    },'📋');
  }
  function deactivateAccount(){
    popup('Confirm Changes','Are you sure you want to deactivate your account?','Confirm',()=>{
      const p=getProfile(); updateProfile(Object.assign({},p,{accountStatus:'deactivated',deactivatedAt:new Date().toISOString()}));
      ['automart_session','automartAuth','authToken','sessionToken'].forEach(k=>{localStorage.removeItem(k);sessionStorage.removeItem(k);});
      toast('Account deactivated successfully'); setTimeout(()=>location.href='/login',700);
    },'⏸');
  }
  function deleteAccount(){
    popup('Confirm Changes','Are you sure you want to delete your account? This action cannot be undone.','Confirm',()=>{
      [PROFILE_KEY,USER_KEY,BUYER_KEY,SETTINGS_KEY,SHARED_KEY,LISTINGS_KEY,REVIEWS_KEY,SUB_KEY,NOTIF_KEY,MSG_KEY,'automartRequestsV1'].forEach(k=>{localStorage.removeItem(k);sessionStorage.removeItem(k);});
      toast('Account deleted successfully'); setTimeout(()=>location.href='/signup',700);
    },'🗑️');
  }


  function wireSellCarForm(){
    const form=document.querySelector('form[action="/sell"]') || document.querySelector("form[action='/sell']");
    if(!form || form.dataset.automartSellFixed) return;
    form.dataset.automartSellFixed='1';
    const file=form.querySelector('#sellImageFile');
    const preview=document.getElementById('sellImagePreview');
    if(file && !file.dataset.automartPreviewFixed){
      file.dataset.automartPreviewFixed='1';
      file.addEventListener('change',async()=>{
        try{
          const data=await compressListingFile(file.files&&file.files[0]);
          if(data && preview){ preview.style.display='block'; preview.style.backgroundImage=`url('${data}')`; preview.textContent=''; form.dataset.image=data; }
        }catch(e){toast(e.message,'error'); file.value='';}
      }, true);
    }
    form.addEventListener('submit',e=>{
      e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
      if(!canList()) return toast('Buyer accounts cannot submit vehicle listings.','error');
      const title=form.querySelector('[name="title"]')?.value.trim()||'';
      const price=form.querySelector('[name="price"]')?.value.trim()||'';
      if(!title || !price) return toast('Vehicle title and price are required.','error');
      popup('Confirm Changes','Do you want to submit this listing as Pending?','Confirm',()=>{
        const id='listing_'+Date.now();
        const imageRef=form.dataset.image ? saveListingImage(id, form.dataset.image) : '';
        const item={
          id, model:title, category:form.querySelector('[name="category"]')?.value||'',
          price: price.toUpperCase().includes('LKR') ? price : 'LKR '+price,
          year:form.querySelector('[name="year"]')?.value.trim()||'',
          mileage:form.querySelector('[name="mileage"]')?.value.trim()||'',
          fuel:form.querySelector('[name="fuel"]')?.value.trim()||'',
          transmission:form.querySelector('[name="transmission"]')?.value.trim()||'',
          description:form.querySelector('[name="description"]')?.value.trim()||'',
          location:(getProfile().city||getProfile().district||'Sri Lanka'),
          imageRef, image:'', status:'pending', boosted:false, views:0, enquiries:0, createdAt:new Date().toISOString()
        };
        const list=listings(); list.unshift(item); saveListings(list);
        const notifications=read(NOTIF_KEY,[]); notifications.unshift({id:'listing_'+Date.now(),title:'Listing submitted',body:'Your vehicle listing is pending admin approval.',time:'Now',read:false}); write(NOTIF_KEY,notifications);
        toast('Listing saved as Pending');
        setTimeout(()=>{location.href='/profile';},650);
      },'🚗');
    }, true);
  }

  function wire(){
    fixSearchInputs();
    wireBudget();
    appendLocalListingsToInventory();
    wireSellCarForm();
    renderListings(false);
    wireReviewSort();

    document.addEventListener('click',e=>{
      const btn=e.target.closest('button,a');
      if(!btn) return;
      const text=cleanText(btn).toLowerCase();
      if(text.includes('reset to default')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();resetPrefs();return;}
      if(text.includes('new listing')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation(); if(!canList()) return toast('Buyer accounts cannot add listings.','error'); location.href='/sell'; return;}
      if(text.includes('view all') && text.includes('listings')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();const showing=btn.dataset.showAll==='1';btn.dataset.showAll=showing?'0':'1';renderListings(!showing);return;}
      const action=btn.dataset.listingAction; if(action){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();listingAction(action,btn.closest('[data-listing-id]')?.dataset.listingId);return;}
      if(text.includes('manage plan') || (text.includes('dealer pro')&&text.includes('plan'))){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();openPlan();return;}
      if(text.includes('change password')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();openPasswordModal();return;}
      if(text.includes('enable 2fa')||text.includes('disable 2fa')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();toggle2FA(btn);return;}
      if(text.includes('sign out all other devices')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();signOutOtherDevices();return;}
      if(text.includes('download my data')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();downloadMyData();return;}
      if(text.includes('deactivate account')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();deactivateAccount();return;}
      if(text.includes('delete account')){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();deleteAccount();return;}
    },true);

    setInterval(fixSearchInputs,1000);
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',wire); else wire();
})();
