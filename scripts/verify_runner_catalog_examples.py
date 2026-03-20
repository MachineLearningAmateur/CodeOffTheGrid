import importlib.util
import json
import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
RUNNER_PATH = ROOT / "app" / "src" / "main" / "python" / "standalone_runner.py"
CATALOG_PATH = ROOT / "app" / "src" / "main" / "assets" / "neetcode150_catalog.json"


def load_runner():
    spec = importlib.util.spec_from_file_location("standalone_runner", RUNNER_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def iter_problems():
    data = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    for folder in data["folders"]:
        for problem_set in folder["sets"]:
            for problem in problem_set["problems"]:
                yield problem


def build_stub_from_starter(starter_code: str) -> str:
    output_lines = []
    for line in starter_code.splitlines():
        output_lines.append(line)
        match = re.match(r"^(\s*)def\s+(\w+)\s*\(([^)]*)\)\s*(?:->[^:]+)?\s*:\s*$", line)
        if not match:
            continue

        indent, name, params = match.groups()
        body_indent = f"{indent}    "
        param_names = extract_param_names(params)

        if name == "__init__":
            output_lines.append(f"{body_indent}pass")
            continue

        first_value = next((param for param in param_names if param != "self"), None)
        if first_value is None:
            output_lines.append(f"{body_indent}return None")
        else:
            output_lines.append(f"{body_indent}return {first_value}")

    return "\n".join(output_lines) + "\n"


def extract_param_names(params: str) -> list[str]:
    names = []
    current = []
    depth = 0
    quote = ""

    for char in params:
        if quote:
            current.append(char)
            if char == quote:
                quote = ""
            continue
        if char in {"'", '"'}:
            quote = char
            current.append(char)
            continue
        if char in "([{" :
            depth += 1
            current.append(char)
            continue
        if char in ")]}":
            depth = max(0, depth - 1)
            current.append(char)
            continue
        if char == "," and depth == 0:
            names.append(extract_param_name("".join(current)))
            current = []
            continue
        current.append(char)

    if current:
        names.append(extract_param_name("".join(current)))
    return [name for name in names if name]


def extract_param_name(raw_param: str) -> str | None:
    param = raw_param.strip()
    if not param or param in {"/", "*"}:
        return None
    param = param.split(":", 1)[0].split("=", 1)[0].strip()
    return param.lstrip("*") or None


def main() -> int:
    runner = load_runner()
    failures = []

    for problem in iter_problems():
        if not problem.get("executionPipeline"):
            failures.append(
                {
                    "title": problem["title"],
                    "summary": "Missing explicit executionPipeline metadata.",
                    "stderr": "",
                    "case": {},
                }
            )
            continue
        code = build_stub_from_starter(problem["starterCode"])
        suite = problem.get("submissionTestSuite") or {
            "cases": [
                {
                    "label": "Bundled Example",
                    "stdin": problem["exampleInput"],
                    "expectedOutput": problem.get("exampleOutput", ""),
                    "enabled": True,
                }
            ]
        }
        suite["executionPipeline"] = problem.get("executionPipeline", "single_method")
        if not suite.get("cases"):
            failures.append(
                {
                    "title": problem["title"],
                    "summary": "Missing bundled submission test suite.",
                    "stderr": "",
                    "case": {},
                }
            )
            continue
        result = json.loads(runner.run_suite(code, json.dumps(suite)))
        failing_case = next(
            (case for case in result.get("cases", []) if case.get("status") == "error"),
            result["cases"][0] if result["cases"] else {},
        )
        if result["status"] == "error" or failing_case.get("status") == "error":
            failures.append(
                {
                    "title": problem["title"],
                    "summary": result.get("summary", ""),
                    "stderr": result.get("stderr", ""),
                    "case": failing_case,
                }
            )

    if failures:
        print(f"{len(failures)} problems failed catalog example compatibility:")
        for failure in failures:
            print("---", failure["title"])
            print(failure["summary"])
            print(failure["stderr"])
        return 1

    print("All 150 bundled problems executed their bundled submission suites without runner errors.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
