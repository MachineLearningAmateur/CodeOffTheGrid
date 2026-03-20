# Session Handoff

## Project

- App name: `Standalone Code Practice`
- Repo: `git@github.com:MachineLearningAmateur/Standalone-Code-Practice.git`
- Branch: `main`
- Latest pushed feature commit before this handoff update: `3f305dd` (`Ship AI workspace and on-device runtime scaffolding`)
- Package / namespace: `dev.kaixinguo.standalonecodepractice`
- Platform: Android tablet-first, Kotlin + Jetpack Compose

## Current State

- The app is now well beyond the earlier sidebar prototype.
- The main UI is still the landscape 3-pane workspace:
  - left: folders / sets / problems / stats / settings / Ask AI entry
  - center: Python editor + sketch workspace
  - right: problem details, custom tests, results
- Ask AI exists as a real feature with modes:
  - `Hint`
  - `Explain`
  - `Review Code`
  - `Test Cases`
  - `Solve`
- Ask AI supports a compact pane and a fullscreen mode.
- AI markdown responses render in chat, and code fences can be tapped to copy.
- Review mode is intended to inspect the current draft only and not overwrite the editor.

## Major Work Landed

### Problem Data / Runner

- Bundled NeetCode catalog was expanded and normalized.
- Submission suites are deterministic and relabeled as `Example 1` through `Example 10`.
- Input normalization was added so bad keys like `dummy_input` do not leak into runner invocation.
- Local runner / catalog plumbing now supports per-problem execution pipeline resolution and JSON-backed submission suites.

### Workspace / UI

- Per-problem workspace state persists.
- Drag/drop ordering now survives app relaunches.
- Bundled NeetCode content is protected from delete / trash actions.
- Theme switching exists in Settings with light and night modes.
- Settings and Stats both live in the left pane.
- Results no longer auto-mark problems solved; the user explicitly chooses.
- Custom test enable/disable state has visible color treatment.
- Launcher icon and drag-handle visuals were refreshed.

### Ask AI

- Prompt-building is centralized in `app/src/main/java/dev/kaixinguo/standalonecodepractice/ai/`.
- Test-case generation can import directly into the Custom tab.
- Explain mode gets compacted problem + test context so it can reference actual failing cases without overflowing context.
- Solve mode asks for a brute-force baseline plus the optimal solution.
- Review mode is constrained away from giving away the whole answer, but this still depends on prompt quality rather than a true static analyzer.

### On-Device Runtime

- The app now includes a native `:llamaandroid` module for on-device llama.cpp inference.
- Settings currently expose fixed Hugging Face model presets instead of arbitrary import:
  - `Qwen2.5 Coder 1.5B Q4_K_M`
  - `Qwen2.5 Coder 3B Q4_K_M`
- The model UX is intentionally one-model-at-a-time:
  - download one preset
  - `Load` to put it in memory
  - `Unload From Memory` to free RAM but keep the file
  - `Remove Model` to delete the stored `.gguf`
- During download, the full preset chooser is hidden and replaced by progress for the active model.

## Verified

- `.\gradlew.bat testDebugUnitTest assembleDebug` succeeds with Android Studio JBR.
- Earlier in this session, `.\gradlew.bat installDebug` also succeeded and the app launched on the emulator.
- The `:llamaandroid` native module now builds successfully after installing the NDK / CMake toolchain.

## Current Blocker

- The current emulator still cannot resolve or reach external hosts.
- `adb` checks showed:
  - `ping google.com` -> `unknown host`
  - `ping huggingface.co` -> `unknown host`
  - `ping 8.8.8.8` -> packet loss
- Because of that, the fixed-model Hugging Face download flow is coded and installed, but not yet verified end to end on this AVD.
- The app now surfaces that failure as a clearer DNS/network error instead of a vague host-resolution exception.

## Likely Next Steps

1. Fix or recreate the emulator network so the device can reach `huggingface.co`.
2. Download one preset model end to end and verify an actual Ask AI turn on-device.
3. If review complexity still drifts on trivial drafts, add a lightweight static-analysis assist instead of relying only on prompt constraints.
4. Add Custom-tab validation for accidental `output = ...` / `expected = ...` pasted into stdin.

## Files To Know

- [MainActivity.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/MainActivity.kt)
- [LandscapeWorkspaceScreen.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ui/workspace/LandscapeWorkspaceScreen.kt)
- [SidebarPane.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ui/workspace/SidebarPane.kt)
- [ProblemPane.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ui/workspace/ProblemPane.kt)
- [WorkspaceComponents.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ui/workspace/WorkspaceComponents.kt)
- [OnDeviceLlamaCppQwenEngine.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ai/OnDeviceLlamaCppQwenEngine.kt)
- [PromptTemplates.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ai/PromptTemplates.kt)
- [ProblemPromptFormatter.kt](/D:/Code%20Projects/StandaloneCodePractice/app/src/main/java/dev/kaixinguo/standalonecodepractice/ai/ProblemPromptFormatter.kt)
- [llamaandroid/build.gradle.kts](/D:/Code%20Projects/StandaloneCodePractice/llamaandroid/build.gradle.kts)
- [ai_chat.cpp](/D:/Code%20Projects/StandaloneCodePractice/llamaandroid/src/main/cpp/ai_chat.cpp)
- [expand_submission_suites_to_target.py](/D:/Code%20Projects/StandaloneCodePractice/scripts/expand_submission_suites_to_target.py)

## Notes

- `app_svg_icons/` is still present locally as an untracked source asset pack and was intentionally not included in the commit.
- The stale emulator screenshots / XML dumps were left on disk but are now ignored by `.gitignore`, so they should stay out of future commits.
