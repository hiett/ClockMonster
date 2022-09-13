export type TimeType = "ONCE" | "REPEATING";

export type TimeConfiguration = {
  nextRunUnix: number;
} & ({
  type: "ONCE";
  nextRunUnix: number;
} | {
  type: "REPEATING";
  iterations: number;
  interval: number;
});

export type ActionConfiguration = {
  http: {
    url: string;
    signingSecret?: string;
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