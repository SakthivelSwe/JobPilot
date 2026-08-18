// JobPilot LinkedIn Apply — MV3 service worker.
// Polls the backend on an alarm; when a LinkedIn job comes back, sends it to
// the content script running on linkedin.com. All API access happens through
// the user's own JWT stored in chrome.storage.local.

const DEFAULTS = {
  apiBase: 'http://localhost:8080',
  pollMinutes: 0.5,
  paused: false,
  todayCount: 0,
  todayDate: null, // 'YYYY-MM-DD'
};

async function getConfig() {
  const stored = await chrome.storage.local.get(['apiBase', 'token', 'paused', 'todayCount', 'todayDate']);
  return { ...DEFAULTS, ...stored };
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

async function incrementToday() {
  const cfg = await getConfig();
  const today = todayIso();
  if (cfg.todayDate !== today) {
    await chrome.storage.local.set({ todayCount: 1, todayDate: today });
  } else {
    await chrome.storage.local.set({ todayCount: (cfg.todayCount || 0) + 1 });
  }
}

async function api(path, opts = {}) {
  const { apiBase, token } = await getConfig();
  if (!token) throw new Error('No JobPilot token — open the extension popup and paste one.');
  const res = await fetch(`${apiBase}${path}`, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(opts.headers || {}),
    },
  });
  return res;
}

async function poll() {
  try {
    const cfg = await getConfig();
    if (cfg.paused) return;

    const res = await api('/api/engine/pending?platform=LINKEDIN');
    if (res.status === 204) return; // nothing pending
    if (!res.ok) {
      console.warn('JobPilot pending failed', res.status);
      return;
    }
    const job = await res.json();
    console.log('JobPilot: picked LinkedIn job', job.id, job.title);

    // Try to reuse an open LinkedIn jobs tab
    const tabs = await chrome.tabs.query({ url: 'https://www.linkedin.com/jobs/*' });
    if (tabs.length > 0 && tabs[0].id) {
      await chrome.tabs.update(tabs[0].id, { url: job.jobUrl, active: true });
      // wait a bit for the SPA to settle then message
      setTimeout(() => chrome.tabs.sendMessage(tabs[0].id, { action: 'APPLY', job }), 4000);
    } else {
      const tab = await chrome.tabs.create({ url: job.jobUrl, active: false });
      setTimeout(() => chrome.tabs.sendMessage(tab.id, { action: 'APPLY', job }), 4500);
    }
  } catch (e) {
    console.warn('JobPilot poll error', e.message);
  }
}

chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create('jobpilot-poll', { periodInMinutes: DEFAULTS.pollMinutes });
});
chrome.runtime.onStartup.addListener(() => {
  chrome.alarms.create('jobpilot-poll', { periodInMinutes: DEFAULTS.pollMinutes });
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === 'jobpilot-poll') void poll();
});

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg.action === 'APPLY_RESULT') {
    (async () => {
      try {
        const res = await api('/api/engine/report', {
          method: 'POST',
          body: JSON.stringify({
            jobQueueId: msg.jobQueueId,
            success: msg.success,
            failureReason: msg.reason || null,
          }),
        });
        if (msg.success) await incrementToday();
        sendResponse({ ok: res.ok });
      } catch (e) {
        console.warn('Report error', e.message);
        sendResponse({ ok: false, error: e.message });
      }
    })();
    return true; // async
  }
  if (msg.action === 'POLL_NOW') {
    void poll();
    sendResponse({ ok: true });
    return true;
  }
});

