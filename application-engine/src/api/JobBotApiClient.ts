import axios, { AxiosInstance } from 'axios';
import { logger } from '../logger';
import { PendingJob, PlatformConfig } from '../types';

/**
 * Authenticated JobBot API client. Refreshes the JWT on 401 by re-logging in
 * with the configured admin credentials, or exits if a static token was
 * provided that has expired.
 */
export class JobBotApiClient {
  private readonly http: AxiosInstance;
  private token: string | null = null;
  private staticToken = false;

  constructor(
    private readonly baseUrl: string,
    private readonly username?: string,
    private readonly password?: string,
    staticToken?: string,
  ) {
    if (staticToken) {
      this.token = staticToken;
      this.staticToken = true;
    }
    this.http = axios.create({ baseURL: baseUrl, timeout: 30_000 });
    this.http.interceptors.request.use((cfg) => {
      if (this.token) cfg.headers.Authorization = `Bearer ${this.token}`;
      return cfg;
    });
  }

  async ensureAuthenticated(): Promise<void> {
    if (this.token) return;
    await this.login();
  }

  private async login(): Promise<void> {
    if (!this.username || !this.password) {
      throw new Error('No API_TOKEN and no API_USERNAME/API_PASSWORD configured.');
    }
    const res = await axios.post(
      `${this.baseUrl}/api/auth/login`,
      { username: this.username, password: this.password },
      { timeout: 15_000 },
    );
    this.token = res.data?.data?.token;
    if (!this.token) throw new Error('Login response missing token');
    logger.info('Authenticated with JobPilot API');
  }

  private async retryOn401<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn();
    } catch (e: any) {
      if (e?.response?.status === 401 && !this.staticToken) {
        logger.warn('401 from API — re-authenticating');
        this.token = null;
        await this.login();
        return await fn();
      }
      throw e;
    }
  }

  /** Returns the next APPROVED job for a platform, or null if 204. */
  async getNextPending(platform: string): Promise<PendingJob | null> {
    return this.retryOn401(async () => {
      const res = await this.http.get(`/api/engine/pending`, {
        params: { platform },
        validateStatus: (s) => s === 200 || s === 204,
      });
      if (res.status === 204) return null;
      return res.data as PendingJob;
    });
  }

  async report(payload: { jobQueueId: string; success: boolean; failureReason?: string | null }): Promise<void> {
    await this.retryOn401(() =>
      this.http.post(`/api/engine/report`, payload),
    );
  }

  async getPlatformConfig(platform: string): Promise<PlatformConfig> {
    return this.retryOn401(async () => {
      const res = await this.http.get(`/api/platform-config/${platform}`);
      return res.data?.data as PlatformConfig;
    });
  }

  /** Fetches the raw session state (cookies) from the backend. */
  async getSessionState(platform: string): Promise<any | null> {
    return this.retryOn401(async () => {
      const res = await this.http.get(`/api/engine/session`, {
        params: { platform },
        validateStatus: (s) => s === 200 || s === 204,
      });
      if (res.status === 204) return null;
      // Spring might return it as a string if not mapped correctly, but if it parses as JSON, Axios gives us an object.
      return typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
    });
  }
}

