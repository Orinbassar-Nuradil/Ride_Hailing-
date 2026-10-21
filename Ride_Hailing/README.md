# Assignment 5 — Strategy Pattern

## Scope

This assignment implements interchangeable pricing algorithms and driver
matching algorithms in the Ride-Hailing domain.

Run `com.ridehail.app.StrategyDemo` for the Assignment 5 demonstration.
The existing booking application belongs to Assignment 4.

## Running

Requirements:
- JDK 17 or newer.
- Maven dependencies loaded.

In IntelliJ IDEA:

1. Open the Maven project inside the `Ride_Hailing` directory.
2. Wait for Maven dependencies to load.
3. Open `src/main/java/com/ridehail/app/StrategyDemo.java`.
4. Run its `main` method.

To run all tests, use **Maven → Lifecycle → test**.

With Maven installed, run this command from the directory containing
`pom.xml`:

```bash
mvn test
```

## Classes and responsibilities

This table describes the classes and interfaces used for Assignment 5.
Each responsibility contains fewer than 25 words and avoids the prohibited
conjunctions.

| Class or interface | Responsibility |
|---|---|
| PricingStrategy | Defines the fare calculation contract. |
| StandardPricingStrategy | Calculates a distance-based fare. |
| TrafficPricingStrategy | Calculates a fare using total traffic delay. |
| AirportPricingStrategy | Calculates an airport package fare with excess-distance charges. |
| PricingContext | Delegates fare calculation to its replaceable strategy. |
| PricingSelector | Chooses a pricing strategy using selection policy. |
| PricingCalculator | Preserves the original calculation entry point during refactoring. |
| TrafficSource | Defines access to additional traffic delay estimates. |
| SimulatedTrafficSource | Supplies a configured additional delay estimate. |
| DriverCandidate | Represents validated driver selection data. |
| DriverMatchingStrategy | Defines the driver selection contract. |
| NearestDriverStrategy | Selects the available driver closest to the passenger. |
| HighestRatedDriverStrategy | Selects the available driver with the highest rating. |
| LongestIdleDriverStrategy | Selects the available driver with the longest idle time. |
| DriverMatchingContext | Delegates driver selection to its replaceable strategy. |
| DriverMatchingSelector | Chooses a matching strategy using selection policy. |
| StrategyDemo | Demonstrates interchangeable algorithms through console output. |
| PricingCalculatorTest | Verifies pricing behavior preserved across refactoring. |
| PricingSelectorTest | Verifies pricing selection rules. |
| PricingContextTest | Verifies pricing delegation through replaceable strategies. |
| TrafficPricingStrategyTest | Verifies traffic pricing using controlled data sources. |
| DriverMatchingTest | Verifies driver matching behavior across interchangeable strategies. |

## Strategy family 1: Pricing

The `PricingStrategy` interface defines:

```java
long calculate(int distanceKm, int delayMinutes);
```

Three implementations provide different calculation rules:

| Strategy | Calculation |
|---|---|
| StandardPricingStrategy | `500 + 180 × distanceKm` |
| TrafficPricingStrategy | `500 + 180 × distanceKm + 50 × totalDelay` |
| AirportPricingStrategy | `3000 + 200 × max(0, distanceKm - 15)` |

The airport strategy includes the first 15 kilometres in a fixed package.
The traffic strategy includes a charge based on total delay.

`totalDelay` combines the supplied delay with additional delay obtained
from `TrafficSource`. The default source supplies zero additional minutes.

Calculations use `long` arithmetic to support large monetary results.
Invalid trip inputs are rejected.

### Pricing context

`PricingContext` receives a strategy through its constructor.
Its `setStrategy()` method replaces the strategy at runtime.
Its `calculate()` method delegates directly to the selected strategy.

The context contains no strategy-type checks.

### Pricing selection

Manual modes:

- `STANDARD`
- `TRAFFIC`
- `AIRPORT`

Automatic selection rules, in priority order:

1. An airport trip uses `AirportPricingStrategy`.
2. A non-airport trip with at least 10 minutes of supplied delay uses
   `TrafficPricingStrategy`.
3. Remaining trips use `StandardPricingStrategy`.

Missing manual modes, unknown manual modes and negative supplied delays
are rejected.

## Strategy family 2: Driver matching

The `DriverMatchingStrategy` interface defines:

```java
Optional<DriverCandidate> choose(List<DriverCandidate> candidates);
```

Three implementations use different selection criteria:

| Strategy | Selection criterion |
|---|---|
| NearestDriverStrategy | Minimum pickup distance |
| HighestRatedDriverStrategy | Maximum driver rating |
| LongestIdleDriverStrategy | Maximum idle time |

All strategies exclude unavailable drivers.
Equal scores are resolved by driver ID.
An empty list or a list without available drivers produces
`Optional.empty()`.

`DriverCandidate` stores the driver ID, pickup distance, rating,
idle time and availability.

### Matching context

`DriverMatchingContext` receives a strategy through its constructor.
Its `setStrategy()` method replaces the strategy at runtime.
Its `choose()` method delegates selection to the strategy.

The context contains no strategy-type checks.

### Matching selection

Manual modes:

- `NEAREST`
- `HIGHEST_RATED`
- `LONGEST_IDLE`

Automatic selection rules, in priority order:

1. A premium customer uses `HighestRatedDriverStrategy`.
2. A non-premium customer under high demand uses
   `NearestDriverStrategy`.
3. Remaining customers use `LongestIdleDriverStrategy`.

Missing or unknown manual modes are rejected.

## Isolation from external data changes

`TrafficPricingStrategy` depends on the `TrafficSource` interface.

`SimulatedTrafficSource` provides a configured additional delay.
A different implementation can supply data from another source without
changing the pricing formula.

Constructor injection supplies the required source:

```java
PricingStrategy strategy =
        new TrafficPricingStrategy(new SimulatedTrafficSource(5));
```

Tests inject fixed fake implementations of `TrafficSource`.
This keeps test results independent of network access or live traffic.

Tests also verify rejection of a null source and negative additional
delay values.

## Why Strategy, not State or Template Method?

This solution uses Strategy because each family represents different
algorithms for the same operation. Pricing strategies calculate a fare.
Matching strategies select an available driver. Each context depends on
a small interface rather than a particular implementation.

The caller supplies a strategy through the context constructor.
The caller can later replace it through `setStrategy()` while retaining
the same context object. `StrategyDemo` demonstrates this behavior using
unchanged trip inputs and unchanged driver candidates. Contexts delegate
execution without inspecting concrete strategy types.

State has a different purpose: it models behavior associated with an
object's internal lifecycle. A reservation becoming RESERVED or RELEASED
is a state transition. Choosing nearest-driver matching instead of
highest-rated matching is an algorithm choice, not a driver lifecycle
transition. These strategy objects do not manage transitions between
internal context states.

Template Method defines an algorithm skeleton in a superclass and lets
subclasses override selected steps. This implementation uses composition:
contexts hold strategy objects. There is no algorithm superclass defining
a fixed sequence of overridable steps. Complete algorithms are replaceable
through their interfaces.

A new strategy can implement the relevant interface and be injected
without modifying the context or existing algorithms. Registering a new
manual mode or changing automatic selection policy requires a deliberate
selector update. Selection policy is isolated from algorithm execution.

`TrafficPricingStrategy` also depends on `TrafficSource`. Another data
source can replace the simulated source through constructor injection
without changing the fare formula. Tests supply fixed fake sources to
keep results independent of live services.

## Refactoring history

The pricing family was first implemented in a single class containing
a switch over the requested pricing mode.

It was then refactored into separate strategy implementations with
delegation through `PricingContext`.

- [Before: pricing variants implemented using switch](https://github.com/Orinbassar-Nuradil/Ride_Hailing-/commit/15ae622)
- [After: pricing algorithms refactored to Strategy](https://github.com/Orinbassar-Nuradil/Ride_Hailing-/commit/18c45c0)

The same 11 `PricingCalculatorTest` cases passed before and after
refactoring. Their behavior assertions were preserved.

`PricingCalculator` preserves the original entry point while delegating
calculation through the strategy-based implementation.

## Unit tests

The Assignment 5 test classes contain 59 test invocations:

| Test class | Test invocations |
|---|---:|
| PricingCalculatorTest | 11 |
| PricingSelectorTest | 10 |
| PricingContextTest | 5 |
| TrafficPricingStrategyTest | 9 |
| DriverMatchingTest | 24 |
| Total | 59 |

JUnit 5 tests cover:

- Every pricing algorithm.
- Every matching algorithm.
- Manual selection modes.
- Missing or unknown manual modes.
- Every automatic selector rule.
- Selector rule priority.
- Pricing selection boundaries at 9 and 10 minutes.
- Constructor strategy injection.
- Runtime strategy replacement.
- Rejection of null replacement strategies.
- Fixed fake traffic sources.
- Invalid input.
- Large monetary calculations.
- Unavailable drivers.
- Empty candidate lists.
- Equal-score driver selection.

`DriverMatchingTest` uses `@ParameterizedTest` with `@MethodSource`
to execute the same contract checks against all three matching strategies.

These shared checks verify that every strategy handles empty input,
excludes unavailable drivers, returns an available input candidate and
resolves equal scores consistently.

## Demonstration

`StrategyDemo` shows:

1. The same trip processed by all three pricing strategies.
2. The same candidates processed by all three matching strategies.
3. Manual selection compared with automatic selection.
4. Runtime replacement within the same context object.
5. Traffic pricing using an injected simulated data source.

### Pricing example

Input: 20 kilometres with 10 minutes of supplied delay.

```text
StandardPricingStrategy: 4100 KZT
TrafficPricingStrategy: 4600 KZT
AirportPricingStrategy: 4000 KZT
Manual choice: StandardPricingStrategy
Automatic choice: TrafficPricingStrategy
Before runtime switch: 4100 KZT
After runtime switch: 4600 KZT
With external source (+5 minutes): 4850 KZT
```

### Driver matching example

The same candidates are supplied to every matching strategy:

| Driver | Pickup distance | Rating | Idle minutes | Available |
|---|---:|---:|---:|---|
| D1 | 1 km | 4.2 | 5 | Yes |
| D2 | 4 km | 4.9 | 10 | Yes |
| D3 | 3 km | 4.5 | 40 | Yes |
| D4 | 0 km | 5.0 | 100 | No |

Expected selection:

```text
NearestDriverStrategy: D1
HighestRatedDriverStrategy: D2
LongestIdleDriverStrategy: D3
Manual choice: LongestIdleDriverStrategy
Automatic choice: HighestRatedDriverStrategy
Before runtime switch: D3
After runtime switch: D2
```

D4 is excluded because the driver is unavailable.

## Assignment 4 documentation

The previous application documentation is preserved in
[README_Assignment4.md](README_Assignment4.md).
