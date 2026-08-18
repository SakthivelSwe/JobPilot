// JobPilot LinkedIn Easy Apply content script.
// Runs on linkedin.com/jobs/*. Waits for APPLY messages, then clicks
// Easy Apply → walks the modal → Submit → reports the outcome back to the
// service worker.

const randomDelay = (min, max) =>
  new Promise((r) => setTimeout(r, min + Math.random() * (max - min)));

function reportResult(jobQueueId, success, reason) {
  chrome.runtime.sendMessage({
    action: 'APPLY_RESULT',
    jobQueueId,
    success,
    reason: reason || null,
  });
}

async function waitFor(selector, timeout = 8000) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    const el = document.querySelector(selector);
    if (el) return el;
    await randomDelay(150, 300);
  }
  return null;
}

async function applyToJob(job) {
  // Easy Apply button variants that LinkedIn has shipped over time.
  const easyBtn =
    document.querySelector('button.jobs-apply-button[aria-label*="Easy Apply"]') ||
    document.querySelector('button[aria-label*="Easy Apply"]') ||
    document.querySelector('button.jobs-apply-button');

  if (!easyBtn) {
    reportResult(job.id, false, 'NO_EASY_APPLY_BUTTON');
    return;
  }
  if (easyBtn.textContent && easyBtn.textContent.toLowerCase().includes('applied')) {
    reportResult(job.id, false, 'ALREADY_APPLIED');
    return;
  }
  easyBtn.click();
  await randomDelay(1500, 2500);

  // Walk the multi-step modal: click Next / Review until Submit appears, or bail.
  for (let step = 0; step < 12; step++) {
    await randomDelay(900, 1800);

    const submit =
      document.querySelector('button[aria-label="Submit application"]') ||
      Array.from(document.querySelectorAll('button')).find(
        (b) => b.textContent && b.textContent.trim().toLowerCase() === 'submit application',
      );
    if (submit) {
      submit.click();
      await randomDelay(2500, 3500);
      break;
    }

    const next =
      document.querySelector('button[aria-label="Continue to next step"]') ||
      document.querySelector('button[aria-label="Review your application"]') ||
      Array.from(document.querySelectorAll('button')).find(
        (b) => b.textContent && ['next', 'continue', 'review'].includes(b.textContent.trim().toLowerCase()),
      );

    if (!next) {
      // Might be a required-field-only step JobPilot cannot answer safely.
      const required = document.querySelector('.artdeco-inline-feedback--error');
      if (required) {
        reportResult(job.id, false, 'REQUIRES_MANUAL_INPUT');
        // Close the modal so the tab isn't stuck.
        const dismiss = document.querySelector('button[aria-label="Dismiss"]');
        if (dismiss) dismiss.click();
        return;
      }
      break;
    }
    next.click();
  }

  await randomDelay(1500, 2500);
  const successText = document.body.innerText.toLowerCase();
  const success =
    successText.includes('application sent') ||
    successText.includes('application was sent') ||
    successText.includes('your application was sent');

  reportResult(job.id, success, success ? null : 'SUBMIT_UNCONFIRMED');
}

chrome.runtime.onMessage.addListener(async (msg) => {
  if (msg.action !== 'APPLY') return;
  // Give the LinkedIn SPA a moment to swap in the target job.
  await randomDelay(2500, 4000);
  try {
    await applyToJob(msg.job);
  } catch (e) {
    reportResult(msg.job.id, false, `EXCEPTION: ${e.message}`);
  }
});

