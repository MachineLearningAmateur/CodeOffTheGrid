# Running a Qwen Model in an Android App (Offline, Download After Install)

## Goal

Build an Android app that can run a Qwen model locally for offline AI features, while keeping the initial app install size smaller by downloading the model after install.

This is a good fit for an offline coding or LeetCode-style tablet app where AI is optional and isolated from the rest of the architecture.

---

## Step 1: Choose a Small Qwen Model

Start with a smaller model that is realistic for Android devices.

Recommended starting options:

- `Qwen2.5-0.5B-Instruct`
- `Qwen2.5-1.5B-Instruct`

For a first prototype, a good default is:

- `Qwen2.5-1.5B-Instruct-GGUF`

Why this is a good starting point:

- small enough to be more realistic on Android
- good enough for hints and lightweight explanations
- much easier than jumping directly to a 7B model

Do not start with:

- 7B+ models
- full solution generation
- multiple model families
- giant context windows

Your first AI feature should be something simple like:

- Give me a hint
- Explain the approach
- Suggest test cases

---

## Step 2: Pick an Android-Friendly Local Runtime

You need a runtime that can load and run the model locally on device.

The easiest path for a first prototype is:

- **Qwen + GGUF + llama.cpp-style runtime**

Other options exist, but for a practical prototype this is the simplest direction.

Suggested runtime choices:

### Option A: llama.cpp-style runtime
Best for:

- quick prototyping
- GGUF support
- local on-device inference experiments

### Option B: MLC LLM
Best for:

- a more productized mobile path
- broader mobile deployment workflows

### Option C: ExecuTorch
Best for:

- heavier ML engineering
- deeper long-term customization

For your app, start with:

- **llama.cpp-style runtime**

---

## Step 3: Understand What You Actually Need to Acquire

To run Qwen locally, you generally need:

- model weights
- tokenizer/config support required by the runtime
- a deployment-friendly format such as GGUF
- a quantized model suitable for Android
- a clear license review for the exact model you plan to ship

In practice, for Android, this usually means:

- download a **quantized GGUF model file**
- load it through a compatible local runtime

---

## Step 4: Decide to Download the Model After Install

Do not bundle a large model directly into the APK/AAB for your main production path.

Instead, use this flow:

1. App installs without the model
2. App fetches model metadata
3. User selects a model or the app picks a default
4. App downloads the model
5. App verifies the file
6. App stores it locally
7. App loads it for offline use

Benefits of download-after-install:

- smaller app install size
- easier model updates
- easier rollback
- more flexibility later
- cleaner user choice for different model sizes

---

## Step 5: Decide Where the App Downloads the Model From

Use one primary download source that you control.

Recommended choices:

### Option A: Your own cloud storage or CDN
This is the best long-term option.

Examples:

- AWS S3
- Cloudflare R2
- Google Cloud Storage
- another object storage provider
- optionally a CDN in front

Why this is best:

- full version control
- easier rollback
- easier checksum verification
- more predictable delivery
- better for multiple model tiers later

### Option B: Firebase Storage
This is a good option for an indie app or prototype.

Why use it:

- easy Android integration
- straightforward file downloads
- practical for early development

### Option C: Google Play Asset Delivery
This is relevant if you want Play-native delivery of large assets.

Why use it:

- tightly integrated with Play Store distribution
- useful for large assets delivered through Play

### Recommendation
For your app, use:

- **Cloud storage or Firebase Storage**
- optionally with a CDN
- plus a small manifest JSON

Do not make a public Hugging Face URL your production download path. It is fine for development, but not the best user-facing production delivery setup.

---

## Step 6: Add a Manifest File for Model Metadata

Your app should not hardcode a giant model URL directly as the entire download strategy.

Instead, your app should fetch a small manifest JSON that describes the model.

Example manifest:

```json
{
  "model_id": "qwen2.5-1.5b-instruct-q4",
  "version": "1.0.0",
  "url": "https://cdn.yourapp.com/models/qwen2.5-1.5b-instruct-q4.gguf",
  "sha256": "abc123def456...",
  "size_bytes": 1234567890,
  "min_ram_gb": 8,
  "format": "gguf",
  "quantization": "Q4_K_M"
}