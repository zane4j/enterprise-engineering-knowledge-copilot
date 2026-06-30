# Grounded Chat and SSE Protocol

## Request flow

```text
POST /knowledge-bases/{id}/chat/stream
  -> tenant identity and knowledge-base read access
  -> pgvector retrieval
  -> freeze evidence and citations
  -> bounded grounded prompt
  -> Spring AI ChatClient streaming call
  -> SSE citations, tokens, completed/error
```

## Grounding rules

1. Retrieval is always scoped to the authenticated tenant and requested knowledge base.
2. Retrieved chunks are treated as untrusted data. Instructions embedded in documents must not override the system prompt.
3. The model is told to answer only from supplied evidence and cite source labels such as `[S1]`.
4. The application emits citations from retrieval, not from model output.
5. When no chunk is found, no model call is made; the response reports insufficient knowledge-base evidence.

## SSE events

| Event | Payload | Meaning |
|---|---|---|
| `citation` | source ID, document, locator, score | Frozen source available to the client before answer generation |
| `token` | text | A model-generated answer fragment |
| `completed` | citation count, grounded flag | Stream completed normally |
| `error` | public code and message | The model stream failed after the response began |

## Configuration

Chat is disabled by default:

```properties
COPILOT_CHAT_PROVIDER=none
SPRING_AI_CHAT_MODEL=none
```

Enable OpenAI chat for both model configuration and application intent:

```properties
COPILOT_CHAT_PROVIDER=openai
SPRING_AI_CHAT_MODEL=openai
OPENAI_API_KEY=<secret>
COPILOT_CHAT_MODEL=gpt-5-mini
COPILOT_CHAT_TEMPERATURE=0.1
```

The API returns HTTP `503` before opening an SSE stream when chat is not configured. API keys belong only in server secrets or a secret manager.
