"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.NaukriApplicator = void 0;
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
const logger_1 = require("../logger");
const HumanBehavior_1 = require("./HumanBehavior");
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
/**
 * Domains that indicate Naukri is redirecting to an external company ATS.
 * These jobs cannot be applied to via the Naukri flow — they're sent to MANUAL.
 */
const COMPANY_ATS_DOMAINS = [
    'greenhouse.io', 'lever.co', 'workday.com', 'myworkdayjobs.com',
    'taleo.net', 'icims.com', 'smartrecruiters.com', 'jobvite.com',
    'successfactors.com', 'brassring.com', 'infytq.com', 'infosys.com',
    'tcs.com', 'wipro.com', 'hcltech.com', 'accenture.com',
];
class NaukriApplicator {
    context;
    isLoggedIn = false;
    constructor(context) {
        this.context = context;
    }
    async apply(job) {
        const page = await this.context.newPage();
        try {
            await (0, HumanBehavior_1.humanDelay)(2000, 4000);
            logger_1.logger.info(`[NAUKRI] Opening ${job.jobUrl}`);
            // BUG FIX: Use domcontentloaded — Naukri never reaches networkidle
            await page.goto(job.jobUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 });
            // Wait for JS-rendered content
            await page.waitForTimeout(4000);
            // Check for CAPTCHA
            if (await this.hasCaptcha(page)) {
                logger_1.logger.warn('[NAUKRI] CAPTCHA detected — waiting 30s for manual solve');
                await (0, HumanBehavior_1.humanDelay)(30_000, 30_000);
                if (await this.hasCaptcha(page))
                    return this.fail('CAPTCHA_UNSOLVED', job, page);
            }
            // Handle login wall if present
            if (await page.locator('input[type=password]').count()) {
                logger_1.logger.warn('[NAUKRI] Encountered login wall. Session is missing or invalid.');
                return this.fail('LOGIN_REQUIRED', job, page);
            }
            // BUG FIX: Detect "Apply on Company Website" redirect BEFORE looking for apply button
            const redirectResult = await this.detectCompanyWebsiteRedirect(page);
            if (redirectResult) {
                logger_1.logger.warn(`[NAUKRI] Job redirects to external ATS (${redirectResult}) — sending to Manual queue`);
                return { success: false, reason: 'COMPANY_WEBSITE_REDIRECT' };
            }
            // Check already applied
            if (await this.isAlreadyApplied(page)) {
                logger_1.logger.info(`[NAUKRI] Already applied to ${job.title}`);
                return { success: false, reason: 'ALREADY_APPLIED' };
            }
            // BUG FIX: Close any open Naukri nav drawer that blocks the Apply button
            await this.closeNavDrawer(page);
            // Find and click apply button
            const applyBtn = await this.findApplyButton(page);
            if (!applyBtn)
                return this.fail('NO_APPLY_BUTTON', job, page);
            // BUG FIX: Use dispatchEvent('click') to bypass Naukri's sticky nav overlay
            // that intercepts pointer events when using normal mouse-click
            logger_1.logger.info(`[NAUKRI] Clicking Apply button for ${job.title}`);
            await applyBtn.dispatchEvent('click');
            await (0, HumanBehavior_1.humanDelay)(3000, 5000);
            // Handle any popup / modal that appeared after clicking Apply
            await this.handleApplyModal(page, job);
            // Take success screenshot
            const successCheck = await page.locator('text=Applied Successfully, text=You have applied, text=Application Submitted, text=successfully applied, text=applied on').count();
            const shot = await this.screenshot(page, job, successCheck > 0 ? 'success' : 'unclear');
            if (successCheck > 0) {
                logger_1.logger.info(`[NAUKRI] ✅ Applied: ${job.title} @ ${job.company}`);
                return { success: true, screenshotPath: shot };
            }
            // Additional check: look for chatbot "Applied" confirmation
            const chatbotSuccess = await page.locator('.chatbot_DrawerContentWrapper, [class*="applySuccess"], [class*="apply-success"]').count();
            if (chatbotSuccess > 0) {
                logger_1.logger.info(`[NAUKRI] ✅ Applied via chatbot: ${job.title} @ ${job.company}`);
                return { success: true, screenshotPath: shot };
            }
            // Final check: look for thank you page or confirmation text
            const thankYou = await page.locator('text=Thank you, text=application has been, text=Application sent, [class*="thankYou"], [class*="thank-you"]').count();
            if (thankYou > 0) {
                logger_1.logger.info(`[NAUKRI] ✅ Applied (thank you page): ${job.title} @ ${job.company}`);
                return { success: true, screenshotPath: shot };
            }
            return { success: false, reason: 'SUBMIT_UNCONFIRMED', screenshotPath: shot };
        }
        catch (e) {
            logger_1.logger.warn(`[NAUKRI] Exception applying to ${job.id}: ${e.message}`);
            return { success: false, reason: `EXCEPTION: ${e.message}` };
        }
        finally {
            await page.close({ runBeforeUnload: true }).catch(() => void 0);
        }
    }
    /**
     * BUG FIX: Detect if this job redirects to a company's own ATS website.
     * Naukri shows a button labeled "Apply on Company Website" or similar,
     * with an href pointing to an external domain.
     * Returns the matched domain string if detected, null if it's a normal Naukri job.
     */
    async detectCompanyWebsiteRedirect(page) {
        try {
            // Check for explicit "Apply on company website" button text
            const redirectBtn = page.locator('a:has-text("Apply on Company Website"), a:has-text("Apply on company website"), ' +
                'button:has-text("Apply on Company Website"), a:has-text("Apply at"), ' +
                '[class*="companyApply"], [class*="company-apply"]');
            if (await redirectBtn.count() > 0) {
                const href = await redirectBtn.first().getAttribute('href').catch(() => null);
                if (href) {
                    for (const domain of COMPANY_ATS_DOMAINS) {
                        if (href.includes(domain))
                            return domain;
                    }
                    // Even if domain not in our list, it's still an external redirect
                    if (href.startsWith('http') && !href.includes('naukri.com')) {
                        return new URL(href).hostname;
                    }
                }
                return 'external-company-site';
            }
            // Check if the page URL itself is no longer on naukri (we got redirected)
            const currentUrl = page.url();
            if (!currentUrl.includes('naukri.com')) {
                for (const domain of COMPANY_ATS_DOMAINS) {
                    if (currentUrl.includes(domain))
                        return domain;
                }
                return new URL(currentUrl).hostname;
            }
            return null;
        }
        catch {
            return null;
        }
    }
    /**
     * BUG FIX: Close Naukri's navigation drawer that overlays the Apply button.
     * The drawer-wrapper div intercepts pointer events, making normal clicks fail.
     */
    async closeNavDrawer(page) {
        try {
            // Close any open drawer by pressing Escape
            await page.keyboard.press('Escape');
            await page.waitForTimeout(500);
            // Also try clicking away from the header area to dismiss dropdowns
            await page.mouse.click(100, 500);
            await page.waitForTimeout(500);
        }
        catch {
            // Ignore — best effort
        }
    }
    /**
     * Handles the post-apply modal/chatbot on Naukri.
     * Naukri's modern Easy Apply shows a chatbot with questions we need to fill.
     */
    async handleApplyModal(page, job) {
        await (0, HumanBehavior_1.humanDelay)(2000, 3000);
        // Handle chatbot questions in a loop (up to 10 rounds)
        for (let round = 0; round < 10; round++) {
            const hasQuestion = await page.locator('.chatbot_Input input, .chatbot_Input textarea, ' +
                '[class*="chatbot"] input, [class*="chatbot"] textarea, ' +
                '.chatbot_DrawerContentWrapper input, ' +
                'input[placeholder*="Answer"], textarea[placeholder*="Answer"]').count();
            if (!hasQuestion)
                break;
            // Get the question text
            const questionEl = page.locator('.chatbot_Text, [class*="chatbot"] .bubble, .bot-message, ' +
                '[class*="message"] p, [class*="question"] p').last();
            const questionText = await questionEl.textContent().catch(() => '');
            logger_1.logger.info(`[NAUKRI] Chatbot question: ${questionText}`);
            const answer = await this.answerQuestion(questionText || '', job);
            logger_1.logger.info(`[NAUKRI] Answering: "${questionText}" → "${answer}"`);
            const inputSel = '.chatbot_Input input, .chatbot_Input textarea, [class*="chatbot"] input:visible, [class*="chatbot"] textarea:visible';
            const inputEl = page.locator(inputSel).last();
            if (await inputEl.count()) {
                await inputEl.click();
                await (0, HumanBehavior_1.humanDelay)(300, 500);
                await inputEl.fill(answer);
                await (0, HumanBehavior_1.humanDelay)(500, 1000);
                const sendBtn = page.locator('.chatbot_Input button, button:has-text("Send"), ' +
                    'button[class*="send"], button[class*="submit" i]:visible, ' +
                    'button:has-text("Next"), button:has-text("Submit")').last();
                if (await sendBtn.count()) {
                    await sendBtn.dispatchEvent('click');
                    await (0, HumanBehavior_1.humanDelay)(1500, 2500);
                }
            }
            else {
                const selectEl = page.locator('[class*="chatbot"] select:visible').last();
                if (await selectEl.count()) {
                    const options = await selectEl.locator('option').allTextContents();
                    const bestOption = this.pickBestOption(options, questionText || '');
                    await selectEl.selectOption({ label: bestOption });
                    await (0, HumanBehavior_1.humanDelay)(800, 1200);
                }
                else {
                    break;
                }
            }
        }
        await this.handleResumeSelection(page);
        // Click final submit if available — use dispatchEvent to avoid overlay issues
        for (const submitSel of [
            'button:has-text("Submit")',
            'button:has-text("Apply")',
            'button:has-text("Send Application")',
            '.chatbot_Submit',
            'button[class*="apply" i]:visible',
        ]) {
            const btn = page.locator(submitSel).last();
            if (await btn.count()) {
                await btn.dispatchEvent('click');
                await (0, HumanBehavior_1.humanDelay)(3000, 5000);
                break;
            }
        }
    }
    async handleResumeSelection(page) {
        const resumeModal = await page.locator('[class*="resumeSelect"], [class*="resume-select"], ' +
            'div:has-text("Select Resume"), div:has-text("Choose Resume")').count();
        if (!resumeModal)
            return;
        const resumeOption = page.locator('[class*="resumeItem"]:first-child, .resume-card:first-child').first();
        if (await resumeOption.count()) {
            await resumeOption.dispatchEvent('click');
            await (0, HumanBehavior_1.humanDelay)(1000, 2000);
        }
        const fileInput = page.locator('input[type="file"]').first();
        if (await fileInput.count() && fs_1.default.existsSync(CANDIDATE.resumePath)) {
            try {
                await fileInput.setInputFiles(CANDIDATE.resumePath);
                await (0, HumanBehavior_1.humanDelay)(2000, 3000);
                logger_1.logger.info('[NAUKRI] Uploaded resume from ' + CANDIDATE.resumePath);
            }
            catch (e) {
                logger_1.logger.warn('[NAUKRI] Resume upload failed: ' + e.message);
            }
        }
    }
    async answerQuestion(question, job) {
        const q = question.toLowerCase();
        if (q.includes('experience') || q.includes('years'))
            return CANDIDATE.experience;
        if (q.includes('current ctc') || q.includes('current salary') || q.includes('current package'))
            return CANDIDATE.currentCTC;
        if (q.includes('expected ctc') || q.includes('expected salary') || q.includes('expected package'))
            return CANDIDATE.expectedCTC;
        if (q.includes('notice'))
            return CANDIDATE.noticePeriod;
        if (q.includes('location') || q.includes('city'))
            return CANDIDATE.currentLocation;
        if (q.includes('name'))
            return CANDIDATE.name;
        if (q.includes('phone') || q.includes('mobile') || q.includes('contact'))
            return CANDIDATE.phone;
        if (q.includes('skill'))
            return CANDIDATE.skills;
        if (q.includes('relocation'))
            return 'Yes, open to relocation';
        if (q.includes('remote') || q.includes('work from home'))
            return 'Yes, comfortable with remote or hybrid';
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
            const data = await res.json();
            const text = data?.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
            if (text)
                return text.substring(0, 200);
        }
        catch (e) {
            logger_1.logger.warn(`[NAUKRI] Gemini AI failed for question "${question}": ${e.message}`);
        }
        return `I have ${CANDIDATE.experience} years of experience in ${CANDIDATE.skills}`;
    }
    pickBestOption(options, question) {
        const q = question.toLowerCase();
        if (q.includes('notice')) {
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
    async hasCaptcha(page) {
        return (await page.locator('iframe[src*="recaptcha"], .g-recaptcha, iframe[title*="captcha" i], [class*="captcha"]').count()) > 0;
    }
    async isAlreadyApplied(page) {
        return (await page.locator('text=Already Applied, text=You applied on, [class*="alreadyApplied"], button[disabled]:has-text("Applied")').count()) > 0;
    }
    async findApplyButton(page) {
        // BUG FIX: Updated selectors to match current Naukri HTML structure (2024-2026)
        const candidates = [
            '#apply-button',
            'button.styles_apply-button__uJI3A',
            'button.apply-button',
            // New Naukri layout selectors
            'button[class*="styles_apply-button"]',
            'button[class*="apply-button"]',
            'button[id*="apply"]',
            'button:has-text("Apply")',
            'button:has-text("Easy Apply")',
            'button:has-text("Apply for this job")',
            '[class*="applyBtn"]',
            '[class*="apply-btn"]',
            // Do NOT match "Apply on Company Website" — that would be a false positive
        ];
        for (const sel of candidates) {
            const l = page.locator(sel).first();
            try {
                if (await l.count() && await l.isVisible({ timeout: 2000 })) {
                    // Verify it's not a company website redirect button
                    const text = (await l.textContent() || '').toLowerCase();
                    if (text.includes('company website') || text.includes('apply at'))
                        continue;
                    return l;
                }
            }
            catch {
                continue;
            }
        }
        return null;
    }
    async screenshot(page, job, kind) {
        const dir = process.env.SCREENSHOT_DIR || './logs/screenshots';
        if (!fs_1.default.existsSync(dir))
            fs_1.default.mkdirSync(dir, { recursive: true });
        const file = path_1.default.join(dir, `${job.id}-${kind}-${Date.now()}.png`);
        try {
            await page.screenshot({ path: file, fullPage: false });
        }
        catch { /* ignore */ }
        return file;
    }
    async fail(reason, job, page) {
        const shot = await this.screenshot(page, job, `fail-${reason}`);
        logger_1.logger.warn(`[NAUKRI] ❌ Failed: ${job.title} @ ${job.company} — ${reason}`);
        return { success: false, reason, screenshotPath: shot };
    }
}
exports.NaukriApplicator = NaukriApplicator;
//# sourceMappingURL=NaukriApplicator.js.map