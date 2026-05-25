document.getElementById('year') && (document.getElementById('year').textContent = new Date().getFullYear());

(function () {
  const loader = document.getElementById('site-loader');
  const loaderText = document.getElementById('loader-text');
  const body = document.body;

  function showLoader(message) {
    if (!loader || !body) return;
    if (message && loaderText) loaderText.textContent = message;
    loader.classList.remove('is-hidden');
    body.classList.add('page-loading');
  }

  function hideLoader() {
    if (!loader || !body) return;
    loader.classList.add('is-hidden');
    body.classList.remove('page-loading');
  }

  document.addEventListener('DOMContentLoaded', function () {
    setTimeout(hideLoader, 180);
  });
  window.addEventListener('load', function () { setTimeout(hideLoader, 80); });

  document.querySelectorAll('.auth-form').forEach(function (form) {
    form.addEventListener('submit', function () {
      showLoader('Loading website...');
    });
  });

  document.querySelectorAll('a[href][data-show-loader]').forEach(function (link) {
    const href = link.getAttribute('href') || '';
    if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;
    link.addEventListener('click', function () {
      if (href.startsWith('/')) showLoader('Loading...');
    });
  });
})();


(function () {
  const reveals = document.querySelectorAll('.reveal-up');
  if (!reveals.length || !('IntersectionObserver' in window)) return;
  const observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (entry.isIntersecting) {
        entry.target.style.animationPlayState = 'running';
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.18 });
  reveals.forEach(function (el) {
    el.style.animationPlayState = 'paused';
    observer.observe(el);
  });
})();


(function () {
  document.querySelectorAll('.animated-card').forEach(function (card, index) {
    card.style.animationDelay = (index * 0.06) + 's';
  });
})();

(function () {
  const premiumOptions = document.querySelectorAll('.premium-option');
  premiumOptions.forEach(function (card) {
    const input = card.querySelector('input[type="checkbox"]');
    const status = card.querySelector('.premium-status');
    function sync() {
      const active = !!(input && input.checked);
      card.classList.toggle('is-active', active);
      if (status) status.textContent = active ? 'Enabled' : 'Tap to enable';
    }
    sync();
    if (input) input.addEventListener('change', sync);
  });
})();

(function () {
  const hero = document.querySelector('.settings-hero');
  if (!hero) return;
  hero.addEventListener('mousemove', function (event) {
    const rect = hero.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width - 0.5) * 10;
    const y = ((event.clientY - rect.top) / rect.height - 0.5) * 10;
    hero.style.transform = 'rotateX(' + (-y * 0.2) + 'deg) rotateY(' + (x * 0.2) + 'deg)';
  });
  hero.addEventListener('mouseleave', function () {
    hero.style.transform = '';
  });
})();

(function () {
  const navLinks = document.querySelectorAll('.settings-nav-link');
  if (!navLinks.length) return;
  const sections = Array.from(document.querySelectorAll('.settings-section'));

  function setActive(id) {
    navLinks.forEach(function (link) {
      link.classList.toggle('is-active', link.getAttribute('href') === '#' + id);
    });
  }

  navLinks.forEach(function (link) {
    link.addEventListener('click', function () {
      const id = (link.getAttribute('href') || '').replace('#', '');
      if (id) setActive(id);
    });
  });

  if ('IntersectionObserver' in window && sections.length) {
    const observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          setActive(entry.target.id);
        }
      });
    }, { threshold: 0.35 });
    sections.forEach(function (section) { observer.observe(section); });
  }
})();

(function () {
  document.querySelectorAll('.settings-mini-card').forEach(function (card) {
    card.addEventListener('click', function (event) {
      const btn = event.target.closest('.mini-action');
      if (!btn) return;
      const title = card.querySelector('.mini-card-title');
      if (!title) return;
      const action = btn.getAttribute('data-action');
      if (action === 'edit') {
        const updated = window.prompt('Update item', title.textContent.trim());
        if (updated && updated.trim()) title.textContent = updated.trim();
      }
      if (action === 'remove') {
        card.style.opacity = '0';
        card.style.transform = 'translateY(12px)';
        setTimeout(function () { card.remove(); }, 220);
      }
    });
  });
})();


(function () {
  const forms = document.querySelectorAll('.settings-action-card');
  forms.forEach(function (card, index) {
    card.style.animationDelay = (0.08 * index) + 's';
    card.classList.add('reveal-up');
  });
})();

(function () {
  document.querySelectorAll('.settings-section, .settings-summary-band, .settings-hero').forEach(function (panel) {
    panel.addEventListener('mousemove', function (event) {
      const rect = panel.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width;
      const y = (event.clientY - rect.top) / rect.height;
      panel.style.setProperty('--mx', (x * 100).toFixed(1) + '%');
      panel.style.setProperty('--my', (y * 100).toFixed(1) + '%');
      panel.style.backgroundImage = 'radial-gradient(circle at ' + (x * 100).toFixed(1) + '% ' + (y * 100).toFixed(1) + '%, rgba(216,255,118,0.10), rgba(216,255,118,0) 32%)';
    });
    panel.addEventListener('mouseleave', function () {
      panel.style.backgroundImage = '';
    });
  });
})();

(function () {
  const sections = Array.from(document.querySelectorAll('.settings-section'));
  const hero = document.querySelector('.settings-hero');
  if (!sections.length || !hero) return;

  const progress = document.createElement('div');
  progress.className = 'settings-scroll-progress';
  const bar = document.createElement('span');
  progress.appendChild(bar);
  hero.appendChild(progress);

  function updateProgress() {
    const first = sections[0].offsetTop;
    const last = sections[sections.length - 1].offsetTop + sections[sections.length - 1].offsetHeight;
    const viewport = window.scrollY + window.innerHeight * 0.45;
    const ratio = Math.max(0, Math.min(1, (viewport - first) / Math.max(1, last - first)));
    bar.style.width = (ratio * 100).toFixed(1) + '%';
  }

  updateProgress();
  window.addEventListener('scroll', updateProgress, { passive: true });
  window.addEventListener('resize', updateProgress);
})();

(function () {
  document.querySelectorAll('.security-submit-btn').forEach(function (btn) {
    const form = btn.closest('form');
    if (!form) return;
    form.addEventListener('submit', function () {
      btn.classList.add('is-loading');
      btn.disabled = true;
      const label = btn.querySelector('.security-btn-label');
      if (label) label.textContent = 'Processing...';
    });
  });
})();

(function () {
  document.querySelectorAll('.quick-stat-card, .settings-insight-card, .summary-pill').forEach(function (card, index) {
    card.style.animationDelay = (index * 0.08) + 's';
  });
})();


(function () {
  const nativeSelects = Array.from(document.querySelectorAll('select')).filter(function (select) {
    return !select.dataset.customized && !select.multiple && !select.closest('.settings-form');
  });
  if (!nativeSelects.length) return;

  let openInstance = null;

  function findHost(wrapper) {
    return wrapper.closest('.settings-section, .subpanel, .inventory-toolbar, .hero-search-bar, .requests-table-wrap, .table-card, .split-glass-card, .panel, .profile-effect-card') || wrapper.parentElement;
  }

  function collapseHost(instance) {
    if (!instance || !instance.host) return;
    instance.host.classList.remove('select-host-expanded', 'select-host-up');
    instance.host.style.removeProperty('--select-space');
  }

  function closeOpen() {
    if (!openInstance) return;
    openInstance.wrapper.classList.remove('is-open', 'menu-up');
    openInstance.trigger.setAttribute('aria-expanded', 'false');
    openInstance.menu.style.maxHeight = '';
    collapseHost(openInstance);
    openInstance = null;
  }

  function positionMenu(instance) {
    const rect = instance.trigger.getBoundingClientRect();
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
    const desiredHeight = Math.min(instance.menu.scrollHeight || 260, 280);
    const spaceBelow = viewportHeight - rect.bottom;
    const spaceAbove = rect.top;
    const openUp = spaceBelow < Math.min(220, desiredHeight + 18) && spaceAbove > spaceBelow;
    const available = openUp ? Math.max(160, spaceAbove - 28) : Math.max(160, spaceBelow - 28);

    instance.wrapper.classList.toggle('menu-up', openUp);
    instance.host.classList.toggle('select-host-up', openUp);
    instance.menu.style.maxHeight = Math.min(desiredHeight, available) + 'px';

    if (!openUp) {
      instance.host.classList.add('select-host-expanded');
      instance.host.style.setProperty('--select-space', Math.min(desiredHeight + 28, 320) + 'px');
    } else {
      instance.host.classList.remove('select-host-expanded');
      instance.host.style.removeProperty('--select-space');
    }
  }

  function openMenu(instance) {
    if (openInstance && openInstance !== instance) closeOpen();
    instance.wrapper.classList.add('is-open');
    instance.trigger.setAttribute('aria-expanded', 'true');
    positionMenu(instance);
    openInstance = instance;
  }

  function buildOption(option, select, labelNode, wrapper) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'custom-select-option' + (option.selected ? ' is-selected' : '');
    btn.textContent = option.textContent;
    btn.dataset.value = option.value;
    btn.setAttribute('role', 'option');
    btn.setAttribute('aria-selected', option.selected ? 'true' : 'false');
    btn.addEventListener('click', function () {
      select.value = option.value;
      labelNode.textContent = option.textContent;
      wrapper.querySelectorAll('.custom-select-option').forEach(function (item) {
        const active = item.dataset.value === option.value;
        item.classList.toggle('is-selected', active);
        item.setAttribute('aria-selected', active ? 'true' : 'false');
      });
      btn.classList.add('is-pulse');
      window.setTimeout(function () { btn.classList.remove('is-pulse'); }, 360);
      select.dispatchEvent(new Event('change', { bubbles: true }));
      window.setTimeout(function () { closeOpen(); }, 120);
    });
    return btn;
  }

  nativeSelects.forEach(function (select) {
    select.dataset.customized = 'true';
    select.classList.add('native-select-hidden');

    const wrapper = document.createElement('div');
    wrapper.className = 'custom-select';
    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.className = 'custom-select-trigger';
    trigger.setAttribute('aria-haspopup', 'listbox');
    trigger.setAttribute('aria-expanded', 'false');

    const labelNode = document.createElement('span');
    labelNode.className = 'custom-select-label';
    labelNode.textContent = (select.options[select.selectedIndex] || {}).textContent || 'Select';
    const chevron = document.createElement('span');
    chevron.className = 'custom-select-chevron';
    trigger.appendChild(labelNode);
    trigger.appendChild(chevron);

    const menu = document.createElement('div');
    menu.className = 'custom-select-menu';
    menu.setAttribute('role', 'listbox');

    Array.from(select.options).forEach(function (option) {
      menu.appendChild(buildOption(option, select, labelNode, wrapper));
    });

    const instance = { wrapper: wrapper, trigger: trigger, menu: menu, host: null };

    trigger.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      instance.host = findHost(wrapper);
      if (openInstance && openInstance.wrapper === wrapper) closeOpen();
      else openMenu(instance);
    });

    trigger.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') closeOpen();
    });

    select.addEventListener('change', function () {
      const current = select.options[select.selectedIndex];
      labelNode.textContent = current ? current.textContent : 'Select';
      wrapper.querySelectorAll('.custom-select-option').forEach(function (item) {
        const active = item.dataset.value === select.value;
        item.classList.toggle('is-selected', active);
        item.setAttribute('aria-selected', active ? 'true' : 'false');
      });
      if (openInstance && openInstance.wrapper === wrapper) positionMenu(openInstance);
    });

    select.insertAdjacentElement('afterend', wrapper);
    wrapper.appendChild(trigger);
    wrapper.appendChild(menu);
  });

  document.addEventListener('click', function (event) {
    if (!openInstance) return;
    if (openInstance.trigger.contains(event.target) || openInstance.menu.contains(event.target)) return;
    closeOpen();
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') closeOpen();
  });

  window.addEventListener('resize', function () {
    if (openInstance) positionMenu(openInstance);
  });
  window.addEventListener('scroll', function () {
    if (openInstance) positionMenu(openInstance);
  }, true);
})();

(function () {
  document.querySelectorAll('[data-toggle-target]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      const target = document.getElementById(btn.getAttribute('data-toggle-target'));
      if (!target) return;
      const open = !target.classList.contains('is-open');
      target.classList.toggle('is-open', open);
      btn.classList.toggle('is-open', open);
    });
  });
})();

(function () {
  document.querySelectorAll('.spotlight-card').forEach(function (card) {
    card.addEventListener('mousemove', function (event) {
      const rect = card.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width) * 100;
      const y = ((event.clientY - rect.top) / rect.height) * 100;
      card.style.setProperty('--sx', x.toFixed(1) + '%');
      card.style.setProperty('--sy', y.toFixed(1) + '%');
    });
  });
})();

(function(){
  const dropdown = document.querySelector('.profile-dropdown');
  if(!dropdown) return;
  const button = dropdown.querySelector('.profile-toggle');
  button && button.addEventListener('click', function(e){
    e.stopPropagation();
    const open = dropdown.classList.toggle('is-open');
    button.setAttribute('aria-expanded', open ? 'true' : 'false');
  });
  document.addEventListener('click', function(e){
    if(!dropdown.contains(e.target)){
      dropdown.classList.remove('is-open');
      button && button.setAttribute('aria-expanded','false');
    }
  });
})();

(function () {
  const settingsMain = document.querySelector('.settings-main');
  document.querySelectorAll('.settings-nav-link').forEach(function (link) {
    link.addEventListener('click', function () {
      if (!settingsMain) return;
      settingsMain.classList.remove('is-transitioning');
      void settingsMain.offsetWidth;
      settingsMain.classList.add('is-transitioning');
    });
  });
})();

(function () {
  document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.premium-password-form input[type="password"], .premium-password-form input[type="text"]').forEach(function (input) {
        input.type = input.type === 'password' ? 'text' : 'password';
      });
    });
  });
})();

(function () {
  document.querySelectorAll('.premium-glow-btn').forEach(function (btn) {
    btn.addEventListener('mousemove', function (event) {
      const rect = btn.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      btn.style.backgroundImage = 'radial-gradient(circle at ' + x + 'px ' + y + 'px, rgba(204,255,0,0.18), rgba(204,255,0,0) 42%)';
    });
    btn.addEventListener('mouseleave', function () {
      btn.style.backgroundImage = '';
    });
  });
})();

(function () {
  document.querySelectorAll('.card, .topic, .hero-copy, .premium-visual-card, .hero-chip-row span').forEach(function (panel, index) {
    panel.style.animationDelay = (index * 0.05) + 's';
    panel.classList.add('reveal-up');
    panel.addEventListener('mousemove', function (event) {
      const rect = panel.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width) * 100;
      const y = ((event.clientY - rect.top) / rect.height) * 100;
      panel.style.setProperty('--sx', x.toFixed(1) + '%');
      panel.style.setProperty('--sy', y.toFixed(1) + '%');
    });
  });
})();


(function () {
  const LOCAL_KEY = 'automartBuyerProfileCustom';

  function readState() {
    try {
      return JSON.parse(window.localStorage.getItem(LOCAL_KEY) || '{}');
    } catch (error) {
      return {};
    }
  }

  function syncHeaderAvatar() {
    const headerAvatar = document.querySelector('.profile-avatar');
    const headerMeta = document.querySelector('.profile-meta strong');
    if (!headerAvatar) return;
    const state = readState();
    const fallback = ((headerMeta && headerMeta.textContent.trim()) || headerAvatar.textContent || 'AU').slice(0, 1).toUpperCase();
    if (state.avatar) {
      let img = headerAvatar.querySelector('img');
      if (!img) {
        img = document.createElement('img');
        img.alt = 'Profile';
        headerAvatar.textContent = '';
        headerAvatar.appendChild(img);
      }
      img.src = state.avatar;
      headerAvatar.classList.add('has-image');
    } else {
      headerAvatar.classList.remove('has-image');
      headerAvatar.innerHTML = '';
      headerAvatar.textContent = fallback;
    }
  }

  syncHeaderAvatar();
  window.addEventListener('storage', syncHeaderAvatar);
})();

(function () {
  const form = document.getElementById('buyer-profile-form');
  if (!form) return;

  const avatarInput = document.getElementById('profile-picture-input');
  const avatarImage = document.getElementById('profile-avatar-image');
  const avatarFallback = document.getElementById('profile-avatar-fallback');
  const resetBtn = document.getElementById('profile-reset-local');
  const removePhotoBtn = document.getElementById('profile-remove-photo');
  const avatarEditText = document.getElementById('profile-avatar-edit-text');
  const usernameInput = form.querySelector('input[name="username"]');
  const completion = document.querySelector('.profile-completion-ring');
  const completionValue = document.getElementById('profile-completion-value');
  const chips = Array.from(document.querySelectorAll('.profile-chip'));
  const LOCAL_KEY = 'automartBuyerProfileCustom';
  const headerAvatar = document.querySelector('.profile-avatar');
  const passwordInput = document.getElementById('profile-password');
  const confirmInput = document.getElementById('profile-password-confirm');
  const passwordStrength = document.getElementById('profile-password-strength');
  const passwordStrengthCopy = document.getElementById('profile-password-strength-copy');
  const passwordMatch = document.getElementById('profile-password-match');

  function readState() {
    try {
      return JSON.parse(window.localStorage.getItem(LOCAL_KEY) || '{}');
    } catch (error) {
      return {};
    }
  }

  function writeState(state) {
    window.localStorage.setItem(LOCAL_KEY, JSON.stringify(state));
  }

  function showToast(message) {
    let toast = document.querySelector('.profile-local-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'profile-local-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('is-visible');
    window.clearTimeout(showToast._timer);
    showToast._timer = window.setTimeout(function () {
      toast.classList.remove('is-visible');
    }, 1700);
  }

  function setHeaderAvatar(src) {
    if (!headerAvatar) return;
    if (src) {
      let img = headerAvatar.querySelector('img');
      if (!img) {
        img = document.createElement('img');
        img.alt = 'Profile';
        headerAvatar.textContent = '';
        headerAvatar.appendChild(img);
      }
      img.src = src;
      headerAvatar.classList.add('has-image');
      headerAvatar.classList.add('is-bouncing');
      window.clearTimeout(setHeaderAvatar._timer);
      setHeaderAvatar._timer = window.setTimeout(function () {
        headerAvatar.classList.remove('is-bouncing');
      }, 650);
    } else {
      headerAvatar.classList.remove('has-image');
      const username = (usernameInput && usernameInput.value.trim()) || headerAvatar.textContent || 'AU';
      headerAvatar.textContent = username.slice(0, 1).toUpperCase();
    }
  }

  function setAvatar(src) {
    if (!avatarImage || !avatarFallback) return;
    if (src) {
      avatarImage.src = src;
      avatarImage.classList.add('is-visible');
      avatarImage.style.display = 'block';
      avatarFallback.style.display = 'none';
      avatarImage.closest('.profile-avatar-frame') && avatarImage.closest('.profile-avatar-frame').classList.add('has-photo');
      if (avatarEditText) avatarEditText.textContent = 'Change profile photo';
      setHeaderAvatar(src);
    } else {
      avatarImage.removeAttribute('src');
      avatarImage.classList.remove('is-visible');
      avatarImage.style.display = 'none';
      avatarFallback.style.display = 'grid';
      avatarImage.closest('.profile-avatar-frame') && avatarImage.closest('.profile-avatar-frame').classList.remove('has-photo');
      if (avatarEditText) avatarEditText.textContent = 'Add profile photo';
      const username = (usernameInput && usernameInput.value.trim()) || avatarFallback.textContent || 'AU';
      avatarFallback.textContent = username.slice(0, 2).toUpperCase();
      setHeaderAvatar('');
    }
  }

  function updateCompletion() {
    if (!completion) return;
    const fields = Array.from(form.querySelectorAll('input[type="text"], input[type="email"], input[type="password"], textarea, select'));
    const filled = fields.filter(function (field) {
      return !!field.value && field.value.trim().length > 0;
    }).length;
    const avatarBonus = avatarImage && avatarImage.getAttribute('src') ? 1 : 0;
    const activeChips = chips.filter(function (chip) { return chip.classList.contains('is-active'); }).length ? 1 : 0;
    const toggles = Array.from(form.querySelectorAll('.profile-toggle-card input:checked')).length;
    const total = fields.length + 3;
    const score = Math.max(32, Math.min(100, Math.round(((filled + avatarBonus + activeChips + (toggles ? 1 : 0)) / total) * 100)));
    completion.dataset.completion = String(score);
    if (completionValue) completionValue.textContent = score + '%';
    const ring = completion.querySelector('.progress');
    if (ring) {
      const circumference = 301.44;
      ring.style.strokeDashoffset = String(circumference * (1 - score / 100));
    }
  }

  function saveFormState() {
    const state = readState();
    const extras = {};
    form.querySelectorAll('textarea, select, input[type="checkbox"]').forEach(function (field) {
      extras[field.name] = field.type === 'checkbox' ? field.checked : field.value;
    });
    state.form = extras;
    state.chips = chips.filter(function (chip) { return chip.classList.contains('is-active'); }).map(function (chip) { return chip.dataset.chip; });
    if (avatarImage && avatarImage.getAttribute('src')) {
      state.avatar = avatarImage.getAttribute('src');
    } else {
      delete state.avatar;
    }
    writeState(state);
    updateCompletion();
  }

  function evaluatePassword() {
    if (!passwordInput || !passwordStrength || !passwordStrengthCopy) return true;
    const value = passwordInput.value || '';
    let score = 0;
    if (value.length >= 8) score += 1;
    if (/[A-Z]/.test(value)) score += 1;
    if (/[a-z]/.test(value)) score += 1;
    if (/\d/.test(value)) score += 1;
    if (/[^A-Za-z0-9]/.test(value)) score += 1;
    let level = 'weak';
    let copy = 'Security level: weak';
    if (score >= 4) {
      level = 'strong';
      copy = 'Security level: strong';
    } else if (score >= 3) {
      level = 'medium';
      copy = 'Security level: medium';
    }
    passwordStrength.dataset.strength = level;
    passwordStrengthCopy.textContent = copy;
    passwordStrengthCopy.classList.toggle('is-error', value.length > 0 && value.length < 8);
    return score >= 3 && value.length >= 8;
  }

  function validatePasswordMatch() {
    if (!passwordInput || !confirmInput || !passwordMatch) return true;
    if (!confirmInput.value) {
      passwordMatch.textContent = 'Re-enter the same password before saving.';
      passwordMatch.classList.remove('is-error');
      return true;
    }
    const ok = passwordInput.value === confirmInput.value;
    passwordMatch.textContent = ok ? 'Passwords match and are ready to save.' : 'Passwords do not match yet.';
    passwordMatch.classList.toggle('is-error', !ok);
    return ok;
  }

  function hydrateFormState() {
    const state = readState();
    if (state.form) {
      Object.keys(state.form).forEach(function (name) {
        const field = form.querySelector('[name="' + name + '"]');
        if (!field) return;
        if (field.type === 'checkbox') {
          field.checked = !!state.form[name];
        } else if (name !== 'username' && name !== 'email' && name !== 'phone' && name !== 'password') {
          field.value = state.form[name];
        }
      });
    }
    if (Array.isArray(state.chips)) {
      chips.forEach(function (chip) {
        chip.classList.toggle('is-active', state.chips.indexOf(chip.dataset.chip) > -1);
      });
    }
    if (state.avatar) setAvatar(state.avatar);
    updateCompletion();
  }

  if (avatarInput) {
    avatarInput.addEventListener('change', function (event) {
      const file = event.target.files && event.target.files[0];
      if (!file) return;
      const tempUrl = URL.createObjectURL(file);
      if (avatarImage) {
        avatarImage.src = tempUrl;
        avatarImage.classList.add('is-visible');
        avatarImage.style.display = 'block';
      }
      if (avatarFallback) {
        avatarFallback.style.display = 'none';
      }
      setHeaderAvatar(tempUrl);

      const reader = new FileReader();
      reader.onload = function (e) {
        const result = e.target && e.target.result;
        if (typeof result !== 'string') return;
        setAvatar(result);
        saveFormState();
        showToast('Profile picture updated');
      };
      reader.readAsDataURL(file);
    });
  }


  if (removePhotoBtn) {
    removePhotoBtn.addEventListener('click', function () {
      if (avatarInput) avatarInput.value = '';
      setAvatar('');
      saveFormState();
      showToast('Profile picture removed');
    });
  }

  if (usernameInput) {
    usernameInput.addEventListener('input', function () {
      if (!avatarImage || !avatarImage.getAttribute('src')) setAvatar('');
    });
    if (!avatarImage || !avatarImage.getAttribute('src')) setHeaderAvatar('');
  }

  document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
    button.addEventListener('click', function () {
      const target = document.getElementById(button.getAttribute('data-password-toggle'));
      if (!target) return;
      const show = target.type === 'password';
      target.type = show ? 'text' : 'password';
      button.textContent = show ? 'Hide' : 'Show';
    });
  });

  if (passwordInput) {
    passwordInput.addEventListener('input', function () {
      evaluatePassword();
      validatePasswordMatch();
    });
  }

  if (confirmInput) {
    confirmInput.addEventListener('input', validatePasswordMatch);
  }

  chips.forEach(function (chip) {
    chip.addEventListener('click', function () {
      chip.classList.toggle('is-active');
      saveFormState();
    });
  });

  form.querySelectorAll('textarea, select, input[type="checkbox"], input[type="text"], input[type="email"]').forEach(function (field) {
    field.addEventListener('input', saveFormState);
    field.addEventListener('change', saveFormState);
  });

  form.addEventListener('submit', function (event) {
    const strongEnough = evaluatePassword();
    const matches = validatePasswordMatch();
    if (!strongEnough || !matches) {
      event.preventDefault();
      showToast(!strongEnough ? 'Use a stronger password to continue' : 'Please make sure both passwords match');
      return;
    }
    saveFormState();
    showToast('Profile customizations saved locally');
  });

  if (resetBtn) {
    resetBtn.addEventListener('click', function () {
      window.localStorage.removeItem(LOCAL_KEY);
      if (avatarInput) avatarInput.value = '';
      setAvatar('');
      chips.forEach(function (chip, index) {
        chip.classList.toggle('is-active', index === 0);
      });
      form.querySelectorAll('textarea').forEach(function (field) { field.value = field.defaultValue; });
      form.querySelectorAll('select').forEach(function (field) { field.value = field.options[0] ? field.options[0].value : ''; });
      form.querySelectorAll('.profile-toggle-card input').forEach(function (field, index) { field.checked = index < 3; });
      updateCompletion();
      showToast('Local customizations reset');
    });
  }

  document.querySelectorAll('.profile-sidebar, .profile-effect-card, .profile-enhanced-form').forEach(function (panel) {
    panel.addEventListener('mousemove', function (event) {
      const rect = panel.getBoundingClientRect();
      const px = (event.clientX - rect.left) / rect.width;
      const py = (event.clientY - rect.top) / rect.height;
      const rx = (0.5 - py) * 5;
      const ry = (px - 0.5) * 6;
      panel.style.transform = 'perspective(1200px) rotateX(' + rx.toFixed(2) + 'deg) rotateY(' + ry.toFixed(2) + 'deg)';
    });
    panel.addEventListener('mouseleave', function () {
      panel.style.transform = '';
    });
  });

  hydrateFormState();
  evaluatePassword();
  validatePasswordMatch();
  const currentState = readState();
  if (currentState.avatar) {
    setHeaderAvatar(currentState.avatar);
  } else {
    setHeaderAvatar('');
  }
})();

(function () {
  const WISHLIST_KEY = 'automartPremiumWishlist';

  function normalizeWishlistItem(item) {
    if (!item) return null;
    const id = String(item.id || '').trim();
    if (!id) return null;
    return {
      id: id,
      title: String(item.title || 'Saved Vehicle'),
      price: String(item.price || ''),
      image: String(item.image || ''),
      category: String(item.category || ''),
      year: String(item.year || ''),
      mileage: String(item.mileage || ''),
      fuel: String(item.fuel || ''),
      transmission: String(item.transmission || ''),
      link: String(item.link || '/inventory'),
      savedAt: Number(item.savedAt || Date.now())
    };
  }

  function dedupeWishlist(items) {
    const map = new Map();
    (Array.isArray(items) ? items : []).forEach(function (item) {
      const normalized = normalizeWishlistItem(item);
      if (!normalized) return;
      const previous = map.get(normalized.id);
      map.set(normalized.id, previous ? Object.assign({}, previous, normalized, { savedAt: previous.savedAt || normalized.savedAt }) : normalized);
    });
    return Array.from(map.values());
  }

  function readWishlist() {
    try {
      const raw = JSON.parse(window.localStorage.getItem(WISHLIST_KEY) || '[]');
      return dedupeWishlist(raw);
    } catch (error) {
      return [];
    }
  }

  function writeWishlist(items) {
    window.localStorage.setItem(WISHLIST_KEY, JSON.stringify(dedupeWishlist(items)));
    syncWishlistUI();
  }

  function itemFromDataset(node) {
    return normalizeWishlistItem({
      id: node.dataset.wishlistId || '',
      title: node.dataset.wishlistTitle || 'Saved Vehicle',
      price: node.dataset.wishlistPrice || '',
      image: node.dataset.wishlistImage || '',
      category: node.dataset.wishlistCategory || '',
      year: node.dataset.wishlistYear || '',
      mileage: node.dataset.wishlistMileage || '',
      fuel: node.dataset.wishlistFuel || '',
      transmission: node.dataset.wishlistTransmission || '',
      link: node.dataset.wishlistLink || '/inventory',
      savedAt: Date.now()
    });
  }

  function inWishlist(id) {
    return readWishlist().some(function (item) { return item.id === id; });
  }

  function updateCounts(count) {
    document.querySelectorAll('[data-wishlist-count]').forEach(function (el) {
      el.textContent = String(count);
      el.classList.toggle('has-items', count > 0);
    });
    const total = document.querySelector('[data-wishlist-total]');
    if (total) total.textContent = String(count);
    const compare = document.querySelector('[data-wishlist-compare]');
    if (compare) compare.textContent = String(Math.min(count, 3));
  }

  function updateButtons() {
    const items = readWishlist();
    document.querySelectorAll('[data-wishlist-id]').forEach(function (button) {
      const active = items.some(function (item) { return item.id === String(button.dataset.wishlistId || '').trim(); });
      button.classList.toggle('is-saved', active);
      const textNode = button.querySelector('.wishlist-btn-text');
      const heartNode = button.querySelector('.wishlist-btn-heart');
      if (textNode) textNode.textContent = active ? 'Saved' : 'Save';
      if (heartNode) heartNode.textContent = active ? '♥' : '♡';
      if (button.classList.contains('wishlist-chip-btn') || button.classList.contains('detail-wishlist-btn')) {
        button.textContent = active ? 'Saved to Wishlist' : 'Add to Wishlist';
      }
      const card = button.closest('.premium-wishlist-card, .detail-premium-actions, .wishlist-display-card');
      if (card) card.classList.toggle('is-wishlisted', active);
    });
    document.querySelectorAll('[data-wishlist-status]').forEach(function (status) {
      const active = items.some(function (item) { return item.id === status.dataset.wishlistStatus; });
      status.textContent = active ? 'Saved in wishlist' : 'Ready to save';
      status.classList.toggle('is-saved', active);
    });
  }

  function createToast(message) {
    let toast = document.querySelector('.wishlist-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'wishlist-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('is-visible');
    clearTimeout(createToast._timer);
    createToast._timer = setTimeout(function () {
      toast.classList.remove('is-visible');
    }, 1800);
  }

  function renderWishlistPage() {
    const grid = document.getElementById('wishlist-grid');
    const empty = document.getElementById('wishlist-empty');
    if (!grid || !empty) return;
    const items = readWishlist().sort(function (a, b) { return (b.savedAt || 0) - (a.savedAt || 0); });
    grid.innerHTML = '';
    empty.style.display = items.length ? 'none' : 'grid';
    grid.style.display = items.length ? 'grid' : 'none';

    items.forEach(function (item, index) {
      const card = document.createElement('article');
      card.className = 'wishlist-display-card animated-card';
      card.style.animationDelay = (index * 0.06) + 's';
      card.innerHTML = '' +
        '<div class="wishlist-display-media">' +
          '<img src="' + item.image + '" alt="' + item.title + '">' +
          '<div class="wishlist-display-glow"></div>' +
        '</div>' +
        '<div class="wishlist-display-body">' +
          '<div class="wishlist-display-top"><span class="badge">' + item.category + '</span><span class="badge soft">' + item.year + '</span></div>' +
          '<h3>' + item.title + '</h3>' +
          '<div class="wishlist-display-price">' + item.price + '</div>' +
          '<div class="mini-specs"><span>' + item.mileage + '</span><span>' + item.fuel + '</span><span>' + item.transmission + '</span></div>' +
          '<div class="wishlist-display-actions">' +
            '<a class="btn btn-primary premium-glow-btn" href="' + item.link + '">View Vehicle</a>' +
            '<button class="btn btn-secondary" type="button" data-wishlist-remove="' + item.id + '">Remove</button>' +
          '</div>' +
        '</div>';
      grid.appendChild(card);
    });

    grid.querySelectorAll('[data-wishlist-remove]').forEach(function (button) {
      button.addEventListener('click', function () {
        const id = button.getAttribute('data-wishlist-remove');
        const next = readWishlist().filter(function (item) { return item.id !== id; });
        writeWishlist(next);
        createToast('Removed from wishlist');
      });
    });
  }

  function syncWishlistUI() {
    const currentItems = readWishlist();
    const items = currentItems.filter(function (item) { return item && item.id && item.title; });
    if (items.length !== currentItems.length) {
      window.localStorage.setItem(WISHLIST_KEY, JSON.stringify(items));
    }
    updateCounts(items.length);
    updateButtons();
    renderWishlistPage();
  }

  document.querySelectorAll('[data-wishlist-id]').forEach(function (button) {
    button.addEventListener('click', function (event) {
      event.preventDefault();
      const item = itemFromDataset(button);
      if (!item || !item.id) return;
      const items = readWishlist();
      const exists = items.some(function (entry) { return entry.id === item.id; });
      if (exists) {
        writeWishlist(items.filter(function (entry) { return entry.id !== item.id; }));
        createToast('Removed from wishlist');
      } else {
        writeWishlist(items.concat([item]));
        createToast('Saved to wishlist');
      }
      const card = button.closest('.premium-wishlist-card, .detail-premium-actions, .wishlist-display-card');
      if (card) {
        card.classList.remove('wishlist-pop');
        void card.offsetWidth;
        card.classList.add('wishlist-pop');
      }
    });
  });

  const clearButton = document.querySelector('[data-wishlist-clear]');
  if (clearButton) {
    clearButton.addEventListener('click', function () {
      writeWishlist([]);
      createToast('Wishlist cleared');
    });
  }

  document.querySelectorAll('[data-tilt-card], .panel, .premium-icon-btn, .profile-toggle, .hero-search-bar, .review-card-pro, .settings-stat, .info-tile').forEach(function (card) {
    const intensity = card.matches('[data-tilt-card], .premium-wishlist-card, .wishlist-display-card') ? 10 : 4;
    const lift = card.matches('[data-tilt-card], .premium-wishlist-card, .wishlist-display-card') ? 8 : 3;
    card.addEventListener('mousemove', function (event) {
      if (window.innerWidth < 860) return;
      const rect = card.getBoundingClientRect();
      const px = (event.clientX - rect.left) / rect.width;
      const py = (event.clientY - rect.top) / rect.height;
      const rx = (0.5 - py) * intensity;
      const ry = (px - 0.5) * intensity;
      card.style.transform = 'perspective(1400px) rotateX(' + rx.toFixed(2) + 'deg) rotateY(' + ry.toFixed(2) + 'deg) translateY(-' + lift + 'px)';
      card.style.setProperty('--sx', (px * 100).toFixed(1) + '%');
      card.style.setProperty('--sy', (py * 100).toFixed(1) + '%');
    });
    card.addEventListener('mouseleave', function () {
      card.style.transform = '';
      card.style.removeProperty('--sx');
      card.style.removeProperty('--sy');
    });
  });

  syncWishlistUI();
  window.addEventListener('storage', syncWishlistUI);
  window.addEventListener('pageshow', syncWishlistUI);
})();


document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.social-auth-btn').forEach(function(btn, index){
    btn.style.animation = 'riseIn .55s ease ' + (index * 0.06) + 's both';
  });
  document.querySelectorAll('.settings-nav-link').forEach(function(link){
    link.addEventListener('click', function(){
      const sidebar = document.querySelector('.settings-sidebar-premium');
      if (sidebar && window.innerWidth < 1100) {
        setTimeout(function(){ sidebar.scrollIntoView({behavior:'smooth', block:'start'}); }, 250);
      }
    });
  });
  document.querySelectorAll('.setting-toggle').forEach(function(toggle){
    toggle.setAttribute('title', toggle.textContent.trim());
  });
});

// Auto Mart validation + custom delete confirmation modal
(function () {
  function normalizeEmail(input) {
    input.value = (input.value || '').toLowerCase().replace(/[#%]/g, '');
  }

  function normalizePhone(input) {
    input.value = (input.value || '').replace(/\D/g, '').slice(0, 10);
  }

  function wireValidation(root) {
    root.querySelectorAll('input[type="email"], input[name*="email" i]').forEach(function (input) {
      input.setAttribute('title', 'Use lowercase email without # or %.');
      input.addEventListener('input', function () { normalizeEmail(input); });
      input.addEventListener('blur', function () { normalizeEmail(input); });
      normalizeEmail(input);
    });
    root.querySelectorAll('input[name*="phone" i], input[name*="whatsapp" i]').forEach(function (input) {
      input.setAttribute('inputmode', 'numeric');
      input.setAttribute('maxlength', '10');
      input.setAttribute('pattern', '[0-9]{7,10}');
      input.setAttribute('title', 'Digits only, maximum 10 digits.');
      input.addEventListener('input', function () { normalizePhone(input); });
      normalizePhone(input);
    });
  }

  function ensureDeleteModal() {
    var modal = document.getElementById('deleteConfirmOverlay');
    if (modal) return modal;

    modal = document.createElement('div');
    modal.className = 'delete-confirm-overlay';
    modal.id = 'deleteConfirmOverlay';
    modal.innerHTML =
      '<div class="delete-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="deleteConfirmTitle">' +
        '<div class="delete-confirm-icon">🗑️</div>' +
        '<h3 id="deleteConfirmTitle">Are you sure you want<br>to delete this?</h3>' +
        '<p id="deleteConfirmText">Once deleted, this item cannot be recovered.</p>' +
        '<div class="delete-confirm-actions">' +
          '<button class="delete-confirm-cancel" type="button">No, keep it</button>' +
          '<button class="delete-confirm-delete" type="button">Yes, delete</button>' +
        '</div>' +
      '</div>';
    document.body.appendChild(modal);
    return modal;
  }

  function openDeleteModal(message, onConfirm) {
    var overlay = ensureDeleteModal();
    var title = overlay.querySelector('#deleteConfirmTitle');
    var text = overlay.querySelector('#deleteConfirmText');
    var cancelBtn = overlay.querySelector('.delete-confirm-cancel');
    var deleteBtn = overlay.querySelector('.delete-confirm-delete');

    title.innerHTML = message || 'Are you sure you want<br>to delete this?';
    text.textContent = 'Once deleted, this item cannot be recovered.';
    overlay.classList.add('show');

    function close() {
      overlay.classList.remove('show');
      cancelBtn.removeEventListener('click', close);
      overlay.removeEventListener('click', outsideClose);
      document.removeEventListener('keydown', escClose);
      deleteBtn.removeEventListener('click', confirmDelete);
    }
    function outsideClose(event) {
      if (event.target === overlay) close();
    }
    function escClose(event) {
      if (event.key === 'Escape') close();
    }
    function confirmDelete() {
      close();
      if (typeof onConfirm === 'function') onConfirm();
    }

    cancelBtn.addEventListener('click', close);
    overlay.addEventListener('click', outsideClose);
    document.addEventListener('keydown', escClose);
    deleteBtn.addEventListener('click', confirmDelete);
    cancelBtn.focus();
  }

  function confirmFormSubmit(form, submitter, message) {
    openDeleteModal(message, function () {
      form.dataset.deleteConfirmed = 'true';
      if (submitter && typeof form.requestSubmit === 'function') {
        form.requestSubmit(submitter);
      } else {
        form.submit();
      }
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    wireValidation(document);

    document.querySelectorAll('form[data-delete-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        if (form.dataset.deleteConfirmed === 'true') {
          delete form.dataset.deleteConfirmed;
          return;
        }
        event.preventDefault();
        var message = form.getAttribute('data-delete-confirm') || 'Are you sure you want<br>to delete this?';
        confirmFormSubmit(form, event.submitter, message);
      });
    });

    document.querySelectorAll('[data-delete-button]').forEach(function (button) {
      var form = button.closest('form');
      if (form && !form.dataset.deleteConfirmWired) {
        form.dataset.deleteConfirmWired = 'true';
        form.addEventListener('submit', function (event) {
          var submitter = event.submitter;
          if (!submitter || !submitter.matches('[data-delete-button]')) return;
          if (form.dataset.deleteConfirmed === 'true') {
            delete form.dataset.deleteConfirmed;
            return;
          }
          event.preventDefault();
          var message = submitter.getAttribute('data-delete-message') || 'Are you sure you want<br>to delete this?';
          confirmFormSubmit(form, submitter, message);
        });
      } else if (!form) {
        button.addEventListener('click', function (event) {
          event.preventDefault();
          var message = button.getAttribute('data-delete-message') || 'Are you sure you want<br>to delete this?';
          openDeleteModal(message, function () {
            var href = button.getAttribute('href');
            if (href) window.location.href = href;
          });
        });
      }
    });
  });
})();

(function () {
  function showAutoMartToast(message) {
    if (!message) return;
    let toast = document.querySelector('.automart-global-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'automart-global-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('is-visible');
    window.setTimeout(function () { toast.classList.remove('is-visible'); }, 3600);
  }

  document.addEventListener('DOMContentLoaded', function () {
    const params = new URLSearchParams(window.location.search);
    const signup = params.get('signupSuccess');
    if (signup === 'buyer') showAutoMartToast('Buyer account created successfully');
    if (signup === 'seller') showAutoMartToast('Seller account created successfully');
  });
})();


/* 2026-05 universal Gmail + 10-digit mobile validation */
(function(){
  function isEmailField(input){ return input && (input.type === 'email' || /email/i.test(input.name || '') || /email/i.test(input.id || '')); }
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


// 2026-05 AutoMart shared admin data bridge + fast loader
(function(){
  const SHARED_KEY='automartSharedMarketplaceDataV2';
  function ready(){document.body.classList.add('page-ready');document.body.classList.remove('page-loading');}
  window.addEventListener('load',()=>setTimeout(ready,320)); setTimeout(ready,1200);
  function renderApprovedAdminListings(){
    let data; try{data=JSON.parse(localStorage.getItem(SHARED_KEY)||'{}');}catch{return;}
    if(!Array.isArray(data.listings)||!data.listings.length)return;
    const grids=document.querySelectorAll('.vehicle-grid,.inventory-grid,.cards-grid,[data-inventory-grid]');
    if(!grids.length)return;
    const html=data.listings.slice(0,12).map(l=>`<article class="vehicle-card admin-synced-listing"><a href="/inventory"><img src="${l.image||'/assets/img/listings/suv-1.jpg'}" alt="${l.make||''} ${l.model||''}" style="width:100%;height:190px;object-fit:cover;border-radius:18px"></a><div class="vehicle-card-body"><p class="eyebrow">${l.type||'Vehicle'} · ${l.year||''}</p><h3>${l.make||''} ${l.model||''}</h3><p>${l.mileage||''} · ${l.fuel||''} · ${l.transmission||''}</p><strong>${l.price||''}</strong></div></article>`).join('');
    grids[0].insertAdjacentHTML('afterbegin', html);
  }
  document.addEventListener('DOMContentLoaded',renderApprovedAdminListings);
})();


// AutoMart real profile sync across header/profile/settings/admin pages
(function(){
  const PROFILE_KEY='automartUserProfileRealV4';
  const ADMIN_PROFILE_KEY='automartAdminProfileRealV2';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function initials(name){return String(name||'AU').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'AU'}
  function apply(){
    const p=Object.assign({},read(ADMIN_PROFILE_KEY,{}),read(PROFILE_KEY,{}));
    const full=(p.fullName||((p.firstName||'')+' '+(p.lastName||'')).trim()||p.displayName||p.username||'Account').trim();
    const av=document.getElementById('globalProfileAvatar');
    const name=document.getElementById('globalProfileName');
    if(av){
      if(p.photo){av.textContent='';av.style.background='url("'+p.photo+'") center/cover';}
      else {av.textContent=initials(full);av.style.background='';}
    }
    if(name) name.textContent=full;
  }
  window.addEventListener('storage',e=>{if([PROFILE_KEY,ADMIN_PROFILE_KEY,'automartSharedMarketplaceDataV2'].includes(e.key))apply();});
  document.addEventListener('DOMContentLoaded',apply);
})();


// 2026-05 AutoMart live header notifications/messages + confirmation saves
(function(){
  const NOTIF_KEY='automartLiveNotificationsV1';
  const MSG_KEY='automartLiveMessagesV1';
  const SHARED_KEY='automartSharedMarketplaceDataV2';
  const PROFILE_KEY='automartUserProfileRealV4';
  const ADMIN_PROFILE_KEY='automartAdminProfileRealV2';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function seed(){
    if(!localStorage.getItem(NOTIF_KEY)) write(NOTIF_KEY,[
      {id:'n1',title:'Listing update',body:'Your saved vehicle listing received a new price update.',time:'Just now',read:false},
      {id:'n2',title:'Request approved',body:'A buyer request was approved by the AutoMart team.',time:'Today',read:false},
      {id:'n3',title:'Profile tip',body:'Add a profile photo to improve trust score.',time:'Yesterday',read:true}
    ]);
    if(!localStorage.getItem(MSG_KEY)) write(MSG_KEY,[
      {id:'m1',from:'Kasun Motors',body:'Hi, the vehicle is still available for inspection.',time:'10:24 AM',read:false},
      {id:'m2',from:'AutoMart Support',body:'Your latest profile changes are synced.',time:'Yesterday',read:true}
    ]);
  }
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function get(type){return read(type==='messages'?MSG_KEY:NOTIF_KEY,[])}
  function set(type,data){write(type==='messages'?MSG_KEY:NOTIF_KEY,data); updateBadges();}
  function updateBadges(){
    const n=get('notifications').filter(x=>!x.read).length, m=get('messages').filter(x=>!x.read).length;
    document.querySelectorAll('[data-notification-count]').forEach(e=>{e.textContent=n;e.classList.toggle('is-zero',n===0)});
    document.querySelectorAll('[data-message-count]').forEach(e=>{e.textContent=m;e.classList.toggle('is-zero',m===0)});
  }
  function panel(type){
    document.querySelectorAll('.automart-live-panel').forEach(p=>p.remove());
    const items=get(type), unread=items.filter(x=>!x.read).length;
    const title=type==='messages'?'Messages':'Notifications';
    const p=document.createElement('div'); p.className='automart-live-panel show'; p.dataset.type=type;
    p.innerHTML=`<div class="automart-live-panel-head"><strong>${title} ${unread?`<span style="color:#c8ff3e">(${unread} unread)</span>`:''}</strong><button type="button" data-mark>Mark all as read</button></div><div class="automart-live-list">${items.length?items.map(x=>`<div class="automart-live-item ${x.read?'read':''}" data-id="${esc(x.id)}"><span class="automart-live-dot"></span><div><strong>${esc(x.title||x.from)}</strong><p>${esc(x.body)}</p><small>${esc(x.time||'')}</small></div></div>`).join(''):`<div class="automart-live-empty"><b>No ${title.toLowerCase()}</b><span>You are all caught up.</span></div>`}</div>`;
    document.body.appendChild(p);
    p.querySelector('[data-mark]')?.addEventListener('click',()=>{const data=get(type).map(x=>Object.assign({},x,{read:true}));set(type,data);panel(type);});
    p.querySelectorAll('.automart-live-item').forEach(item=>item.addEventListener('click',()=>{const id=item.dataset.id;const data=get(type).map(x=>x.id===id?Object.assign({},x,{read:true}):x);set(type,data);item.classList.add('read');}));
  }
  function confirmBox(title,message,okText,cb){
    const o=document.createElement('div');o.className='confirm-modal-lite';o.innerHTML=`<div class="confirm-modal-lite-card"><h3>${esc(title||'Confirm Changes')}</h3><p>${esc(message||'Do you want to save these changes?')}</p><div class="confirm-modal-lite-actions"><button class="confirm-modal-lite-cancel" type="button">Cancel</button><button class="confirm-modal-lite-ok" type="button">${esc(okText||'Confirm Save')}</button></div></div>`;document.body.appendChild(o);
    o.querySelector('.confirm-modal-lite-cancel').onclick=()=>o.remove();o.onclick=e=>{if(e.target===o)o.remove()};o.querySelector('.confirm-modal-lite-ok').onclick=()=>{try{cb&&cb();}finally{o.remove();}};
  }
  if(!window.AutoMartConfirm) if(!window.AutoMartConfirm) window.AutoMartConfirm=confirmBox;
  function initials(n){return String(n||'AU').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'AU'}
  function applyProfileSync(){
    const p=Object.assign({},read(ADMIN_PROFILE_KEY,{}),read(PROFILE_KEY,{}));
    const full=(p.fullName||((p.firstName||'')+' '+(p.lastName||'')).trim()||p.displayName||p.username||'Account').trim();
    document.querySelectorAll('#globalProfileAvatar,.profile-avatar,.tb-av').forEach(av=>{if(p.photo){av.textContent='';av.style.background=`url("${p.photo}") center/cover`;}else if(av.id==='globalProfileAvatar'||av.classList.contains('tb-av')){av.textContent=initials(full);}});
    document.querySelectorAll('#globalProfileName').forEach(e=>e.textContent=full);
    const shared=read(SHARED_KEY,{}); shared.profile=p; shared.updatedAt=Date.now(); write(SHARED_KEY,shared);
  }
  function wire(){
    seed(); updateBadges(); applyProfileSync();
    document.querySelectorAll('[data-live-panel-toggle]').forEach(btn=>btn.addEventListener('click',e=>{e.preventDefault(); const existing=document.querySelector('.automart-live-panel.show'); if(existing&&existing.dataset.type===btn.dataset.livePanelToggle){existing.remove();return;} panel(btn.dataset.livePanelToggle);}));
    document.addEventListener('click',e=>{if(!e.target.closest('.automart-live-panel')&&!e.target.closest('[data-live-panel-toggle]'))document.querySelectorAll('.automart-live-panel').forEach(p=>p.remove());});
  }
  document.addEventListener('DOMContentLoaded',wire);
  window.addEventListener('storage',e=>{if([NOTIF_KEY,MSG_KEY,PROFILE_KEY,ADMIN_PROFILE_KEY,SHARED_KEY].includes(e.key)){updateBadges();applyProfileSync();}});
})();


// 2026-05 role-based common header, live profile sync, shared notifications/messages
(function(){
  const PROFILE_KEY='automartUserProfileRealV4';
  const BUYER_KEY='automartBuyerProfileRealV1';
  const ADMIN_PROFILE_KEY='automartAdminProfileRealV2';
  const SHARED_KEY='automartSharedMarketplaceDataV2';
  const NOTIF_KEY='automartLiveNotificationsV1';
  const MSG_KEY='automartLiveMessagesV1';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function initials(n){return String(n||'AU').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'AU'}
  function currentRole(){return (document.getElementById('globalProfileRole')?.textContent||'guest').trim().toLowerCase();}
  function roleProfile(){
    const role=currentRole();
    const base= role.includes('admin') ? read(ADMIN_PROFILE_KEY,{}) : role.includes('buyer') ? read(BUYER_KEY,read(PROFILE_KEY,{})) : read(PROFILE_KEY,{});
    const name=(base.fullName||((base.firstName||'')+' '+(base.lastName||'')).trim()||base.displayName||base.username||document.getElementById('globalProfileName')?.textContent||'Account').trim();
    return {role, name, photo:base.photo||base.avatar||'', email:base.email||'', phone:base.phone||'', raw:base};
  }
  function apply(){
    const p=roleProfile();
    document.querySelectorAll('#globalProfileAvatar,.tb-av,.profile-avatar').forEach(av=>{
      if(!av) return;
      if(p.photo){av.textContent='';av.style.background='url("'+p.photo+'") center/cover';}
      else {av.textContent=initials(p.name);av.style.background='';}
    });
    document.querySelectorAll('#globalProfileName').forEach(e=>e.textContent=p.name);
    const shared=read(SHARED_KEY,{});
    shared.activeRole=p.role; shared.activeProfile=p.raw; shared.updatedAt=Date.now();
    write(SHARED_KEY,shared);
  }
  function seedLive(){
    if(!localStorage.getItem(NOTIF_KEY)) write(NOTIF_KEY,[]);
    if(!localStorage.getItem(MSG_KEY)) write(MSG_KEY,[]);
  }
  function unread(k){return read(k,[]).filter(x=>!x.read).length}
  function updateCounts(){
    const n=unread(NOTIF_KEY), m=unread(MSG_KEY);
    document.querySelectorAll('[data-notification-count]').forEach(e=>{e.textContent=n;e.classList.toggle('is-zero',n===0)});
    document.querySelectorAll('[data-message-count]').forEach(e=>{e.textContent=m;e.classList.toggle('is-zero',m===0)});
  }
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function openPanel(type){
    document.querySelectorAll('.automart-live-panel').forEach(p=>p.remove());
    const key=type==='messages'?MSG_KEY:NOTIF_KEY, title=type==='messages'?'Messages':'Notifications';
    const items=read(key,[]);
    const unreadCount=items.filter(x=>!x.read).length;
    const el=document.createElement('div'); el.className='automart-live-panel show'; el.dataset.type=type;
    el.innerHTML='<div class="automart-live-panel-head"><strong>'+title+(unreadCount?' <span style="color:#c8ff3e">('+unreadCount+' unread)</span>':'')+'</strong><button type="button" data-mark>Mark as read</button></div><div class="automart-live-list">'+(items.length?items.map(x=>'<div class="automart-live-item '+(x.read?'read':'')+'" data-id="'+esc(x.id)+'"><span class="automart-live-dot"></span><div><strong>'+esc(x.title||x.from||'AutoMart')+'</strong><p>'+esc(x.body||x.message||'')+'</p><small>'+esc(x.time||'Now')+'</small></div></div>').join(''):'<div class="automart-live-empty"><b>No '+title.toLowerCase()+'</b><span>You are all caught up.</span></div>')+'</div>';
    document.body.appendChild(el);
    el.querySelector('[data-mark]').onclick=()=>{write(key,items.map(x=>Object.assign({},x,{read:true})));updateCounts();openPanel(type);};
    el.querySelectorAll('.automart-live-item').forEach(i=>i.onclick=()=>{write(key,read(key,[]).map(x=>x.id===i.dataset.id?Object.assign({},x,{read:true}):x));i.classList.add('read');updateCounts();});
  }
  function wire(){
    seedLive(); apply(); updateCounts();
    document.querySelectorAll('[data-live-panel-toggle]').forEach(btn=>{btn.onclick=e=>{e.preventDefault();const old=document.querySelector('.automart-live-panel.show'); if(old&&old.dataset.type===btn.dataset.livePanelToggle){old.remove();return;} openPanel(btn.dataset.livePanelToggle);};});
    document.querySelector('[data-header-search]')?.addEventListener('keydown',e=>{if(e.key==='Enter'&&e.target.value.trim()){location.href='/inventory?q='+encodeURIComponent(e.target.value.trim());}});
  }
  window.AutoMartSharedApplyProfile=apply;
  window.AutoMartPushNotification=function(title,body){const list=read(NOTIF_KEY,[]);list.unshift({id:'n_'+Date.now(),title,body,time:'Now',read:false});write(NOTIF_KEY,list);updateCounts();};
  window.addEventListener('storage',e=>{if([PROFILE_KEY,BUYER_KEY,ADMIN_PROFILE_KEY,SHARED_KEY,NOTIF_KEY,MSG_KEY].includes(e.key)){apply();updateCounts();}});
  document.addEventListener('DOMContentLoaded',wire);
})();



// 2026-05 final shared avatar/header sync + common account actions
(function(){
  const PROFILE_KEY='automartUserProfileRealV4';
  const BUYER_KEY='automartBuyerProfileRealV1';
  const ADMIN_KEY='automartAdminProfileRealV2';
  const SETTINGS_KEY='automartSettingsPremiumV4';
  const OLD_SETTINGS_KEY='automartPremiumSettingsV2';
  const SHARED_KEY='automartSharedMarketplaceDataV2';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function initials(n){return String(n||'AU').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'AU'}
  function profileFromAll(){
    const settings=read(SETTINGS_KEY,{}), old=read(OLD_SETTINGS_KEY,{}), seller=read(PROFILE_KEY,{}), buyer=read(BUYER_KEY,{}), admin=read(ADMIN_KEY,{});
    const role=(document.getElementById('globalProfileRole')?.textContent||'').toLowerCase();
    const base=role.includes('buyer')?buyer:role.includes('admin')?admin:seller;
    const merged=Object.assign({}, old.profile||{}, settings.profile||{}, seller, buyer, admin, base);
    if(merged.photoRemoved||merged.profilePhotoRemoved){merged.photo='';}else{merged.photo=localStorage.getItem('automartMediaAvatarV2')||localStorage.getItem('automartMediaAvatarV1')||'';}
    merged.firstName=merged.firstName||((merged.fullName||'').split(' ')[0]||'');
    merged.lastName=merged.lastName||((merged.fullName||'').split(' ').slice(1).join(' ')||'');
    merged.fullName=(merged.fullName||((merged.firstName||'')+' '+(merged.lastName||'')).trim()||merged.displayName||merged.username||document.getElementById('globalProfileName')?.textContent||'Account').trim();
    return {role, data:merged};
  }
  function setAvatar(el, p){
    if(!el) return;
    if(p.photo){el.classList.add('has-photo'); el.style.setProperty('--avatar-photo','url("'+p.photo+'")'); el.textContent='';}
    else {el.classList.remove('has-photo'); el.style.removeProperty('--avatar-photo'); el.textContent=initials(p.fullName);}
  }
  function apply(){
    const {data:p}=profileFromAll();
    document.querySelectorAll('#globalProfileAvatar,.profile-avatar,.tb-av,.avatar-inner,.profile-avatar-lg').forEach(el=>setAvatar(el,p));
    document.querySelectorAll('#globalProfileName').forEach(e=>e.textContent=p.fullName);
    const shared=read(SHARED_KEY,{}); shared.activeProfile=p; shared.profile=p; shared.updatedAt=Date.now(); write(SHARED_KEY,shared);
  }
  function updateAllProfiles(changes){
    const current=profileFromAll().data;
    const next=Object.assign({}, current, changes||{});
    write(PROFILE_KEY, Object.assign(read(PROFILE_KEY,{}), next));
    write(BUYER_KEY, Object.assign(read(BUYER_KEY,{}), next));
    write(ADMIN_KEY, Object.assign(read(ADMIN_KEY,{}), {fullName:next.fullName, username:next.displayName||next.username, email:next.email, phone:next.phone, photoRef: next.photo ? 'automartMediaAvatarV1' : '', bannerRef: next.banner ? 'automartMediaBannerV1' : ''}));
    const s=read(SETTINGS_KEY,{}); const light=Object.assign({}, next); delete light.photo; delete light.banner; light.photoRef=next.photo?'automartMediaAvatarV1':''; light.bannerRef=next.banner?'automartMediaBannerV1':''; s.profile=Object.assign(s.profile||{}, light); delete s.profilePhoto; write(SETTINGS_KEY,s);
    apply();
  }
  function confirmBox(title,message,confirmText,cb){
    if(window.AutoMartConfirm) return window.AutoMartConfirm(title||'Confirm Changes',message||'Do you want to continue?',confirmText||'Confirm',cb);
    if(confirm(message||'Do you want to continue?')) cb&&cb();
  }
  function exportData(){
    const payload={profile:profileFromAll().data,settings:read(SETTINGS_KEY,{}),shared:read(SHARED_KEY,{}),exportedAt:new Date().toISOString()};
    const blob=new Blob([JSON.stringify(payload,null,2)],{type:'application/json'});
    const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='automart-my-data.json'; document.body.appendChild(a); a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(a.href),1000);
  }
  function deactivate(){confirmBox('Deactivate Account','Are you sure you want to deactivate your account?','Deactivate',()=>{updateAllProfiles({deactivated:true,deactivatedAt:new Date().toISOString()}); localStorage.removeItem('automart_session'); location.href='/logout';});}
  function deleteAccount(){confirmBox('Delete Account','Are you sure you want to delete your account? This action cannot be undone.','Delete',()=>{try{fetch('/profile/delete',{method:'POST'}).finally(()=>{['automart_session','automart_admin_session','adminSession'].forEach(k=>localStorage.removeItem(k)); updateAllProfiles({deleted:true,deletedAt:new Date().toISOString(),photo:'',banner:''}); location.href='/auth';});}catch(e){location.href='/profile/delete';}});}
  window.AutoMartApplySharedProfile=apply;
  window.AutoMartUpdateSharedProfile=updateAllProfiles;
  window.AutoMartExportMyData=exportData;
  window.AutoMartDeactivateAccount=deactivate;
  window.AutoMartDeleteAccount=deleteAccount;
  document.addEventListener('DOMContentLoaded',apply);
  window.addEventListener('storage',e=>{if([PROFILE_KEY,BUYER_KEY,ADMIN_KEY,SETTINGS_KEY,OLD_SETTINGS_KEY,SHARED_KEY].includes(e.key))apply();});
})();



// 2026-05 final global notification/chat button reliability patch
(function(){
  const NOTIF_KEY='automartLiveNotificationsV1';
  const MSG_KEY='automartLiveMessagesV1';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function popup(title,message,okText,onConfirm){
    if(window.AutoMartConfirm)return window.AutoMartConfirm(title,message,okText,onConfirm,'✓');
    if(confirm(message))onConfirm&&onConfirm();
  }
  function updateBadges(){
    const n=read(NOTIF_KEY,[]).filter(x=>!x.read).length;
    const m=read(MSG_KEY,[]).filter(x=>!x.read).length;
    document.querySelectorAll('[data-notification-count]').forEach(e=>{e.textContent=n;e.classList.toggle('is-zero',n===0)});
    document.querySelectorAll('[data-message-count]').forEach(e=>{e.textContent=m;e.classList.toggle('is-zero',m===0)});
  }
  function openLive(type){
    const key=type==='messages'?MSG_KEY:NOTIF_KEY;
    const title=type==='messages'?'Messages':'Notifications';
    document.querySelectorAll('.automart-live-panel').forEach(x=>x.remove());
    const items=read(key,[]);
    const p=document.createElement('div');
    p.className='automart-live-panel show';
    p.dataset.type=type;
    p.innerHTML='<div class="automart-live-panel-head"><strong>'+title+'</strong><button type="button" data-mark-read>Mark as read</button></div><div class="automart-live-list">'+(items.length?items.map(x=>'<div class="automart-live-item '+(x.read?'read':'')+'" data-id="'+esc(x.id)+'"><span class="automart-live-dot"></span><div><strong>'+esc(x.title||x.from||'AutoMart')+'</strong><p>'+esc(x.body||x.message||'')+'</p><small>'+esc(x.time||'Now')+'</small></div></div>').join(''):'<div class="automart-live-empty"><b>No '+title.toLowerCase()+' yet</b><span>You are all caught up.</span></div>')+'</div>';
    document.body.appendChild(p);
    p.querySelector('[data-mark-read]').onclick=()=>popup('Confirm Changes','Do you want to mark all '+title.toLowerCase()+' as read?','Confirm',()=>{write(key,items.map(x=>Object.assign({},x,{read:true})));updateBadges();openLive(type);});
    p.querySelectorAll('.automart-live-item').forEach(item=>item.onclick=()=>{write(key,read(key,[]).map(x=>x.id===item.dataset.id?Object.assign({},x,{read:true}):x));item.classList.add('read');updateBadges();});
  }
  document.addEventListener('DOMContentLoaded',()=>{if(!localStorage.getItem(NOTIF_KEY))write(NOTIF_KEY,[]);if(!localStorage.getItem(MSG_KEY))write(MSG_KEY,[]);updateBadges();});
  document.addEventListener('click',e=>{
    const btn=e.target.closest('[data-live-panel-toggle]');
    if(!btn)return;
    e.preventDefault();e.stopImmediatePropagation();
    openLive(btn.dataset.livePanelToggle||'notifications');
  },true);
  window.addEventListener('storage',e=>{if([NOTIF_KEY,MSG_KEY].includes(e.key))updateBadges();});
})();



// 2026-05 storage-safe global profile + notification/chat fix
(function(){
  const PROFILE_KEY='automartUserProfileRealV4';
  const BUYER_KEY='automartBuyerProfileRealV1';
  const ADMIN_KEY='automartAdminProfileRealV2';
  const SHARED_KEY='automartSharedMarketplaceDataV2';
  const SETTINGS_KEY='automartSettingsProfileV1';
  const AVATAR_KEY='automartMediaAvatarV1';
  const BANNER_KEY='automartMediaBannerV1';
  const NOTIF_KEY='automartLiveNotificationsV1';
  const MSG_KEY='automartLiveMessagesV1';
  function read(k,f){try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f));}catch{return f;}}
  function write(k,v){localStorage.setItem(k,JSON.stringify(v));}
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function initials(n){return String(n||'AU').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'AU'}
  function role(){return (document.getElementById('globalProfileRole')?.textContent||'').toLowerCase();}
  function activeProfile(){
    const base=role().includes('admin')?read(ADMIN_KEY,{}):role().includes('buyer')?read(BUYER_KEY,read(PROFILE_KEY,{})):read(PROFILE_KEY,{});
    if(base.photoRef&&!base.photo)base.photo=localStorage.getItem(base.photoRef)||'';
    if(!base.photo)base.photo=localStorage.getItem(AVATAR_KEY)||'';
    if(base.bannerRef&&!base.banner)base.banner=localStorage.getItem(base.bannerRef)||'';
    if(!base.banner)base.banner=localStorage.getItem(BANNER_KEY)||'';
    return base;
  }
  function apply(){
    const p=activeProfile();
    const name=(p.fullName||[p.firstName,p.lastName].filter(Boolean).join(' ')||p.displayName||p.username||document.getElementById('globalProfileName')?.textContent||'Account').trim();
    document.querySelectorAll('#globalProfileAvatar,.profile-avatar,.tb-av').forEach(av=>{
      if(p.photo){av.textContent='';av.style.backgroundImage='url("'+p.photo+'")';av.style.backgroundSize='cover';av.style.backgroundPosition='center';}
      else{av.textContent=initials(name);av.style.backgroundImage='';}
    });
    document.querySelectorAll('#globalProfileName').forEach(e=>e.textContent=name);
  }
  function updateBadges(){
    const n=read(NOTIF_KEY,[]).filter(x=>!x.read).length;
    const m=read(MSG_KEY,[]).filter(x=>!x.read).length;
    document.querySelectorAll('[data-notification-count]').forEach(e=>{e.textContent=n;e.classList.toggle('is-zero',n===0)});
    document.querySelectorAll('[data-message-count]').forEach(e=>{e.textContent=m;e.classList.toggle('is-zero',m===0)});
  }
  function popup(title,message,okText,onConfirm){
    if(window.AutoMartConfirm)return window.AutoMartConfirm(title,message,okText,onConfirm,'✓');
    onConfirm&&onConfirm();
  }
  function panel(type){
    const key=type==='messages'?MSG_KEY:NOTIF_KEY,title=type==='messages'?'Messages':'Notifications';
    const old=document.querySelector('.automart-live-panel.show');
    if(old&&old.dataset.type===type){old.remove();return;}
    document.querySelectorAll('.automart-live-panel').forEach(x=>x.remove());
    const items=read(key,[]);
    const p=document.createElement('div');p.className='automart-live-panel show';p.dataset.type=type;
    p.innerHTML='<div class="automart-live-panel-head"><strong>'+title+'</strong><button type="button" data-mark-read>Mark as read</button></div><div class="automart-live-list">'+(items.length?items.map(x=>'<div class="automart-live-item '+(x.read?'read':'')+'" data-id="'+esc(x.id)+'"><span class="automart-live-dot"></span><div><strong>'+esc(x.title||x.from||'AutoMart')+'</strong><p>'+esc(x.body||x.message||'')+'</p><small>'+esc(x.time||'Now')+'</small></div></div>').join(''):'<div class="automart-live-empty"><b>No '+title.toLowerCase()+' yet</b><span>You are all caught up.</span></div>')+'</div>';
    document.body.appendChild(p);
    p.querySelector('[data-mark-read]').onclick=()=>popup('Confirm Changes','Do you want to mark all '+title.toLowerCase()+' as read?','Confirm',()=>{write(key,items.map(x=>Object.assign({},x,{read:true})));updateBadges();panel(type);});
    p.querySelectorAll('.automart-live-item').forEach(item=>item.onclick=()=>{write(key,read(key,[]).map(x=>x.id===item.dataset.id?Object.assign({},x,{read:true}):x));item.classList.add('read');updateBadges();});
  }
  document.addEventListener('DOMContentLoaded',()=>{if(!localStorage.getItem(NOTIF_KEY))write(NOTIF_KEY,[]);if(!localStorage.getItem(MSG_KEY))write(MSG_KEY,[]);apply();updateBadges();});
  document.addEventListener('click',e=>{
    const live=e.target.closest('[data-live-panel-toggle]');
    if(live){e.preventDefault();e.stopImmediatePropagation();panel(live.dataset.livePanelToggle||'notifications');return;}
    if(!e.target.closest('.automart-live-panel')&&!e.target.closest('[data-live-panel-toggle]'))document.querySelectorAll('.automart-live-panel').forEach(x=>x.remove());
  },true);
  window.addEventListener('storage',e=>{if([PROFILE_KEY,BUYER_KEY,ADMIN_KEY,SHARED_KEY,SETTINGS_KEY,AVATAR_KEY,BANNER_KEY,NOTIF_KEY,MSG_KEY].includes(e.key)){apply();updateBadges();}});
})();

