import { BrowserContext, Page } from 'playwright';
import fs from 'fs';
import path from 'path';
import { logger } from '../logger';
import { ApplyResult, PendingJob } from '../types';
import { humanClick, humanDelay, humanType } from './HumanBehavior';

const CANDIDATE = {
  name: process.env.CANDIDATE_NAME || 'Sakthivel Vinayagam',
  email: process.env.NAUKRI_EMAIL || 'mohanapriya11052@gmail.com',
  password: process.env.NAUKRI_PASSWORD || 'Mohana@123#',
  phone: process.env.CANDIDATE_PHONE || '8428731729',
  experience: process.env.CANDIDATE_EXPERIENCE || '3',
  currentCTC: process.env.CANDIDATE_CURRENT_CTC || '10',
  expectedCTC: process.env.CANDIDATE_EXPECTED_CTC || '15',
  noticePeriod: process.env.CANDIDATE_NOTICE_PERIOD || '15',
  currentLocation: process.env.CANDIDATE_LOCATION || 'Chennai',
  skills: 'Java, Spring Boot, Kafka, Microservices, AWS, Kubernetes',
  resumePath: process.env.RESUME_PATH || 'C:\\Resume\\Mohana priya_Resume.pdf',
};

const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`;

export class NaukriApplicator {
  private isLoggedIn = false;

  constructor(private readonly context: BrowserContext) {}

  async apply(job: PendingJob): Promise<ApplyResult> {
    const page = await this.context.newPage();
    try {
      await humanDelay(2000, 4000);
      logger.info(`[NAUKRI] Opening ${job.jobUrl}`);
      await page.goto(job.jobUrl, { waitUntil: 'networkidle', timeout: 45_000 });
      // Wait an extra 3 seconds for JS-rendered apply button
      await page.waitForTimeout(3000);

      // Check for CAPTCHA
      if (await this.hasCaptcha(page)) {
        logger.warn('[NAUKRI] CAPTCHA detected — waiting 30s for manual solve');
        await humanDelay(30_000, 30_000);
        if (await this.hasCaptcha(page)) return this.fail('CAPTCHA_UNSOLVED', job, page);
      }

      // Handle login wall if present
      if (await page.locator('input[type=password]').count()) {
        await this.doLogin(page);
      }

      // Check already applied
      if (await this.isAlreadyApplied(page)) {
        logger.info(`[NAUKRI] Already applied to ${job.title}`);
        return { success: false, reason: 'ALREADY_APPLIED' };
      }

      // Find and click apply button
      const applyBtn = await this.findApplyButton(page);
      if (!applyBtn) return this.fail('NO_APPLY_BUTTON', job, page);

      await humanClick(page, applyBtn);
      await humanDelay(3000, 5000);

      // Handle any popup / modal that appeared after clicking Apply
      await this.handleApplyModal(page, job);

      // Take success screenshot
      const successCheck = await page.locator(
        'text=Applied Successfully, text=You have applied, text=Application Submitted, text=successfully applied, text=applied on'
      ).count();

      const shot = await this.screenshot(page, job, successCheck > 0 ? 'success' : 'unclear');

      if (successCheck > 0) {
        logger.info(`[NAUKRI] ✅ Applied: ${job.title} @ ${job.company}`);
        return { success: true, screenshotPath: shot };
      }

      // Additional check: look for chatbot "Applied" confirmation  
      const chatbotSuccess = await page.locator('.chatbot_DrawerContentWrapper, [class*="applySuccess"]').count();
      if (chatbotSuccess > 0) {
        logger.info(`[NAUKRI] ✅ Applied via chatbot: ${job.title} @ ${job.company}`);
        return { success: true, screenshotPath: shot };
      }

      return { success: false, reason: 'SUBMIT_UNCONFIRMED', screenshotPath: shot };
    } catch (e: any) {
      logger.warn(`[NAUKRI] Exception applying to ${job.id}: ${e.message}`);
      return { success: false, reason: `EXCEPTION: ${e.message}` };
    } finally {
      await page.close({ runBeforeUnload: true }).catch(() => void 0);
    }
  }

  private async doLogin(page: Page): Promise<void> {
    logger.info('[NAUKRI] Login wall — filling credentials');
    try {
      const emailSel = 'input#usernameField, input[name=email], input[type=email], input[placeholder*="Email" i]';
      const passSel = 'input#passwordField, input[name=password], input[type=password]';
      await humanType(page, emailSel, CANDIDATE.email);
      await humanDelay(600, 900);
      await humanType(page, passSel, CANDIDATE.password);
      await humanDelay(600, 1000);
      const submitBtn = page.locator('button[type=submit], button:has-text("Login"), button:has-text("Sign in")').first();
      await humanClick(page, submitBtn);
      await humanDelay(4000, 6000);
      this.isLoggedIn = true;
      logger.info('[NAUKRI] Login completed');
    } catch (e: any) {
      logger.warn(`[NAUKRI] Login failed: ${e.message}`);
    }
  }

  /**
   * Handles the post-apply modal/chatbot on Naukri.
   * Naukri's modern Easy Apply shows a chatbot with questions we need to fill.
   */
  private async handleApplyModal(page: Page, job: PendingJob): Promise<void> {
    // Wait a bit for the modal to open
    await humanDelay(2000, 3000);

    // Handle chatbot questions in a loop (up to 10 rounds)
    for (let round = 0; round < 10; round++) {
      const hasQuestion = await page.locator(
        '.chatbot_Input input, .chatbot_Input textarea, ' +
        '[class*="chatbot"] input, [class*="chatbot"] textarea, ' +
        '.chatbot_DrawerContentWrapper input, ' +
        'input[placeholder*="Answer"], textarea[placeholder*="Answer"]'
      ).count();

      if (!hasQuestion) break;

      // Get the question text
      const questionEl = page.locator(
        '.chatbot_Text, [class*="chatbot"] .bubble, .bot-message, ' +
        '[class*="message"] p, [class*="question"] p'
      ).last();
      const questionText = await questionEl.textContent().catch(() => '');

      logger.info(`[NAUKRI] Chatbot question: ${questionText}`);

      // Determine answer using AI or defaults
      const answer = await this.answerQuestion(questionText || '', job);
      logger.info(`[NAUKRI] Answering: "${questionText}" → "${answer}"`);

      // Fill the input
      const inputSel = '.chatbot_Input input, .chatbot_Input textarea, [class*="chatbot"] input:visible, [class*="chatbot"] textarea:visible';
      const inputEl = page.locator(inputSel).last();

      if (await inputEl.count()) {
        await inputEl.click();
        await humanDelay(300, 500);
        await inputEl.fill(answer);
        await humanDelay(500, 1000);

        // Click send/next button
        const sendBtn = page.locator(
          '.chatbot_Input button, button:has-text("Send"), ' +
          'button[class*="send"], button[class*="submit" i]:visible, ' +
          'button:has-text("Next"), button:has-text("Submit")'
        ).last();
        if (await sendBtn.count()) {
          await humanClick(page, sendBtn);
          await humanDelay(1500, 2500);
        }
      } else {
        // Maybe it's a select/dropdown
        const selectEl = page.locator('[class*="chatbot"] select:visible').last();
        if (await selectEl.count()) {
          const options = await selectEl.locator('option').allTextContents();
          const bestOption = this.pickBestOption(options, questionText || '');
          await selectEl.selectOption({ label: bestOption });
          await humanDelay(800, 1200);
        } else {
          break; // No fillable element found
        }
      }
    }

    // Handle resume selection modal if it appears
    await this.handleResumeSelection(page);

    // Click final submit if available
    for (const submitSel of [
      'button:has-text("Submit")',
      'button:has-text("Apply")',
      'button:has-text("Send Application")',
      '.chatbot_Submit',
      'button[class*="apply" i]:visible',
    ]) {
      const btn = page.locator(submitSel).last();
      if (await btn.count()) {
        await humanClick(page, btn);
        await humanDelay(3000, 5000);
        break;
      }
    }
  }

  private async handleResumeSelection(page: Page): Promise<void> {
    // If Naukri shows a resume picker
    const resumeModal = await page.locator(
      '[class*="resumeSelect"], [class*="resume-select"], ' +
      'div:has-text("Select Resume"), div:has-text("Choose Resume")'
    ).count();
    
    if (!resumeModal) return;

    // Try to select the most recent resume
    const resumeOption = page.locator('[class*="resumeItem"]:first-child, .resume-card:first-child').first();
    if (await resumeOption.count()) {
      await humanClick(page, resumeOption);
      await humanDelay(1000, 2000);
    }

    // If there's a file upload option and resume not on Naukri, upload it
    const fileInput = page.locator('input[type="file"]').first();
    if (await fileInput.count() && fs.existsSync(CANDIDATE.resumePath)) {
      try {
        await fileInput.setInputFiles(CANDIDATE.resumePath);
        await humanDelay(2000, 3000);
        logger.info('[NAUKRI] Uploaded resume from ' + CANDIDATE.resumePath);
      } catch (e: any) {
        logger.warn('[NAUKRI] Resume upload failed: ' + e.message);
      }
    }
  }

  /**
   * Answer a question using Gemini AI, with fallback to keyword-based defaults.
   */
  private async answerQuestion(question: string, job: PendingJob): Promise<string> {
    const q = question.toLowerCase();

    // Fast keyword-based defaults first (no API call needed)
    if (q.includes('experience') || q.includes('years')) return CANDIDATE.experience;
    if (q.includes('current ctc') || q.includes('current salary') || q.includes('current package')) return CANDIDATE.currentCTC;
    if (q.includes('expected ctc') || q.includes('expected salary') || q.includes('expected package')) return CANDIDATE.expectedCTC;
    if (q.includes('notice') ) return CANDIDATE.noticePeriod;
    if (q.includes('location') || q.includes('city')) return CANDIDATE.currentLocation;
    if (q.includes('name')) return CANDIDATE.name;
    if (q.includes('phone') || q.includes('mobile') || q.includes('contact')) return CANDIDATE.phone;
    if (q.includes('skill')) return CANDIDATE.skills;
    if (q.includes('relocation')) return 'Yes, open to relocation';
    if (q.includes('remote') || q.includes('work from home')) return 'Yes, comfortable with remote or hybrid';

    // Use Gemini for complex questions
    try {
      const prompt = `Answer this job application question professionally and concisely in first person.
Candidate Profile: ${CANDIDATE.name}, Java Backend Developer, ${CANDIDATE.experience} years experience, 
Skills: ${CANDIDATE.skills}, Expected CTC: ${CANDIDATE.expectedCTC} LPA, Notice: ${CANDIDATE.noticePeriod} days.
Job: ${job.title} at ${job.company}.

Question: "${question}"

Answer in under 100 words. Be direct and professional.`;

      const res = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
      });
      const data = await res.json() as any;
      const text = data?.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
      if (text) return text.substring(0, 200); // truncate if too long
    } catch (e: any) {
      logger.warn(`[NAUKRI] Gemini AI failed for question "${question}": ${e.message}`);
    }

    // Generic fallback
    return `I have ${CANDIDATE.experience} years of experience in ${CANDIDATE.skills}`;
  }

  /** Pick the best option from a dropdown based on the question context */
  private pickBestOption(options: string[], question: string): string {
    const q = question.toLowerCase();
    if (q.includes('notice')) {
      // Find the shortest notice period option
      const sorted = options.filter(o => o !== '').sort((a, b) => {
        const numA = parseInt(a) || 999;
        const numB = parseInt(b) || 999;
        return numA - numB;
      });
      return sorted[0] || options[0];
    }
    if (q.includes('relocation')) {
      return options.find(o => o.toLowerCase().includes('yes')) || options[0];
    }
    return options[options.length > 1 ? 1 : 0] || '';
  }

  private async hasCaptcha(page: Page): Promise<boolean> {
    return (await page.locator('iframe[src*="recaptcha"], .g-recaptcha, iframe[title*="captcha" i], [class*="captcha"]').count()) > 0;
  }

  private async isAlreadyApplied(page: Page): Promise<boolean> {
    return (await page.locator('text=Already Applied, text=You applied on, [class*="alreadyApplied"], button[disabled]:has-text("Applied")').count()) > 0;
  }

  private async findApplyButton(page: Page) {
    const candidates = [
      '#apply-button',                         // Confirmed: Naukri's main apply button ID
      'button.apply-button',                   // Confirmed: Naukri apply button class
      'button.styles_apply-button__uJI3A',     // Confirmed: Naukri scoped class
      'button:has-text("Apply")',
      'button:has-text("Easy Apply")',
      'button:has-text("Apply for this job")',
      'a:has-text("Apply")',
      'button[id*="apply" i]',
      '[class*="applyBtn"]',
      '[class*="apply-btn"]',
    ];
    for (const sel of candidates) {
      const l = page.locator(sel).first();
      if (await l.count() && await l.isVisible()) return l;
    }
    return null;
  }

  private async screenshot(page: Page, job: PendingJob, kind: string) {
    const dir = process.env.SCREENSHOT_DIR || './logs/screenshots';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const file = path.join(dir, `${job.id}-${kind}-${Date.now()}.png`);
    try { await page.screenshot({ path: file, fullPage: false }); } catch { /* ignore */ }
    return file;
  }

  private async fail(reason: string, job: PendingJob, page: Page): Promise<ApplyResult> {
    const shot = await this.screenshot(page, job, `fail-${reason}`);
    logger.warn(`[NAUKRI] ❌ Failed: ${job.title} @ ${job.company} — ${reason}`);
    return { success: false, reason, screenshotPath: shot };
  }
}
