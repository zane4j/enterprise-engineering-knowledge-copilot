# ADR-005: Grounded chat uses frozen application citations

## Status
Accepted

## Decision
The application performs retrieval before calling the chat model, freezes the selected citations, emits those citations to the client, and passes the same evidence to the model as untrusted reference data.

## Context
Letting the model choose application citations after generation weakens traceability and can create unsupported source references. The retrieval layer already enforces tenant and knowledge-base access filters.

## Consequences
- The client receives citations that are directly tied to retrieved chunks, not model-generated URLs or names.
- The system prompt instructs the model to treat evidence as data and ignore instructions embedded in source files.
- If retrieval returns no evidence, the service does not call the model and responds with an explicit knowledge-gap message.
- The first chat increment is stateless. Session memory and persisted conversations are separate follow-up work.
