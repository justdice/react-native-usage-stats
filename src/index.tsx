import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-usage-stats-manager' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const JustDiceReactNativeUsageStats =
  NativeModules.JustDiceReactNativeUsageStats
    ? NativeModules.JustDiceReactNativeUsageStats
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );

export function queryUsageStats(
  interval: number,
  startTime: number,
  endTime: number
): Promise<any> {
  return JustDiceReactNativeUsageStats.queryUsageStats(
    interval,
    startTime,
    endTime
  );
}

export function queryAndAggregateUsageStats(
  startTime: number,
  endTime: number
): Promise<any> {
  return JustDiceReactNativeUsageStats.queryAndAggregateUsageStats(
    startTime,
    endTime
  );
}

export interface UsageEvent {
  eventType: EventType;
  timeStamp: number;
  packageName: string;
}

export enum EventType {
  NONE = 0,
  ACTIVITY_RESUMED = 1,
  ACTIVITY_PAUSED = 2,
  CONFIGURATION_CHANGE = 5,
  USER_INTERACTION = 7,
  SHORTCUT_INVOCATION = 8,
  STANDBY_BUCKET_CHANGED = 11,
  SCREEN_INTERACTIVE = 15,
  SCREEN_NON_INTERACTIVE = 16,
  KEYGUARD_SHOWN = 17,
  KEYGUARD_HIDDEN = 18,
  FOREGROUND_SERVICE_START = 19,
  FOREGROUND_SERVICE_STOP = 20,
  ACTIVITY_STOPPED = 23,
  DEVICE_SHUTDOWN = 26,
  DEVICE_STARTUP = 27,
}

export function queryEvents(
  startTime: number,
  endTime: number
): Promise<UsageEvent[]> {
  return JustDiceReactNativeUsageStats.queryEvents(startTime, endTime);
}

export function queryEventsStats(
  interval: number,
  startTime: number,
  endTime: number
): Promise<any> {
  return JustDiceReactNativeUsageStats.queryEventsStats(
    interval,
    startTime,
    endTime
  );
}

export function showUsageAccessSettings(packageName: string) {
  return JustDiceReactNativeUsageStats.showUsageAccessSettings(packageName);
}

export function checkForPermission(): Promise<any> {
  return JustDiceReactNativeUsageStats.checkForPermission();
}

export function getAppDataUsage(
  packageName: string,
  networkType: number,
  startTime: number,
  endTime: number
): Promise<any> {
  return JustDiceReactNativeUsageStats.getAppDataUsage(
    packageName,
    networkType,
    startTime,
    endTime
  );
}

export enum EventFrequency {
  INTERVAL_DAILY = 0,
  INTERVAL_WEEKLY = 1,
  INTERVAL_MONTHLY = 2,
  INTERVAL_YEARLY = 3,
  INTERVAL_BEST = 4,
  INTERVAL_COUNT = 4,
}
