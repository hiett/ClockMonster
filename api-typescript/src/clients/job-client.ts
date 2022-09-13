import {IdentifiedJob, UnidentifiedJob} from "../types";
import axios, {AxiosInstance} from "axios";

export class JobClient {

  private readonly baseUrl: string;
  private readonly httpClient: AxiosInstance;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
    this.httpClient = axios.create();
    this.httpClient.interceptors.request.use(config => {
      config.url = `${this.baseUrl}${config.url}`;
      return config;
    });
  }

  async createJob(job: UnidentifiedJob): Promise<IdentifiedJob> {
    const {data} = await this.httpClient.post<IdentifiedJob>("/v1/job", job);
    return data;
  }

  async getJob(id: number): Promise<IdentifiedJob> {
    const {data} = await this.httpClient.get<IdentifiedJob>(`/v1/job?id=${id}`);
    return data;
  }

  async deleteJob(id: number): Promise<void> {
    await this.httpClient.delete<IdentifiedJob>(`/v1/job?id=${id}`);
  }
}