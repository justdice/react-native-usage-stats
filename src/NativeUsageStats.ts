import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface UsageStatSpec {
  packageName: string;
  appName: string;
  totalTimeInForeground: number;
  totalTimeVisible: number;
  firstTimeStamp: number;
  lastTimeStamp: number;
  lastTimeUsed: number;
  isSystem: boolean;
}

export interface UsageEventSpec {
  eventType: number;
  timeStamp: number;
  packageName: string;
}

export interface EventStatSpec {
  firstTimeStamp: number;
  lastTimeStamp: number;
  /** Total time spent in this event type (milliseconds) */
  lastTimeUsed: number;
  describeContents: number;
}

/** Map keyed by eventType (as string) to its aggregated stats */
export type EventStatsResult = Record<string, EventStatSpec>;

export interface Spec extends TurboModule {
  queryUsageStats(
    interval: number,
    startTime: number,
    endTime: number
  ): Promise<ReadonlyArray<UsageStatSpec>>;

  queryAndAggregateUsageStats(
    startTime: number,
    endTime: number
  ): Promise<ReadonlyArray<UsageStatSpec>>;

  queryEvents(
    startTime: number,
    endTime: number
  ): Promise<ReadonlyArray<UsageEventSpec>>;

  queryEventsStats(
    interval: number,
    startTime: number,
    endTime: number
  ): Promise<EventStatsResult>;

  showUsageAccessSettings(packageName: string): void;

  checkForPermission(): Promise<boolean>;

  getAppDataUsage(
    packageName: string,
    networkType: number,
    startTime: number,
    endTime: number
  ): Promise<number>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'JustDiceReactNativeUsageStats'
);
