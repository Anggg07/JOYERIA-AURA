;(function () {
  try {
    var stored = localStorage.getItem('aura-theme')
    var dark = stored === 'dark' || (!stored && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)
    if (dark) document.documentElement.classList.add('dark')
  } catch (e) {}
})()
