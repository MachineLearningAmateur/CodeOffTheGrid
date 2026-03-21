# OffTheGrid Android Design Plan

## Status
- Draft v3
- Last updated: 2026-03-18
- Primary target: Android tablet
- Secondary target: iOS later, not part of v1 architecture constraints
- Current project state: Kotlin/Compose Android prototype with a working landscape UI shell and local mock interactions

## 1. Product Summary

OffTheGrid is an offline-first Android tablet app for storing, organizing, and practicing coding problems locally. The app provides a GUI for browsing a personal problem repository, reading problem content, writing Python solutions, running local test cases through an embedded Python runtime, and tracking notes and progress without requiring internet access.

The first release is optimized for a Samsung Galaxy Tab S7 class device, with a tablet-first workspace and a local-native execution model.

## 2. Current State

The repository now contains an Android Studio project plus a substantial interactive prototype:
- Kotlin-based Android app
- Jetpack Compose enabled
- `minSdk = 29`
- package/application ID set to `dev.kaixinguo.standalonecodepractice`
- custom dark tablet-first theme
- landscape three-pane workspace implemented as a mock UI
- project initialized in Git and pushed to GitHub

The project is no longer in the "blank scaffold" stage. It now has a serious UI prototype, but it is still mostly presentation-layer code with mock state.

### What Already Exists in the Prototype
- collapsible left sidebar with `Problems`, `Ask AI`, and `Settings`
- problem search and local filter chips: `All`, `Open`, `Solved`
- solved badges in the problem list
- bounded swipe-left reveal for deleting problems from the local mock list
- center `Workspace` pane with `Keyboard` and `Sketch` modes sharing one surface
- Python-only editable text area with syntax highlighting
- sketch overlay with draw, eraser, size controls, and color controls
- right support pane with `Problem`, `Custom`, and `Results`
- always-visible `Submit` action with mock loading state
- editable custom test block for local unit tests
- staged hint reveal in the `Problem` tab

### What Is Still Mocked
- no Room database
- no real navigation stack
- no persistent drafts, notes, or progress
- no real problem schema or import pipeline
- no embedded Python runtime
- no real custom test execution or submission logic
- no AI integration beyond placeholder UI

## 3. Goals

- Provide a local repository of coding problems for offline use.
- Support writing and running Python solutions on-device.
- Make the app usable in low-connectivity or no-connectivity environments.
- Offer a tablet-friendly workspace with simultaneous access to problem content, notes, and code.
- Persist all important data locally, including problem statements, drafts, notes, tags, and progress.
- Keep AI features optional and decoupled from the core app.

## 4. Non-Goals

- Full parity with the LeetCode website.
- Account sync with external platforms in v1.
- Real-time collaboration.
- Multi-language execution support in v1.
- iOS compatibility in the first implementation.
- Automatic scraping as a required import path for MVP.

## 5. Core User Scenarios

### 4.1 Offline Practice
- User opens the app with no internet.
- User searches local problems by tag, difficulty, or status.
- User reads the cached statement and examples.
- User writes Python code and runs local test cases.
- User saves progress and returns later.

### 4.2 Personal Knowledge Base
- User stores handwritten or typed notes per problem.
- User marks problems as not started, in progress, solved, or review later.
- User groups problems by topic such as arrays, graphs, DP, or trees.

### 4.3 Focused Tablet Workspace
- User works in landscape mode with a multi-pane layout.
- User keeps the problem statement visible while editing and testing code.
- User uses the app as a dedicated study tool on a tablet stand or keyboard case.

## 6. Platform Strategy

### 5.1 v1 Platform Decision
- Build natively for Android.
- Use Kotlin and Jetpack Compose.
- Optimize for tablets first, then support smaller Android devices if needed.

### 5.2 Why Native Android
- Better control over large-screen layouts.
- Easier integration with embedded runtimes.
- Stronger performance characteristics for local Python execution.
- Cleaner path for future native on-device model integration.

### 5.3 iOS Strategy
- Defer iOS implementation until Android validates the product.
- Preserve clean boundaries around execution and AI services so future iOS work is possible without rewriting the entire app.
- Do not let iOS constraints weaken the Android v1 architecture.

## 7. Product Features

## 6.1 MVP Features
- Local problem library
- Problem import from local JSON or packaged seed data
- Search, filter, and sort
- Problem detail view
- Python editor with saved drafts
- Local custom test case execution
- Notes and bookmarks
- Progress tracking
- Offline-first persistence

## 6.2 Phase 2 Features
- Problem attempt history
- Import and export backups
- Folder or collection support
- Better editor features such as syntax highlighting and auto-indent
- Local AI assistant using a small quantized Qwen-class model
- Hint generation and explanation tools

## 6.3 Deferred Features
- Cloud sync
- Account login
- Cross-device sync
- iOS app
- External problem source integrations that rely on unstable scraping flows

## 8. Current Layout Direction

The current implemented target is a tablet-first three-pane workspace in landscape mode.

Primary landscape layout:
- Left pane: collapsible problem browser with search, status filters, solved flags, and delete reveal
- Center pane: primary workspace for Python typing and sketch annotation on the same surface
- Right pane: support column for problem reference, custom tests, results, hints, and submit actions

This layout is intended to keep the full study workflow visible without constant screen switching while still allowing a focus mode by collapsing the left pane.

## 8.1 Why This Layout
- It matches the strengths of a tablet-sized display.
- It reduces context switching between reading and coding.
- It supports a more serious study workflow than a phone-style stacked screen design.

## 8.2 Alternate Layouts

Portrait or narrow widths:
- Switch to a two-pane or tabbed layout
- Prioritize problem content and editor
- Collapse filters and notes into drawers, tabs, or bottom sheets

## 8.3 Planned Primary Screens

### Current Landscape Prototype
- collapsible sidebar with problem browsing and basic filtering
- shared code/sketch workspace
- support pane with problem details, custom tests, and result state

### Planned Real Screens After Prototype Phase
- library/data-backed problem browser
- problem detail and notes flow
- workspace with persisted draft and execution state
- settings/import/export screen

## 8.4 Interaction Principles
- Fast local navigation
- Minimal loading states after initial app startup
- Persistent drafts
- No data loss on app backgrounding
- Clear distinction between saved draft code and temporary execution state

## 9. Technical Architecture

## 9.1 Stack
- Language: Kotlin
- UI: Jetpack Compose
- Persistence: Room over SQLite
- Concurrency: Kotlin coroutines
- Background jobs: WorkManager where needed
- Python runtime: embedded native Python interpreter
- Future AI runtime: native local inference backend, isolated from Python runtime

## 9.2 High-Level Modules

### `app`
- App entrypoint
- Dependency wiring
- Navigation shell

### `feature-library`
- Problem list UI
- Search, filters, tags, status views

### `feature-problem`
- Problem details
- Examples
- Notes panel

### `feature-editor`
- Code editor screen
- Draft persistence
- Test case input and run controls

### `feature-progress`
- Solve status
- Attempt summaries
- Review queues

### `data-local`
- Room entities
- DAOs
- repository implementations

### `domain`
- Business models
- use cases
- service interfaces

### `runtime-python`
- Python embedding
- script execution
- test harness integration
- output capture

### `runtime-ai` (future)
- local model loading
- prompt construction
- inference interface

## 9.3 Architectural Boundaries

Keep these interfaces stable:
- `ProblemRepository`
- `DraftRepository`
- `NotesRepository`
- `PythonRunner`
- `AiAssistant`

This allows the UI and domain layers to stay independent from low-level runtime details.

## 10. Data Model

## 10.1 Core Entities

### `Problem`
- `id`
- `title`
- `slug`
- `difficulty`
- `statement`
- `constraints`
- `source`
- `createdAt`
- `updatedAt`

### `ProblemTag`
- `problemId`
- `tag`

### `ProblemExample`
- `id`
- `problemId`
- `input`
- `output`
- `explanation`

### `SolutionDraft`
- `problemId`
- `language` (v1 fixed to python)
- `code`
- `updatedAt`

### `CustomTestCase`
- `id`
- `problemId`
- `label`
- `input`
- `expectedOutput`
- `updatedAt`

### `RunRecord`
- `id`
- `problemId`
- `testInput`
- `output`
- `success`
- `durationMs`
- `createdAt`

### `ProblemNote`
- `id`
- `problemId`
- `content`
- `updatedAt`

### `ProgressState`
- `problemId`
- `status`
- `lastOpenedAt`
- `lastSolvedAt`
- `reviewLater`

## 10.2 Storage Notes
- Room stores structured metadata and user state.
- Large problem content may remain in SQLite unless profiling shows a need for file-based storage.
- Backups should export to a portable JSON bundle later.

## 11. Python Execution Design

## 11.1 Objective
Run user-authored Python solution code locally against custom or problem-derived test cases.

## 11.2 Responsibilities of `PythonRunner`
- Load the embedded interpreter
- Accept user code and test input
- Execute inside a constrained harness
- Capture stdout, stderr, and execution errors
- Return structured run results to the UI

## 11.3 Execution Model
- Runs should happen off the main thread.
- The UI must remain responsive during execution.
- Each run should use a clean harness context when possible.
- The first version may allow only straightforward stdin/stdout or function-based invocation depending on the chosen problem format.

## 11.4 Safety and Stability
- Prevent app crashes from interpreter failures.
- Add execution timeout support.
- Capture Python exceptions cleanly and show them in the run panel.
- Avoid exposing unnecessary filesystem access to user code.

## 11.5 Input Format Decision
For MVP, prefer one controlled problem format:
- either function-signature based execution
- or stdin/stdout based execution

Function-signature based execution is likely cleaner for structured testing and UI validation.

## 12. AI Integration Plan

AI is not part of MVP.

If added later, local AI should be narrowly scoped:
- hint generation
- note summarization
- solution explanation
- problem tagging assistance

AI should be optional and isolated behind `AiAssistant`.

Do not couple local model loading or inference lifecycle to the Python execution layer.

## 13. Import Strategy

## 13.1 MVP Import
- Seed the app with a small starter set of problems
- Support import from a local JSON format
- Keep the schema simple and versioned

## 13.2 Why Not Depend on Scraping
- External sites change frequently
- Import pipelines become brittle
- Content and legal constraints may need review

The app should remain useful even if external imports do not exist.

## 14. Navigation and Screens

## 14.1 Main Screens
- Library
- Problem Detail
- Workspace
- Notes and History
- Settings

## 14.2 Suggested Navigation Model
- Permanent navigation rail on tablet
- Nested pane navigation inside the library and workspace
- Save user pane widths and last opened problem where practical

## 15. Current Priorities

Based on the current state of the repository, the next priorities are:

1. Refactor the large prototype file into smaller UI/workspace files before adding real data flow.
2. Define the local problem JSON schema and seed format.
3. Start the Room data model and repository layer.
4. Replace hardcoded prototype state with repository-backed screen state.
5. Introduce real draft, progress, and custom-test persistence.

## 16. MVP Milestones

## Milestone 1: UI Prototype Stabilization
- split the monolithic `LandscapeWorkspaceScreen.kt`
- keep the current landscape behavior intact during refactor
- establish screen-level state holders that can later connect to repositories

## Milestone 2: Local Problem Data
- define `Problem`, `Example`, `CustomTestCase`, `Draft`, and `ProgressState` storage models
- build Room entities and DAOs
- seed the database from local JSON or bundled starter data
- replace hardcoded problem lists with data-backed state

## Milestone 3: Persistence Layer
- persist selected problem, solved state, draft code, custom tests, and hint progress
- save problem deletion/archive behavior as real data actions
- add repository interfaces and use cases around problem browsing

## Milestone 4: Execution Layer
- integrate embedded Python runtime
- run local custom tests from the `Custom` tab
- capture output, failures, and runtime errors into the `Results` pane
- implement real submit semantics for local validation/saved submissions

## Milestone 5: Notes and Workflow Completion
- add per-problem notes and review state
- store accepted submissions and use them to drive solved flags
- restore workspace state when reopening the app
- tighten tablet UX polish and prepare for portrait/narrow handling

## 17. Risks

## 17.1 Embedded Python Complexity
- Native integration may take more time than standard app features.
- Library packaging and ABI handling need careful setup.

## 17.2 Editor Quality
- A weak editor will make the app feel unfinished.
- Syntax highlighting and text editing performance should be validated early.

## 17.3 Problem Format Drift
- If imported content is inconsistent, execution UX becomes messy.
- Standardize the local problem schema early.

## 17.4 AI Scope Creep
- Local model integration can dominate engineering time.
- Keep it out of the critical path for v1.

## 18. Open Questions

- Which embedded Python approach should be used on Android?
- What exact local problem JSON schema should be supported?
- Should execution target function-based problems only in v1?
- Should deletion really remove a problem from the local repository, or should it archive/hide it?
- How should custom test cases be structured in the UI: text block, form rows, or both?
- Should notes live in the right pane or as a separate view in the first MVP?
- Should the first runtime support only deterministic local validation, or also ad hoc input/output runs?

## 19. Recommended First Build Order

1. Refactor the prototype UI into smaller files.
2. Define the local problem schema.
3. Build the Room data model and repository layer.
4. Replace hardcoded problem/sidebar/right-pane state with real stored state.
5. Add draft, custom-test, and progress persistence.
6. Integrate the Python runner.
7. Implement real results/submission flow and error handling.
8. Evaluate whether local AI is worth a phase 2 prototype.

## 20. Final Recommendation

The correct v1 strategy is a native Android, offline-first tablet app with a strong local repository model and an embedded Python execution layer. The app should be useful without internet, without AI, and without cross-platform support. If that foundation is solid, iOS and local AI can be added later behind stable service boundaries rather than forcing premature compromise into the first architecture.

## 21. MVP Review

### Already Strong
- the landscape UX direction is now concrete instead of speculative
- the center workspace model is clear: Python typing and sketching share one main surface
- the right pane has a sensible split between problem reference, custom tests, and results
- solved status and filtering are already represented in the UI

### Biggest Gaps Before MVP
- the prototype is still largely hardcoded and lives in one oversized UI file
- there is no real persistence layer yet
- there is no actual Python execution path
- solved state is visual only and not derived from real submissions
- notes, import, and schema-driven problem content do not exist yet

### Recommended Next MVP Slice
- refactor the UI file
- define the local JSON/Room schema
- load a seeded problem set from local data
- persist draft code, solved state, and custom tests
- wire the right pane to real stored problem data

Once those are in place, the next true MVP gate is the embedded Python runner.

