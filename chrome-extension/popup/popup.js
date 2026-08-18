const $ = (id) => document.getElementById(id);

async function load() {
  const cfg = await chrome.storage.local.get([
    'apiBase',
    'token',
    'paused',
    'todayCount',
    'todayDate',
  ]);
  $('api-base').value = cfg.apiBase || 'http://localhost:8080';
  $('token').value = cfg.token || '';
  $('today-count').textContent = cfg.todayCount || 0;
  const dot = $('status-dot');
  dot.className = 'dot ' + (cfg.token && !cfg.paused ? 'dot-ok' : 'dot-err');
  $('pause-toggle').textContent = cfg.paused ? 'Resume polling' : 'Pause polling';
}

$('save').addEventListener('click', async () => {
  await chrome.storage.local.set({
    apiBase: $('api-base').value.trim(),
    token: $('token').value.trim(),
  });
  await load();
});

$('pause-toggle').addEventListener('click', async () => {
  const { paused } = await chrome.storage.local.get('paused');
  await chrome.storage.local.set({ paused: !paused });
  await load();
});

$('poll-now').addEventListener('click', async () => {
  chrome.runtime.sendMessage({ action: 'POLL_NOW' });
});

void load();

