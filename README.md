# @justdice/react-native-usage-stats

This is a fork of https://github.com/bright-hustle/react-native-usage-stats-manager The goal of this fork is to provide a very simple bridge between Android UsageStats and react-native, without any custom aggregation. Instead the raw usage stats and events are made available in react-native. 

## Installation

```sh
npm install @justdice/react-native-usage-stats
```

## Android

Permission need to be added on AndroidManifest.xml

```xml

<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />

```

If Build Error on kotlin add kotlinVersion to the gradle.properties

```xml

kotlinVersion=1.8.10

```

## Usage

```js
import {
  EventFrequency,
  checkForPermission,
  queryUsageStats,
  showUsageAccessSettings,
} from '@justdice/react-native-usage-stats';

Time needs to be in millisecond for function queryUsageStats

const startDateString = '2023-06-11T12:34:56';
const endDateString = '2023-07-11T12:34:56';

const startMilliseconds = new Date(startDateString).getTime();
const endMilliseconds = new Date(endDateString).getTime();

const result = await queryUsageStats(
      EventFrequency.INTERVAL_DAILY,
      startMilliseconds,
      endMilliseconds
    )
```

## Check Permission & Open Permission Activity

```js
checkForPermission().then((res: any) => {
  if (!res) {
    showUsageAccessSettings('');
  }
});
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
