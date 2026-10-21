# Assignment 5 — Strategy Pattern

## Scope

This assignment adds interchangeable pricing algorithms and driver
matching algorithms to the Ride-Hailing domain.

Run `com.ridehail.app.StrategyDemo` for the Assignment 5 demonstration.
The existing booking application remains the Assignment 4 workflow.

## Running

Requirements: JDK 17 or newer; Maven dependencies loaded.

In IntelliJ, open the Maven project containing `pom.xml`.
Run the `main` method in `StrategyDemo`.

Run all tests through Maven → Lifecycle → test.

With Maven installed, from the directory containing `pom.xml`:

```bash
mvn test
```

## Classes and responsibilities

The following table covers Assignment 5 additions.
Each responsibility describes one purpose.

| Class or interface | Responsibility |
|---|---|
| PricingStrategy | Defines the fare calculation contract. |
| StandardPricingStrategy | Calculates a distance-based fare. |
| TrafficPricingStrategy | Calculates a fare using total traffic delay. |
| AirportPricingStrategy | Calculates an airport package fare with excess-distance charges. |
| PricingContext | Delegates fare calculation to its replaceable strategy. |
| PricingSelector | Selects a pricing strategy from a requested mode or current trip conditions. |
| PricingCalculator | Preserves the original calculation entry point during refactoring. |
| TrafficSource | Defines access to additional traffic delay estimates. |
| SimulatedTrafficSource | Supplies a configured additional delay estimate. |
| DriverCandidate | Represents validated driver selection data. |
| DriverMatchingStrategy | Defines the driver selection contract. |
| NearestDriverStrategy | Selects the available driver closest to the passenger. |
| HighestRatedDriverStrategy | Selects the available driver with the highest rating. |
| LongestIdleDriverStrategy | Selects the available driver with the longest idle time. |
| DriverMatchingContext | Delegates driver selection to its replaceable strategy. |
| DriverMatchingSelector | Selects a matching strategy from a requested mode or customer conditions. |
| StrategyDemo | Demonstrates interchangeable algorithms through console output. |
| PricingCalculatorTest | Verifies pricing behavior preserved across refactoring. |
| PricingSelectorTest | Verifies pricing selection rules. |
| PricingContextTest | Verifies pricing delegation through replaceable strategies. |
| TrafficPricingStrategyTest | Verifies traffic pricing using controlled data sources. |
| DriverMatchingTest | Verifies driver matching behavior across interchangeable strategies. |

## Algorithms and selection rules

Pricing:
- Standard: 500 + 180 × distanceKm.
- Traffic: 500 + 180 × distanceKm + 50 × totalDelay.
- Airport: 3000 + 200 × max(0, distanceKm - 15).

Total delay combines the supplied delay with additional delay from
TrafficSource. The default source supplies zero additional minutes.

Automatic pricing selection prioritizes airport trips.
Other trips with at least 10 minutes of supplied delay use traffic pricing.
Remaining trips use standard pricing.

Driver matching:
- Nearest minimizes pickup distance.
- Highest-rated maximizes rating.
- Longest-idle maximizes idle minutes.

Unavailable drivers are excluded. Equal scores are resolved by driver ID.
No available candidate produces Optional.empty().

Automatic matching selection prioritizes premium customers using rating.
Other customers under high demand use nearest-driver matching.
Remaining customers use longest-idle matching.

## Why Strategy, not State or Template Method?

This solution uses Strategy because each family represents different
algorithms for the same operation. Pricing strategies calculate a fare.
Matching strategies select an available driver. Each context depends on
a small interface rather than a particular implementation.

The caller supplies a strategy through the context constructor.
The caller can later replace it through setStrategy() while retaining
the same context object. StrategyDemo demonstrates this behavior using
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

TrafficPricingStrategy also depends on TrafficSource. Another data source
can replace the simulated source through constructor injection without
changing the fare formula. Tests supply fixed fake sources to keep
results independent of live services.

## Refactoring history

- [Before: pricing variants implemented using switch](https://github.com/Orinbassar-Nuradil/Ride_Hailing-/commit/15ae622)
- [After: pricing algorithms refactored to Strategy](https://github.com/Orinbassar-Nuradil/Ride_Hailing-/commit/18c45c0)

The same 11 PricingCalculatorTest cases passed before and after refactoring.
Their behavior assertions were preserved.

## Tests

JUnit 5 tests cover:
- Every pricing algorithm.
- Every matching algorithm.
- Manual selection modes and invalid modes.
- Every automatic selector rule and rule priority.
- Pricing selection boundaries at 9 and 10 minutes.
- Runtime strategy replacement.
- Fixed fake traffic sources.
- Invalid input and large monetary calculations.
- Unavailable drivers, empty input and tied scores.

DriverMatchingTest uses @ParameterizedTest with @MethodSource to execute
the same contract checks against all three matching strategies.

The Assignment 5 test classes contain 59 test invocations in total.

## Demonstration

StrategyDemo shows:
1. The same trip processed by all three pricing strategies.
2. The same candidates processed by all three matching strategies.
3. Manual selection compared with automatic selection.
4. Runtime replacement within the same context object.
5. Traffic pricing using an injected simulated data source.

Expected pricing results for 20 km and 10 minutes:
Standard = 4100 KZT; Traffic = 4600 KZT; Airport = 4000 KZT.
An additional five minutes from the data source produces 4850 KZT.

Expected matching results:
Nearest = D1; Highest-rated = D2; Longest-idle = D3.
Runtime replacement changes the demonstrated selection from D3 to D2.