# Question Answer
[Answer](Inovace Answers.pdf)

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
spring.ai.google.genai.api-key=YOUR_GEMINI_API_KEY_HERE //If api key in default project doesn't work, create a seperate project in google ai studio
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
  -e SPRING_AI_GOOGLE_GENAI_API_KEY=<GEMINI_API_KEY> \
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

The system prompt (in `SystemPrompt`) is designed around three principles:

1. **Role framing** — "You are an expert SRE" positions the model for domain-specific reasoning rather than generic summarisation.
2. **Output contract** — an explicit JSON schema with field names, types, and semantics eliminates ambiguity and prevents hallucination of unknown fields.
3. **Analysis instructions** — enumerates specific patterns to look for (causal chains, bursts, cascading failures) guiding the model beyond surface-level observation.
4. **Few shots** — example of sample logs and summaries to help the model understand the structure of the input.
5. **Structured** — prompts are structured in xml tags for llm's better understanding.


### Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Spring AI over raw HTTP | Hides low-level HTTP/auth; slightly less control over raw request payload |
| `analyzed_log_count` overwritten by service | Prevents LLM hallucination of wrong count; slight duplication of information |

---

## Full Engineered Prompt
Full prompt: [System Prompt](src/main/java/com/innovace/Innovacechallenge/service/SystemPrompt.java)
