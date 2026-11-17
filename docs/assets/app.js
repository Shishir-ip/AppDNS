/* AppDNS landing page interactions */
(function () {
  'use strict';
  var root = document.documentElement;
  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- theme (black / white, like the app) ---------- */
  var themeMeta = document.querySelector('meta[name="theme-color"]');
  var heroShot = document.getElementById('heroShot');
  function applyTheme(t, persist) {
    root.setAttribute('data-theme', t);
    if (themeMeta) themeMeta.setAttribute('content', t === 'dark' ? '#0a0a0a' : '#ffffff');
    if (heroShot) {
      var want = 'screenshots/' + (t === 'dark' ? 'home-dark.png' : 'home-light.png');
      if (heroShot.getAttribute('src') !== want) {
        heroShot.style.opacity = '0';
        setTimeout(function () {
          heroShot.src = want;
          heroShot.style.opacity = '1';
        }, 180);
      }
    }
    if (persist) { try { localStorage.setItem('appdns-theme', t); } catch (e) {} }
  }
  if (heroShot) heroShot.style.transition = 'opacity .18s ease';
  applyTheme(root.getAttribute('data-theme') || 'light', false);
  var themeBtn = document.getElementById('themeToggle');
  if (themeBtn) themeBtn.addEventListener('click', function () {
    applyTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark', true);
  });

  /* ---------- nav: scrolled state, mobile menu, progress ---------- */
  var nav = document.getElementById('nav');
  var progress = document.getElementById('progress');
  var menuBtn = document.getElementById('menuBtn');
  var navLinks = document.getElementById('navLinks');
  var ticking = false;
  function onScroll() {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(function () {
      var y = window.scrollY;
      if (nav) nav.classList.toggle('scrolled', y > 8);
      if (progress) {
        var max = document.documentElement.scrollHeight - window.innerHeight;
        progress.style.width = (max > 0 ? (y / max) * 100 : 0) + '%';
      }
      ticking = false;
    });
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
  function closeMenu() {
    if (!navLinks) return;
    navLinks.classList.remove('open');
    if (menuBtn) { menuBtn.classList.remove('open'); menuBtn.setAttribute('aria-expanded', 'false'); }
  }
  if (menuBtn && navLinks) {
    menuBtn.addEventListener('click', function () {
      var open = navLinks.classList.toggle('open');
      menuBtn.classList.toggle('open', open);
      menuBtn.setAttribute('aria-expanded', String(open));
    });
    navLinks.addEventListener('click', function (e) { if (e.target.tagName === 'A') closeMenu(); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeMenu(); });
  }

  /* ---------- reveal on scroll ---------- */
  var revealEls = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window && !reduceMotion) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add('visible'); io.unobserve(en.target); }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -30px 0px' });
    revealEls.forEach(function (el) { io.observe(el); });
  } else {
    revealEls.forEach(function (el) { el.classList.add('visible'); });
  }

  /* ---------- 3D hero phone tilt ---------- */
  var tilt = document.getElementById('heroTilt');
  var visual = document.querySelector('.hero-visual');
  if (tilt && visual && !reduceMotion && window.matchMedia('(hover: hover) and (min-width: 960px)').matches) {
    visual.addEventListener('pointermove', function (e) {
      var r = visual.getBoundingClientRect();
      var x = (e.clientX - r.left) / r.width - 0.5;
      var y = (e.clientY - r.top) / r.height - 0.5;
      tilt.style.transform = 'rotateY(' + (x * 16) + 'deg) rotateX(' + (-y * 12) + 'deg)';
    });
    visual.addEventListener('pointerleave', function () { tilt.style.transform = ''; });
  }

  /* ---------- screenshots carousel ---------- */
  var shots = Array.prototype.slice.call(document.querySelectorAll('#shotStack .shot'));
  var tabs = Array.prototype.slice.call(document.querySelectorAll('#shotTabs button'));
  var phone = document.getElementById('shotPhone');
  var caption = document.querySelector('.shots-caption');
  var capTitle = document.getElementById('shotTitle');
  var capDesc = document.getElementById('shotDesc');
  var idx = 0, timer = null;
  function show(n, swing) {
    if (!shots.length) return;
    idx = (n + shots.length) % shots.length;
    shots.forEach(function (s, i) { s.classList.toggle('active', i === idx); });
    tabs.forEach(function (t, i) { t.setAttribute('aria-selected', String(i === idx)); });
    if (caption && capTitle && capDesc) {
      caption.classList.add('fading');
      setTimeout(function () {
        capTitle.textContent = shots[idx].getAttribute('data-title') || '';
        capDesc.textContent = shots[idx].getAttribute('data-desc') || '';
        caption.classList.remove('fading');
      }, 180);
    }
    if (phone && swing && !reduceMotion) {
      phone.classList.remove('swing');
      void phone.offsetWidth;
      phone.classList.add('swing');
    }
    restartAuto();
  }
  function restartAuto() {
    if (timer) clearInterval(timer);
    if (!reduceMotion) timer = setInterval(function () { if (!document.hidden) show(idx + 1, true); }, 5200);
  }
  var prev = document.getElementById('shotPrev');
  var next = document.getElementById('shotNext');
  if (prev) prev.addEventListener('click', function () { show(idx - 1, true); });
  if (next) next.addEventListener('click', function () { show(idx + 1, true); });
  tabs.forEach(function (t, i) { t.addEventListener('click', function () { show(i, true); }); });
  var stage = document.querySelector('.shots-stage');
  if (stage) {
    stage.addEventListener('pointerenter', function () { if (timer) clearInterval(timer); });
    stage.addEventListener('pointerleave', restartAuto);
    stage.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowLeft') { show(idx - 1, true); e.preventDefault(); }
      if (e.key === 'ArrowRight') { show(idx + 1, true); e.preventDefault(); }
    });
    var tx = null;
    stage.addEventListener('touchstart', function (e) { tx = e.touches[0].clientX; }, { passive: true });
    stage.addEventListener('touchend', function (e) {
      if (tx === null) return;
      var dx = e.changedTouches[0].clientX - tx;
      if (Math.abs(dx) > 42) show(idx + (dx < 0 ? 1 : -1), true);
      tx = null;
    }, { passive: true });
  }
  restartAuto();

  /* ---------- FAQ accordion (one open at a time) ---------- */
  document.querySelectorAll('.faq-q').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var item = btn.parentElement;
      var wasOpen = item.classList.contains('open');
      document.querySelectorAll('.faq-item.open').forEach(function (o) {
        o.classList.remove('open');
        o.querySelector('.faq-q').setAttribute('aria-expanded', 'false');
      });
      if (!wasOpen) { item.classList.add('open'); btn.setAttribute('aria-expanded', 'true'); }
    });
  });

  /* ---------- copy ADB command ---------- */
  var copyBtn = document.getElementById('copyAdb');
  var adbCmd = document.getElementById('adbCmd');
  if (copyBtn && adbCmd) {
    copyBtn.addEventListener('click', function () {
      var text = adbCmd.textContent.trim();
      function done() {
        copyBtn.classList.add('copied');
        var label = copyBtn.querySelector('span');
        if (label) label.textContent = 'Copied';
        setTimeout(function () {
          copyBtn.classList.remove('copied');
          if (label) label.textContent = 'Copy';
        }, 2000);
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () { fallback(); });
      } else { fallback(); }
      function fallback() {
        var ta = document.createElement('textarea');
        ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select();
        try { document.execCommand('copy'); done(); } catch (e) {}
        document.body.removeChild(ta);
      }
    });
  }

  /* ---------- live version badge from GitHub releases ---------- */
  fetch('https://api.github.com/repos/Shishir-ip/AppDNS/releases/latest')
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (d) {
      if (d && d.tag_name) {
        document.querySelectorAll('[data-version]').forEach(function (el) { el.textContent = d.tag_name; });
      }
    })
    .catch(function () {});

  /* ---------- footer year ---------- */
  var year = document.getElementById('year');
  if (year) year.textContent = String(new Date().getFullYear());
})();