# llm-router-common

Shared Kotlin API contracts for the LLM Router. This module intentionally contains only DTOs, enums and pure contract helpers—no Spring, JPA, HTTP client or business logic.

Other JVM services can depend on this artifact to consume the router API without duplicating request and response classes.
