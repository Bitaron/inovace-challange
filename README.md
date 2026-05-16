# LogSummarizer Service

A Spring Boot microservice that accepts a batch of application log entries via a REST API, uses Google Gemini to perform AI-powered anomaly detection, and returns a structured JSON report of potential issues and recommendations.

---

## Features

- **Model-agnostic LLM interface** — add a new provider (OpenAI, Claude, etc.) by implementing one interface; zero changes to service or controller code
- **Factory pattern** — the active LLM provider is selected at runtime from `application.properties`
- **Strict JSON enforcement** — dual strategy: Gemini API `responseMimeType=application/json` + explicit prompt schema
- **Structured error responses** — every error returns a typed `ApiError` with a machine-readable code
- **Bean Validation** — all request fields are validated before the LLM is called

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ (or use the included `./mvnw`) |
| Docker | 24+ (optional, for containerised run) |
| Gemini API key | Free key from [Google AI Studio](https://aistudio.google.com/apikey) |

---

## Configuration

Edit `src/main/resources/application.properties` and set your API key:

```properties
spring.ai.google.genai.api-key=YOUR_GEMINI_API_KEY_HERE
```

To switch the LLM provider (after implementing a new `LlmClient` bean):

```properties
app.llm.provider=gemini   # change to: openai, claude, etc.
```

---

## Running Locally

```bash
# 1. Clone / enter the project
cd Innovacechallenge

# 2. Run directly with Maven
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

---

## Running with Docker

```bash
# 1. Build the image
docker build -t log-summarizer .

# 2. Run with your API key passed as an env variable
docker run -p 8080:8080 \
  -e SPRING_AI_GOOGLE_GENAI_API_KEY=YOUR_GEMINI_API_KEY_HERE \
  log-summarizer
```

---

## API Reference

### `POST /summarize-logs`

Accepts a batch of log entries and returns a structured anomaly report.

**Request body:**

```json
{
  "logs": [
    {
      "timestamp": "2025-10-15T10:00:01Z",
      "level": "ERROR",
      "service": "payment-service",
      "message": "Database connection timed out after 3001ms"
    },
    {
      "timestamp": "2025-10-15T10:00:03Z",
      "level": "ERROR",
      "service": "payment-service",
      "message": "Database connection timed out after 3005ms"
    },
    {
      "timestamp": "2025-10-15T10:00:07Z",
      "level": "ERROR",
      "service": "order-service",
      "message": "Failed to process payment due to upstream service unavailability"
    }
  ]
}
```

**Example `curl` command:**

```bash
curl -s -X POST http://localhost:8080/summarize-logs \
  -H "Content-Type: application/json" \
  -d '{
    "logs": [
      {"timestamp": "2025-10-15T10:00:01Z", "level": "ERROR", "service": "payment-service", "message": "Database connection timed out after 3001ms"},
      {"timestamp": "2025-10-15T10:00:03Z", "level": "ERROR", "service": "payment-service", "message": "Database connection timed out after 3005ms"},
      {"timestamp": "2025-10-15T10:00:07Z", "level": "ERROR", "service": "order-service",   "message": "Failed to process payment due to upstream service unavailability"}
    ]
  }' | jq
```

**200 OK response:**

```json
{
  "summary": "A database connectivity issue in 'payment-service' is causing cascading failures. Repeated timeout errors between 10:00:01Z and 10:00:03Z are preventing payment processing, which in turn causes 'order-service' to fail order fulfilment.",
  "key_error_signatures": [
    "Database connection timed out after ~3000ms",
    "Upstream service unavailability in order-service"
  ],
  "recommendation": "Investigate database load, connection pool exhaustion, and network latency for the payment-service. Check if a deployment or traffic spike occurred around 10:00Z.",
  "analyzed_log_count": 3
}
```

**Error response (example — empty logs array):**

```json
{
  "errorCode": 4001,
  "message": "Request validation failed",
  "details": "logs array must contain at least one log entry",
  "timestamp": "2025-10-15T10:01:00Z"
}
```

---

## Error Code Reference

| Code | HTTP | Meaning |
|------|------|---------|
| 4001 | 400 | Request validation failed |
| 5001 | 500 | LLM API call failed (network / auth / quota) |
| 5002 | 500 | LLM returned unparseable JSON |
| 5003 | 500 | Configured LLM provider is not registered |
| 5099 | 500 | Unexpected internal error |

---

## Architecture & Design Decisions

### LLM Abstraction Layer

```
LlmClient (interface)
    └── GeminiLlmClient  ← current implementation
         
LlmClientFactory         ← reads app.llm.provider → returns correct LlmClient
LlmProperties            ← @ConfigurationProperties for app.llm.*
LlmProvider (enum)       ← catalogue of supported provider keys
```

**Why the Factory pattern?** The challenge requirement specifies Gemini today but good architecture shouldn't hard-code a vendor. The factory auto-discovers all `LlmClient` beans at startup via Spring's dependency injection (`List<LlmClient>` parameter). Adding a new provider is one file — no changes anywhere else.

### Strict JSON Output (dual strategy)

| Layer | Mechanism |
|-------|-----------|
| Gemini API | `responseMimeType=application/json` — the API itself filters output |
| Prompt | Explicit JSON schema + "respond ONLY with JSON" instruction |
| Application | Jackson deserialization; failure → `LlmParsingException` → `ApiError` |

### Prompt Engineering

The system prompt (in `LogSummarizerService.SYSTEM_PROMPT`) is designed around three principles:

1. **Role framing** — "You are an expert SRE" positions the model for domain-specific reasoning rather than generic summarisation.
2. **Output contract** — an explicit JSON schema with field names, types, and semantics eliminates ambiguity and prevents hallucination of unknown fields.
3. **Analysis instructions** — enumerates specific patterns to look for (causal chains, bursts, cascading failures) guiding the model beyond surface-level observation.

The user message wraps log lines in an `<LOGS>` XML tag block, providing a clear structural boundary that the model can parse reliably.

### Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Spring AI over raw HTTP | Hides low-level HTTP/auth; slightly less control over raw request payload |
| Single `/summarize-logs` endpoint | Simple and per-spec; a production system could add streaming (`/summarize-logs/stream`) |
| Synchronous call | Easiest to reason about; a high-volume system should use async + a job queue |
| `analyzed_log_count` overwritten by service | Prevents LLM hallucination of wrong count; slight duplication of information |

---

## Full Engineered Prompt

The following is the exact system prompt sent to the LLM on every request (see `LogSummarizerService.SYSTEM_PROMPT`):

```
You are an expert Site Reliability Engineer (SRE) specialising in application log analysis and anomaly detection.

CRITICAL OUTPUT RULES — follow these exactly:
1. Your ENTIRE response MUST be a single, valid JSON object.
2. Do NOT include markdown code fences (```json or ```), explanations, or any text outside the JSON object.
3. The JSON MUST conform strictly to this schema:
   {
     "summary":              "<string: one-paragraph narrative of the root cause or system health>",
     "key_error_signatures": ["<string: distinct error pattern>", ...],
     "recommendation":       "<string: concrete, actionable next steps for an on-call engineer>",
     "analyzed_log_count":   <integer: exact number of log entries you received>
   }
4. All four fields are required. Do not omit or rename them.

ANALYSIS INSTRUCTIONS:
- Identify recurring ERROR or WARN messages and group them into distinct error signatures.
- Look for causal chains across services (e.g., database timeouts → downstream payment failures).
- Detect anomalies: bursts of errors from a single service, mixed severity spikes, or unusual timing patterns.
- Note which microservices are affected and whether failures appear isolated or cascading.
- If all logs are INFO level with no issues, state "System appears healthy" in the summary and return an empty array for key_error_signatures.
- Be concise but precise: include service names, error types, and relevant time windows.
```

The user message appends the formatted log lines wrapped in `<LOGS>…</LOGS>` tags.
