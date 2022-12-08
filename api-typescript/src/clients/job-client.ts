import {IdentifiedJob, UnidentifiedJob} from "../types";
import axios, {AxiosError, AxiosInstance} from "axios";

interface JobClientOptions {
  bearerToken?: string
}

export class JobClient {

  private readonly baseUrl: string;
  private readonly httpClient: AxiosInstance;

  constructor(baseUrl: string, options?: JobClientOptions) {
    this.baseUrl = baseUrl;
    this.httpClient = axios.create();
    this.httpClient.interceptors.request.use(config => {
      config.url = `${this.baseUrl}${config.url}`;

      if (!config.headers) {
        config.headers = {};
      }

      if (options?.bearerToken) {
        config.headers["authorization"] = `Bearer ${options.bearerToken}`;
      }

      return config;
    });
  }

  async createJob<T>(job: UnidentifiedJob<T>): Promise<IdentifiedJob<T>> {
    const {data} = await this.httpClient.post<IdentifiedJob<T>>("/v2/job", job);
    return data;
  }

  async getJob<T = unknown>(id: number): Promise<IdentifiedJob<T> | null> {
    try {
      const {data} = await this.httpClient.get<IdentifiedJob<T>>(`/v2/job?id=${id}`);
      return data;
    } catch (e: unknown) {
      if (!axios.isAxiosError(e)) {
        throw e; // Some other error, raise up the chain
      }

      const error = e as AxiosError;
      if (error.response?.status === 404) {
        // Not found
        return null;
      }

      // Other HTTP error, not yet handled here
      throw e;
    }
  }

  async deleteJob(id: number): Promise<void> {
    await this.httpClient.delete<IdentifiedJob>(`/v2/job?id=${id}`);
  }
}