export type TimeConfigurationModes = {
  type: "ONCE";
} | {
  type: "REPEATING";
  iterations: number;
  interval: number;
};

export type TimeConfiguration = {
  nextRunUnix: number;
} & TimeConfigurationModes;

export type ActionConfiguration = {
  http: {
    url: string;
    signingSecret?: string;
    additionalHeaders?: Record<string, string>;
  }
} | {
  sqs: {
    queueUrl: string;
    accessKeyId: string;
    secretAccessKey: string;
    region: string;
  }
};

export type FailureConfiguration = {
  backoff?: number[];
  deadLetter?: ActionConfiguration;
};

export type UnidentifiedJob<T = unknown> = {
  payload: T,
  time: TimeConfiguration;
  action: ActionConfiguration;
  failure?: FailureConfiguration;
};

export type IdentifiedJob<T = unknown> = UnidentifiedJob<T> & {
  id: number;
  time: TimeConfiguration & {iterationsCount: number;};
  failure: FailureConfiguration & {backoff: number[];};
};