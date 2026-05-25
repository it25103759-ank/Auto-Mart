
(function(){
  function qs(sel, root){ return (root || document).querySelector(sel); }
  function qsa(sel, root){ return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
  function pretty(role){ return role === 'seller' ? 'Seller' : 'Buyer'; }

  qsa('[data-toggle-password]').forEach(function(btn){
    btn.addEventListener('click', function(){
      var input = document.getElementById(btn.getAttribute('data-toggle-password'));
      if (!input) return;
      input.type = input.type === 'password' ? 'text' : 'password';
    });
  });

  function cleanEmail(input){ input.value = (input.value || '').toLowerCase().replace(/[#%]/g, ''); }
  function cleanPhone(input){ input.value = (input.value || '').replace(/\D/g, '').slice(0, 10); }

  qsa('input[type="email"], input[name*="email" i]').forEach(function(input){
    cleanEmail(input);
    input.addEventListener('input', function(){ cleanEmail(input); });
    input.addEventListener('blur', function(){ cleanEmail(input); });
  });
  qsa('input[name*="phone" i], [data-phone-input]').forEach(function(input){
    input.setAttribute('maxlength', '10');
    input.setAttribute('inputmode', 'numeric');
    cleanPhone(input);
    input.addEventListener('input', function(){ cleanPhone(input); });
  });

  var picker = qs('[data-role-picker]');
  var form = qs('[data-signup-form]');
  var roleInput = qs('[data-role-form-input]');
  var gmailLink = qs('[data-gmail-role-link]');
  var gmailLabel = qs('[data-gmail-role-label]');
  var submitBtn = qs('[data-signup-submit]');

  function setRole(role){
    role = role === 'seller' ? 'seller' : 'buyer';
    if (roleInput) roleInput.value = role;
    qsa('[data-role-option]').forEach(function(btn){ btn.classList.toggle('active', btn.getAttribute('data-role-option') === role); });
    qsa('[data-role-field]').forEach(function(field){
      var show = field.getAttribute('data-role-field') === role;
      field.classList.toggle('active', show);
      qsa('[data-role-required]', field).forEach(function(input){ input.required = show; if(!show){ input.classList.remove('is-invalid'); var err = field.querySelector('.field-error'); if(err) err.textContent=''; } });
    });
    qsa('[data-social-role-link]').forEach(function(link){
      var provider = link.getAttribute('data-provider') || 'gmail';
      link.setAttribute('href', '/social-auth?provider=' + encodeURIComponent(provider) + '&mode=signup&role=' + role);
    });
    if (gmailLink) gmailLink.setAttribute('href', '/social-auth?provider=gmail&mode=signup&role=' + role);
    if (gmailLabel) gmailLabel.textContent = 'Continue as ' + pretty(role);
    qsa('[data-gmail-role-label]').forEach(function(label){ label.textContent = 'Continue as ' + pretty(role); });
    if (submitBtn) submitBtn.textContent = role === 'seller' ? 'Create seller account' : 'Create buyer account';
  }
  if (picker) {
    qsa('[data-role-option]', picker).forEach(function(btn){ btn.addEventListener('click', function(){ setRole(btn.getAttribute('data-role-option')); }); });
    setRole(roleInput && roleInput.value ? roleInput.value : 'buyer');
  }

  function setError(input, message){
    var fg = input.closest('.fg');
    var err = fg ? fg.querySelector('.field-error') : null;
    input.classList.toggle('is-invalid', Boolean(message));
    if (err) err.textContent = message || '';
  }
  function visible(input){
    var roleField = input.closest('[data-role-field]');
    return !roleField || roleField.classList.contains('active');
  }
  function validate(input){
    if (!visible(input)) { setError(input, ''); return true; }
    if (input.matches('[data-lowercase-email], input[type="email"]')) {
      cleanEmail(input);
      if (!input.value.trim()) { setError(input, 'Email address is required.'); return false; }
      if (/[#%]/.test(input.value)) { setError(input, 'Email cannot include # or %.'); return false; }
      if (!/^[a-z0-9._]+@gmail\.com$/.test(input.value)) { setError(input, 'Enter a valid Gmail address like example@gmail.com.'); return false; }
    } else if (input.matches('[data-phone-input], input[name*="phone" i]')) {
      cleanPhone(input);
      if (!input.value.trim()) { setError(input, 'Phone number is required.'); return false; }
      if (!/^\d{10}$/.test(input.value)) { setError(input, 'Phone number must contain exactly 10 digits.'); return false; }
    } else if (input.name === 'password') {
      if (!input.value) { setError(input, 'Password is required.'); return false; }
      if (input.value.length < 8 || !/[A-Z]/.test(input.value) || !/[a-z]/.test(input.value) || !/[0-9]/.test(input.value)) { setError(input, 'Use at least 8 characters with upper, lower, and a number.'); return false; }
    } else if (input.name === 'confirmPassword') {
      var password = form ? qs('input[name="password"]', form) : null;
      if (!input.value) { setError(input, 'Confirm your password.'); return false; }
      if (password && input.value !== password.value) { setError(input, 'Password and confirm password must match.'); return false; }
    } else if (input.required && !input.value.trim()) {
      setError(input, 'This field is required.'); return false;
    }
    setError(input, '');
    return true;
  }
  function updateStrength(input){
    var wrap = input.closest('.fg');
    if (!wrap) return;
    var bars = qsa('.pw-bar', wrap);
    var label = qs('[data-pw-label]', wrap);
    var v = input.value || '';
    var score = 0;
    if (v.length >= 8) score++;
    if (/[A-Z]/.test(v) && /[0-9]/.test(v)) score++;
    if (/[^A-Za-z0-9]/.test(v)) score++;
    var classes = ['weak','medium','strong'];
    var labels = ['Weak','Medium','Strong'];
    bars.forEach(function(bar, i){ bar.className = 'pw-bar'; if (i < score) bar.classList.add(classes[score - 1] || 'weak'); });
    if (label) label.textContent = v ? (labels[score - 1] || labels[0]) : '';
  }

  function cleanNic(input){
    input.value = (input.value || '').toUpperCase().replace(/[^0-9VX]/g,'').slice(0,12);
  }
  function validateNic(input){
    if (!visible(input)) { setError(input, ''); return true; }
    cleanNic(input);
    if (!input.value.trim()) { setError(input, 'NIC / ID number is required.'); return false; }
    if (!/^(\d{12}|\d{9}[VX])$/.test(input.value)) { setError(input, 'Use 12 digits or 9 digits followed by V/X.'); return false; }
    setError(input, '');
    return true;
  }
  qsa('[data-nic-input], input[name*="nic" i]').forEach(function(input){
    cleanNic(input);
    input.addEventListener('input', function(){ cleanNic(input); validateNic(input); });
    input.addEventListener('blur', function(){ validateNic(input); });
  });

  function splitName(name){
    name = (name || '').trim().replace(/\s+/g,' ');
    if(!name) return {firstName:'', lastName:''};
    var parts = name.split(' ');
    return {firstName: parts.shift() || '', lastName: parts.join(' ') || ''};
  }
  function todayLabel(){
    return new Date().toLocaleDateString('en-GB', {year:'numeric', month:'long', day:'numeric'});
  }
  function buildSignupProfileFromForm(){
    if(!form) return null;
    var fd = new FormData(form);
    var role = (fd.get('role') || 'buyer').toString().toLowerCase() === 'seller' ? 'seller' : 'buyer';
    var username = (fd.get('username') || '').toString().trim();
    var email = (fd.get('email') || '').toString().trim().toLowerCase();
    var phone = (fd.get('phone') || '').toString().replace(/\D/g,'').slice(0,10);
    var fullName = role === 'seller' ? (fd.get('businessName') || '').toString().trim() : (fd.get('fullName') || '').toString().trim();
    var names = splitName(fullName);
    var now = new Date().toISOString();
    return {
      profileId: 'profile_' + role + '_' + (username || email).replace(/[^a-z0-9_-]/gi,'_').toLowerCase(),
      accountId: username || email,
      username: username,
      role: role,
      firstName: names.firstName,
      lastName: names.lastName,
      fullName: fullName || username,
      displayName: username || fullName,
      email: email,
      phone: phone,
      nic: role === 'seller' ? (fd.get('sellerNic') || '').toString().trim().toUpperCase() : '',
      sellerNic: role === 'seller' ? (fd.get('sellerNic') || '').toString().trim().toUpperCase() : '',
      businessName: role === 'seller' ? (fd.get('businessName') || '').toString().trim() : '',
      shopLocation: role === 'seller' ? (fd.get('shopLocation') || '').toString().trim() : '',
      province: '',
      district: '',
      city: role === 'seller' ? (fd.get('shopLocation') || '').toString().trim() : '',
      fullAddress: '',
      bio: '',
      memberSince: todayLabel(),
      memberSinceIso: now,
      status: 'active',
      profileStatus: 'Active',
      verifiedStatus: role === 'seller' ? 'Pending Verification' : 'Basic Verified',
      verified: role === 'seller' ? false : true,
      profilePhotoRemoved: true,
      photoRemoved: true,
      photoRef: '',
      profilePhotoRef: '',
      bannerRef: '',
      bannerImageRef: '',
      profilePhotoVersion: Date.now(),
      profileFields: {
        first_name: names.firstName,
        last_name: names.lastName,
        display_name: username || fullName,
        email_address: email,
        phone_number: phone,
        nic: role === 'seller' ? (fd.get('sellerNic') || '').toString().trim().toUpperCase() : '',
        role: role,
        member_since: todayLabel(),
        profile_status: 'Active',
        verified_status: role === 'seller' ? 'Pending Verification' : 'Basic Verified',
        business_seller_name: role === 'seller' ? (fd.get('businessName') || '').toString().trim() : '',
        shop_location: role === 'seller' ? (fd.get('shopLocation') || '').toString().trim() : ''
      },
      marketplacePreferences: {
        bodyTypes: [],
        minimumBudget: '',
        maximumBudget: '',
        preferredFuelType: '',
        transmission: '',
        preferredMake: '',
        maxMileage: '',
        inspectionCity: role === 'seller' ? (fd.get('shopLocation') || '').toString().trim() : '',
        toggles: {}
      },
      notificationPreferences: {},
      securityPreferences: { twoFactorEnabled: false },
      autoCreatedFromSignup: true,
      autoCreatedAt: now
    };
  }
  function savePendingSignupProfile(){
    var profile = buildSignupProfileFromForm();
    if(profile) localStorage.setItem('automartPendingSignupProfileV1', JSON.stringify(profile));
  }

  if (form) {
    qsa('.finput', form).forEach(function(input){
      input.addEventListener('input', function(){ if (input.name === 'password') updateStrength(input); validate(input); if(input.name === 'password'){ var c = qs('input[name="confirmPassword"]', form); if(c && c.value) validate(c); } });
      input.addEventListener('blur', function(){ validate(input); });
    });
    form.addEventListener('submit', function(e){
      var ok = true;
      qsa('.finput', form).forEach(function(input){ if (!validate(input)) ok = false; });
      var terms = qs('input[name="termsAccepted"]', form);
      if (terms && !terms.checked) { ok = false; terms.closest('.chk-row').classList.add('is-invalid'); }
      else if (terms) terms.closest('.chk-row').classList.remove('is-invalid');
      qsa('[data-nic-input], input[name*="nic" i]', form).forEach(function(input){ if (!validateNic(input)) ok = false; });
      if (!ok) { e.preventDefault(); var first = qs('.is-invalid', form); if (first && first.focus) first.focus(); }
      else { savePendingSignupProfile(); }
    });
  }
})();


// Admin signup polish: lowercase email, 10 digit phone, matching password.
document.addEventListener('DOMContentLoaded', () => {
  const adminSignup = document.querySelector('[data-admin-signup-form]');
  if (!adminSignup) return;
  const email = adminSignup.querySelector('[data-email-normalize]');
  const phone = adminSignup.querySelector('[data-phone-field]');
  const pass = adminSignup.querySelector('input[name="password"]');
  const confirm = adminSignup.querySelector('input[name="confirmPassword"]');
  if (email) email.addEventListener('input', () => { email.value = email.value.toLowerCase().replace(/[ #%]/g, ch => ch === ' ' ? '' : ''); });
  if (phone) phone.addEventListener('input', () => { phone.value = phone.value.replace(/\D/g, '').slice(0, 10); });

  function adminTodayLabel(){ return new Date().toLocaleDateString('en-GB', {year:'numeric', month:'long', day:'numeric'}); }
  function adminSplitName(name){
    name=(name||'').trim().replace(/\s+/g,' ');
    if(!name) return {firstName:'',lastName:''};
    var parts=name.split(' ');
    return {firstName:parts.shift()||'', lastName:parts.join(' ')||''};
  }
  function savePendingAdminSignupProfile(){
    var fd = new FormData(adminSignup);
    var fullName = (fd.get('fullName') || '').toString().trim();
    var names = adminSplitName(fullName);
    var username = (fd.get('username') || '').toString().trim();
    var email = (fd.get('email') || '').toString().trim().toLowerCase();
    var phone = (fd.get('phone') || '').toString().replace(/\D/g,'').slice(0,10);
    var now = new Date().toISOString();
    localStorage.setItem('automartPendingSignupProfileV1', JSON.stringify({
      profileId:'profile_admin_' + (username || email).replace(/[^a-z0-9_-]/gi,'_').toLowerCase(),
      accountId: username || email,
      username: username,
      role:'admin',
      firstName:names.firstName,
      lastName:names.lastName,
      fullName:fullName || username,
      displayName:username || fullName,
      email:email,
      phone:phone,
      adminCode:(fd.get('adminCode') || '').toString().trim(),
      nic:'',
      memberSince:adminTodayLabel(),
      memberSinceIso:now,
      status:'active',
      profileStatus:'Active',
      verifiedStatus:'Admin Verified',
      verified:true,
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
        display_name:username || fullName,
        email_address:email,
        phone_number:phone,
        role:'admin',
        member_since:adminTodayLabel(),
        profile_status:'Active',
        verified_status:'Admin Verified'
      },
      marketplacePreferences:{bodyTypes:[],minimumBudget:'',maximumBudget:'',preferredFuelType:'',transmission:'',preferredMake:'',maxMileage:'',inspectionCity:'',toggles:{}},
      notificationPreferences:{},
      securityPreferences:{twoFactorEnabled:false},
      autoCreatedFromSignup:true,
      autoCreatedAt:now
    }));
  }

  adminSignup.addEventListener('submit', (event) => {
    if (email) email.value = email.value.toLowerCase().replace(/\s/g,'').replace(/[^a-z0-9._@]/g,'');
    if (email && !/^[a-z0-9._]+@gmail\.com$/.test(email.value)) {
      event.preventDefault();
      if(window.AutoMartToast) window.AutoMartToast('Use a valid Gmail address like example@gmail.com.','error');
      email.focus();
      return;
    }
    if (phone) phone.value = phone.value.replace(/\D/g,'').slice(0,10);
    if (phone && !/^\d{10}$/.test(phone.value)) {
      event.preventDefault();
      if(window.AutoMartToast) window.AutoMartToast('Phone number must contain exactly 10 digits.','error');
      phone.focus();
      return;
    }
    if (pass && confirm && pass.value !== confirm.value) {
      event.preventDefault();
      if(window.AutoMartToast) window.AutoMartToast('Password and confirm password must match.','error');
      confirm.focus();
      return;
    }
    savePendingAdminSignupProfile();
  });
});


/* 2026-05 universal Gmail + 10-digit mobile validation */
(function(){
  function isEmailField(input){ return input && !input.hasAttribute('data-login-email') && (input.type === 'email' || /email/i.test(input.name || '') || /email/i.test(input.id || '')); }
  function isPhoneField(input){ return input && (/phone|mobile|whatsapp|contactPhone/i.test(input.name || '') || /phone|mobile|whatsapp/i.test(input.id || '') || input.hasAttribute('data-phone-input')); }
  function getError(input){
    var wrap = input.closest('.fg,.form-group,.field,.input-group,.control') || input.parentElement;
    var err = wrap ? wrap.querySelector('.field-error,.validation-error,.form-error,.input-error') : null;
    if (!err && wrap) { err = document.createElement('small'); err.className = 'field-error validation-error'; err.style.display='block'; err.style.minHeight='16px'; err.style.marginTop='5px'; err.style.fontSize='11px'; wrap.appendChild(err); }
    return err;
  }
  function mark(input, ok, message){
    input.classList.toggle('is-invalid', !ok);
    input.classList.toggle('is-valid', ok && !!input.value.trim());
    input.style.borderColor = !input.value.trim() ? '' : (ok ? '#7EC820' : '#e25a5a');
    var err = getError(input); if (err) { err.textContent = message || ''; err.style.color = '#ff8f8f'; }
  }
  function cleanEmail(input){ input.value = (input.value || '').toLowerCase().replace(/\s/g,'').replace(/[^a-z0-9._@]/g,''); }
  function cleanPhone(input){ input.value = (input.value || '').replace(/\D/g,'').slice(0,10); }
  function validateEmail(input){
    cleanEmail(input);
    input.placeholder = 'Enter your Gmail address';
    var v = input.value.trim();
    if (!v) { mark(input, false, 'Enter your Gmail address.'); return false; }
    if ((v.match(/@/g)||[]).length !== 1) { mark(input, false, 'Use only one @ symbol.'); return false; }
    if (!v.endsWith('@gmail.com')) { mark(input, false, 'Email must end with @gmail.com.'); return false; }
    var u = v.split('@')[0];
    if (!u) { mark(input, false, 'Enter the username before @gmail.com.'); return false; }
    if (!/^[a-z0-9._]+$/.test(u)) { mark(input, false, 'Only letters, numbers, dots, and underscores are allowed.'); return false; }
    mark(input, true, ''); return true;
  }
  function validatePhone(input){
    cleanPhone(input);
    input.placeholder = 'Enter 10-digit mobile number';
    input.maxLength = 10; input.inputMode = 'numeric';
    var v = input.value.trim();
    if (!v) { mark(input, false, 'Enter 10-digit mobile number.'); return false; }
    if (!/^\d{10}$/.test(v)) { mark(input, false, 'Mobile number must contain exactly 10 digits.'); return false; }
    mark(input, true, ''); return true;
  }
  function wire(root){
    (root || document).querySelectorAll('input').forEach(function(input){
      if (isEmailField(input)) { input.placeholder = 'Enter your Gmail address'; cleanEmail(input); input.addEventListener('input', function(){ validateEmail(input); }); input.addEventListener('blur', function(){ validateEmail(input); }); }
      if (isPhoneField(input)) { input.placeholder = 'Enter 10-digit mobile number'; input.maxLength = 10; input.inputMode = 'numeric'; cleanPhone(input); input.addEventListener('input', function(){ validatePhone(input); }); input.addEventListener('blur', function(){ validatePhone(input); }); }
    });
    (root || document).querySelectorAll('form').forEach(function(form){
      if (form.dataset.universalValidationWired === 'true') return;
      form.dataset.universalValidationWired = 'true';
      form.addEventListener('submit', function(e){
        var ok = true;
        form.querySelectorAll('input').forEach(function(input){
          if (isEmailField(input) && input.value.trim() && !validateEmail(input)) ok = false;
          if (isPhoneField(input) && !validatePhone(input)) ok = false;
        });
        if (!ok) { e.preventDefault(); var first = form.querySelector('.is-invalid'); if (first) first.focus(); }
      });
    });
  }
  document.addEventListener('DOMContentLoaded', function(){ wire(document); });
  window.AutoMartWireValidation = wire;
})();


// AutoMart Sri Lankan NIC realtime validation
(function(){
  function validNic(v){v=String(v||'').trim().toUpperCase();return /^\d{12}$/.test(v)||/^\d{9}[VX]$/.test(v);}
  function setState(input, ok, msg){
    const group=input.closest('.fg')||input.parentElement;
    const err=group&&group.querySelector('.field-error');
    input.classList.toggle('is-valid', ok);
    input.classList.toggle('is-invalid', !ok);
    if(err) err.textContent=ok?'':msg;
  }
  document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll('[data-nic-input]').forEach(input=>{
      input.addEventListener('input',()=>{
        input.value=input.value.toUpperCase().replace(/[^0-9VX]/g,'').slice(0,12);
        setState(input, validNic(input.value), 'Use 12 digits or 9 digits followed by V/X.');
      });
      const form=input.closest('form');
      if(form&&!form.dataset.nicGuard){
        form.dataset.nicGuard='1';
        form.addEventListener('submit',e=>{
          const role=form.querySelector('[name=role]')?.value||'buyer';
          const nic=form.querySelector('[data-nic-input]');
          if(role==='seller' && nic && !validNic(nic.value)){
            e.preventDefault();
            setState(nic,false,'Use a valid Sri Lankan NIC before signing up.');
            nic.focus();
          }
        });
      }
    });
  });
})();



/* 2026-05 Admin/User login Gmail input unlock fix */
(function(){
  function qsa(sel, root){ return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  function isLoginEmail(input){
    return input && (input.hasAttribute('data-login-email') || input.id === 'adminEmail');
  }

  function errorNode(input){
    var wrap = input.closest('.admin-field,.fg,.form-group,.field') || input.parentElement;
    if(!wrap) return null;
    var err = wrap.querySelector('.field-error,.validation-error,.login-email-error,.admin-login-email-error');
    if(!err){
      err = document.createElement('small');
      err.className = 'field-error login-email-error admin-login-email-error';
      err.style.display = 'block';
      err.style.minHeight = '16px';
      err.style.marginTop = '6px';
      err.style.fontSize = '11px';
      err.style.color = '#ff8f8f';
      wrap.appendChild(err);
    }
    return err;
  }

  function mark(input, message, valid){
    var err = errorNode(input);
    input.classList.toggle('is-invalid', Boolean(message));
    input.classList.toggle('is-valid', Boolean(valid));
    input.style.borderColor = !input.value.trim() ? '' : (message ? 'rgba(226,90,90,.72)' : (valid ? '#7EC820' : ''));
    if(err) err.textContent = message || '';
  }

  function normalizeEditableEmail(input, keepCaret){
    var old = input.value || '';
    var start = input.selectionStart;
    var end = input.selectionEnd;
    var next = old.toLowerCase().replace(/\s/g, '');
    if(next !== old){
      input.value = next;
      if(keepCaret && typeof start === 'number'){
        var diff = old.length - next.length;
        var pos = Math.max(0, start - diff);
        try { input.setSelectionRange(pos, Math.max(pos, (end || start) - diff)); } catch(e) {}
      }
    }
  }

  function isCompleteGmail(v){
    return /^[a-z0-9._]+@gmail\.com$/.test(String(v || '').trim().toLowerCase());
  }

  function validateLoginEmail(input, mode){
    normalizeEditableEmail(input, false);
    var v = (input.value || '').trim();
    var admin = input.getAttribute('data-login-email') === 'admin' || input.id === 'adminEmail';

    if(!v){
      mark(input, '', false);
      return !mode || mode === 'input';
    }

    // While typing, do not block partial addresses. Only show green when complete.
    if(mode === 'input'){
      if(isCompleteGmail(v)) mark(input, '', true);
      else mark(input, '', false);
      return true;
    }

    if(admin){
      if(!isCompleteGmail(v)){
        mark(input, 'Enter a valid Gmail address like example@gmail.com.', false);
        return false;
      }
      mark(input, '', true);
      return true;
    }

    // User login still allows username. If they type an email, validate it as Gmail.
    if(v.includes('@') && !isCompleteGmail(v)){
      mark(input, 'Use a valid Gmail address like example@gmail.com, or enter your username.', false);
      return false;
    }

    mark(input, '', v.includes('@') && isCompleteGmail(v));
    return true;
  }

  function unlock(input){
    if(!input || input.dataset.automartEmailUnlocked === 'true') return;
    input.dataset.automartEmailUnlocked = 'true';

    input.removeAttribute('readonly');
    input.removeAttribute('disabled');
    input.readOnly = false;
    input.disabled = false;
    input.autocomplete = 'off';
    input.autocapitalize = 'none';
    input.spellcheck = false;
    input.style.pointerEvents = 'auto';
    input.style.userSelect = 'text';

    // Keep only the server-rendered value from first page load. Never restore it after user edits.
    input.dataset.initialLoginValue = input.value || '';

    var composing = false;
    input.addEventListener('compositionstart', function(){ composing = true; });
    input.addEventListener('compositionend', function(){ composing = false; normalizeEditableEmail(input, true); validateLoginEmail(input, 'input'); });

    input.addEventListener('keydown', function(){
      // Ensure backspace/delete/select-all can work even if another script toggles attributes.
      input.removeAttribute('readonly');
      input.removeAttribute('disabled');
      input.readOnly = false;
      input.disabled = false;
    }, true);

    input.addEventListener('input', function(){
      if(composing) return;
      normalizeEditableEmail(input, true);
      validateLoginEmail(input, 'input');
    }, true);

    input.addEventListener('paste', function(){
      setTimeout(function(){
        normalizeEditableEmail(input, true);
        validateLoginEmail(input, 'input');
      }, 0);
    }, true);

    input.addEventListener('blur', function(){
      validateLoginEmail(input, 'blur');
    });

    // If any older script/browser autofill locks it later, unlock again.
    var observer = new MutationObserver(function(){
      if(input.hasAttribute('readonly') || input.hasAttribute('disabled') || input.readOnly || input.disabled){
        input.removeAttribute('readonly');
        input.removeAttribute('disabled');
        input.readOnly = false;
        input.disabled = false;
      }
    });
    observer.observe(input, {attributes:true, attributeFilter:['readonly','disabled','value']});
  }

  function wire(){
    qsa('[data-login-email], #adminEmail').forEach(unlock);

    qsa('form').forEach(function(form){
      if(form.dataset.automartLoginEmailSubmitFixed === 'true') return;
      var input = form.querySelector('[data-login-email], #adminEmail');
      if(!input) return;
      form.dataset.automartLoginEmailSubmitFixed = 'true';

      form.addEventListener('submit', function(e){
        if(!validateLoginEmail(input, 'submit')){
          e.preventDefault();
          e.stopImmediatePropagation();
          input.focus();
          return false;
        }
      }, true);
    });
  }

  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wire);
  else wire();

  // Run again after autofill/layout scripts complete.
  setTimeout(wire, 50);
  setTimeout(wire, 500);
})();


/* 2026-05 Admin/User login — hard-clear admin email field on load */
(function(){
  // Empty: we rely on data-clear-on-load attribute, not hardcoded emails.
  // The root cause (applyProfileToPage injecting user email) is fixed in
  // automart-shared-profile.js. This block just keeps the field clear on load.
  const STUCK_EMAILS = [];

  function qsa(sel, root){ return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  function toast(msg,type){ if(window.AutoMartToast) window.AutoMartToast(msg,type||'error'); else console.log(msg); }

  function normalize(v){
    return String(v || '').toLowerCase().replace(/\s/g,'');
  }

  function validAdminGmail(v){
    return /^[a-z0-9._]+@gmail\.com$/.test(normalize(v));
  }

  function unlockInput(input){
    if(!input) return;
    input.removeAttribute('readonly');
    input.removeAttribute('disabled');
    input.readOnly = false;
    input.disabled = false;
    input.autocomplete = 'new-password';
    input.autocapitalize = 'none';
    input.spellcheck = false;
    input.style.pointerEvents = 'auto';
    input.style.userSelect = 'text';
    input.style.webkitUserSelect = 'text';
  }

  function clearIfOld(input){
    if(!input) return;
    const v = normalize(input.value);
    if(input.hasAttribute('data-clear-on-load') || STUCK_EMAILS.includes(v)){
      input.value = '';
      input.defaultValue = '';
      input.removeAttribute('value');
    }
  }

  function syncHidden(input){
    if(!input) return;
    const form = input.closest('form');
    if(!form) return;
    let hidden = form.querySelector('input[type="hidden"][name="username"]');
    if(!hidden){
      hidden = document.createElement('input');
      hidden.type = 'hidden';
      hidden.name = 'username';
      form.appendChild(hidden);
    }
    hidden.value = normalize(input.value);
  }

  function attach(input){
    if(!input || input.dataset.automartHardEmailClear === 'true') return;
    input.dataset.automartHardEmailClear = 'true';
    unlockInput(input);
    clearIfOld(input);
    syncHidden(input);

    ['keydown','keyup','mousedown','focus','click','input','paste','cut'].forEach(evt=>{
      input.addEventListener(evt,()=>{
        unlockInput(input);
        if(evt === 'input' || evt === 'paste' || evt === 'cut' || evt === 'keyup'){
          setTimeout(()=>{
            const pos = input.selectionStart;
            const old = input.value;
            const next = normalize(old);
            if(old !== next){
              input.value = next;
              try{ input.setSelectionRange(Math.max(0,pos-(old.length-next.length)), Math.max(0,pos-(old.length-next.length))); }catch(e){}
            }
            syncHidden(input);
          },0);
        } else {
          syncHidden(input);
        }
      }, true);
    });

    const observer = new MutationObserver(()=>{
      unlockInput(input);
      if(input.hasAttribute('value') && STUCK_EMAILS.includes(normalize(input.getAttribute('value')))){
        input.removeAttribute('value');
        input.value = '';
      }
      syncHidden(input);
    });
    observer.observe(input,{attributes:true,attributeFilter:['readonly','disabled','value']});

    const form = input.closest('form');
    if(form && form.dataset.automartHardEmailSubmit !== 'true'){
      form.dataset.automartHardEmailSubmit = 'true';
      form.addEventListener('submit', e=>{
        unlockInput(input);
        input.value = normalize(input.value);
        syncHidden(input);
        const admin = input.getAttribute('data-login-email') === 'admin' || input.id === 'adminEmail';
        if(admin && !validAdminGmail(input.value)){
          e.preventDefault();
          e.stopImmediatePropagation();
          toast('Enter a valid Gmail address like example@gmail.com.','error');
          input.focus();
          return false;
        }
      }, true);
    }
  }

  function wire(){
    qsa('#adminEmail,[data-login-email],#userLoginVisible').forEach(attach);
    qsa('#adminUsernameHidden,#userLoginHidden').forEach(h=>{ if(h.value && STUCK_EMAILS.includes(normalize(h.value))) h.value=''; });
  }

  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wire);
  else wire();

  // Run again after autofill and any late profile-apply scripts.
  // 5000ms covers scripts that run very late after DOMContentLoaded.
  [50, 250, 700, 1500, 3000, 5000].forEach(t => setTimeout(wire, t));
})();
