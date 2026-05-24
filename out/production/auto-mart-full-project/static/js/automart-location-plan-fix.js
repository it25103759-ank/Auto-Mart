
/* AutoMart location realtime save + Dealer Pro plan manager */
(function(){
  'use strict';

  const PROFILE_FIELDS = ['automartCurrentProfileV2','automartUserProfileRealV4','automartBuyerProfileRealV1','automartSettingsProfileV1'];
  const SHARED_KEY = 'automartSharedMarketplaceDataV2';
  const SUB_KEY = 'automartDealerProSubscriptionV2';

  const districtProvince = {
    'Colombo':'Western Province','Gampaha':'Western Province','Kalutara':'Western Province',
    'Kandy':'Central Province','Matale':'Central Province','Nuwara Eliya':'Central Province',
    'Galle':'Southern Province','Matara':'Southern Province','Hambantota':'Southern Province',
    'Jaffna':'Northern Province','Kilinochchi':'Northern Province','Mannar':'Northern Province','Vavuniya':'Northern Province','Mullaitivu':'Northern Province',
    'Batticaloa':'Eastern Province','Ampara':'Eastern Province','Trincomalee':'Eastern Province',
    'Kurunegala':'North Western Province','Puttalam':'North Western Province',
    'Anuradhapura':'North Central Province','Polonnaruwa':'North Central Province',
    'Badulla':'Uva Province','Monaragala':'Uva Province',
    'Ratnapura':'Sabaragamuwa Province','Kegalle':'Sabaragamuwa Province'
  };
  const provinces = [...new Set(Object.values(districtProvince))];
  const districts = Object.keys(districtProvince);

  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function toast(msg,type){ if(window.AutoMartToast) window.AutoMartToast(msg,type||'success'); else console.log(msg); }
  function popup(title,msg,ok,cb,icon){ if(window.AutoMartConfirm) return window.AutoMartConfirm(title,msg,ok,cb,icon||'✓'); if(cb) cb(); }

  function cleanLabel(s){ return String(s||'').toLowerCase().replace(/\*/g,'').replace(/\(.*?\)/g,'').replace(/\s+/g,' ').trim(); }
  function slug(s){ return String(s||'').toLowerCase().replace(/\*/g,'').replace(/\(.*?\)/g,'').replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,''); }
  function fieldByLabel(labelText){
    const wanted = cleanLabel(labelText);
    const labels = [...document.querySelectorAll('.form-label,label')];
    const label = labels.find(l => cleanLabel(l.textContent).includes(wanted));
    return label ? (label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea')) : null;
  }
  function val(label, fallback=''){
    const f = fieldByLabel(label);
    return f ? String(f.value||'').trim() : fallback;
  }
  function setVal(label, value){
    const f = fieldByLabel(label);
    if(!f || value === undefined || value === null) return;
    if(f.tagName === 'SELECT') {
      let found=false;
      [...f.options].forEach(o=>{const same=o.value===value || o.textContent.trim()===value; if(same) found=true; o.selected=same;});
      if(!found && value){
        const opt=document.createElement('option'); opt.value=value; opt.textContent=value; opt.selected=true; f.appendChild(opt);
      }
    } else f.value = String(value);
  }
  function ensureSelect(label, items){
    const f = fieldByLabel(label);
    if(!f || f.tagName !== 'SELECT') return;
    const current = f.value;
    f.innerHTML = items.map(x=>`<option value="${esc(x)}">${esc(x)}</option>`).join('');
    if(current && items.includes(current)) f.value = current;
  }
  function getProfile(){
    if(window.AutoMartProfileStore?.getCurrentUserProfile) return window.AutoMartProfileStore.getCurrentUserProfile();
    return Object.assign({}, read('automartCurrentProfileV2',{}), read('automartUserProfileRealV4',{}), read('automartSettingsProfileV1',{}));
  }
  function updateProfile(p){
    if(window.AutoMartProfileStore?.updateCurrentUserProfile) return window.AutoMartProfileStore.updateCurrentUserProfile(p);
    PROFILE_FIELDS.forEach(k=>write(k,Object.assign(read(k,{}),p)));
    const shared=read(SHARED_KEY,{});
    shared.activeProfile=Object.assign(shared.activeProfile||{},p);
    shared.profile=Object.assign(shared.profile||{},p);
    shared.buyerProfile=Object.assign(shared.buyerProfile||{},p);
    shared.updatedAt=Date.now();
    write(SHARED_KEY,shared);
    return true;
  }

  function applyLocationFields(){
    ensureSelect('Province', provinces);
    ensureSelect('District', districts);
    const p = getProfile();
    const fields = p.profileFields || {};
    const district = p.district || fields.district || val('District','');
    const province = p.province || fields.province || (districtProvince[district] || '');
    setVal('Province', province);
    setVal('District', district);
    setVal('City', p.city || fields.city || '');
    setVal('Full Address', p.fullAddress || p.address || fields.full_address || '');
    setVal('Inspection City', (p.marketplacePreferences||{}).inspectionCity || fields.inspection_city || '');
    updateHeroLocation(p);
  }

  function updateHeroLocation(p){
    p = p || getProfile();
    const handle = document.getElementById('profileHeroHandle') || document.getElementById('buyerHeroHandle');
    if(handle){
      const name = (p.displayName || p.username || p.fullName || 'automart_user').replace(/\s+/g,'_').toLowerCase();
      const location = [p.city, p.district, p.province].filter(Boolean).slice(0,2).join(', ') || 'Sri Lanka';
      handle.textContent = '@' + name.replace(/^@/,'') + ' · Member since January 2021 · ' + location;
    }
    document.querySelectorAll('[data-profile-location],.profile-location').forEach(el=>{
      el.textContent = [p.city,p.district,p.province].filter(Boolean).join(', ') || 'Sri Lanka';
    });
  }

  function collectLocation(){
    const district = val('District');
    const province = districtProvince[district] || val('Province');
    const city = val('City');
    const fullAddress = val('Full Address');
    const inspectionCity = val('Inspection City');
    return { province, district, city, fullAddress, address: fullAddress, inspectionCity };
  }

  function saveLocation(){
    const loc = collectLocation();
    if(!loc.province || !loc.district || !loc.city) return toast('Please select Province, District, and City before saving.', 'error');
    popup('Confirm Changes','Do you want to save these profile changes?','Confirm',()=>{
      const current = getProfile();
      const profileFields = Object.assign({}, current.profileFields||{}, {
        province: loc.province,
        district: loc.district,
        city: loc.city,
        full_address: loc.fullAddress,
        inspection_city: loc.inspectionCity
      });
      const marketplacePreferences = Object.assign({}, current.marketplacePreferences || current.preferences || {}, {
        inspectionCity: loc.inspectionCity || current.marketplacePreferences?.inspectionCity || ''
      });
      const next = Object.assign({}, current, loc, {profileFields, marketplacePreferences, preferences:marketplacePreferences, updatedAt:new Date().toISOString()});
      const ok = updateProfile(next);
      if(ok){
        applyLocationFields();
        if(window.AutoMartProfileStore?.applyProfileToPage) window.AutoMartProfileStore.applyProfileToPage();
        window.dispatchEvent(new StorageEvent('storage',{key:'automartCurrentProfileV2',newValue:JSON.stringify(next)}));
        toast('Location saved successfully');
      } else toast('Location save failed.', 'error');
    }, '💾');
  }

  function wireLocation(){
    applyLocationFields();
    const district = fieldByLabel('District');
    const province = fieldByLabel('Province');
    const city = fieldByLabel('City');
    if(district && !district.dataset.automartDistrictSync){
      district.dataset.automartDistrictSync='1';
      district.addEventListener('change',()=>{
        const prov = districtProvince[district.value];
        if(prov) setVal('Province', prov);
      });
    }
    [province,district,city,fieldByLabel('Full Address'),fieldByLabel('Inspection City')].filter(Boolean).forEach(f=>{
      if(!f.dataset.automartLocationControlled){
        f.dataset.automartLocationControlled='1';
        f.addEventListener('input',()=>updateHeroLocation(Object.assign(getProfile(), collectLocation())));
        f.addEventListener('change',()=>updateHeroLocation(Object.assign(getProfile(), collectLocation())));
      }
    });
  }

  function subscription(){
    return Object.assign({
      plan:'Dealer Pro',
      status:'active',
      renewalDate:'15 June 2026',
      price:'LKR 3,990 / month',
      paymentMethod:'',
      paymentRef:'',
      updatedAt:''
    }, read(SUB_KEY,{}));
  }
  function saveSubscription(s){ write(SUB_KEY, Object.assign(s,{updatedAt:new Date().toISOString()})); renderPlanBadges(); }
  function renderPlanBadges(){
    const s = subscription();
    document.querySelectorAll('.dealer-pro-status,[data-subscription-status]').forEach(el=>el.textContent=s.status==='cancelled'?'Cancelled':s.plan);
    document.querySelectorAll('.dealer-pro-renewal,[data-subscription-renewal]').forEach(el=>el.textContent=s.status==='cancelled'?'Subscription cancelled':'Renews '+s.renewalDate+' · '+s.price);
  }

  function ensureModalStyles(){
    if(document.getElementById('automart-plan-modal-style')) return;
    const style=document.createElement('style');
    style.id='automart-plan-modal-style';
    style.textContent=`
      .automart-plan-overlay{position:fixed;inset:0;z-index:999998;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(0,0,0,.66);backdrop-filter:blur(14px)}
      .automart-plan-card{width:min(100%,760px);max-height:92vh;overflow:auto;background:linear-gradient(145deg,rgba(12,20,22,.98),rgba(4,9,10,.98));border:1px solid rgba(200,255,62,.28);border-radius:24px;padding:24px;box-shadow:0 34px 100px rgba(0,0,0,.7),0 0 36px rgba(200,255,62,.12);color:#eef6e9}
      .automart-plan-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:18px}.automart-plan-head h3{margin:0;font-size:26px}.automart-plan-head p{margin:5px 0 0;color:#9cac9b;font-size:13px}
      .automart-plan-close{width:36px;height:36px;border-radius:10px;border:1px solid rgba(255,255,255,.13);background:rgba(255,255,255,.04);color:#fff;cursor:pointer}
      .automart-plan-current{display:grid;grid-template-columns:1fr auto;gap:12px;align-items:center;border:1px solid rgba(200,255,62,.18);background:rgba(200,255,62,.06);border-radius:18px;padding:16px;margin-bottom:16px}
      .automart-plan-current strong{font-size:18px;color:#c8ff3e}.automart-plan-current span{display:block;color:#9cac9b;font-size:12px;margin-top:4px}
      .automart-plan-actions{display:flex;gap:8px;flex-wrap:wrap}.automart-plan-btn{border:1px solid rgba(200,255,62,.25);background:rgba(9,16,18,.76);color:#eaf6df;border-radius:12px;padding:10px 14px;font-weight:800;cursor:pointer}.automart-plan-btn.primary{background:#c8ff3e;color:#07100b;border-color:#c8ff3e}.automart-plan-btn.danger{border-color:rgba(255,79,110,.35);color:#ff9caf}
      .automart-plan-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:14px 0}.automart-plan-option{border:1px solid rgba(255,255,255,.12);border-radius:16px;padding:14px;background:rgba(255,255,255,.035);cursor:pointer}.automart-plan-option.selected{border-color:#c8ff3e;box-shadow:0 0 22px rgba(200,255,62,.14)}.automart-plan-option b{display:block;color:#fff;margin-bottom:4px}.automart-plan-option small{color:#9cac9b}
      .automart-payment-box{border:1px solid rgba(255,255,255,.1);background:rgba(255,255,255,.025);border-radius:18px;padding:16px;margin-top:14px}.automart-payment-methods{display:flex;gap:8px;margin:10px 0}.automart-payment-methods button.active{background:#c8ff3e;color:#07100b}.automart-payment-fields{display:grid;grid-template-columns:1fr 1fr;gap:10px}.automart-payment-fields input{width:100%;padding:12px;border-radius:12px;background:#101a20;color:#fff;border:1px solid rgba(255,255,255,.12);outline:0}.automart-payment-fields input:focus{border-color:#c8ff3e}
      .automart-loading-line{height:4px;border-radius:999px;background:linear-gradient(90deg,transparent,#c8ff3e,transparent);animation:planLoad .8s linear infinite;margin:12px 0}@keyframes planLoad{from{background-position:-200px 0}to{background-position:200px 0}}
      @media(max-width:720px){.automart-plan-grid,.automart-payment-fields{grid-template-columns:1fr}.automart-plan-current{grid-template-columns:1fr}}
    `;
    document.head.appendChild(style);
  }

  function openPlanModal(){
    ensureModalStyles();
    document.getElementById('automartPlanModal')?.remove();
    const sub=subscription();
    let selectedPlan=sub.plan || 'Dealer Pro';
    let method=sub.paymentMethod || 'Card';
    const plans=[
      ['Basic','LKR 0 / month','Starter browsing and saved cars'],
      ['Dealer Pro','LKR 3,990 / month','Seller tools, boosts, analytics'],
      ['Premium','LKR 6,990 / month','Priority placement and pro support']
    ];
    const overlay=document.createElement('div');
    overlay.className='automart-plan-overlay';
    overlay.id='automartPlanModal';
    overlay.innerHTML=`
      <div class="automart-plan-card">
        <div class="automart-plan-head">
          <div><h3>Dealer Pro Plan Manager</h3><p>Manage your AutoMart subscription, plan and payment methods.</p></div>
          <button class="automart-plan-close" type="button">✕</button>
        </div>
        <div class="automart-plan-current">
          <div><strong data-plan-name>${esc(sub.status==='cancelled'?'No active plan':sub.plan)}</strong><span data-plan-renewal>${esc(sub.status==='cancelled'?'Subscription cancelled':'Renews '+sub.renewalDate+' · '+sub.price)}</span></div>
          <div class="automart-plan-actions">
            <button class="automart-plan-btn" data-plan-action="change">Change Plan</button>
            <button class="automart-plan-btn primary" data-plan-action="upgrade">Upgrade Plan</button>
            <button class="automart-plan-btn danger" data-plan-action="cancel">Cancel Subscription</button>
          </div>
        </div>
        <div class="automart-plan-grid">${plans.map(p=>`<div class="automart-plan-option ${p[0]===selectedPlan?'selected':''}" data-plan="${esc(p[0])}" data-price="${esc(p[1])}"><b>${esc(p[0])}</b><small>${esc(p[1])}</small><br><small>${esc(p[2])}</small></div>`).join('')}</div>
        <div id="planLoading" style="display:none" class="automart-loading-line"></div>
        <div class="automart-payment-box" id="paymentBox">
          <b>Payment Methods</b>
          <div class="automart-payment-methods">
            <button type="button" class="automart-plan-btn ${method==='Card'?'active':''}" data-method="Card">Card Payment</button>
            <button type="button" class="automart-plan-btn ${method==='PayPal'?'active':''}" data-method="PayPal">PayPal</button>
          </div>
          <div class="automart-payment-fields" id="cardFields">
            <input id="cardName" placeholder="Cardholder Name" value="">
            <input id="cardNumber" placeholder="Card Number" inputmode="numeric" maxlength="19">
            <input id="cardExpiry" placeholder="MM/YY" maxlength="5">
            <input id="cardCvv" placeholder="CVV" inputmode="numeric" maxlength="4">
          </div>
          <div class="automart-payment-fields" id="paypalFields" style="display:none">
            <input id="paypalEmail" placeholder="PayPal Email">
          </div>
          <div class="automart-plan-actions" style="justify-content:flex-end;margin-top:14px">
            <button type="button" class="automart-plan-btn primary" id="subscribeBtn">Subscribe / Upgrade</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    const close=()=>overlay.remove();
    overlay.querySelector('.automart-plan-close').onclick=close;
    overlay.onclick=e=>{if(e.target===overlay)close();};

    function selectMethod(m){
      method=m;
      overlay.querySelectorAll('[data-method]').forEach(b=>b.classList.toggle('active',b.dataset.method===m));
      overlay.querySelector('#cardFields').style.display=m==='Card'?'grid':'none';
      overlay.querySelector('#paypalFields').style.display=m==='PayPal'?'grid':'none';
    }
    overlay.querySelectorAll('[data-method]').forEach(b=>b.onclick=()=>selectMethod(b.dataset.method));

    function showLoadingThen(cb){
      const loader=overlay.querySelector('#planLoading');
      loader.style.display='block';
      setTimeout(()=>{loader.style.display='none'; cb&&cb();},450);
    }
    overlay.querySelectorAll('.automart-plan-option').forEach(opt=>opt.onclick=()=>{
      selectedPlan=opt.dataset.plan;
      overlay.querySelectorAll('.automart-plan-option').forEach(o=>o.classList.remove('selected'));
      opt.classList.add('selected');
      showLoadingThen(()=>toast(selectedPlan+' selected. Add payment details to continue.','info'));
    });
    overlay.querySelector('[data-plan-action="change"]').onclick=()=>showLoadingThen(()=>toast('Choose a plan and add payment details.','info'));
    overlay.querySelector('[data-plan-action="upgrade"]').onclick=()=>showLoadingThen(()=>toast('Upgrade flow ready. Add payment details.','info'));
    overlay.querySelector('[data-plan-action="cancel"]').onclick=()=>popup('Confirm Changes','Are you sure you want to cancel your Dealer Pro subscription?','Confirm',()=>{
      const next=Object.assign(subscription(),{status:'cancelled',plan:'No Active Plan'});
      saveSubscription(next);
      close();
      toast('Subscription cancelled successfully');
    },'🗑️');

    overlay.querySelector('#subscribeBtn').onclick=()=>{
      const price=(plans.find(p=>p[0]===selectedPlan)||plans[1])[1];
      let paymentRef='', ok=false;
      if(method==='Card'){
        const number=overlay.querySelector('#cardNumber').value.replace(/\s/g,'');
        const name=overlay.querySelector('#cardName').value.trim();
        const expiry=overlay.querySelector('#cardExpiry').value.trim();
        const cvv=overlay.querySelector('#cardCvv').value.trim();
        ok=!!name && /^\d{12,19}$/.test(number) && /^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry) && /^\d{3,4}$/.test(cvv);
        paymentRef= ok ? 'Card ending '+number.slice(-4) : '';
      } else {
        const email=overlay.querySelector('#paypalEmail').value.trim().toLowerCase();
        ok=/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email);
        paymentRef=email;
      }
      if(!ok) return toast('Enter valid '+(method==='Card'?'card':'PayPal')+' payment details.','error');
      popup('Confirm Changes','Do you want to subscribe/upgrade to '+selectedPlan+'?','Confirm',()=>{
        saveSubscription({plan:selectedPlan,status:'active',renewalDate:'15 June 2026',price, paymentMethod:method, paymentRef});
        close();
        toast(selectedPlan+' subscription updated successfully');
      },'💳');
    };
  }

  function wirePlan(){
    renderPlanBadges();
  }

  function wire(){
    wireLocation();
    wirePlan();
    document.addEventListener('click',e=>{
      const btn=e.target.closest('button,a');
      if(!btn) return;
      const text=(btn.textContent||btn.getAttribute('aria-label')||btn.getAttribute('title')||'').trim().toLowerCase();
      if(text.includes('save location')){
        e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
        saveLocation(); return;
      }
      if(text.includes('manage plan')){
        e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
        popup('Confirm Changes','Open Dealer Pro plan manager?','Confirm',openPlanModal,'⭐');
        return;
      }
    }, true);
  }

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',wire); else wire();
  window.addEventListener('storage',e=>{
    if(['automartCurrentProfileV2','automartUserProfileRealV4','automartBuyerProfileRealV1','automartSettingsProfileV1',SUB_KEY].includes(e.key)){
      applyLocationFields(); renderPlanBadges();
    }
  });
})();
