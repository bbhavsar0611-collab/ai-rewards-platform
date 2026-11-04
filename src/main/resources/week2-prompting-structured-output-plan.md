# Week 2 --- Prompt Engineering and Structured Output

## AI Rewards Platform: Learning Plan, End-to-End Flow, Business Value, and Interview Guide

## 1. Purpose of this module

Customer:
"I want to check how many reward points I have"
|
v
RewardPromptBuilder
|
v
OpenAI LLM
|
v
LLM-generated JSON
|
v
RewardIntentParser
|
v
RewardIntentValidator
|
v
REST API Response

This module turns the existing Spring Boot endpoint into a small,
meaningful AI-enabled rewards workflow. The customer describes what they
want in natural language; the LLM interprets the request and returns
structured data; Java parses and validates that data; trusted backend
services enforce the business rules; and the application returns a
controlled outcome.

The purpose is not to let an LLM perform or authorize a
financial/business action. The LLM is used for **language understanding
and intent extraction**. The Java application remains responsible for
authentication, authorization, data integrity, eligibility, and any
actual redemption.

### What we are building

Example customer message:

> "I want to redeem headphones."

The AI layer should interpret the message into something like:

``` json
{
  "intent": "REDEEM_REWARD",
  "rewardId": "PREMIUM_HEADPHONES"
}
```

This is an **interpretation**, not an authorization or a completed
redemption. The backend must verify that the reward exists, the
authenticated customer is allowed to redeem it, and the customer has
enough points. If any check fails, the application safely rejects the
request or asks the customer for clarification.

## 2. Business functionality

### Customer problem

Customers may express the same goal in many different ways:

-   "I want to redeem headphones."
-   "Can I use my points for the headphones?"
-   "Get me the headphones using my rewards."
-   "Show me rewards I can afford."

Traditional code based only on exact phrases becomes brittle. An LLM can
help interpret natural language and map it to a small, known set of
application intents.

### Business outcome

The feature converts a free-form customer message into a typed request
that existing application services can understand. It can support
intents such as:

-   `CHECK_BALANCE`
-   `REDEEM_REWARD`
-   `FIND_REWARDS`
-   `UNKNOWN`

For a redemption request, the AI layer may extract a reward identifier.
The backend then performs authoritative checks.

### What the feature does and does not do

**It does:** - Accept natural-language customer messages. - Ask an LLM
to classify intent and extract relevant fields. - Request structured
JSON output. - Parse the response into a Java DTO. - Validate required
fields and supported values. - Pass a validated interpretation to
business logic. - Return a controlled success, rejection, or
clarification result. - Provide test cases and basic metrics to evaluate
reliability.

**It does not automatically:** - Prove that the model's answer is
factually correct. - Prove that a reward exists. - Authenticate the
customer. - Decide whether the customer is authorized. - Guarantee that
the customer has enough points. - Execute a redemption safely without
backend checks. - Guarantee valid JSON merely because the prompt
requests JSON.

## 3. End-to-end architecture and flow

``` text
Customer / Client
    |
    | POST /api/ai/rewards/interpret
    | { "message": "I want to redeem headphones" }
    v
RewardIntentController
    |
    v
RewardIntentService (orchestrator)
    |
    +--> RewardPromptBuilder
    |       Builds instructions, allowed intents, output expectations,
    |       and the customer's message as untrusted input.
    |
    +--> LLM client / existing Spring AI integration
    |       Sends prompt to the model and receives response text/structured output.
    |
    +--> RewardIntentParser
    |       Uses Jackson to deserialize JSON into RewardIntent.
    |
    +--> RewardIntentValidator
    |       Checks required fields and allowed combinations.
    |
    v
RewardRedemptionService / Rewards business service
    |
    +--> Authenticate customer using trusted security context
    +--> Verify reward exists in the trusted catalog
    +--> Check customer ownership and authorization
    +--> Check points balance using authoritative data
    +--> Apply redemption rules and prevent duplicate/racing redemptions
    |
    v
Controlled API response
    |
    +--> Accepted / eligible
    +--> Rejected (invalid reward, insufficient points, unauthorized)
    +--> Clarification required (ambiguous request)
    +--> Safe error (provider failure, malformed response, timeout)
```

### Example success path

1.  Customer sends: "I want to redeem headphones."
2.  Controller validates that the message is present and within
    acceptable size limits.
3.  `RewardIntentService` asks the prompt builder to construct the
    classification prompt.
4.  The LLM returns a structured interpretation: `REDEEM_REWARD` and
    `PREMIUM_HEADPHONES`.
5.  Jackson parses the response into a `RewardIntent` record.
6.  The validator checks that `intent` is supported and that a reward ID
    is present for a redemption intent.
7.  The business service looks up the reward in the trusted catalog.
8.  The business service obtains the authenticated customer identity and
    authoritative point balance.
9.  If all rules pass, the service executes the redemption using the
    application's normal transactional controls.
10. The API returns a controlled result.

### Example rejection path

The model returns valid JSON with `rewardId = "PREMIUM_HEADPHONES"`, but
the catalog has no such reward. The JSON can be syntactically and
structurally valid while the requested action is invalid. The business
service rejects it; the model's response does not override the database.

### Example malformed-response path

The LLM returns prose or invalid JSON. Jackson fails to parse it. The
orchestration layer catches the failure and returns a safe error or uses
a carefully bounded recovery policy. It must not continue as if a valid
intent had been produced.

## 4. Components and responsibilities

  --------------------------------------------------------------------------------
Component                        Responsibility          Why it exists
  -------------------------------- ----------------------- -----------------------
`RewardIntentController`         HTTP boundary; accepts  Separates transport/API
request and returns     concerns from
response                application logic

`RewardIntentService`            Coordinates prompt,     Provides one
model call, parsing,    understandable
validation, and next    application workflow
step

`RewardPromptBuilder`            Creates instructions    Makes prompts reusable,
and inserts necessary   testable, and
context                 versionable

LLM client / Spring AI client    Calls the configured    Isolates provider
model provider          communication from
business logic

`RewardIntent`                   DTO representing model  Gives downstream Java
interpretation          code a typed contract

`RewardIntentType`               Enum of supported       Prevents arbitrary
intents                 intent strings from
flowing through the app

`RewardIntentParser`             Uses Jackson to map     Separates parsing from
JSON to DTO             business validation

`RewardIntentValidator`          Checks required fields  Rejects incomplete or
and permitted field     malformed
combinations            interpretations

`RewardRedemptionService`        Applies trusted         Keeps the source of
business rules and      truth in Java and
performs allowed        trusted data stores
redemption

`RewardIntentEvaluationRunner` / Evaluates prompt        Detects regressions and
tests                            behavior and failure    measures reliability
cases

Observability/logging            Tracks latency, errors, Helps diagnose
prompt version, and     production issues
safe metadata           without leaking
sensitive data
  --------------------------------------------------------------------------------

The exact class names may differ in the existing project. The important
point is to keep each responsibility clear.

## 5. Topics we are learning in detail

### Topic 1 --- Prompt engineering fundamentals

**What it is:** Designing input instructions and context so the model
understands the task, constraints, audience, and expected result.

**Why it matters:** "Tell me about rewards" is ambiguous. "Classify the
customer message into one of four supported intents and return the
requested fields" is a much narrower task.

**What to practice:** - Compare vague and specific prompts. - Add one
constraint at a time. - Check whether the model follows the
instruction. - Remember that specificity improves guidance but does not
guarantee correctness.

**Interview explanation:** "Prompt engineering is the practice of
specifying the task, context, constraints, and expected output to reduce
ambiguity and improve response usefulness. I still evaluate outputs
because prompts do not guarantee correctness."

### Topic 2 --- Prompt anatomy and reusable templates

**What it is:** Organizing a prompt into task instructions, context,
constraints, examples, and output expectations.

**Why it matters:** Prompts embedded as scattered string literals are
hard to review and test. A dedicated builder makes the prompt easier to
maintain and version.

**What to practice:** - Define supported intents. - Specify what to do
when the message is unclear. - Separate trusted application facts from
customer-provided text. - Keep prompts focused and avoid sending
unnecessary private data.

**Interview explanation:** "I keep prompt construction in a dedicated
component so instructions are consistent, testable, and versionable.
Dynamic values are supplied deliberately, and user text is treated as
untrusted input."

### Topic 3 --- Zero-shot and few-shot prompting

**Zero-shot:** Instructions without examples.

**Few-shot:** Instructions plus a small set of input/output examples.

**Why it matters:** Examples can clarify how unusual or domain-specific
requests should be classified. Poor, conflicting, or unrepresentative
examples can also make the model less reliable.

**What to practice:** Run the same classification test set against
zero-shot and few-shot prompts. Compare correct labels, malformed
outputs, and ambiguous cases.

**Interview explanation:** "I start with zero-shot prompting for simple
tasks. If classification is inconsistent, I evaluate a few
representative examples and compare results on a fixed test set rather
than assuming few-shot is always better."

### Topic 4 --- Context and dynamic prompt construction

**What it is:** Adding relevant runtime information to the prompt, such
as a reward's displayed cost or a list of supported intents.

**Why it matters:** The model cannot reliably use facts that were never
provided or retrieved. However, data included in a prompt should not
become the authority for sensitive decisions.

**What to practice:** Build `RewardPromptBuilder` with values such as
available points, reward cost, and customer question. Validate inputs
and make clear which facts are trusted application context.

**Interview explanation:** "I inject only relevant context into the
prompt. The LLM may explain that context, but authoritative balances and
eligibility are verified again by the backend."

### Topic 5 --- Structured output, JSON, and schemas

**Prompting for JSON:** Asking the model to return JSON in natural
language.

**Schema-constrained output:** Using a supported provider/API feature to
constrain the response shape.

**Application validation:** Checking the result after it reaches Java.

These are separate layers. A JSON prompt may still produce invalid JSON.
A schema can constrain shape, but it cannot prove the requested reward
exists or that the user is authorized.

**What to practice:** - Define `RewardIntent` and `RewardIntentType`. -
Request the exact fields needed by the application. - Explore the
structured-output feature supported by the actual model/API in use. -
Test missing fields, unexpected values, and malformed responses.

**Interview explanation:** "I use structured output to make the response
contract more predictable, then parse and validate it in Java. Schema
conformance does not replace domain validation."

### Topic 6 --- Jackson, DTOs, and validation

**What it is:** Mapping model JSON into typed Java data and checking
that the result is acceptable.

**Layers:** 1. **Syntax:** Is the JSON valid? 2. **Structure:** Can it
map to the expected DTO and required fields? 3. **Domain:** Does it
represent an allowed operation?

**What to practice:** Parse a response using Jackson; test invalid JSON;
use an enum for intent; reject a redemption without a reward ID; add
JUnit tests.

**Interview explanation:** "Jackson handles deserialization. A validator
checks application-level invariants, and the business service
independently checks live domain rules. I handle parsing failures
explicitly rather than allowing invalid output downstream."

### Topic 7 --- Prompt injection and AI security

**What it is:** Untrusted text tries to override the model's task,
reveal instructions, or trigger actions outside the intended scope.

**Why it matters:** A customer could ask the model to ignore the
classification rules or claim a reward is free. Prompt instructions
alone are not a security boundary.

**What to practice:** - Test adversarial messages. - Keep secrets out of
prompts. - Treat model output and user text as untrusted. - Enforce
authentication, authorization, ownership, balances, and allowed actions
in Java. - Avoid executing arbitrary model-generated instructions.

**Interview explanation:** "I assume model output can be wrong or
manipulated. The LLM does not get authority from its response;
authorization and business rules remain enforced by backend services."

### Topic 8 --- Prompt evaluation and regression testing

**What it is:** Testing prompt behavior against fixed examples with
expected outcomes.

**Why it matters:** A prompt that succeeds once may fail on a different
wording or after a prompt/model change.

**What to practice:** - Create examples for each supported intent. -
Include ambiguous, unrelated, and adversarial messages. - Track
classification accuracy separately from parse success. - Rerun the
dataset after prompt changes. - Review failure examples manually.

**Interview explanation:** "I evaluate prompts on a representative
dataset and track task correctness, parse failures, and important edge
cases. I use regression tests when changing prompts or model
configuration."

### Topic 9 --- Production concerns: latency, retries, logging, and versioning

**What it is:** Making the model call observable and resilient.

**What to practice:** - Measure endpoint and model-call latency. - Use
timeouts and bounded retries for suitable transient failures. - Handle
rate limits and provider errors. - Track prompt version and safe
metadata. - Avoid logging API keys, sensitive personal data, or full
prompts unnecessarily. - Monitor token usage where provider data is
available.

**Interview explanation:** "I treat the LLM as a remote dependency. I
use timeouts, bounded retry policies, structured error handling, safe
observability, and prompt versioning. I do not retry every error
indiscriminately."

### Topic 10 --- Interview-level architecture and trade-offs

Be ready to explain: - Why the LLM interprets language but Java owns
business decisions. - Why a DTO and enum are useful. - Why JSON parsing
differs from validation. - Why structured output is helpful but not
sufficient. - How you handle ambiguity, malformed output, and provider
failure. - How you measure prompt quality. - How you defend against
prompt injection. - How you keep the workflow testable and maintainable.

## 6. Suggested package structure

This is the target structure for the completed workflow. Adapt it to the
package layout already used by the project.

``` text
src/
  main/
    java/
      .../airewards/
        controller/
          RewardIntentController.java
        service/
          RewardIntentService.java
          RewardRedemptionService.java
        ai/
          RewardPromptBuilder.java
          RewardIntentParser.java
          RewardIntentValidator.java
          RewardIntent.java
          RewardIntentType.java
        integration/
          LlmClient.java                 # or use the existing Spring AI client
        domain/
          Reward.java
          CustomerRewardsAccount.java    # examples; use existing domain models
  test/
    java/
      .../airewards/
        ai/
          RewardPromptBuilderTest.java
          RewardIntentParserTest.java
          RewardIntentValidatorTest.java
        service/
          RewardIntentServiceTest.java
          RewardRedemptionServiceTest.java
```

Do not duplicate existing controller, domain, or client classes just to
match this diagram. Reuse the existing working API integration and adapt
the class names to your actual project.

## 7. Controlled outcomes and failure handling

  -----------------------------------------------------------------------
Situation                           Expected handling
  ----------------------------------- -----------------------------------
Empty customer message              Reject at API boundary

Clear, supported intent             Continue to parse and validate

Ambiguous message                   Return `UNKNOWN` or ask for
clarification

Malformed JSON                      Stop the workflow and return a safe
error

Unsupported intent value            Reject or map to `UNKNOWN` under an
explicit policy

Missing reward ID for redemption    Reject as invalid interpretation

Reward does not exist               Reject using the trusted reward
catalog

Customer lacks permission           Reject using backend authorization

Insufficient points                 Reject using the authoritative
balance

LLM timeout or provider failure     Return a controlled dependency
error; do not redeem

All validations pass                Execute through the normal business
service and transactional controls
  -----------------------------------------------------------------------

A valid model response is not the same as a successful redemption. A
successful redemption is only possible after trusted business checks and
the application's normal consistency controls succeed.

## 8. Test plan

At minimum, test these cases:

1.  "How many points do I have?" → `CHECK_BALANCE`
2.  "Redeem my points for headphones." → `REDEEM_REWARD`
3.  "Show me available rewards." → `FIND_REWARDS`
4.  "Tell me a joke." → `UNKNOWN`
5.  "Do the thing we discussed." → `UNKNOWN` or clarification
6.  Model returns malformed JSON → controlled failure
7.  Model returns an unsupported enum value → controlled failure or
    explicit fallback
8.  Redemption intent has no reward ID → validation failure
9.  Reward ID is valid JSON but nonexistent → business rejection
10. Customer lacks enough points → business rejection
11. Customer tries to override instructions → no bypass of backend
    controls
12. LLM provider times out → controlled failure, no redemption

Use unit tests for the builder, parser, validator, and business rules.
Use mocked LLM responses for deterministic service tests. Keep a smaller
number of integration tests that call the actual provider, since those
can be slower and nondeterministic.

## 9. Implementation plan

### Phase 1 --- Understand the pieces

-   Compare vague and specific prompts.
-   Build a reusable prompt template.
-   Experiment with zero-shot and few-shot examples.
-   Implement dynamic context in `RewardPromptBuilder`.

### Phase 2 --- Create a typed response contract

-   Define `RewardIntentType`.
-   Define `RewardIntent`.
-   Request JSON/structured output.
-   Parse with Jackson.
-   Validate fields and enum values.

### Phase 3 --- Connect the workflow

-   Add or adapt `RewardIntentController`.
-   Add `RewardIntentService` to orchestrate prompt → model → parse →
    validate.
-   Reuse the existing LLM integration rather than duplicating API
    calls.
-   Return controlled outcomes for valid, ambiguous, and failed
    responses.

### Phase 4 --- Enforce business rules

-   Use authenticated customer identity.
-   Verify reward existence from trusted data.
-   Check available points from the authoritative source.
-   Execute redemption only after checks pass.
-   Use transactions and concurrency safeguards appropriate to the
    existing system.

### Phase 5 --- Test and prepare for interviews

-   Add unit tests and mocked service tests.
-   Run prompt evaluation and inspect failures.
-   Add safe timing and error metrics.
-   Practice explaining the design, risks, and trade-offs.

## 10. Interview-ready project explanation

### 30-second version

"I built an AI-assisted rewards workflow in Spring Boot that interprets
natural-language customer requests. The LLM classifies the intent and
returns structured data, which I parse into a Java DTO using Jackson and
validate before passing it to business services. The backend remains the
source of truth for customer authorization, reward availability, and
points balance, so model output cannot directly authorize a redemption."

### Two-minute version

"The business problem is that customers express the same rewards task in
different natural-language forms. Instead of relying only on brittle
keyword matching, I use an LLM to interpret the request into a small set
of supported intents such as checking a balance, finding rewards, or
redeeming a reward.

I construct a focused prompt with explicit instructions and a defined
output contract. I request structured output and map the response to a
typed Java DTO using Jackson. I validate the intent and required fields
separately from business rules. The rewards service then checks the
authenticated customer, verifies that the reward exists, and checks the
authoritative points balance before performing any redemption.

I also consider ambiguous requests, malformed model responses, prompt
injection, provider timeouts, and regression testing. The key
architectural choice is that the LLM handles language interpretation,
while deterministic Java services enforce business rules and execute the
operation. This keeps the AI feature useful without treating
probabilistic output as trusted application state."

### Trade-offs to discuss

-   **LLM versus deterministic rules:** LLMs handle varied language
    well; business rules are more predictable and auditable in Java.
-   **Prompt JSON versus constrained output:** A prompt is easy to start
    with; a supported schema constraint improves format reliability;
    both still need validation.
-   **Retry versus fail safely:** A limited retry may recover from
    transient failures; unlimited retries increase latency and cost.
-   **More context versus less context:** Additional context can help,
    but increases token usage and can introduce irrelevant or sensitive
    information.
-   **Automatic execution versus confirmation:** Interpreting intent can
    be automated; sensitive or irreversible actions may need
    confirmation and always require backend authorization.

## 11. Definition of done for Week 2

-   [ ] I can explain prompt anatomy, zero-shot, and few-shot prompting.
-   [ ] I have compared prompt variants using the existing endpoint.
-   [ ] I can build a prompt with dynamic context.
-   [ ] I can explain prompting for JSON versus schema-constrained
    output.
-   [ ] I can deserialize a response into a Java DTO using Jackson.
-   [ ] I validate model output and handle parsing errors.
-   [ ] I keep reward existence, authorization, and points checks in
    trusted backend services.
-   [ ] I have tested ambiguous, malformed, and adversarial inputs.
-   [ ] I can explain the architecture and its trade-offs in an
    interview.

**Final principle:** The LLM proposes an interpretation. Java validates
the interpretation. Trusted business services determine what the
customer is allowed to do.
