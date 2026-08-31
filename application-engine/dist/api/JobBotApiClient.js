"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.JobBotApiClient = void 0;
const axios_1 = __importDefault(require("axios"));
const logger_1 = require("../logger");
/**
 * Authenticated JobBot API client. Refreshes the JWT on 401 by re-logging in
 * with the configured admin credentials, or exits if a static token was
 * provided that has expired.
 */
class JobBotApiClient {
    baseUrl;
    username;
    password;
    http;
    token = null;
    staticToken = false;
    constructor(baseUrl, username, password, staticToken) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        if (staticToken) {
            this.token = staticToken;
            this.staticToken = true;
        }
        this.http = axios_1.default.create({ baseURL: baseUrl, timeout: 30_000 });
        this.http.interceptors.request.use((cfg) => {
            if (this.token)
                cfg.headers.Authorization = `Bearer ${this.token}`;
            return cfg;
        });
    }
    async ensureAuthenticated() {
        if (this.token)
            return;
        await this.login();
    }
    async login() {
        if (!this.username || !this.password) {
            throw new Error('No API_TOKEN and no API_USERNAME/API_PASSWORD configured.');
        }
        const res = await axios_1.default.post(`${this.baseUrl}/api/auth/login`, { username: this.username, password: this.password }, { timeout: 15_000 });
        this.token = res.data?.data?.token;
        if (!this.token)
            throw new Error('Login response missing token');
        logger_1.logger.info('Authenticated with JobPilot API');
    }
    async retryOn401(fn) {
        try {
            return await fn();
        }
        catch (e) {
            if (e?.response?.status === 401 && !this.staticToken) {
                logger_1.logger.warn('401 from API — re-authenticating');
                this.token = null;
                await this.login();
                return await fn();
            }
            throw e;
        }
    }
    /** Returns the next APPROVED job for a platform, or null if 204. */
    async getNextPending(platform) {
        return this.retryOn401(async () => {
            const res = await this.http.get(`/api/engine/pending`, {
                params: { platform },
                validateStatus: (s) => s === 200 || s === 204,
            });
            if (res.status === 204)
                return null;
            return res.data;
        });
    }
    async report(payload) {
        await this.retryOn401(() => this.http.post(`/api/engine/report`, payload));
    }
    async getPlatformConfig(platform) {
        return this.retryOn401(async () => {
            const res = await this.http.get(`/api/platform-config/${platform}`);
            return res.data?.data;
        });
    }
    /** Fetches the raw session state (cookies) from the backend. */
    async getSessionState(platform) {
        return this.retryOn401(async () => {
            const res = await this.http.get(`/api/engine/session`, {
                params: { platform },
                validateStatus: (s) => s === 200 || s === 204,
            });
            if (res.status === 204)
                return null;
            // Spring might return it as a string if not mapped correctly, but if it parses as JSON, Axios gives us an object.
            return typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
        });
    }
}
exports.JobBotApiClient = JobBotApiClient;
//# sourceMappingURL=JobBotApiClient.js.map