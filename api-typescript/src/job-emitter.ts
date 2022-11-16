import {JobClient} from "./clients";
import {IdentifiedJob, TimeConfiguration, TimeConfigurationModes, UnidentifiedJob} from "./types";

type RelativeTimeConfiguration = TimeConfigurationModes & {
  relativeNextRunUnix: number; // The number of seconds after current that this job should run
};
type JobEmitterConfig = Omit<UnidentifiedJob, "payload" | "time"> & {time: RelativeTimeConfiguration};

export class JobEmitter<T = unknown> {
  private readonly client: JobClient;
  private readonly config: JobEmitterConfig;

  constructor(client: JobClient, config: JobEmitterConfig) {
    this.client = client;
    this.config = config;
  }

  async emit(payload: T): Promise<IdentifiedJob<T>> {
    // Convert the relative time into a current time;
    const fullPayload = {
      ...this.config,
      time: this.createTimeConfiguration(),
      payload,
    };

    return this.client.createJob(fullPayload);
  }

  private createTimeConfiguration(): TimeConfiguration {
    const nextRunUnix = Math.floor(Date.now() / 1000) + this.config.time.relativeNextRunUnix;

    switch(this.config.time.type) {
      case "ONCE":
        return {
          type: "ONCE",
          nextRunUnix,
        };
      case "REPEATING":
        return {
          type: "REPEATING",
          iterations: this.config.time.iterations,
          interval: this.config.time.interval,
          nextRunUnix
        };
      default:
        throw new Error('Unknown time type was provided. Must be either ONCE or REPEATING.');
    }
  }
}