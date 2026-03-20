import copy
import hashlib
import html
import importlib.util
import json
import random
import re
import sys
import time
from functools import lru_cache
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "app" / "src" / "main" / "assets" / "neetcode150_catalog.json"
RUNNER_PATH = ROOT / "app" / "src" / "main" / "python" / "standalone_runner.py"
CACHE_DIR = ROOT / "scripts" / ".cache" / "neetcode_solution_code"
TARGET_CASES = 10
MAX_EXPECTED_OUTPUT_CHARS = 200_000


def load_runner():
    spec = importlib.util.spec_from_file_location("standalone_runner", RUNNER_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


RUNNER = load_runner()


SERIALIZE_BANKS = {
    "Encode and Decode Strings": [
        {"stdin": 'strs = []'},
        {"stdin": 'strs = [""]'},
        {"stdin": 'strs = ["a"]'},
        {"stdin": 'strs = ["", ""]'},
        {"stdin": 'strs = ["hello", "world"]'},
        {"stdin": 'strs = ["#", "##", "###"]'},
        {"stdin": 'strs = ["leet", "code", "practice"]'},
        {"stdin": 'strs = ["  spaced  ", "tabs\\tinside"]'},
        {"stdin": 'strs = ["mix", "", "values", "#hash"]'},
        {"stdin": 'strs = ["123", "45#6", "7890"]'},
    ],
    "Serialize And Deserialize Binary Tree": [
        {"stdin": "root = []"},
        {"stdin": "root = [1]"},
        {"stdin": "root = [1, 2, 3]"},
        {"stdin": "root = [1, null, 2, null, 3]"},
        {"stdin": "root = [4, 2, 6, 1, 3, 5, 7]"},
        {"stdin": "root = [5, 4, null, 3, null, 2]"},
        {"stdin": "root = [0, -1, 1]"},
        {"stdin": "root = [1, 2, null, 3, null, 4, null]"},
        {"stdin": "root = [2, 1, 3, null, null, null, 4]"},
        {"stdin": "root = [7, 3, 9, 1, 5, 8, 10, null, 2]"},
    ],
}


OPERATION_SEQUENCE_BANKS = {
    "Min Stack": [
        '["MinStack","push",1,"getMin","top","pop"]',
        '["MinStack","push",-2,"push",0,"push",-3,"getMin","pop","top","getMin"]',
        '["MinStack","push",2,"push",2,"getMin","pop","getMin"]',
        '["MinStack","push",5,"push",1,"push",3,"getMin","pop","getMin","pop","getMin"]',
        '["MinStack","push",7,"top","getMin","pop"]',
        '["MinStack","push",0,"push",-1,"push",-1,"getMin","pop","getMin"]',
        '["MinStack","push",9,"push",8,"push",7,"push",6,"getMin"]',
        '["MinStack","push",4,"push",5,"top","pop","top"]',
        '["MinStack","push",-5,"push",-2,"push",-7,"getMin","pop","getMin"]',
        '["MinStack","push",10,"push",10,"push",10,"getMin","pop","getMin"]',
    ],
    "Time Based Key Value Store": [
        '["TimeMap","set","foo","bar",1,"get","foo",1,"get","foo",3]',
        '["TimeMap","set","foo","bar",1,"set","foo","bar2",4,"get","foo",4,"get","foo",5]',
        '["TimeMap","set","love","high",10,"set","love","low",20,"get","love",5,"get","love",10,"get","love",15,"get","love",20,"get","love",25]',
        '["TimeMap","get","ghost",1]',
        '["TimeMap","set","a","x",1,"set","a","y",2,"set","a","z",3,"get","a",2]',
        '["TimeMap","set","key","value",100,"get","key",99,"get","key",100]',
        '["TimeMap","set","k","v1",1,"set","k","v2",1,"get","k",1]',
        '["TimeMap","set","x","a",1,"set","y","b",2,"get","x",2,"get","y",1]',
        '["TimeMap","set","foo","a",5,"get","foo",5,"get","foo",6]',
        '["TimeMap","set","same","x",1,"set","same","y",3,"get","same",2,"get","same",3]',
    ],
    "LRU Cache": [
        '["LRUCache",1,"put",1,1,"get",1,"put",2,2,"get",1,"get",2]',
        '["LRUCache",2,"put",1,1,"put",2,2,"get",1,"put",3,3,"get",2,"put",4,4,"get",1,"get",3,"get",4]',
        '["LRUCache",2,"put",2,1,"put",2,2,"get",2,"put",1,1,"put",4,1,"get",2]',
        '["LRUCache",2,"get",2,"put",2,6,"get",1,"put",1,5,"put",1,2,"get",1,"get",2]',
        '["LRUCache",3,"put",1,1,"put",2,2,"put",3,3,"get",2,"put",4,4,"get",1,"get",3,"get",4]',
        '["LRUCache",2,"put",1,10,"put",2,20,"put",1,15,"get",1,"get",2]',
        '["LRUCache",1,"put",2,1,"get",2,"put",3,2,"get",2,"get",3]',
        '["LRUCache",2,"put",1,1,"get",1,"put",2,2,"get",2,"put",3,3,"get",1]',
        '["LRUCache",2,"put",1,1,"put",2,2,"put",3,3,"get",2,"get",3]',
        '["LRUCache",2,"put",5,5,"put",6,6,"get",5,"put",7,7,"get",6,"get",7]',
    ],
    "Kth Largest Element In a Stream": [
        '["KthLargest",3,[4,5,8,2],"add",3,"add",5,"add",10,"add",9,"add",4]',
        '["KthLargest",1,[],"add",-3,"add",-2,"add",-4,"add",0,"add",4]',
        '["KthLargest",2,[0],"add",-1,"add",1,"add",-2,"add",-4,"add",3]',
        '["KthLargest",3,[5,5,5],"add",5,"add",6,"add",4]',
        '["KthLargest",2,[2,3],"add",1,"add",10,"add",4]',
        '["KthLargest",3,[10,9,8,7],"add",6,"add",5,"add",11]',
        '["KthLargest",1,[1],"add",2,"add",3,"add",0]',
        '["KthLargest",4,[4,1,7,2],"add",6,"add",5,"add",8]',
        '["KthLargest",2,[-1,-2],"add",-3,"add",0,"add",1]',
        '["KthLargest",3,[100,90,80],"add",70,"add",110,"add",95]',
    ],
    "Design Twitter": [
        '["Twitter","postTweet",1,5,"getNewsFeed",1]',
        '["Twitter","postTweet",1,5,"follow",1,2,"postTweet",2,6,"getNewsFeed",1]',
        '["Twitter","postTweet",1,5,"follow",1,2,"postTweet",2,6,"unfollow",1,2,"getNewsFeed",1]',
        '["Twitter","postTweet",1,5,"postTweet",1,3,"getNewsFeed",1]',
        '["Twitter","follow",1,2,"unfollow",1,2,"getNewsFeed",1]',
        '["Twitter","postTweet",1,5,"postTweet",2,6,"follow",1,2,"follow",2,1,"getNewsFeed",1,"getNewsFeed",2]',
        '["Twitter","postTweet",1,5,"postTweet",1,6,"postTweet",1,7,"getNewsFeed",1]',
        '["Twitter","postTweet",2,5,"follow",1,2,"getNewsFeed",1,"unfollow",1,2,"getNewsFeed",1]',
        '["Twitter","postTweet",1,5,"follow",1,1,"getNewsFeed",1]',
        '["Twitter","postTweet",1,1,"postTweet",2,2,"follow",3,1,"follow",3,2,"getNewsFeed",3]',
    ],
    "Find Median From Data Stream": [
        '["MedianFinder","addNum",1,"findMedian"]',
        '["MedianFinder","addNum",1,"addNum",2,"findMedian"]',
        '["MedianFinder","addNum",1,"addNum",2,"addNum",3,"findMedian"]',
        '["MedianFinder","addNum",5,"addNum",15,"addNum",1,"addNum",3,"findMedian"]',
        '["MedianFinder","addNum",-1,"addNum",-2,"findMedian"]',
        '["MedianFinder","addNum",2,"addNum",2,"addNum",2,"findMedian"]',
        '["MedianFinder","addNum",100,"addNum",50,"addNum",75,"findMedian"]',
        '["MedianFinder","addNum",0,"findMedian","addNum",1,"findMedian"]',
        '["MedianFinder","addNum",7,"addNum",8,"addNum",9,"addNum",10,"findMedian"]',
        '["MedianFinder","addNum",-5,"addNum",5,"findMedian"]',
    ],
    "Implement Trie Prefix Tree": [
        '["Trie","insert","apple","search","apple","search","app","startsWith","app","insert","app","search","app"]',
        '["Trie","insert","a","search","a","startsWith","a"]',
        '["Trie","insert","apple","insert","app","search","app"]',
        '["Trie","insert","hello","startsWith","he","startsWith","hi"]',
        '["Trie","insert","dog","search","do","startsWith","do"]',
        '["Trie","insert","cat","insert","car","startsWith","ca","search","car"]',
        '["Trie","insert","z","search","z","startsWith","z"]',
        '["Trie","insert","prefix","startsWith","pre","search","prefix"]',
        '["Trie","insert","same","insert","same","search","same"]',
        '["Trie","insert","tree","search","trie","startsWith","tre"]',
    ],
    "Design Add And Search Words Data Structure": [
        '["WordDictionary","addWord","bad","addWord","dad","addWord","mad","search","pad","search","bad","search",".ad","search","b.."]',
        '["WordDictionary","addWord","a","search",".","search","a"]',
        '["WordDictionary","addWord","at","addWord","and","addWord","an","search","a.","search",".n"]',
        '["WordDictionary","addWord","bat","search","b.t","search","ba."]',
        '["WordDictionary","addWord","hello","search","hello","search","h...."]',
        '["WordDictionary","addWord","abc","search","...","search",".."]',
        '["WordDictionary","addWord","same","addWord","came","search",".ame"]',
        '["WordDictionary","addWord","z","search","z","search","."]',
        '["WordDictionary","addWord","lead","addWord","load","search","l..d"]',
        '["WordDictionary","addWord","node","search","n.de","search","no.."]',
    ],
    "Detect Squares": [
        '["DetectSquares","add",[3,10],"add",[11,2],"add",[3,2],"count",[11,10]]',
        '["DetectSquares","add",[3,10],"add",[11,2],"add",[3,2],"count",[14,8]]',
        '["DetectSquares","add",[3,10],"add",[11,2],"add",[3,2],"add",[11,2],"count",[11,10]]',
        '["DetectSquares","add",[1,1],"add",[1,2],"add",[2,1],"count",[2,2]]',
        '["DetectSquares","add",[0,0],"count",[0,0]]',
        '["DetectSquares","add",[5,5],"add",[5,7],"add",[7,5],"count",[7,7]]',
        '["DetectSquares","add",[10,10],"add",[10,12],"add",[12,10],"add",[12,12],"count",[10,10]]',
        '["DetectSquares","add",[1,2],"add",[2,1],"add",[1,0],"count",[0,1]]',
        '["DetectSquares","add",[2,2],"add",[2,4],"add",[4,2],"count",[4,4]]',
        '["DetectSquares","add",[3,3],"add",[3,6],"add",[6,3],"add",[6,6],"count",[6,6]]',
    ],
}


OVERRIDE_INPUT_BANKS = {
    "Evaluate Reverse Polish Notation": [
        'tokens = ["2","1","+","3","*"]',
        'tokens = ["4","13","5","/","+"]',
        'tokens = ["10","6","9","3","+","-11","*","/","*","17","+","5","+"]',
        'tokens = ["3","4","+"]',
        'tokens = ["5","1","2","+","4","*","+","3","-"]',
        'tokens = ["7","2","/"]',
        'tokens = ["4","-2","/","2","-3","-","-"]',
        'tokens = ["18"]',
        'tokens = ["2","3","1","*","+","9","-"]',
        'tokens = ["8","2","5","*","+","1","3","2","*","+","/"]',
    ],
    "Koko Eating Bananas": [
        "piles = [1], h = 1",
        "piles = [3,6,7,11], h = 8",
        "piles = [30,11,23,4,20], h = 5",
        "piles = [30,11,23,4,20], h = 6",
        "piles = [312884470], h = 312884469",
        "piles = [9,9,9], h = 3",
        "piles = [9,9,9], h = 9",
        "piles = [2,2,2,2], h = 4",
        "piles = [2,8,16], h = 7",
        "piles = [19,15,10,17], h = 8",
    ],
    "Clone Graph": [
        "adjList = []",
        "adjList = [[2],[1]]",
        "adjList = [[2,4],[1,3],[2,4],[1,3]]",
        "adjList = [[2],[1,3],[2]]",
        "adjList = [[2,3,4],[1],[1],[1]]",
        "adjList = [[2],[1,3],[2,4],[3]]",
        "adjList = [[2,5],[1,3],[2,4],[3,5],[1,4]]",
        "adjList = [[2,3],[1,4],[1,4],[2,3]]",
        "adjList = [[2,3],[1,3],[1,2]]",
        "adjList = [[2],[1,3,4],[2],[2]]",
    ],
    "Generate Parentheses": [
        "n = 0",
        "n = 1",
        "n = 2",
        "n = 3",
        "n = 4",
        "n = 5",
        "n = 6",
        "n = 7",
        "n = 8",
        "n = 9",
    ],
    "N Queens": [
        "n = 0",
        "n = 1",
        "n = 2",
        "n = 3",
        "n = 4",
        "n = 5",
        "n = 6",
        "n = 7",
        "n = 8",
        "n = 9",
    ]
}


SUPPLEMENTAL_INPUT_BANKS = {
    "Generate Parentheses": [f"n = {value}" for value in range(0, 9)],
    "N Queens": [f"n = {value}" for value in range(1, 9)],
    "Climbing Stairs": [f"n = {value}" for value in range(0, 10)],
    "Counting Bits": [f"n = {value}" for value in range(0, 10)],
    "Happy Number": [f"n = {value}" for value in [1, 2, 3, 4, 7, 10, 19, 20, 23, 28]],
    "Palindrome Partitioning": [
        's = ""',
        's = "a"',
        's = "aa"',
        's = "ab"',
        's = "aba"',
        's = "abba"',
        's = "banana"',
        's = "racecar"',
        's = "abc"',
        's = "efe"',
    ],
    "Min Cost to Connect All Points": [
        "points = []",
        "points = [[0, 0]]",
        "points = [[0, 0], [1, 1]]",
        "points = [[0, 0], [2, 2], [3, 10], [5, 2], [7, 0]]",
        "points = [[3, 12], [-2, 5], [-4, 1]]",
        "points = [[0, 0], [1, 1], [1, 0], [-1, 1]]",
        "points = [[2, -3], [-17, -8], [13, 8], [-17, -15]]",
        "points = [[0, 0], [0, 2], [2, 2], [2, 0]]",
        "points = [[1, 1], [3, 4], [6, 1], [7, 7], [2, 8]]",
        "points = [[-5, -4], [-3, -2], [0, 0], [3, 2], [5, 4]]",
    ],
    "Copy List With Random Pointer": [
        "head = []",
        "head = [[1,null]]",
        "head = [[1,0]]",
        "head = [[7,null],[13,0]]",
        "head = [[1,1],[2,1]]",
        "head = [[3,null],[3,0],[3,1]]",
        "head = [[5,null],[1,0],[9,1],[2,2]]",
        "head = [[10,2],[20,null],[30,1]]",
        "head = [[4,3],[8,0],[15,1],[16,2]]",
        "head = [[2,null],[4,0],[6,null],[8,2],[10,1]]",
    ],
}


@lru_cache(maxsize=None)
def fetch_solution_code(solution_url: str) -> str | None:
    if not solution_url:
        return None

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9._-]+", "_", solution_url.rstrip("/").split("/")[-1])
    cache_path = CACHE_DIR / f"{slug}.py"
    if cache_path.exists():
        return cache_path.read_text(encoding="utf-8")

    last_error = None
    for attempt in range(3):
        try:
            with urlopen(solution_url, timeout=15) as response:
                page = response.read().decode("utf-8", errors="ignore")
            match = re.search(r"```python\\n(.*?)```", page, re.S)
            if not match:
                return None
            raw = match.group(1)
            decoded = html.unescape(raw.encode("utf-8").decode("unicode_escape")).strip() + "\n"
            cache_path.write_text(decoded, encoding="utf-8")
            return decoded
        except (OSError, TimeoutError, URLError) as exc:
            last_error = exc
            time.sleep(0.5 * (attempt + 1))

    if cache_path.exists():
        return cache_path.read_text(encoding="utf-8")
    if last_error is not None:
        raise last_error
    return None


def run_reference(problem: dict, code: str, stdin: str) -> str | None:
    suite = {
        "executionPipeline": problem["executionPipeline"],
        "cases": [
            {
                "id": "generated",
                "label": "Generated",
                "stdin": stdin,
                "expectedOutput": "",
                "enabled": True,
            }
        ],
    }
    result = json.loads(RUNNER.run_suite(code, json.dumps(suite)))
    case = result.get("cases", [{}])[0]
    if result.get("status") == "error" or case.get("status") == "error":
        return None
    return case.get("actualOutput")


def parse_input(stdin: str):
    payload = RUNNER._parse_validation_payload(stdin)
    return None if payload is RUNNER._UNPARSED else payload


def to_input_string(payload) -> str:
    if isinstance(payload, dict):
        return "\n".join(f"{key} = {python_literal(value)}" for key, value in payload.items())
    return python_literal(payload)


def python_literal(value):
    return repr(value)


def unique_preserving_order(items):
    seen = set()
    ordered = []
    for item in items:
        if item in seen:
            continue
        seen.add(item)
        ordered.append(item)
    return ordered


def shuffled_bank_inputs(title: str, bank_name: str, items: list[str]) -> list[str]:
    ordered = unique_preserving_order([item for item in items if item.strip()])
    shuffled = ordered.copy()
    seeded_random(title, bank_name).shuffle(shuffled)
    return shuffled


def normalize_case_key(case: dict) -> tuple:
    return (
        case.get("stdin", "").strip(),
        case.get("expectedOutput", "").strip(),
        case.get("comparisonMode", "exact"),
        tuple(case.get("acceptableOutputs", [])),
    )


def dedupe_suite_cases(cases: list[dict]) -> list[dict]:
    seen: set[tuple] = set()
    deduped: list[dict] = []
    for case in cases:
        key = normalize_case_key(case)
        if key in seen:
            continue
        seen.add(key)
        deduped.append(case)
    return deduped


def normalize_case_labels(cases: list[dict]) -> None:
    for index, case in enumerate(cases, start=1):
        case["id"] = f"example-{index}"
        case["label"] = f"Example {index}"


def seeded_random(*parts) -> random.Random:
    digest = hashlib.sha256("||".join(map(str, parts)).encode("utf-8")).hexdigest()
    return random.Random(int(digest[:16], 16))


def extract_starter_parameter_names(starter_code: str, preferred_method: str | None) -> list[str]:
    lines = starter_code.splitlines()
    if not lines:
        return []

    method_regex = re.compile(r"^\s*def\s+(\w+)\s*\(([^)]*)\)")
    methods: list[tuple[str, str]] = []
    in_solution_class = False

    for line in lines:
        trimmed = line.lstrip()
        if trimmed.startswith("class "):
            in_solution_class = trimmed.startswith("class Solution")
        match = method_regex.search(line)
        if match is None:
            continue
        if line.startswith("    ") or not line.startswith(" "):
            if in_solution_class or not line.startswith(" "):
                methods.append((match.group(1), match.group(2)))

    if preferred_method is not None:
        target = next((method for method in methods if method[0] == preferred_method), None)
    else:
        target = methods[0] if methods else None
    if target is None:
        return []

    parameter_names = []
    for parameter in target[1].split(","):
        normalized = parameter.strip()
        if not normalized or normalized == "self":
            continue
        normalized = normalized.split(":", 1)[0].split("=", 1)[0].strip()
        if normalized:
            parameter_names.append(normalized)
    return parameter_names


def extract_example_assignments(raw_input: str) -> list[tuple[str, str]]:
    matches = list(re.finditer(r"(?m)([A-Za-z_][A-Za-z0-9_]*)\s*=", raw_input))
    if not matches:
        return []

    assignments: list[tuple[str, str]] = []
    for index, match in enumerate(matches):
        name = match.group(1)
        value_start = match.end()
        value_end = matches[index + 1].start() if index + 1 < len(matches) else len(raw_input)
        raw_value = raw_input[value_start:value_end].strip().removesuffix(",").strip()
        if raw_value:
            assignments.append((name, raw_value))
    return assignments


def normalize_input_names(problem: dict, stdin: str) -> str:
    raw_input = (stdin or "").strip()
    if not raw_input:
        return raw_input

    execution_pipeline = problem.get("executionPipeline", "single_method")
    if execution_pipeline == "operation_sequence":
        return raw_input

    preferred_method = None
    if execution_pipeline == "encode_decode_round_trip":
        preferred_method = "encode"
    elif execution_pipeline == "serialize_deserialize_round_trip":
        preferred_method = "serialize"

    parameter_names = extract_starter_parameter_names(problem.get("starterCode", ""), preferred_method)
    if not parameter_names:
        return raw_input

    assignments = extract_example_assignments(raw_input)
    if not assignments:
        return raw_input
    if len(assignments) != len(parameter_names):
        return raw_input
    if [name for name, _ in assignments] == parameter_names[: len(assignments)]:
        return raw_input

    return "\n".join(
        f"{parameter_names[index]} = {value}"
        for index, (_, value) in enumerate(assignments)
    )


def normalize_suite_cases(problem: dict, cases: list[dict]) -> list[dict]:
    normalized_cases = []
    for case in cases:
        if len(case.get("expectedOutput", "")) > MAX_EXPECTED_OUTPUT_CHARS:
            continue
        updated = dict(case)
        updated["stdin"] = normalize_input_names(problem, updated.get("stdin", ""))
        normalized_cases.append(updated)
    normalized_cases = dedupe_suite_cases(normalized_cases)
    normalize_case_labels(normalized_cases)
    return normalized_cases


def source_example_count(problem: dict, cases: list[dict]) -> int:
    statement_markdown = problem.get("statementMarkdown", "")
    heading_count = len(re.findall(r"(?im)^\*\*Example(?:\s+\d+)?:\*\*\s*$", statement_markdown))
    if heading_count > 0:
        return min(heading_count, len(cases))
    if cases and (problem.get("exampleInput", "").strip() or problem.get("exampleOutput", "").strip()):
        return 1
    return 0


def reorder_suite_cases(problem: dict, cases: list[dict]) -> list[dict]:
    if len(cases) <= 1:
        normalize_case_labels(cases)
        return cases

    preserved_count = source_example_count(problem, cases)
    preserved_cases = cases[:preserved_count]
    reorderable_cases = cases[preserved_count:]
    if not reorderable_cases:
        normalize_case_labels(cases)
        return cases

    desired_inputs = generate_payload_candidates(problem)
    cases_by_input = {case.get("stdin", "").strip(): case for case in reorderable_cases}
    ordered_remainder = []
    seen_inputs = set()

    for stdin in desired_inputs:
        normalized_stdin = normalize_input_names(problem, stdin).strip()
        case = cases_by_input.get(normalized_stdin)
        if case is None or normalized_stdin in seen_inputs:
            continue
        ordered_remainder.append(case)
        seen_inputs.add(normalized_stdin)

    for case in reorderable_cases:
        normalized_stdin = case.get("stdin", "").strip()
        if normalized_stdin in seen_inputs:
            continue
        ordered_remainder.append(case)
        seen_inputs.add(normalized_stdin)

    reordered_cases = [*preserved_cases, *ordered_remainder]
    normalize_case_labels(reordered_cases)
    return reordered_cases


def clamp_nonnegative(values):
    return [max(0, value) for value in values]


def mutate_value(value, key: str, title: str):
    key = key.lower()
    candidates = []
    rng = seeded_random(title, key, json.dumps(value, sort_keys=True, default=list))
    nonnegative_key = key in {
        "k",
        "n",
        "m",
        "target",
        "amount",
        "capacity",
        "groupsize",
        "timestamp",
        "hours",
        "h",
    }

    if isinstance(value, bool):
        candidates.append(not value)
    elif isinstance(value, int) and not isinstance(value, bool):
        offset = max(2, min(97, abs(value) + rng.randint(2, 11)))
        base_numbers = [
            0,
            1,
            value + 1,
            value - 1,
            value + offset,
            value - offset,
            value * 2 if value else offset,
            -value if value else -offset,
            rng.randint(-offset * 2, offset * 2),
        ]
        if nonnegative_key:
            base_numbers = clamp_nonnegative(base_numbers)
        candidates.extend(base_numbers)
    elif isinstance(value, float):
        offset = rng.uniform(0.5, max(1.5, abs(value) + 3.5))
        candidates.extend(
            [
                0.0,
                1.0,
                value + 1.0,
                value / 2 if value else 0.5,
                value + offset,
                value - offset,
                round(rng.uniform(-offset, offset), 3),
            ]
        )
    elif isinstance(value, str):
        candidates.extend(
            [
                "",
                value[:1],
                value[::-1],
                value + value[:1] if value else "a",
                value[1:] if len(value) > 1 else value,
                f"{value}{rng.choice(['x', 'z', '9'])}" if value else rng.choice(["alpha", "omega", "z9"]),
                f"{rng.choice(['pre', 'mid', 'tail'])}{value}" if value else rng.choice(["debug", "trace"]),
            ]
        )
    elif isinstance(value, list):
        candidates.extend(mutate_list(values=value, key=key, title=title))
    elif isinstance(value, tuple):
        candidates.extend(
            tuple(item) if isinstance(item, list) else item
            for item in mutate_list(values=list(value), key=key, title=title)
        )

    return unique_structured(candidates)


def mutate_list(values: list, key: str, title: str):
    rng = seeded_random(title, key, json.dumps(values, sort_keys=True, default=list))
    if not values:
        if key in {"strs", "words", "tasks", "tokens"}:
            return [[rng.choice(["alpha", "omega"])], ["trace", "debug"]]
        if key in {"intervals", "points", "times"}:
            return [
                [[rng.randint(-9, 9), rng.randint(10, 25)]],
                [[0, 0]],
            ]
        return [[rng.randint(3, 27)], [rng.randint(31, 79)], [rng.randint(-9, 9), rng.randint(10, 25)]]

    candidates = [
        [],
        values[:1],
        values[-1:],
        values[::-1],
        values + values[:1],
        values[1:] if len(values) > 1 else values,
        values[:-1] if len(values) > 1 else values,
    ]

    if all(isinstance(item, int) for item in values):
        shift = rng.randint(2, 17)
        scaled = max(2, rng.randint(2, 4))
        ascending = values == sorted(values)
        descending = values == sorted(values, reverse=True)
        shifted_up = [item + shift for item in values]
        shifted_down = [item - shift for item in values]
        candidates.extend(
            [
                sorted(values),
                sorted(values, reverse=True),
                values + [0],
                values + [values[0]],
                [0] * len(values),
                shifted_up,
                shifted_down,
                [item * scaled for item in values],
            ]
        )
        if ascending and shifted_up:
            candidates.extend(
                [
                    [shifted_up[0] - shift] + shifted_up,
                    shifted_up + [shifted_up[-1] + shift],
                ]
            )
        if descending and shifted_up:
            candidates.extend(
                [
                    [shifted_up[0] + shift] + shifted_up,
                    shifted_up + [shifted_up[-1] - shift],
                ]
            )
    elif all(isinstance(item, str) for item in values):
        candidates.extend(
            [
                sorted(values),
                values + [values[0]],
                [item[::-1] for item in values],
                [f"{item}{rng.choice(['x', '7'])}" for item in values],
            ]
        )
    elif all(isinstance(item, (list, tuple)) for item in values):
        candidates.extend(
            [
                [list(item) for item in values[:1]],
                [list(item) for item in values[::-1]],
            ]
        )
        first = values[0]
        if isinstance(first, (list, tuple)):
            candidates.append([list(first)])
            if all(
                all(isinstance(inner, int) for inner in item)
                for item in values
                if isinstance(item, (list, tuple))
            ):
                nested_shift = rng.randint(1, 9)
                candidates.append([[0 for _ in first]])
                candidates.append([[inner + nested_shift for inner in item] for item in values])
    return candidates


def unique_structured(values):
    seen = set()
    ordered = []
    for value in values:
        key = json.dumps(value, sort_keys=True, default=list)
        if key in seen:
            continue
        seen.add(key)
        ordered.append(value)
    return ordered


def candidate_priority_key(stdin: str) -> tuple[float, str]:
    payload = parse_input(stdin)
    score = score_payload(payload)
    return (-score, stdin)


def score_payload(payload) -> float:
    if payload is None:
        return -50.0
    if isinstance(payload, bool):
        return 1.0
    if isinstance(payload, int) and not isinstance(payload, bool):
        if abs(payload) <= 3:
            return float(abs(payload))
        return 6.0 + min(abs(payload), 100) / 10.0
    if isinstance(payload, float):
        return 4.0 + min(abs(payload), 100.0) / 10.0
    if isinstance(payload, str):
        if not payload:
            return -8.0
        return 2.0 + min(len(payload), 20)
    if isinstance(payload, dict):
        score = 6.0 + len(payload) * 3.0
        for key, value in payload.items():
            score += score_named_value(key, value)
        return score
    if isinstance(payload, (list, tuple)):
        return score_sequence(payload)
    return 0.0


def score_named_value(key: str, value) -> float:
    key = key.lower()
    score = score_payload(value)
    if isinstance(value, (list, tuple)) and key in {
        "nums",
        "prices",
        "stones",
        "coins",
        "points",
        "intervals",
        "times",
        "matrix",
        "grid",
        "board",
        "tasks",
        "temperatures",
        "heights",
        "queries",
        "candidates",
        "worddict",
    }:
        score += 4.0
    if isinstance(value, int) and key in {"target", "amount", "k", "n", "m", "capacity"}:
        if abs(value) <= 3:
            score -= 4.0
        else:
            score += 4.0
    return score


def score_sequence(values) -> float:
    values = list(values)
    if not values:
        return -30.0
    score = len(values) * 4.0
    if len(values) == 1:
        score -= 10.0
    scalar_values = [item for item in values if isinstance(item, (int, float)) and not isinstance(item, bool)]
    if scalar_values:
        score += min(sum(abs(item) for item in scalar_values), 250.0) / 8.0
        score += len(set(scalar_values)) * 1.5
        if all(abs(item) <= 3 for item in scalar_values):
            score -= 15.0
        if any(abs(item) >= 7 for item in scalar_values):
            score += 12.0
        if any(item < 0 for item in scalar_values) and any(item > 0 for item in scalar_values):
            score += 8.0
    string_values = [item for item in values if isinstance(item, str)]
    if string_values:
        score += sum(min(len(item), 12) for item in string_values)
        if all(len(item) <= 1 for item in string_values):
            score -= 8.0
    if all(isinstance(item, (list, tuple)) for item in values):
        score += len(values) * 6.0
        score += sum(score_sequence(item) for item in values) / 4.0
    return score


def generate_named_candidates(problem: dict, payload: dict):
    candidates = []
    keys = list(payload.keys())
    for key in keys:
        for mutated_value in mutate_value(payload[key], key, problem["title"]):
            candidate = copy.deepcopy(payload)
            candidate[key] = mutated_value
            candidates.append(candidate)

    if len(keys) >= 2:
        first, second = keys[:2]
        first_mutations = mutate_value(payload[first], first, problem["title"])[:4]
        second_mutations = mutate_value(payload[second], second, problem["title"])[:4]
        for first_value in first_mutations:
            for second_value in second_mutations:
                candidate = copy.deepcopy(payload)
                candidate[first] = first_value
                candidate[second] = second_value
                candidates.append(candidate)

    return candidates


def generate_payload_candidates(problem: dict):
    pipeline = problem["executionPipeline"]
    title = problem["title"]
    suite = problem["submissionTestSuite"]["cases"]
    existing_inputs = [case["stdin"] for case in suite]

    if title in OVERRIDE_INPUT_BANKS:
        return shuffled_bank_inputs(title, "override", OVERRIDE_INPUT_BANKS[title])
    if pipeline == "operation_sequence":
        return shuffled_bank_inputs(title, "operation-sequence", OPERATION_SEQUENCE_BANKS.get(title, []))
    if title in SERIALIZE_BANKS:
        return shuffled_bank_inputs(
            title,
            "serialize",
            [item["stdin"] for item in SERIALIZE_BANKS[title]]
        )

    generated_inputs = []
    for stdin in existing_inputs:
        payload = parse_input(stdin)
        if payload is None:
            continue
        if isinstance(payload, dict):
            for candidate in generate_named_candidates(problem, payload):
                generated_inputs.append(to_input_string(candidate))
        elif isinstance(payload, list):
            for candidate in mutate_list(payload, "payload", problem["title"]):
                generated_inputs.append(to_input_string(candidate))
        elif isinstance(payload, (int, float, str, bool)):
            for candidate in mutate_value(payload, "value", problem["title"]):
                generated_inputs.append(to_input_string(candidate))

    prioritized_generated_inputs = sorted(
        unique_preserving_order(generated_inputs),
        key=candidate_priority_key,
    )
    combined_inputs = [
        *shuffled_bank_inputs(title, "supplemental", SUPPLEMENTAL_INPUT_BANKS.get(title, [])),
        *prioritized_generated_inputs
    ]
    return unique_preserving_order([item for item in combined_inputs if item.strip()])


def fill_problem(problem: dict) -> int:
    problem["exampleInput"] = normalize_input_names(problem, problem.get("exampleInput", ""))
    suite = problem.setdefault("submissionTestSuite", {"draft": "", "cases": []})
    original_cases = suite.setdefault("cases", [])
    cases = normalize_suite_cases(problem, original_cases)
    cases = reorder_suite_cases(problem, cases)
    suite["cases"] = cases
    if len(cases) >= TARGET_CASES:
        return 0

    code = fetch_solution_code(problem.get("solutionUrl", ""))
    if code is None:
        return 0

    comparison_mode = "exact"
    acceptable_outputs = []
    if cases:
        comparison_mode = cases[0].get("comparisonMode", "exact")
        acceptable_outputs = cases[0].get("acceptableOutputs", [])

    existing_inputs = {case.get("stdin", "").strip() for case in cases}
    added = 0

    for stdin in generate_payload_candidates(problem):
        if len(cases) >= TARGET_CASES:
            break
        normalized_stdin = normalize_input_names(problem, stdin)
        if normalized_stdin.strip() in existing_inputs:
            continue
        expected_output = run_reference(problem, code, normalized_stdin)
        if expected_output is None:
            continue
        if len(expected_output) > MAX_EXPECTED_OUTPUT_CHARS:
            continue
        cases.append(
            {
                "id": "",
                "label": "",
                "stdin": normalized_stdin,
                "expectedOutput": expected_output,
                "enabled": True,
                "comparisonMode": comparison_mode,
                "acceptableOutputs": acceptable_outputs,
            }
        )
        existing_inputs.add(normalized_stdin.strip())
        added += 1

    suite["cases"] = reorder_suite_cases(problem, normalize_suite_cases(problem, cases))
    return added


def main() -> int:
    root = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    added_total = 0
    shortfalls = []

    for folder in root.get("folders", []):
        for problem_set in folder.get("sets", []):
            for problem in problem_set.get("problems", []):
                added_total += fill_problem(problem)
                case_count = len(problem.get("submissionTestSuite", {}).get("cases", []))
                if case_count < TARGET_CASES:
                    shortfalls.append((problem["title"], case_count))

    CATALOG_PATH.write_text(json.dumps(root, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Added {added_total} generated cases.")
    print(f"Problems below target: {len(shortfalls)}")
    for title, count in shortfalls[:50]:
        print(f"{title}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
