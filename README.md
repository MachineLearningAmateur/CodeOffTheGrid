# Standalone Code Practice

Standalone Code Practice is an Android app for practicing interview-style coding problems in one place without depending on a cloud AI service.

It combines a local problem workspace, Python execution, a built-in problem catalog, and an on-device GGUF-backed AI assistant so users can read problems, write code, test ideas, and get help directly on the device.

## Purpose

This app is meant to feel like a portable coding-practice workspace for:

- solving curated coding problems on Android
- running local checks against custom and submission-style test suites
- getting hints, explanations, reviews, and full solutions from an on-device model
- drafting brand new problems with an AI-assisted problem composer
- importing and exporting problem definitions for reuse

## Core Features

- Built-in coding problem catalog with folders, sets, and per-problem workspace state
- Python editing and local execution
- Custom test cases plus local submission-style validation
- Ask AI modes for:
  - hints
  - explanations
  - code review
  - full solutions
  - custom test generation
  - problem drafting inside the composer
- Problem Composer for creating, editing, importing, and exporting problems
- On-device llama.cpp runtime for local AI inference
- GGUF model support, including:
  - recommended in-app preset download for the supported small model path
  - manual `.gguf` import for advanced users
  - switching between installed imported models inside the app

## AI Runtime

The app is designed around on-device inference rather than a hosted API.

Current intended model strategy:

- recommend a smaller Android-friendly GGUF preset in-app
- allow users to import other compatible GGUF files themselves
- keep larger or differently licensed models as manual-import paths instead of first-class bundled recommendations

The in-app recommended preset is the smaller Qwen Coder path that is practical on more devices.

Users can also import compatible `.gguf` files manually when they want a different model. Imported models are stored on-device and can be switched from Settings.

## Problem Composer

The Problem Composer is used to author new coding problems.

It supports:

- title, difficulty, summary, and statement drafting
- examples, hints, and starter code
- execution pipeline selection
- advanced submission suite JSON
- import/export of problem definitions
- AI-assisted draft generation that writes directly back into the composer fields

## Intended Workflow

1. Pick a problem from the catalog or open the composer.
2. Write or edit Python code in the workspace.
3. Run custom checks or local submission checks.
4. Use Ask AI for hints, explanations, review, or a full solution.
5. Import a different GGUF model if you want to compare local model behavior.

## Third-Party Models and Licensing

This repository contains app code, not third-party model weights.

Important boundaries:

- the repository license applies to the app source code
- imported or downloaded model files remain subject to their own licenses
- users are responsible for obtaining third-party GGUF models from legitimate sources and complying with their terms

To keep distribution simpler, this project should avoid committing third-party model weights into the repository.

## Notes for Contributors

- Do not commit large GGUF files into the repo.
- Treat model integration as generic runtime support, not ownership of the model weights.
- Be explicit about licensing when adding new model-related UX or documentation.
- Prefer user-import flows for models that are not safe to recommend or redistribute directly.
