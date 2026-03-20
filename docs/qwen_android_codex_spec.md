# Codex Task: Implement Prompt Templates for Local Qwen LeetCode Assistant

## Goal

Implement a prompt templating layer for the Android app's local AI assistant so the app can support three tutoring modes:

1. **Hint**
2. **Explain**
3. **Solve**

The model is a local Qwen coder model running on-device. The prompt layer should produce clean, structured prompts for the inference engine.

---

## Requirements

### Functional Requirements

Implement support for these prompt modes:

- `HINT`
- `EXPLAIN`
- `SOLVE`

Each mode should:

- inject the problem statement
- inject optional user code
- apply a mode-specific instruction set
- return a final prompt string ready to send to the local model runtime

---

## Behavior Requirements

### HINT mode

The assistant should:

- give small hints only
- not reveal the full solution
- not provide full code
- guide the user step by step
- be concise and helpful

### EXPLAIN mode

The assistant should:

- explain the solution clearly
- focus on intuition first
- then explain the algorithm
- mention time and space complexity
- avoid unnecessary verbosity

### SOLVE mode

The assistant should:

- provide the solution when the user is stuck
- first explain the high-level idea
- then provide code
- keep the code clean and readable
- include time and space complexity

---

## Implementation Tasks

### 1. Add a prompt mode enum

Create an enum to represent the available prompt modes.

Suggested example:

```kotlin
enum class PromptMode {
    HINT,
    EXPLAIN,
    SOLVE
}
```

---

### 2. Add prompt templates

Create a dedicated prompt template holder.

Suggested file name:

```text
runtime-ai/src/main/java/.../PromptTemplates.kt
```

Suggested structure:

```kotlin
object PromptTemplates {
    val hint = """
        You are a helpful LeetCode tutor.

        Your job:
        - Give small hints only
        - Do not reveal the full solution
        - Do not provide full code
        - Guide the user step by step
        - Be clear and concise

        Problem:
        {problem}

        User code:
        {code}

        User request:
        Give me a hint.
    """.trimIndent()

    val explain = """
        You are a helpful LeetCode tutor.

        Your job:
        - Explain the solution clearly
        - Focus on intuition first
        - Then explain the algorithm
        - Mention time and space complexity
        - Do not be overly verbose

        Problem:
        {problem}

        User code:
        {code}

        User request:
        Explain the solution in a beginner-friendly way.
    """.trimIndent()

    val solve = """
        You are a helpful LeetCode tutor.

        Your job:
        - Solve the problem correctly
        - First explain the high-level idea
        - Then provide the code
        - Keep the code clean and readable
        - Include time and space complexity

        Problem:
        {problem}

        User code:
        {code}

        User request:
        Provide the solution.
    """.trimIndent()
}
```

---

### 3. Add a prompt builder

Create a builder responsible for filling templates.

Suggested file name:

```text
runtime-ai/src/main/java/.../PromptBuilder.kt
```

Suggested API:

```kotlin
interface PromptBuilder {
    fun build(
        mode: PromptMode,
        problem: String,
        code: String?
    ): String
}
```

Suggested implementation behavior:

- choose the correct template from `PromptTemplates`
- replace `{problem}` with the problem text
- replace `{code}` with the user code if present
- if code is null or blank, replace with `No code provided.`
- return the final prompt string

Suggested implementation example:

```kotlin
class DefaultPromptBuilder : PromptBuilder {
    override fun build(
        mode: PromptMode,
        problem: String,
        code: String?
    ): String {
        val template = when (mode) {
            PromptMode.HINT -> PromptTemplates.hint
            PromptMode.EXPLAIN -> PromptTemplates.explain
            PromptMode.SOLVE -> PromptTemplates.solve
        }

        return template
            .replace("{problem}", problem.trim())
            .replace("{code}", code?.takeIf { it.isNotBlank() }?.trim() ?: "No code provided.")
    }
}
```

---

### 4. Add request models if needed

If the current architecture would benefit from a request model, add one.

Suggested example:

```kotlin
data class PromptRequest(
    val mode: PromptMode,
    val problem: String,
    val code: String?
)
```

This is optional, but useful if the AI layer already uses request objects.

---

### 5. Integrate prompt building into the AI assistant layer

Update the AI assistant flow so inference no longer uses raw ad hoc strings.

Instead:

- build prompts through `PromptBuilder`
- send the returned prompt into the local model runtime

Suggested flow:

```text
UI action
-> AI use case
-> PromptBuilder
-> Local Qwen engine
-> Response returned to UI
```

---

### 6. Add high-level AI assistant methods

Expose methods aligned with the app use cases.

Suggested example:

```kotlin
interface AiAssistant {
    suspend fun generateHint(problem: String, code: String?): String
    suspend fun explainSolution(problem: String, code: String?): String
    suspend fun solveProblem(problem: String, code: String?): String
}
```

Suggested implementation pattern:

- `generateHint()` uses `PromptMode.HINT`
- `explainSolution()` uses `PromptMode.EXPLAIN`
- `solveProblem()` uses `PromptMode.SOLVE`

---

### 7. Add unit tests

Add tests for prompt generation.

Test cases should cover:

#### Template selection
- HINT mode uses hint template
- EXPLAIN mode uses explain template
- SOLVE mode uses solve template

#### Placeholder substitution
- problem text is inserted correctly
- code is inserted correctly
- missing code becomes `No code provided.`

#### Output sanity
- HINT prompt includes “Do not reveal the full solution”
- EXPLAIN prompt includes complexity instructions
- SOLVE prompt includes both explanation and code instructions

Suggested test names:

```text
build_hint_prompt_with_code
build_hint_prompt_without_code
build_explain_prompt_with_code
build_solve_prompt_without_code
```

---

## Non-Goals

Do not implement these yet unless already required by the architecture:

- chat history or multi-turn memory
- advanced system/user role formatting
- chain-of-thought prompting
- prompt compression
- RAG/context retrieval
- multiple languages
- automatic difficulty adaptation

Keep the first version simple.

---

## Expected File Additions

Suggested new files:

```text
PromptMode.kt
PromptTemplates.kt
PromptBuilder.kt
DefaultPromptBuilder.kt
```

Possible updated files:

```text
AiAssistant.kt
AiAssistantImpl.kt
QwenLocalEngine integration layer
unit test files
```

---

## Acceptance Criteria

The task is complete when:

1. The app has a `PromptMode` enum with `HINT`, `EXPLAIN`, and `SOLVE`
2. Prompt templates are stored centrally
3. A prompt builder fills `{problem}` and `{code}` correctly
4. Missing code is handled gracefully
5. The AI assistant uses the builder instead of raw prompt strings
6. Unit tests validate template selection and substitution
7. The implementation is clean, readable, and easy to extend later

---

## Notes for Implementation

- Prefer small, focused files
- Keep templates centralized
- Avoid hardcoding prompt strings in UI code
- Keep prompt generation deterministic
- Make it easy to add future modes later, such as:
  - `DEBUG`
  - `OPTIMIZE`
  - `TEST_CASES`

---

## Example Expected Usage

```kotlin
val prompt = promptBuilder.build(
    mode = PromptMode.HINT,
    problem = problemStatement,
    code = currentUserCode
)

val response = localQwenEngine.generate(prompt)
```

---

## Deliverable

Implement the prompt templating layer and wire it into the local AI assistant path with tests.
