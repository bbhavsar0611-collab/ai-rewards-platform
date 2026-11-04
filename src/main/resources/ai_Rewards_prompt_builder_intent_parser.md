# AI Rewards Platform --- Today's Code Flow and Explanation

**Date:** November 4, 2026\
**Project:** `ai-rewards-platform`\
**Stack:** Java 17, Spring Boot 4.1.1, Spring AI `ChatClient`, OpenAI,
Jackson 3

## 1. Today's goal

Build and run the first AI intent-classification flow for the Rewards
Platform.

A customer sends a natural-language message. The LLM classifies it into
one of the application's supported intents. The application parses the
model's JSON response into a Java object, validates it, and returns the
result through a REST endpoint.

**Important:** Today's implementation interprets the request. It does
not check a real customer's points balance or redeem a reward.

## 2. High-level code flow

``` text
HTTP request
    |
    v
RewardIntentController
    |
    v
RewardIntentService
    |
    +--> RewardPromptBuilder
    |        |
    |        v
    |      Prompt text
    |
    +--> Spring AI ChatClient
    |        |
    |        v
    |      OpenAI LLM
    |        |
    |        v
    |      JSON response
    |
    +--> RewardIntentParser
    |        |
    |        v
    |      RewardIntent Java object
    |
    +--> RewardIntentValidator
    |
    v
Return RewardIntent as JSON
```

### Example request

``` bash
curl -X POST http://localhost:8080/api/rewards/intent/interpret \
  -H "Content-Type: text/plain" \
  -d "I want to check how many reward points I have"
```

### Actual response observed

``` json
{
  "intent": "CHECK_BALANCE",
  "rewardId": null
}
```

This response came from the actual OpenAI integration, not manually
supplied JSON.

## 3. Components and responsibilities

### A. Existing `AIController`

The existing general-purpose controller uses Spring AI's `ChatClient`:

``` java
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final ChatClient chatClient;

    public AIController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/ask")
    public String ask(@RequestBody String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
```

It accepts general text at `POST /api/ai/ask`, sends it to the LLM, and
returns the model's text response. We kept this endpoint; the new
Rewards workflow uses the same Spring AI `ChatClient` approach.

### B. `RewardPromptBuilder`

**Responsibility:** Build the instruction sent to the LLM.

The prompt defines the supported intents:

-   `CHECK_BALANCE`
-   `REDEEM_REWARD`
-   `FIND_REWARDS`
-   `UNKNOWN`

It asks the model to classify the customer's message, return JSON only,
avoid inventing reward IDs, use `null` when no reward is specified, and
return `UNKNOWN` for unsupported requests.

**Why it matters:** The prompt communicates the expected contract. A
prompt is an instruction, not a hard guarantee; the application must
still parse and validate the response.

### C. `RewardIntentType`

The enum defines the supported intent values:

``` java
public enum RewardIntentType {
    CHECK_BALANCE,
    REDEEM_REWARD,
    FIND_REWARDS,
    UNKNOWN
}
```

**Why an enum instead of `String`?** It defines a controlled set of
values and makes downstream branching type-safe.

An unrecognized enum string may cause Jackson deserialization to fail,
depending on configuration. Having an `UNKNOWN` enum member does not
automatically map every arbitrary string to `UNKNOWN`; that behavior
must be configured or handled explicitly.

### D. `RewardIntent`

The record is the Java response contract:

``` java
public record RewardIntent(
        RewardIntentType intent,
        String rewardId
) {
}
```

Example:

``` json
{
  "intent": "REDEEM_REWARD",
  "rewardId": "HEADPHONE_001"
}
```

A record is a concise data carrier. It does not automatically make every
field non-null. Jackson can deserialize `REDEEM_REWARD` with a null
`rewardId`; a separate validator must reject that case.

### E. `RewardIntentParser`

**Responsibility:** Convert the model's JSON text into a `RewardIntent`
object using Jackson.

Conceptually:

``` java
RewardIntent rewardIntent =
        objectMapper.readValue(jsonResponse, RewardIntent.class);
```

For the Spring Boot 4.1.1 / Jackson 3 setup used in this project, the
parser uses:

``` java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
```

Parsing checks whether JSON can be deserialized into the Java type. It
does not check whether a reward exists, whether the customer is
authorized, or whether the customer has enough points.

### F. `RewardIntentValidator`

**Responsibility:** Check intent-specific required fields.

The initial rule implemented:

-   For `REDEEM_REWARD`, `rewardId` must be present and non-blank.
-   For `CHECK_BALANCE`, `FIND_REWARDS`, and `UNKNOWN`, a `rewardId` is
    not required by this initial validator.

Example:

``` java
if (rewardIntent.intent() == RewardIntentType.REDEEM_REWARD
        && (rewardIntent.rewardId() == null
        || rewardIntent.rewardId().isBlank())) {
    throw new IllegalArgumentException(
            "Reward ID is required for redemption"
    );
}
```

This is intent-contract validation, not business validation.

### G. `RewardIntentService`

**Responsibility:** Orchestrate prompt building, LLM inference, parsing,
and validation.

The central flow is:

``` java
String prompt = promptBuilder.buildPrompt(customerMessage);

String llmResponse = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

RewardIntent rewardIntent = parser.parse(llmResponse);

validator.validate(rewardIntent);

return rewardIntent;
```

The `.call().content()` step sends the prompt through Spring AI and
receives the model-generated content.

### H. `RewardIntentController`

**Responsibility:** Accept the customer's message and delegate to
`RewardIntentService`.

Endpoint:

``` text
POST /api/rewards/intent/interpret
Content-Type: text/plain
```

It returns the `RewardIntent` object, which Spring serializes as JSON.

## 4. What we tested today

### Test 1 --- Parser and validator with manually supplied JSON

We supplied JSON directly to the parser, without calling OpenAI:

``` json
{
  "intent": "REDEEM_REWARD",
  "rewardId": "HEADPHONE_001"
}
```

Observed:

``` text
Parsed successfully: RewardIntent[intent=REDEEM_REWARD, rewardId=HEADPHONE_001]
Validation successful!
```

This tested parsing and validation independently. It did **not** test
LLM inference.

### Test 2 --- Null reward ID for redemption

Input:

``` json
{
  "intent": "REDEEM_REWARD",
  "rewardId": null
}
```

Observed behavior:

-   Jackson successfully created a `RewardIntent`.
-   The validator rejected it because `rewardId` is required for
    redemption.

This demonstrated the difference between deserialization and validation.

### Test 3 --- Actual LLM integration

We sent a real HTTP request to `POST /api/rewards/intent/interpret`
with:

``` text
I want to check how many reward points I have
```

The application called OpenAI through Spring AI, parsed and validated
the response, and returned:

``` json
{
  "intent": "CHECK_BALANCE",
  "rewardId": null
}
```

**This was the first successful end-to-end LLM intent-classification
test.**

## 5. Configuration issue resolved

Initially, OpenAI received the literal placeholder `${OPENAI_API_KEY}`
instead of the actual key, causing a `401 Unauthorized` response.

The local project configuration was changed to import the project-root
`.env` file through Spring Boot.

In `src/main/resources/application.properties`:

``` properties
spring.config.import=optional:file:.env[.properties]
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

In the project-root `.env` file:

``` properties
OPENAI_API_KEY=your-real-api-key
```

The actual `.env` must not be committed.

In `.gitignore`:

``` gitignore
.env
.env.*
!.env.example
```

The committed `.env.example` contains only a placeholder:

``` properties
OPENAI_API_KEY=your-api-key-goes-here
```

`.env.example` documents which variable another developer must provide.
It must never contain the real key.

The successful LLM response confirmed that the key was resolved
correctly in the tested local run.

## 6. Key concepts learned

### Prompting is not enforcement

Asking an LLM to return JSON does not guarantee valid JSON or compliance
with the contract. The application must parse and validate the response.

### Parsing and validation are different

-   **Parsing:** Can JSON be converted into a Java object?
-   **Intent validation:** Does the object satisfy required intent-field
    rules?
-   **Business validation:** Is the requested operation allowed and
    valid against trusted application data?

### The LLM is not the source of truth

The LLM interprets customer language. The Java backend remains
responsible for authorization, balances, reward availability,
eligibility, and transactions.

### The service coordinates the workflow

`RewardIntentService` orchestrates this initial AI workflow by
connecting prompt construction, model inference, parsing, and
validation.

## 7. What is not implemented yet

Today's successful response does not mean the customer can actually
check a balance or redeem a reward.

Not implemented in this flow:

-   Fetching the authenticated customer's identity.
-   Reading the customer's points balance from the database.
-   Searching the MongoDB reward catalog.
-   Confirming that a reward ID exists.
-   Checking reward availability and eligibility.
-   Checking whether the customer has enough points.
-   Executing a redemption transaction.
-   Preventing duplicate or concurrent redemptions.
-   Production-grade error handling for malformed model responses,
    timeouts, rate limits, and API failures.
-   Automated JUnit tests for the integrated workflow.

The current `RewardIntentValidator` is an initial response-contract
validator, not a complete security or business-rule layer.

## 8. Next implementation steps

1.  Run additional real LLM classification tests:
    -   `I want to redeem headphones`
    -   `Show me available rewards`
    -   `What is the weather today?`
2.  Review actual model responses and improve prompt reliability if
    needed.
3.  Add automated tests for the parser, validator, and service.
4.  Connect the intent workflow to the trusted MongoDB reward catalog.
5.  Add backend business validation.
6.  Implement actual redemption only after authorization and business
    rules are in place.

## 9. Interview-ready explanation

"I built an intent-classification workflow in a Spring Boot application
using Spring AI and an OpenAI chat model. A REST controller accepts a
customer's natural-language message. A prompt builder instructs the LLM
to classify it into a predefined set of supported reward intents and
return structured JSON. The service sends the prompt through Spring AI's
ChatClient, receives the model response, and uses Jackson to deserialize
it into a Java record. A separate validator checks intent-specific
required fields, such as requiring a reward ID for a redemption request.
The LLM is only used for language understanding; actual customer
authorization, points balance, reward availability, and redemption
decisions remain responsibilities of the Java backend."

**Be precise in interviews:** This is a learning implementation and a
working local LLM integration. Do not describe the unimplemented catalog
lookup or redemption transaction as completed production functionality.
