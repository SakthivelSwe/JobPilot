"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.humanDelay = humanDelay;
exports.humanType = humanType;
exports.humanMouseTo = humanMouseTo;
exports.humanClick = humanClick;
/**
 * Small utility toolkit that adds a bit of randomness to timing and typing.
 * This does not "spoof" anything about the browser — it just keeps the pace
 * closer to what a person would naturally do at a keyboard.
 */
function humanDelay(min = 2000, max = 5000) {
    const ms = min + Math.random() * (max - min);
    return new Promise((r) => setTimeout(r, ms));
}
/** Type into an already-focused element one character at a time. */
async function humanType(page, selector, text) {
    await page.click(selector, { delay: 50 });
    for (const ch of text) {
        await page.keyboard.type(ch, { delay: 60 + Math.random() * 90 });
        if (Math.random() < 0.07) {
            // occasional typo → backspace → retype
            await page.keyboard.type(ch, { delay: 30 });
            await page.keyboard.press('Backspace');
        }
    }
}
/** Move mouse to the centre of a locator with a small random offset. */
async function humanMouseTo(page, locator) {
    const box = await locator.boundingBox();
    if (!box)
        return;
    const x = box.x + box.width / 2 + (Math.random() - 0.5) * 6;
    const y = box.y + box.height / 2 + (Math.random() - 0.5) * 6;
    await page.mouse.move(x, y, { steps: 8 + Math.floor(Math.random() * 6) });
}
async function humanClick(page, locator) {
    await humanMouseTo(page, locator);
    await humanDelay(300, 800);
    await locator.click();
}
//# sourceMappingURL=HumanBehavior.js.map