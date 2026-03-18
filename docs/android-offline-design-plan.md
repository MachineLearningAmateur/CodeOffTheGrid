# StandaloneLeetCode Android Design Plan

## Status
- Draft v2
- Last updated: 2026-03-18
- Primary target: Android tablet
- Secondary target: iOS later, not part of v1 architecture constraints
- Current project state: Android Studio project created with Kotlin, Jetpack Compose, and `minSdk = 29`

## 1. Product Summary

StandaloneLeetCode is an offline-first Android tablet app for storing, organizing, and practicing LeetCode-style problems locally. The app provides a GUI for browsing a personal problem repository, reading problem content, writing Python solutions, running local test cases through an embedded Python runtime, and tracking notes and progress without requiring internet access.

The first release is optimized for a Samsung Galaxy Tab S7 class device, with a tablet-first workspace and a local-native execution model.

## 2. Current State

The repository now contains an initial Android Studio project scaffold:
- Kotlin-based Android app
- Jetpack Compose enabled
- `minSdk = 29`
- default single-activity template

This means the project has moved from pure brainstorming into early implementation setup. The next design work should focus on:
- finalizing the package name
- replacing template content with the first real app shell
- defining screen mockups and navigation structure
- deciding the local problem schema before data-layer work starts

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

The current target layout is a tablet-first three-pane workspace in landscape mode.

Primary landscape layout:
- Left pane: problem list, filters, search, tags, solve status
- Center pane: problem statement, examples, constraints, and notes
- Right pane: Python editor, test case input, run controls, and output

This layout is intended to keep the full study workflow visible without constant screen switching.

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

### Library Screen
- Search bar
- Difficulty and tag filters
- Problem list with status indicators
- Quick access to recent and bookmarked problems

### Problem Detail Screen
- Problem statement
- Examples and constraints
- Tags and metadata
- Notes panel

### Workspace Screen
- Python editor
- Saved draft state
- Custom test case input
- Run button and output panel

### Settings and Import Screen
- Import local problem bundles
- Manage local data
- Future export/backup actions
- Runtime and editor preferences

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

1. Package and application ID renamed to `dev.kaixinguo.standaloneleetcode`; keep future modules aligned with this namespace.
2. Create screen mockups for the tablet layout.
3. Replace the template `Hello Android` content with a real app shell.
4. Define the local problem JSON schema.
5. Start the Room data model and repository layer.

## 16. MVP Milestones

## Milestone 1: Foundation
- Create Android project
- Set up Compose, Room, navigation, and DI
- Define domain models and repository interfaces

## Milestone 2: Problem Library
- Build problem entities and local database
- Add seeded data or JSON import
- Implement library browsing, search, and filters

## Milestone 3: Problem Detail and Notes
- Show statement, examples, and metadata
- Add note-taking and status tracking

## Milestone 4: Python Workspace
- Integrate embedded Python runtime
- Add editor, draft saving, and test runner
- Capture execution results and errors

## Milestone 5: Tablet UX Polish
- Multi-pane layout improvements
- State restoration
- Performance tuning
- Import/export groundwork

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
- Does the MVP need syntax highlighting immediately, or can the first editor be plain text with basic indentation support?
- Should the app support backup/export in the first release or only import?
- What should the final package name and application ID be?

## 19. Recommended First Build Order

1. Define the local problem schema.
2. Build the Room data model and repository layer.
3. Create the tablet library and detail UI.
4. Add drafts, notes, and progress persistence.
5. Integrate the Python runner.
6. Polish execution UX and error handling.
7. Evaluate whether local AI is worth a phase 2 prototype.

## 20. Final Recommendation

The correct v1 strategy is a native Android, offline-first tablet app with a strong local repository model and an embedded Python execution layer. The app should be useful without internet, without AI, and without cross-platform support. If that foundation is solid, iOS and local AI can be added later behind stable service boundaries rather than forcing premature compromise into the first architecture.
