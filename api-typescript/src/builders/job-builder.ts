import {JobClient} from "../clients/job-client";
import {ActionConfiguration, IdentifiedJob, TimeConfiguration} from "../types";

export class JobBuilder<T = object> {

  private readonly client: JobClient;

  public time: TimeConfiguration;
  public action: ActionConfiguration;
  public payload: T | null;

  constructor(client: JobClient) {
    this.client = client;

    this.time = {
      type: "ONCE",
      firstRunUnix: 0,
    };
    this.action = {
      type: "HTTP",
      url: "",
    };
    this.payload = null;
  }

  once() {
    this.time.type = "ONCE";
    return this;
  }

  repeating(interval = 0, iterations = 1) {
    this.time.type = "REPEATING";
    this.time.interval = interval;
    this.time.iterations = iterations;
    return this;
  }

  withPayload(payload: T) {
    this.payload = payload;
    return this;
  }

  runFirstAt(date: Date) {
    this.time.firstRunUnix = date.getTime() / 1000;
    return this;
  }

  toHttp(url: string) {
    this.action.type = "HTTP";
    this.action.url = url;
    return this;
  }

  async create(): Promise<IdentifiedJob> {
    return this.client.createJob({
      time: this.time,
      action: this.action,
      payload: this.payload,
    });
  }
}