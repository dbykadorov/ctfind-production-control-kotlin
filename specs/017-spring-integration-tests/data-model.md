# Data Model: Phase 1 Spring Integration Scenarios

This feature models test artifacts and scenario fixtures, not production database entities.

## Integration Scenario

Represents one end-to-end backend business-flow test.

**Fields**

- `name`: Scenario class or scenario group name.
- `priority`: `P1` for MVP scenarios, `P2` for follow-up scenarios.
- `coveredFlow`: Phase 1 workflow protected by the scenario.
- `happyPathAssertions`: Main successful flow assertions.
- `negativeAssertions`: 2-4 security, conflict, validation, or rejection checks.
- `residualRisks`: Behaviors intentionally left to unit tests or manual smoke.
- `verificationCommand`: Command that runs the scenario.

**Validation Rules**

- MVP must contain exactly three P1 scenarios.
- Full planned suite must contain no more than six scenario classes or groups.
- Every scenario must include at least one happy path and at least two negative/security/conflict checks.
- Scenario names should be business-flow names, not controller names.

## Scenario Actor

Represents a user participating in a scenario.

**Fields**

- `login`: Unique login for the scenario.
- `roleCodes`: One or more canonical backend role codes.
- `tokenSource`: How the authenticated session is obtained.
- `purpose`: Why the actor exists in the scenario.

**Validation Rules**

- Authenticated behavior should use the normal login flow.
- Role codes must use Phase 1 canonical role vocabulary: `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`.
- Each actor must have a documented purpose to prevent unnecessary fixture sprawl.

## Scenario Fixture

Represents data required for a scenario.

**Fields**

- `customers`: Customer records needed for orders.
- `orders`: Orders and order items needed for order/production/inventory flows.
- `materials`: Materials and stock quantities needed for warehouse flows.
- `productionTasks`: Tasks created from orders or set up for notification/audit flows.
- `notifications`: Expected notification facts.
- `auditEvents`: Expected audit facts.

**Validation Rules**

- Fixtures must be isolated between scenarios.
- Data may be created through application flows or explicit setup helpers, but verification must happen through the real application path.
- Fixture setup must not bypass the behavior the scenario is meant to verify.

## Verification Evidence

Represents proof that the integration suite ran and what it covered.

**Fields**

- `command`: Root or backend command used to run integration scenarios.
- `result`: Pass/fail status.
- `coveredScenarios`: Scenario names included in the run.
- `date`: Date of verification.
- `notes`: Residual-risk or environment notes.

**Validation Rules**

- The verification command must be documented in the repository.
- Release summaries must distinguish fast unit tests from Docker-backed integration tests.

## Residual Risk

Represents behavior intentionally not covered by the integration suite.

**Fields**

- `scenario`: Scenario that leaves the risk.
- `omittedBehavior`: Behavior not covered by integration tests.
- `reason`: Why it is omitted.
- `coveredBy`: Unit test, contract, manual smoke, or future feature that covers or owns it.

**Validation Rules**

- Any skipped or deferred Phase 1 category must have a residual-risk note.
- Residual risk must not hide missing MVP coverage.
