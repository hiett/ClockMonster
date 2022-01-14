export type TimeType = "ONCE" | "REPEATING";

export type TimeConfiguration = {
  type: TimeType;
  firstRunUnix: number;
  iterations?: number;
  interval?: number;
};

export type ActionConfiguration = {
  type: "HTTP",
  url: string;
};

export type UnidentifiedJob<T = unknown> = {
  payload: T,
  time: TimeConfiguration;
  action: ActionConfiguration;
};

export type IdentifiedJob<T = unknown> = UnidentifiedJob<T> & {
  id: number;
  time: Required<TimeConfiguration & {iterationsCount: number;}>;
};