
/* AutoMart shared profile/avatar store — hard remove final */
(function(){
  'use strict';

  const KEYS = {
    current: 'automartCurrentProfileV2',
    user: 'automartUserProfileRealV4',
    buyer: 'automartBuyerProfileRealV1',
    admin: 'automartAdminProfileRealV2',
    settings: 'automartSettingsProfileV1',
    settingsPremium: 'automartSettingsPremiumV4',
    oldSettings: 'automartPremiumSettingsV2',
    shared: 'automartSharedMarketplaceDataV2',
    avatar: 'automartMediaAvatarV1',
    avatar2: 'automartMediaAvatarV2',
    banner: 'automartMediaBannerV1',
    banner2: 'automartMediaBannerV2',
    notifications: 'automartLiveNotificationsV1',
    messages: 'automartLiveMessagesV1'
  };

  const PROFILE_PHOTO_FIELDS = [
    'photo','photoRef','profilePhoto','profilePhotoRef','profileImage','profileImageRef',
    'avatar','avatarRef','photoURL','photoUrl','profilePicture','profilePictureRef',
    'profilePhotoBase64','avatarBase64','userPhoto','userPhotoRef'
  ];
  const BANNER_FIELDS = ['banner','bannerRef','bannerImage','bannerImageRef','bannerBase64','coverPhoto','coverPhotoRef'];

  function read(key, fallback){
    try { return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback)); }
    catch { return fallback; }
  }
  function write(key, value){ localStorage.setItem(key, JSON.stringify(value)); }
  function esc(v){ return String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
  function initials(name){ return String(name || 'AutoMart User').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase() || 'AU'; }
  function toast(message, type){
    let old = document.querySelector('.automart-toast-lite'); if (old) old.remove();
    const t = document.createElement('div');
    t.className = 'automart-toast-lite ' + (type || 'success');
    t.textContent = message || 'Done';
    document.body.appendChild(t);
    setTimeout(()=>t.classList.add('show'),10);
    setTimeout(()=>{t.classList.remove('show');setTimeout(()=>t.remove(),220);},2600);
  }

  function ensureStyles(){
    if (document.getElementById('automart-shared-profile-style')) return;
    const style = document.createElement('style');
    style.id = 'automart-shared-profile-style';
    style.textContent = `
      .automart-confirm-overlay{position:fixed;inset:0;z-index:999999;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.62);backdrop-filter:blur(12px);padding:22px}
      .automart-confirm-card{width:min(100%,420px);background:linear-gradient(145deg,rgba(12,20,22,.98),rgba(4,9,10,.98));border:1px solid rgba(200,255,62,.28);border-top:3px solid #c8ff3e;border-radius:20px;padding:28px 24px 22px;text-align:center;box-shadow:0 32px 90px rgba(0,0,0,.62),0 0 34px rgba(200,255,62,.12);animation:automartConfirmIn .24s ease both}
      @keyframes automartConfirmIn{from{opacity:0;transform:translateY(18px) scale(.96)}to{opacity:1;transform:none}}
      .automart-confirm-icon{width:56px;height:56px;margin:0 auto 16px;border-radius:50%;display:grid;place-items:center;background:rgba(200,255,62,.12);border:1px solid rgba(200,255,62,.24);color:#c8ff3e;font-size:24px;box-shadow:0 0 24px rgba(200,255,62,.14)}
      .automart-confirm-title{font-size:20px;font-weight:800;color:#f3f8ed;margin:0 0 8px;font-family:inherit}
      .automart-confirm-message{font-size:13px;color:#aab6ac;line-height:1.6;margin:0 0 22px}
      .automart-confirm-actions{display:flex;gap:10px}.automart-confirm-actions button{flex:1;padding:12px 0;border-radius:12px;font-size:14px;font-weight:800;cursor:pointer;transition:.16s ease;font-family:inherit}
      .automart-confirm-cancel{background:transparent;border:1px solid rgba(255,255,255,.14);color:#aebbad}.automart-confirm-cancel:hover{background:rgba(255,255,255,.05);color:#fff}
      .automart-confirm-ok{background:#c8ff3e;border:1px solid rgba(200,255,62,.65);color:#07100b;box-shadow:0 0 20px rgba(200,255,62,.18)}
      .automart-toast-lite{position:fixed;right:22px;bottom:22px;z-index:999999;background:#35ff9a;color:#06100a;border-radius:14px;padding:13px 18px;font-weight:800;box-shadow:0 22px 60px rgba(0,0,0,.35),0 0 28px rgba(53,255,154,.18);opacity:0;transform:translateY(12px);transition:.22s ease}
      .automart-toast-lite.show{opacity:1;transform:none}.automart-toast-lite.error{background:#ff4f6e;color:#fff}.automart-toast-lite.info{background:#3efff8;color:#001112}
      .avatar-inner.has-photo,.tb-av.has-photo,#globalProfileAvatar.has-photo,.profile-avatar.has-photo,.avatar.has-photo,.settings-avatar.has-photo,.avatar-preview.has-photo,#adminProfilePhoto.has-photo,.profile-avatar-lg.has-photo{background-image:var(--automart-avatar-photo)!important;background-size:cover!important;background-position:center!important;color:transparent!important}
      .automart-profile-mini-btn{display:inline-flex!important;align-items:center!important;justify-content:center!important;gap:7px!important;border:1px solid rgba(200,255,62,.2)!important;background:rgba(9,16,18,.72)!important;color:#dce8d6!important;border-radius:10px!important;padding:8px 14px!important;font-size:12px!important;font-weight:700!important;cursor:pointer!important;text-decoration:none!important;box-shadow:0 0 14px rgba(200,255,62,.08)!important}
      .automart-profile-mini-btn:hover{border-color:rgba(200,255,62,.45)!important;color:#c8ff3e!important;background:rgba(200,255,62,.08)!important}
      .automart-profile-save-btn-fixed{background:#c8ff3e!important;color:#07100b!important;border:0!important;border-radius:12px!important;box-shadow:0 0 24px rgba(200,255,62,.22)!important;font-weight:900!important}
      .automart-crop-overlay{position:fixed;inset:0;z-index:999998;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.66);backdrop-filter:blur(12px);padding:20px}
      .automart-crop-card{width:min(100%,520px);background:linear-gradient(145deg,rgba(12,20,22,.98),rgba(4,9,10,.98));border:1px solid rgba(200,255,62,.28);border-radius:22px;padding:22px;box-shadow:0 32px 90px rgba(0,0,0,.62)}
      .automart-crop-card h3{margin:0 0 6px;color:#f3f8ed;font-size:22px}.automart-crop-card p{margin:0 0 14px;color:#94a193;font-size:13px}
      .automart-crop-preview{width:100%;aspect-ratio:1/1;max-height:360px;border-radius:18px;background:#081010;border:1px solid rgba(200,255,62,.16);display:grid;place-items:center;overflow:hidden}.automart-crop-preview canvas{max-width:100%;max-height:100%;border-radius:14px}
      .automart-crop-controls{display:grid;gap:10px;margin-top:14px}.automart-crop-controls label{display:grid;gap:6px;font-size:12px;color:#aebbad}.automart-crop-controls input{accent-color:#c8ff3e}
      .automart-crop-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:16px}
    `;
    document.head.appendChild(style);
  }

  function commonPopup(title,message,okText,onConfirm,icon){
    ensureStyles();
    document.querySelectorAll('.automart-confirm-overlay').forEach(x=>x.remove());
    const overlay=document.createElement('div');
    overlay.className='automart-confirm-overlay';
    overlay.innerHTML = `<div class="automart-confirm-card" role="dialog" aria-modal="true">
      <div class="automart-confirm-icon">${icon || '✓'}</div>
      <h3 class="automart-confirm-title">${esc(title || 'Confirm Changes')}</h3>
      <p class="automart-confirm-message">${esc(message || 'Do you want to continue?')}</p>
      <div class="automart-confirm-actions"><button type="button" class="automart-confirm-cancel">Cancel</button><button type="button" class="automart-confirm-ok">${esc(okText || 'Confirm')}</button></div>
    </div>`;
    document.body.appendChild(overlay);
    overlay.querySelector('.automart-confirm-cancel').onclick = () => overlay.remove();
    overlay.onclick = e => { if(e.target === overlay) overlay.remove(); };
    overlay.querySelector('.automart-confirm-ok').onclick = () => {
      try { onConfirm && onConfirm(); }
      catch(err){ console.error(err); toast('Action failed: ' + (err.message || err), 'error'); }
      overlay.remove();
    };
  }

  window.alert = function(message){ toast(String(message || ''), 'info'); };
  window.confirm = function(){ return false; };
  window.prompt = function(){ toast('This action now uses the AutoMart popup.', 'info'); return null; };
  window.AutoMartConfirm = commonPopup;
  window.AutoMartPremiumConfirm = commonPopup;
  window.AutoMartToast = toast;

  function stripPhotoFields(obj){
    if(!obj || typeof obj !== 'object') return obj;
    PROFILE_PHOTO_FIELDS.forEach(k => { if(k in obj) delete obj[k]; });
    Object.keys(obj).forEach(k => {
      if(obj[k] && typeof obj[k] === 'object') stripPhotoFields(obj[k]);
      if(typeof obj[k] === 'string' && obj[k].startsWith('data:image') && /photo|avatar|profile/i.test(k)) delete obj[k];
    });
    return obj;
  }
  function stripBannerFields(obj){
    if(!obj || typeof obj !== 'object') return obj;
    BANNER_FIELDS.forEach(k => { if(k in obj) delete obj[k]; });
    Object.keys(obj).forEach(k => {
      if(obj[k] && typeof obj[k] === 'object') stripBannerFields(obj[k]);
      if(typeof obj[k] === 'string' && obj[k].startsWith('data:image') && /banner|cover/i.test(k)) delete obj[k];
    });
    return obj;
  }
  function removeStorageKey(k){ try { localStorage.removeItem(k); } catch{} }

  function hardClearPhotoFromAllStores(){
    [KEYS.avatar, KEYS.avatar2, 'automartProfilePhoto', 'automartAvatarCache', 'automartProfileAvatar',
     'automartOldProfilePhoto', 'automartUserAvatar', 'profilePhoto', 'avatarPhoto'].forEach(removeStorageKey);
    [KEYS.current, KEYS.user, KEYS.buyer, KEYS.admin, KEYS.settings, KEYS.settingsPremium, KEYS.oldSettings, KEYS.shared].forEach(k => {
      const value = read(k, null);
      if(!value || typeof value !== 'object') return;
      const cleaned = stripPhotoFields(JSON.parse(JSON.stringify(value)));
      cleaned.photoRemoved = true;
      cleaned.profilePhotoRemoved = true;
      cleaned.profilePhotoVersion = Date.now();
      if(cleaned.profile && typeof cleaned.profile === 'object') {
        cleaned.profile.photoRemoved = true;
        cleaned.profile.profilePhotoRemoved = true;
        cleaned.profile.profilePhotoVersion = Date.now();
      }
      if(cleaned.activeProfile && typeof cleaned.activeProfile === 'object') {
        cleaned.activeProfile.photoRemoved = true;
        cleaned.activeProfile.profilePhotoRemoved = true;
        cleaned.activeProfile.profilePhotoVersion = Date.now();
      }
      try { write(k, cleaned); } catch {}
    });
  }

  function sanitizeHugeMediaFromSettings(){
    [KEYS.settingsPremium, KEYS.oldSettings, KEYS.settings, KEYS.shared].forEach(k => {
      const value = read(k, null);
      if(!value || typeof value !== 'object') return;
      const cleaned = JSON.parse(JSON.stringify(value));
      function walk(o){
        if(!o || typeof o !== 'object') return;
        Object.keys(o).forEach(key => {
          if(typeof o[key] === 'string' && o[key].startsWith('data:image') && !/listing|vehicle|car/i.test(key)) delete o[key];
          else walk(o[key]);
        });
      }
      walk(cleaned);
      try { write(k, cleaned); } catch {}
    });
  }

  function getRole(){ return (document.getElementById('globalProfileRole')?.textContent || '').toLowerCase().trim(); }
  function normalizeProfile(p){
    p = Object.assign({}, p || {});
    const removed = p.photoRemoved || p.profilePhotoRemoved;
    if(removed) {
      stripPhotoFields(p);
      p.photoRemoved = true;
      p.profilePhotoRemoved = true;
      return p;
    }
    if (p.photoRef && !p.photo) p.photo = localStorage.getItem(p.photoRef) || '';
    if (p.profilePhotoRef && !p.photo) p.photo = localStorage.getItem(p.profilePhotoRef) || '';
    if (p.avatarRef && !p.photo) p.photo = localStorage.getItem(p.avatarRef) || '';
    if (!p.photo) p.photo = localStorage.getItem(KEYS.avatar2) || localStorage.getItem(KEYS.avatar) || '';
    if (p.bannerRef && !p.banner) p.banner = localStorage.getItem(p.bannerRef) || '';
    if (p.bannerImageRef && !p.banner) p.banner = localStorage.getItem(p.bannerImageRef) || '';
    if (!p.banner) p.banner = localStorage.getItem(KEYS.banner2) || localStorage.getItem(KEYS.banner) || '';
    return p;
  }
  function getCurrentProfile(){
    sanitizeHugeMediaFromSettings();
    const role = getRole();
    const base = role.includes('admin') ? read(KEYS.admin,{}) : role.includes('buyer') ? read(KEYS.buyer, read(KEYS.user,{})) : read(KEYS.user,{});
    return normalizeProfile(base);
  }
  function lightProfile(p){
    const copy = Object.assign({}, p || {});
    stripPhotoFields(copy);
    stripBannerFields(copy);
    if(p && p.photo) {
      copy.photoRef = KEYS.avatar2;
      copy.profilePhotoRef = KEYS.avatar2;
      copy.profilePhotoRemoved = false;
      copy.photoRemoved = false;
    } else {
      copy.photoRef = '';
      copy.profilePhotoRef = '';
      copy.profilePhotoRemoved = true;
      copy.photoRemoved = true;
    }
    if(p && p.banner) {
      copy.bannerRef = KEYS.banner2;
      copy.bannerImageRef = KEYS.banner2;
    } else {
      copy.bannerRef = '';
      copy.bannerImageRef = '';
    }
    copy.profilePhotoVersion = p.profilePhotoVersion || Date.now();
    return copy;
  }

  function writeLightToAll(light, replace = false){
    const role = String(light.role || getRole() || 'seller').toLowerCase();
    const activeKey = role.includes('admin') ? KEYS.admin : role.includes('buyer') ? KEYS.buyer : KEYS.user;

    [KEYS.current, KEYS.settings, activeKey].forEach(k => {
      const old = replace ? {} : stripBannerFields(stripPhotoFields(read(k,{})));
      write(k, Object.assign(old, light));
    });

    if(role.includes('admin')){
      const adminOld = replace ? {} : stripBannerFields(stripPhotoFields(read(KEYS.admin,{})));
      write(KEYS.admin, Object.assign(adminOld, {
        fullName: light.fullName,
        username: light.displayName || light.username,
        email: light.email,
        phone: light.phone,
        role:'admin',
        photoRef: light.photoRef,
        profilePhotoRef: light.profilePhotoRef,
        bannerRef: light.bannerRef,
        bannerImageRef: light.bannerImageRef,
        profilePhotoRemoved: light.profilePhotoRemoved,
        photoRemoved: light.photoRemoved,
        profilePhotoVersion: light.profilePhotoVersion
      }));
    }

    const premium = replace ? {} : stripBannerFields(stripPhotoFields(read(KEYS.settingsPremium,{})));
    premium.profile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(premium.profile||{})), light);
    write(KEYS.settingsPremium, premium);

    const shared = replace ? {} : stripBannerFields(stripPhotoFields(read(KEYS.shared,{})));
    shared.activeProfile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(shared.activeProfile||{})), light);
    if(role.includes('buyer')) {
      shared.buyerProfile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(shared.buyerProfile||{})), light);
    } else if(role.includes('admin')) {
      shared.adminProfile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(shared.adminProfile||{})), light);
    } else {
      shared.profile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(shared.profile||{})), light);
      shared.sellerProfile = Object.assign(replace ? {} : stripBannerFields(stripPhotoFields(shared.sellerProfile||{})), light);
    }
    shared.updatedAt = Date.now();
    write(KEYS.shared, shared);
  }

  function updateCurrentUserProfile(p, replace = false){
    p = replace ? (p || {}) : Object.assign(getCurrentProfile(), p || {});
    p.profilePhotoVersion = Date.now();
    const light = lightProfile(p);
    try {
      if (p.photo) {
        hardClearPhotoFromAllStores();
        localStorage.setItem(KEYS.avatar2, p.photo);
        localStorage.setItem(KEYS.avatar, p.photo);
        light.photoRef = KEYS.avatar2;
        light.profilePhotoRef = KEYS.avatar2;
        light.photoRemoved = false;
        light.profilePhotoRemoved = false;
      }
      if (p.banner) {
        localStorage.setItem(KEYS.banner2, p.banner);
        localStorage.setItem(KEYS.banner, p.banner);
        light.bannerRef = KEYS.banner2;
        light.bannerImageRef = KEYS.banner2;
      } else if(p.banner === '') {
        localStorage.removeItem(KEYS.banner2);
        localStorage.removeItem(KEYS.banner);
      }
      writeLightToAll(light, replace);
      applyProfileToPage();
      window.dispatchEvent(new StorageEvent('storage', {key: KEYS.current, newValue: JSON.stringify(light)}));
      window.dispatchEvent(new StorageEvent('storage', {key: KEYS.avatar2, newValue: p.photo || ''}));
      return true;
    } catch(err){
      console.error(err);
      toast('Save failed. Browser storage is full. Clear old site data and try again.', 'error');
      return false;
    }
  }

  function updateCurrentUserProfilePhoto(photo){
    hardClearPhotoFromAllStores();
    if(!photo) return removeCurrentUserProfilePhoto();
    return updateCurrentUserProfile({photo, photoRemoved:false, profilePhotoRemoved:false, profilePhotoVersion:Date.now()});
  }
  function removeCurrentUserProfilePhoto(){
    try {
      hardClearPhotoFromAllStores();
      const profile = Object.assign(getCurrentProfile(), {photo:'', photoRemoved:true, profilePhotoRemoved:true, profilePhotoVersion:Date.now()});
      stripPhotoFields(profile);
      writeLightToAll(lightProfile(profile));
      applyProfileToPage();
      window.dispatchEvent(new StorageEvent('storage', {key: KEYS.current, newValue: JSON.stringify(profile)}));
      window.dispatchEvent(new StorageEvent('storage', {key: KEYS.avatar2, newValue: ''}));
      return true;
    } catch(err) {
      console.error(err);
      toast('Remove failed: ' + (err.message || err), 'error');
      return false;
    }
  }
  function updateCurrentUserBanner(banner){ return updateCurrentUserProfile({banner: banner || ''}); }
  function removeCurrentUserBanner(){
    localStorage.removeItem(KEYS.banner2); localStorage.removeItem(KEYS.banner);
    const p = getCurrentProfile(); p.banner = '';
    return updateCurrentUserProfile(p);
  }

  function setAvatar(el, profile){
    if(!el) return;
    const name = profile.fullName || [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.displayName || profile.username || 'AutoMart User';
    if(profile.photo && !profile.profilePhotoRemoved && !profile.photoRemoved){
      el.classList.add('has-photo');
      el.style.setProperty('--automart-avatar-photo', 'url("' + profile.photo + '#v=' + (profile.profilePhotoVersion || Date.now()) + '")');
      el.style.backgroundImage = 'url("' + profile.photo + '#v=' + (profile.profilePhotoVersion || Date.now()) + '")';
      el.style.backgroundSize = 'cover';
      el.style.backgroundPosition = 'center';
      el.textContent = '';
    } else {
      el.classList.remove('has-photo');
      el.style.removeProperty('--automart-avatar-photo');
      el.style.backgroundImage = '';
      el.textContent = initials(name);
    }
  }
  function applyBanner(profile){
    document.querySelectorAll('.hero-banner,.settings-banner,.profile-banner').forEach(b => {
      if(profile.banner){
        b.style.backgroundImage = 'linear-gradient(135deg,rgba(5,9,12,.30),rgba(5,9,12,.62)),url("' + profile.banner + '")';
        b.style.backgroundSize = 'cover'; b.style.backgroundPosition = 'center';
      } else {
        b.style.backgroundImage = ''; b.style.backgroundSize = ''; b.style.backgroundPosition = '';
      }
    });
  }
  function setFieldByLabel(labelText, value){
    if(value === undefined || value === null) return;
    const labels = [...document.querySelectorAll('.form-label,label')];
    const clean = s => String(s || '').toLowerCase().replace(/\*/g,'').replace(/\s+/g,' ').trim();
    const wanted = clean(labelText);
    const label = labels.find(l => clean(l.textContent).includes(wanted));
    if(!label) return;
    const field = label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea');
    if(!field) return;
    if(field.tagName === 'SELECT') {
      const valueText = String(value);
      [...field.options].forEach(opt => { opt.selected = opt.value === valueText || opt.textContent.trim() === valueText; });
    } else {
      field.value = String(value);
    }
  }
  function setHeroText(el, text){
    if(!el || !text) return;
    const badge = el.querySelector('.verified-badge');
    [...el.childNodes].forEach(n => { if(n.nodeType === Node.TEXT_NODE) n.textContent = ''; });
    el.insertAdjacentText('afterbegin', text + ' ');
    if(badge && !el.contains(badge)) el.appendChild(badge);
  }
  function applyPreferenceControls(p){
    const prefs = p.marketplacePreferences || p.preferences || {};
    if(Array.isArray(prefs.bodyTypes)){
      document.querySelectorAll('.pref-chip').forEach(chip => {
        const label = chip.querySelector('.pref-chip-label')?.textContent?.trim();
        if(label) chip.classList.toggle('selected', prefs.bodyTypes.includes(label));
      });
    }
    const toggles = Object.assign({}, prefs.toggles || {}, p.notificationPreferences || {}, p.securityPreferences || {});
    document.querySelectorAll('.toggle').forEach(toggle => {
      const row = toggle.closest('.toggle-row,.notif-channel,.sec-item');
      const label = row?.querySelector('.t-name,.toggle-name,.sec-name')?.textContent?.trim()
        || row?.querySelector('div[style*="font-size:13px"]')?.textContent?.trim()
        || row?.textContent?.split('\n')[0]?.trim();
      if(label && Object.prototype.hasOwnProperty.call(toggles, label)) toggle.classList.toggle('on', !!toggles[label]);
    });
  }
  function setFieldBySlug(slug, value){
    if(value === undefined || value === null) return;
    const labels = [...document.querySelectorAll('.form-label,label')];
    const label = labels.find(l => labelSlug(l.textContent) === slug || labelSlug(l.textContent).includes(slug) || slug.includes(labelSlug(l.textContent)));
    if(!label) return;
    const field = label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea');
    if(!field || field.type === 'file' || field.type === 'password') return;
    if(field.type === 'checkbox') field.checked = !!value;
    else if(field.tagName === 'SELECT') {
      const valueText = String(value);
      let matched = false;
      [...field.options].forEach(opt => {
        const same = opt.value === valueText || opt.textContent.trim() === valueText;
        if(same) matched = true;
        opt.selected = same;
      });
      if(!matched && valueText) {
        const opt = document.createElement('option');
        opt.textContent = valueText;
        opt.value = valueText;
        opt.selected = true;
        field.appendChild(opt);
      }
    } else {
      field.value = String(value);
    }
  }
  function setHeroText(el, text){
    if(!el || !text) return;
    const badge = el.querySelector('.verified-badge');
    [...el.childNodes].forEach(n => { if(n.nodeType === Node.TEXT_NODE) n.textContent = ''; });
    el.insertAdjacentText('afterbegin', text + ' ');
    if(badge && !el.contains(badge)) el.appendChild(badge);
  }
  function applyPreferenceControls(p){
    const prefs = p.marketplacePreferences || p.preferences || {};
    if(Array.isArray(prefs.bodyTypes)){
      document.querySelectorAll('.pref-chip').forEach(chip => {
        const label = chip.querySelector('.pref-chip-label')?.textContent?.trim();
        if(label) chip.classList.toggle('selected', prefs.bodyTypes.includes(label));
      });
    }
    const toggles = Object.assign({}, prefs.toggles || {}, p.notificationPreferences || {}, p.securityPreferences || {});
    document.querySelectorAll('.toggle').forEach(toggle => {
      const row = toggle.closest('.toggle-row,.notif-channel,.sec-item');
      const label = row?.querySelector('.t-name,.toggle-name,.sec-name')?.textContent?.trim()
        || row?.querySelector('div[style*="font-size:13px"]')?.textContent?.trim()
        || row?.textContent?.replace(/\s+/g,' ').trim();
      if(label && Object.prototype.hasOwnProperty.call(toggles, label)) toggle.classList.toggle('on', !!toggles[label]);
    });
  }
  function applyProfileToPage(){
    // CRITICAL: Never inject user profile data into the admin login page.
    // The label "Admin Email / Username" contains "email", so setFieldByLabel()
    // would overwrite the admin input with the current buyer/seller's email.
    if (document.querySelector('.admin-portal-page')) return;
    ensureStyles();
    const p = getCurrentProfile();
    document.querySelectorAll('#globalProfileAvatar,.profile-avatar,.tb-av,.avatar,.avatar-inner,.settings-avatar,.avatar-preview,#adminProfilePhoto,.profile-avatar-lg').forEach(el => setAvatar(el,p));

    const name = p.fullName || [p.firstName,p.lastName].filter(Boolean).join(' ') || p.displayName || p.username || '';
    if(name) {
      document.querySelectorAll('#globalProfileName,.profile-display-name,[data-profile-name],.profile-meta h3').forEach(el => el.textContent = name);
      setHeroText(document.getElementById('profileHeroName') || document.getElementById('buyerHeroName'), name);
    }

    const handle = document.getElementById('profileHeroHandle') || document.getElementById('buyerHeroHandle');
    if(handle) {
      const display = (p.displayName || p.username || name || 'automart_user').replace(/\s+/g,'_').toLowerCase();
      const city = p.city || p.district || 'Sri Lanka';
      handle.textContent = '@' + display.replace(/^@/,'') + ' · Member since January 2021 · ' + city;
    }

    if(p.email) {
      document.querySelectorAll('.profile-meta p,[data-profile-email]').forEach(el => { if(/@|Member since/i.test(el.textContent)) el.textContent = p.email + ' · Member since 2021'; });
      document.querySelectorAll('.sec-item .sec-sub').forEach(el => { if(el.textContent.includes('@')) el.textContent = p.email; });
    }
    if(p.phone) {
      document.querySelectorAll('.sec-item .sec-sub').forEach(el => { if(el.textContent.includes('+94') || /^\d{10}$/.test(el.textContent.trim())) el.textContent = p.phone; });
    }

    const fields = p.profileFields || {};
    Object.keys(fields).forEach(key => setFieldBySlug(key, fields[key]));

    setFieldByLabel('First Name', p.firstName || '');
    setFieldByLabel('Last Name', p.lastName || '');
    setFieldByLabel('Display Name', p.displayName || p.username || '');
    setFieldByLabel('Date of Birth', p.dateOfBirth || fields.date_of_birth || '');
    setFieldByLabel('Gender', p.gender || fields.gender || '');
    setFieldByLabel('Nationality', p.nationality || fields.nationality || '');
    setFieldByLabel('Email Address', p.email || fields.email_address || '');
    setFieldByLabel('Email', p.email || fields.email || '');
    setFieldByLabel('Phone Number', p.phone || fields.phone_number || '');
    setFieldByLabel('WhatsApp Number', p.whatsapp || p.whatsApp || fields.whatsapp_number || p.phone || '');
    setFieldByLabel('Contact Preference', p.contactPreference || fields.contact_preference || '');
    setFieldByLabel('Website / Social Link', p.websiteSocialLink || fields.website_social_link || '');
    setFieldByLabel('Preferred Language', p.preferredLanguage || fields.preferred_language || '');
    setFieldByLabel('About Me / Seller Bio', p.bio || fields.about_me_seller_bio || '');
    setFieldByLabel('Bio', p.bio || fields.bio || '');
    setFieldByLabel('Province', p.province || fields.province || '');
    setFieldByLabel('District', p.district || fields.district || '');
    setFieldByLabel('City', p.city || fields.city || '');
    setFieldByLabel('Full Address', p.fullAddress || p.address || fields.full_address || '');

    const prefs = p.marketplacePreferences || p.preferences || {};
    setFieldByLabel('Minimum Budget', prefs.minimumBudget || fields.minimum_budget || '');
    setFieldByLabel('Maximum Budget', prefs.maximumBudget || fields.maximum_budget || '');
    setFieldByLabel('Preferred Fuel Type', prefs.preferredFuelType || fields.preferred_fuel_type || '');
    setFieldByLabel('Transmission', prefs.transmission || fields.transmission || '');
    setFieldByLabel('Preferred Make', prefs.preferredMake || fields.preferred_make || '');
    setFieldByLabel('Max Mileage', prefs.maxMileage || fields.max_mileage || fields.max_mileage_km || '');
    setFieldByLabel('Preferred Contact Method', prefs.preferredContactMethod || fields.preferred_contact_method || '');
    setFieldByLabel('Inspection City', prefs.inspectionCity || fields.inspection_city || '');
    applyPreferenceControls(p);

    applyBanner(p);
  }
  function subscribeToProfileChanges(fn){
    window.addEventListener('storage', e => {
      if([KEYS.current,KEYS.user,KEYS.buyer,KEYS.admin,KEYS.settings,KEYS.settingsPremium,KEYS.shared,KEYS.avatar,KEYS.avatar2,KEYS.banner,KEYS.banner2].includes(e.key)){
        applyProfileToPage();
        if(fn) fn(getCurrentProfile());
      }
    });
  }

  function loadImage(file){
    return new Promise((resolve,reject)=>{
      if(!file || !file.type.startsWith('image/')) return reject(new Error('Please choose a valid JPG, PNG, or WEBP image.'));
      const reader = new FileReader();
      reader.onerror = () => reject(new Error('Could not read image.'));
      reader.onload = () => {
        const img = new Image();
        img.onerror = () => reject(new Error('Could not load image.'));
        img.onload = () => resolve(img);
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }
  function canvasFromImage(img, opts){
    opts = opts || {};
    const maxW = opts.maxW || 512, maxH = opts.maxH || 512, quality = opts.quality || 0.70;
    let w = img.width, h = img.height;
    const ratio = Math.min(maxW / w, maxH / h, 1);
    w = Math.max(1, Math.round(w * ratio)); h = Math.max(1, Math.round(h * ratio));
    const canvas = document.createElement('canvas');
    canvas.width = w; canvas.height = h;
    canvas.getContext('2d').drawImage(img,0,0,w,h);
    return canvas.toDataURL('image/jpeg',quality);
  }
  function openCropModal(file, opts, done){
    ensureStyles();
    loadImage(file).then(img => {
      let zoom = 1, rotation = 0;
      const overlay = document.createElement('div');
      overlay.className = 'automart-crop-overlay';
      overlay.innerHTML = `<div class="automart-crop-card">
        <h3>Preview & Crop Photo</h3>
        <p>Adjust zoom or rotation, then save using the AutoMart confirmation popup.</p>
        <div class="automart-crop-preview"><canvas id="automartCropCanvas"></canvas></div>
        <div class="automart-crop-controls">
          <label>Zoom <input id="automartCropZoom" type="range" min="1" max="2.5" step="0.05" value="1"></label>
          <label>Rotate <input id="automartCropRotate" type="range" min="-180" max="180" step="5" value="0"></label>
        </div>
        <div class="automart-crop-actions">
          <button type="button" class="automart-profile-mini-btn" data-crop-cancel>Cancel</button>
          <button type="button" class="automart-profile-mini-btn automart-profile-save-btn-fixed" data-crop-save>Save Photo</button>
        </div>
      </div>`;
      document.body.appendChild(overlay);
      const canvas = overlay.querySelector('#automartCropCanvas');
      const size = 360;
      canvas.width = size; canvas.height = size;
      const ctx = canvas.getContext('2d');
      function draw(){
        ctx.clearRect(0,0,size,size);
        ctx.save();
        ctx.translate(size/2,size/2);
        ctx.rotate(rotation*Math.PI/180);
        const scale = Math.max(size/img.width, size/img.height) * zoom;
        ctx.drawImage(img, -img.width*scale/2, -img.height*scale/2, img.width*scale, img.height*scale);
        ctx.restore();
      }
      draw();
      overlay.querySelector('#automartCropZoom').oninput = e => { zoom = Number(e.target.value); draw(); };
      overlay.querySelector('#automartCropRotate').oninput = e => { rotation = Number(e.target.value); draw(); };
      overlay.querySelector('[data-crop-cancel]').onclick = () => overlay.remove();
      overlay.onclick = e => { if(e.target === overlay) overlay.remove(); };
      overlay.querySelector('[data-crop-save]').onclick = () => {
        const data = canvas.toDataURL('image/jpeg', opts.quality || .72);
        overlay.remove();
        done(data);
      };
    }).catch(err => toast(err.message || 'Upload failed.', 'error'));
  }
  function pickImage(opts, cb, crop){
    const input = document.createElement('input');
    input.type='file'; input.accept='image/png,image/jpeg,image/webp,image/*'; input.style.display='none';
    document.body.appendChild(input);
    input.onchange = async () => {
      const file = input.files && input.files[0];
      input.remove();
      if(!file) return;
      if(crop) return openCropModal(file, opts, cb);
      try { cb(canvasFromImage(await loadImage(file), opts)); }
      catch(err){ toast(err.message || 'Upload failed.', 'error'); }
    };
    input.click();
  }
  function changePhoto(){
    pickImage({maxW:360,maxH:360,quality:.72}, data => {
      setAvatar(document.querySelector('#globalProfileAvatar'), Object.assign(getCurrentProfile(),{photo:data,photoRemoved:false,profilePhotoRemoved:false}));
      commonPopup('Confirm Changes','Do you want to save this profile picture?','Confirm',()=>{
        if(updateCurrentUserProfilePhoto(data)) toast('Profile picture saved successfully');
      }, '📷');
    }, true);
  }
  function removePhoto(){
    commonPopup('Confirm Changes','Are you sure you want to remove your profile picture?','Confirm',()=>{
      if(removeCurrentUserProfilePhoto()) toast('Profile picture removed successfully');
    }, '🗑️');
  }
  function changeBanner(){
    pickImage({maxW:1200,maxH:360,quality:.68}, data => {
      applyBanner({banner:data});
      commonPopup('Confirm Changes','Do you want to save this banner?','Confirm',()=>{
        if(updateCurrentUserBanner(data)) toast('Banner saved successfully');
      }, '🖼️');
    }, false);
  }
  function removeBanner(){
    commonPopup('Confirm Changes','Are you sure you want to remove this banner?','Confirm',()=>{
      if(removeCurrentUserBanner()) toast('Banner removed successfully');
    }, '🗑️');
  }

  function normalizePhone(raw){
    let digits = String(raw || '').replace(/\D/g,'');
    if(digits.length === 11 && digits.startsWith('94')) digits = '0' + digits.slice(2);
    if(digits.length === 9 && digits.startsWith('7')) digits = '0' + digits;
    return digits.slice(0,10);
  }
  function labelSlug(text){
    return String(text || '').toLowerCase().replace(/\*/g,'').replace(/\(.*?\)/g,'').replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');
  }
  function fieldByLabel(needle){
    const labels = [...document.querySelectorAll('.form-label,label')];
    const clean = s => String(s || '').toLowerCase().replace(/\*/g,'').replace(/\s+/g,' ').trim();
    const label = labels.find(l => clean(l.textContent).includes(clean(needle)));
    return label ? (label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea')) : null;
  }
  function fieldValueByLabel(needle, fallback){
    const f = fieldByLabel(needle);
    if(!f) return fallback;
    if(f.type === 'checkbox') return !!f.checked;
    return String(f.value || '').trim();
  }
  function collectAllProfileFields(){
    const fields = {};
    document.querySelectorAll('.form-group').forEach(group => {
      const label = group.querySelector('.form-label,label');
      const field = group.querySelector('input,select,textarea');
      if(!label || !field || field.type === 'file' || field.type === 'password') return;
      const key = labelSlug(label.textContent);
      if(!key) return;
      fields[key] = field.type === 'checkbox' ? !!field.checked : String(field.value || '').trim();
    });
    return fields;
  }
  function collectToggles(scopeSelector){
    const out = {};
    const root = scopeSelector ? document.querySelector(scopeSelector) : document;
    if(!root) return out;
    root.querySelectorAll('.toggle').forEach(toggle => {
      const row = toggle.closest('.toggle-row,.notif-channel,.sec-item') || toggle.parentElement;
      const label = row?.querySelector('.t-name,.toggle-name,.sec-name')?.textContent?.trim()
        || row?.querySelector('div[style*="font-size:13px"]')?.textContent?.trim()
        || row?.textContent?.replace(/\s+/g,' ').trim();
      if(label) out[label] = toggle.classList.contains('on');
    });
    return out;
  }
  function collectMarketplacePreferences(){
    const fields = collectAllProfileFields();
    const bodyTypes = [...document.querySelectorAll('#panel-marketplace .pref-chip.selected .pref-chip-label')].map(x => x.textContent.trim()).filter(Boolean);
    return {
      bodyTypes,
      minimumBudget: fields.minimum_budget || '',
      maximumBudget: fields.maximum_budget || '',
      preferredFuelType: fields.preferred_fuel_type || '',
      transmission: fields.transmission || '',
      preferredMake: fields.preferred_make || '',
      maxMileage: fields.max_mileage || fields.max_mileage_km || '',
      preferredContactMethod: fields.preferred_contact_method || '',
      inspectionCity: fields.inspection_city || '',
      toggles: collectToggles('#panel-marketplace')
    };
  }
  function collectNotificationPreferences(){
    return collectToggles('#panel-notifications');
  }
  function collectSecurityPreferences(){
    const sec = collectToggles('#panel-security');
    const twoFactorStatus = [...document.querySelectorAll('#panel-security .sec-item')].find(x => /Two-Factor/i.test(x.textContent));
    if(twoFactorStatus) sec.twoFactorEnabled = /Enabled/i.test(twoFactorStatus.textContent) && !/Disabled/i.test(twoFactorStatus.textContent);
    return sec;
  }
  function normalizePhone(raw){
    let digits = String(raw || '').replace(/\D/g,'');
    if(digits.length === 11 && digits.startsWith('94')) digits = '0' + digits.slice(2);
    if(digits.length === 9 && digits.startsWith('7')) digits = '0' + digits;
    return digits.slice(0,10);
  }
  function labelSlug(text){
    return String(text || '').toLowerCase().replace(/\*/g,'').replace(/\(.*?\)/g,'').replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');
  }
  function fieldByLabel(needle){
    const labels = [...document.querySelectorAll('.form-label,label')];
    const clean = s => String(s || '').toLowerCase().replace(/\*/g,'').replace(/\s+/g,' ').trim();
    const label = labels.find(l => clean(l.textContent).includes(clean(needle)));
    return label ? (label.parentElement.querySelector('input,select,textarea') || label.querySelector('input,select,textarea')) : null;
  }
  function fieldValueByLabel(needle, fallback){
    const f = fieldByLabel(needle);
    if(!f) return fallback;
    if(f.type === 'checkbox') return !!f.checked;
    return String(f.value || '').trim();
  }
  function collectAllProfileFields(){
    const fields = {};
    document.querySelectorAll('.form-group').forEach(group => {
      const label = group.querySelector('.form-label,label');
      const field = group.querySelector('input,select,textarea');
      if(!label || !field || field.type === 'file' || field.type === 'password') return;
      const key = labelSlug(label.textContent);
      if(!key) return;
      fields[key] = field.type === 'checkbox' ? !!field.checked : String(field.value || '').trim();
    });
    return fields;
  }
  function collectToggles(scopeSelector){
    const out = {};
    const root = scopeSelector ? document.querySelector(scopeSelector) : document;
    if(!root) return out;
    root.querySelectorAll('.toggle').forEach(toggle => {
      const row = toggle.closest('.toggle-row,.notif-channel,.sec-item') || toggle.parentElement;
      const label = row?.querySelector('.t-name,.toggle-name,.sec-name')?.textContent?.trim()
        || row?.querySelector('div[style*="font-size:13px"]')?.textContent?.trim()
        || row?.textContent?.replace(/\s+/g,' ').trim();
      if(label) out[label] = toggle.classList.contains('on');
    });
    return out;
  }
  function collectMarketplacePreferences(){
    const fields = collectAllProfileFields();
    const bodyTypes = [...document.querySelectorAll('#panel-marketplace .pref-chip.selected .pref-chip-label')].map(x => x.textContent.trim()).filter(Boolean);
    return {
      bodyTypes,
      minimumBudget: fields.minimum_budget || '',
      maximumBudget: fields.maximum_budget || '',
      budgetRange: (document.getElementById('budgetVal')?.textContent || ''),
      preferredFuelType: fields.preferred_fuel_type || '',
      transmission: fields.transmission || '',
      preferredMake: fields.preferred_make || '',
      maxMileage: fields.max_mileage || fields.max_mileage_km || '',
      preferredContactMethod: fields.preferred_contact_method || '',
      inspectionCity: fields.inspection_city || '',
      toggles: collectToggles('#panel-marketplace')
    };
  }
  function collectNotificationPreferences(){
    return collectToggles('#panel-notifications');
  }
  function collectSecurityPreferences(){
    const sec = collectToggles('#panel-security');
    const twoFactorStatus = [...document.querySelectorAll('#panel-security .sec-item')].find(x => /Two-Factor/i.test(x.textContent));
    if(twoFactorStatus) sec.twoFactorEnabled = /Enabled/i.test(twoFactorStatus.textContent) && !/Disabled/i.test(twoFactorStatus.textContent);
    return sec;
  }
  function saveVisibleProfileForm(){
    const p = getCurrentProfile();
    const profileFields = Object.assign({}, p.profileFields || {}, collectAllProfileFields());

    const firstName = fieldValueByLabel('First Name', p.firstName || profileFields.first_name || '');
    const lastName = fieldValueByLabel('Last Name', p.lastName || profileFields.last_name || '');
    const displayName = fieldValueByLabel('Display Name', p.displayName || p.username || profileFields.display_name || '');
    const dateOfBirth = fieldValueByLabel('Date of Birth', p.dateOfBirth || profileFields.date_of_birth || '');
    const gender = fieldValueByLabel('Gender', p.gender || profileFields.gender || '');
    const nationality = fieldValueByLabel('Nationality', p.nationality || profileFields.nationality || '');
    const email = (fieldValueByLabel('Email Address', fieldValueByLabel('Email', p.email || profileFields.email_address || '')) || '').toLowerCase().replace(/\s/g,'');
    const phone = normalizePhone(fieldValueByLabel('Phone Number', p.phone || profileFields.phone_number || ''));
    const whatsapp = normalizePhone(fieldValueByLabel('WhatsApp Number', p.whatsapp || p.whatsApp || profileFields.whatsapp_number || phone || ''));
    const contactPreference = fieldValueByLabel('Contact Preference', p.contactPreference || profileFields.contact_preference || '');
    const websiteSocialLink = fieldValueByLabel('Website / Social Link', p.websiteSocialLink || profileFields.website_social_link || '');
    const preferredLanguage = fieldValueByLabel('Preferred Language', p.preferredLanguage || profileFields.preferred_language || '');
    const bio = fieldValueByLabel('About Me / Seller Bio', fieldValueByLabel('Bio', p.bio || profileFields.about_me_seller_bio || ''));
    const province = fieldValueByLabel('Province', p.province || profileFields.province || '');
    const district = fieldValueByLabel('District', p.district || profileFields.district || '');
    const city = fieldValueByLabel('City', p.city || profileFields.city || '');
    const fullAddress = fieldValueByLabel('Full Address', p.fullAddress || p.address || profileFields.full_address || '');

    Object.assign(profileFields, {
      first_name:firstName, last_name:lastName, display_name:displayName, date_of_birth:dateOfBirth,
      gender, nationality, email_address:email, phone_number:phone, whatsapp_number:whatsapp,
      contact_preference:contactPreference, website_social_link:websiteSocialLink, preferred_language:preferredLanguage,
      about_me_seller_bio:bio, bio, province, district, city, full_address:fullAddress
    });

    const next = Object.assign({}, p, {
      firstName, lastName, displayName, username: displayName || p.username || '',
      dateOfBirth, gender, nationality,
      email, phone, whatsapp, whatsApp: whatsapp, contactPreference, websiteSocialLink, preferredLanguage,
      bio, province, district, city, fullAddress, address: fullAddress,
      profileFields,
      marketplacePreferences: collectMarketplacePreferences(),
      preferences: collectMarketplacePreferences(),
      notificationPreferences: collectNotificationPreferences(),
      securityPreferences: collectSecurityPreferences(),
      updatedAt: new Date().toISOString()
    });
    next.fullName = [next.firstName,next.lastName].filter(Boolean).join(' ') || next.fullName || next.displayName || 'AutoMart User';

    if(!next.firstName || !next.lastName) return toast('Please enter first name and last name.', 'error');
    if(next.email && !/^[a-z0-9._]+@gmail\.com$/.test(next.email)) return toast('Enter a valid Gmail address like example@gmail.com.', 'error');
    if(next.phone && !/^\d{10}$/.test(next.phone)) return toast('Phone number must contain exactly 10 digits.', 'error');
    if(next.whatsapp && !/^\d{10}$/.test(next.whatsapp)) return toast('WhatsApp number must contain exactly 10 digits.', 'error');

    commonPopup('Confirm Changes','Do you want to save these profile changes?','Confirm',()=>{
      const ok = updateCurrentUserProfile(next);
      if(ok) {
        applyProfileToPage();
        toast('Changes saved successfully');
      }
    }, '💾');
  }
  function dedupeActionButtons(){
    const seen = {};
    [...document.querySelectorAll('button')].forEach(btn => {
      if((btn.getAttribute('data-settings-action') || '').includes('remove-photo')) btn.textContent = 'Remove Photo';
      const t = btn.textContent.trim().toLowerCase().replace(/\s+/g,' ');
      if(['remove photo','remove banner'].includes(t)){
        if(seen[t]){ btn.remove(); return; }
        seen[t] = true;
      }
      if(t.includes('change banner')||t.includes('remove banner')||t.includes('remove photo')) btn.classList.add('automart-profile-mini-btn');
      if(t.includes('save profile')||t.includes('save changes')) btn.classList.add('automart-profile-save-btn-fixed');
    });
  }

  function livePanel(type){
    const key = type === 'messages' ? KEYS.messages : KEYS.notifications;
    const title = type === 'messages' ? 'Messages' : 'Notifications';
    const old = document.querySelector('.automart-live-panel.show');
    if(old && old.dataset.type === type){ old.remove(); return; }
    document.querySelectorAll('.automart-live-panel').forEach(x=>x.remove());
    const items = read(key, []);
    const panel = document.createElement('div');
    panel.className = 'automart-live-panel show'; panel.dataset.type = type;
    panel.innerHTML = '<div class="automart-live-panel-head"><strong>'+title+'</strong><button type="button" data-mark-read>Mark as read</button></div><div class="automart-live-list">'+(items.length?items.map(x=>'<div class="automart-live-item '+(x.read?'read':'')+'" data-id="'+esc(x.id)+'"><span class="automart-live-dot"></span><div><strong>'+esc(x.title||x.from||'AutoMart')+'</strong><p>'+esc(x.body||x.message||'')+'</p><small>'+esc(x.time||'Now')+'</small></div></div>').join(''):'<div class="automart-live-empty"><b>No '+title.toLowerCase()+' yet</b><span>You are all caught up.</span></div>')+'</div>';
    document.body.appendChild(panel);
    panel.querySelector('[data-mark-read]').onclick = () => commonPopup('Confirm Changes','Do you want to mark all '+title.toLowerCase()+' as read?','Confirm',()=>{
      write(key,items.map(x=>Object.assign({},x,{read:true})));
      updateBadges(); livePanel(type);
    }, '✓');
    panel.querySelectorAll('.automart-live-item').forEach(item => item.onclick = () => {
      write(key,read(key,[]).map(x=>x.id===item.dataset.id?Object.assign({},x,{read:true}):x));
      item.classList.add('read'); updateBadges();
    });
  }
  function updateBadges(){
    const n = read(KEYS.notifications,[]).filter(x=>!x.read).length;
    const m = read(KEYS.messages,[]).filter(x=>!x.read).length;
    document.querySelectorAll('[data-notification-count]').forEach(e=>{e.textContent=n;e.classList.toggle('is-zero',n===0);});
    document.querySelectorAll('[data-message-count]').forEach(e=>{e.textContent=m;e.classList.toggle('is-zero',m===0);});
  }
  function wire(){
    ensureStyles();
    sanitizeHugeMediaFromSettings();
    dedupeActionButtons();
    applyProfileToPage();
    updateBadges();
    createProfileFromSignupIfNeeded();

    document.addEventListener('click', e => {
      const live = e.target.closest('[data-live-panel-toggle]');
      if(live){ e.preventDefault(); e.stopImmediatePropagation(); livePanel(live.dataset.livePanelToggle || 'notifications'); return; }
      if(!e.target.closest('.automart-live-panel') && !e.target.closest('[data-live-panel-toggle]')) document.querySelectorAll('.automart-live-panel').forEach(x=>x.remove());

      const btn = e.target.closest('button,.avatar-upload,[data-settings-action]');
      if(!btn) return;
      const action = (btn.getAttribute('data-settings-action') || '').toLowerCase();
      const text = (btn.textContent || btn.getAttribute('title') || btn.getAttribute('aria-label') || '').trim().toLowerCase();
      if(btn.classList.contains('avatar-upload') || action.includes('change-photo') || text.includes('change photo') || text.includes('upload photo')){ e.preventDefault(); e.stopImmediatePropagation(); changePhoto(); return; }
      if(action.includes('remove-photo') || text.includes('remove photo')){ e.preventDefault(); e.stopImmediatePropagation(); removePhoto(); return; }
      if(action.includes('change-banner') || text.includes('change banner')){ e.preventDefault(); e.stopImmediatePropagation(); changeBanner(); return; }
      if(action.includes('remove-banner') || text.includes('remove banner')){ e.preventDefault(); e.stopImmediatePropagation(); removeBanner(); return; }
      if(action.includes('save') || text.includes('save profile') || text.includes('save changes') || text.includes('save preferences')){ e.preventDefault(); e.stopImmediatePropagation(); saveVisibleProfileForm(); return; }
    }, true);
  }

  
  function splitAutoName(name){
    name = String(name || '').trim().replace(/\s+/g,' ');
    if(!name) return {firstName:'', lastName:''};
    const parts = name.split(' ');
    return {firstName:parts.shift() || '', lastName:parts.join(' ') || ''};
  }
  function todayAutoLabel(){
    return new Date().toLocaleDateString('en-GB', {year:'numeric', month:'long', day:'numeric'});
  }
  function currentHeaderProfile(){
    const username = (document.getElementById('globalProfileName')?.textContent || '').trim();
    const roleText = getRole();
    const role = roleText.includes('admin') ? 'admin' : roleText.includes('buyer') ? 'buyer' : 'seller';
    const names = splitAutoName(username);
    return {
      profileId:'profile_' + role + '_' + (username || 'automart_user').replace(/[^a-z0-9_-]/gi,'_').toLowerCase(),
      accountId: username,
      username,
      role,
      firstName:names.firstName,
      lastName:names.lastName,
      fullName:username,
      displayName:username,
      email:'',
      phone:'',
      nic:'',
      memberSince:todayAutoLabel(),
      memberSinceIso:new Date().toISOString(),
      status:'active',
      profileStatus:'Active',
      verifiedStatus:role === 'admin' ? 'Admin Verified' : role === 'seller' ? 'Pending Verification' : 'Basic Verified',
      verified:role !== 'seller',
      profilePhotoRemoved:true,
      photoRemoved:true,
      photoRef:'',
      profilePhotoRef:'',
      bannerRef:'',
      bannerImageRef:'',
      profilePhotoVersion:Date.now(),
      profileFields:{
        first_name:names.firstName,
        last_name:names.lastName,
        display_name:username,
        role,
        member_since:todayAutoLabel(),
        profile_status:'Active',
        verified_status:role === 'admin' ? 'Admin Verified' : role === 'seller' ? 'Pending Verification' : 'Basic Verified'
      },
      marketplacePreferences:{bodyTypes:[],minimumBudget:'',maximumBudget:'',preferredFuelType:'',transmission:'',preferredMake:'',maxMileage:'',inspectionCity:'',toggles:{}},
      notificationPreferences:{},
      securityPreferences:{twoFactorEnabled:false},
      autoCreatedFromSignup:true,
      autoCreatedAt:new Date().toISOString()
    };
  }
  function completeAutoProfile(raw){
    const header = currentHeaderProfile();
    const p = Object.assign({}, header, raw || {});
    p.role = String(p.role || header.role || 'buyer').toLowerCase();
    if(!['buyer','seller','admin'].includes(p.role)) p.role = header.role || 'buyer';
    p.memberSince = p.memberSince || todayAutoLabel();
    p.memberSinceIso = p.memberSinceIso || new Date().toISOString();
    p.status = p.status || 'active';
    p.profileStatus = p.profileStatus || 'Active';
    p.verifiedStatus = p.verifiedStatus || (p.role === 'admin' ? 'Admin Verified' : p.role === 'seller' ? 'Pending Verification' : 'Basic Verified');
    p.verified = p.verified !== undefined ? p.verified : p.role !== 'seller';
    p.bio = p.bio || '';
    p.province = p.province || '';
    p.district = p.district || '';
    p.city = p.city || p.shopLocation || '';
    p.fullAddress = p.fullAddress || '';
    p.photoRemoved = true;
    p.profilePhotoRemoved = true;
    p.photoRef = '';
    p.profilePhotoRef = '';
    p.bannerRef = p.bannerRef || '';
    p.bannerImageRef = p.bannerImageRef || p.bannerRef || '';
    const names = splitAutoName(p.fullName || p.displayName || p.username);
    p.firstName = p.firstName || names.firstName;
    p.lastName = p.lastName || names.lastName;
    p.fullName = p.fullName || [p.firstName,p.lastName].filter(Boolean).join(' ') || p.displayName || p.username || 'AutoMart User';
    p.displayName = p.displayName || p.username || p.fullName;
    p.accountId = p.accountId || p.username || p.email || p.fullName;
    p.profileId = p.profileId || ('profile_' + p.role + '_' + String(p.accountId).replace(/[^a-z0-9_-]/gi,'_').toLowerCase());
    p.profileFields = Object.assign({
      first_name:p.firstName || '',
      last_name:p.lastName || '',
      display_name:p.displayName || '',
      email_address:p.email || '',
      phone_number:p.phone || '',
      nic:p.nic || p.sellerNic || '',
      role:p.role,
      member_since:p.memberSince,
      profile_status:p.profileStatus,
      verified_status:p.verifiedStatus,
      business_seller_name:p.businessName || '',
      shop_location:p.shopLocation || '',
      bio:p.bio || '',
      province:p.province || '',
      district:p.district || '',
      city:p.city || '',
      full_address:p.fullAddress || ''
    }, p.profileFields || {});
    p.marketplacePreferences = Object.assign({bodyTypes:[],minimumBudget:'',maximumBudget:'',preferredFuelType:'',transmission:'',preferredMake:'',maxMileage:'',inspectionCity:p.shopLocation || '',toggles:{}}, p.marketplacePreferences || {});
    p.preferences = Object.assign({}, p.marketplacePreferences);
    p.notificationPreferences = Object.assign({}, p.notificationPreferences || {});
    p.securityPreferences = Object.assign({twoFactorEnabled:false}, p.securityPreferences || {});
    return p;
  }
  function hasExistingProfileFor(profile){
    const existing = getCurrentProfile();
    if(!existing) return false;
    if(existing.accountId && profile.accountId && String(existing.accountId).toLowerCase() === String(profile.accountId).toLowerCase()) return true;
    if(existing.email && profile.email && String(existing.email).toLowerCase() === String(profile.email).toLowerCase()) return true;
    if(existing.username && profile.username && String(existing.username).toLowerCase() === String(profile.username).toLowerCase()) return true;
    return false;
  }
  function createProfileFromSignupIfNeeded(){
    const params = new URLSearchParams(location.search);
    const signupSuccess = params.has('signupSuccess') || (params.has('msg') && /account created/i.test(params.get('msg') || '')) || (params.get('mode') === 'signup');
    if(!signupSuccess) return;

    let pending = null;
    try { pending = JSON.parse(localStorage.getItem('automartPendingSignupProfileV1') || 'null'); } catch { pending = null; }
    if(!pending) {
      const roleParam = (params.get('role') || params.get('signupSuccess') || '').toLowerCase();
      pending = currentHeaderProfile();
      if(['buyer','seller','admin'].includes(roleParam)) pending.role = roleParam;
    }

    const profile = completeAutoProfile(pending);
    const existing = getCurrentProfile();
    const already = hasExistingProfileFor(profile) && (existing.autoCreatedFromSignup || existing.profileId);
    if(!already) {
      updateCurrentUserProfile(profile, true);
      localStorage.setItem('automartActiveRoleV1', profile.role);
      if(profile.role === 'buyer') localStorage.setItem('automartBuyerProfileCreatedV1','true');
      if(profile.role === 'seller') localStorage.setItem('automartSellerProfileCreatedV1','true');
      if(profile.role === 'admin') localStorage.setItem('automartAdminProfileCreatedV1','true');
      toast('Account created successfully. Your profile has been created automatically.');
    } else {
      const merged = Object.assign({}, existing, profile, {profileFields:Object.assign({}, existing.profileFields || {}, profile.profileFields || {})});
      updateCurrentUserProfile(merged);
    }
    localStorage.removeItem('automartPendingSignupProfileV1');
    applyProfileToPage();
  }

  window.AutoMartProfileStore = {
    getCurrentUserProfile: getCurrentProfile,
    updateCurrentUserProfile,
    updateCurrentUserProfilePhoto,
    removeCurrentUserProfilePhoto,
    updateCurrentUserBanner,
    removeCurrentUserBanner,
    subscribeToProfileChanges,
    applyProfileToPage,
    changePhoto,
    removePhoto,
    changeBanner,
    removeBanner,
    hardClearPhotoFromAllStores,
    createProfileFromSignupIfNeeded
  };

  
  // One-time cleanup: remove old previously-added profile photos from all old AutoMart keys.
  try {
    if (!localStorage.getItem('automartProfilePhotoInitialClearV5')) {
      hardClearPhotoFromAllStores();
      localStorage.setItem('automartProfilePhotoInitialClearV5', 'done');
      applyProfileToPage();
    }
  } catch(e) { console.warn('AutoMart initial photo cleanup failed', e); }

  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wire); else wire();
  subscribeToProfileChanges();
  window.addEventListener('storage', e => {
    if([KEYS.notifications,KEYS.messages].includes(e.key)) updateBadges();
  });
})();
