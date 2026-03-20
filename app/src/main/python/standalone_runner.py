from __future__ import annotations

import ast
import bisect
import collections
import contextlib
import functools
import heapq
import inspect
import io
import itertools
import json
import math
import re
import random
import string
import time
import traceback
from collections import *  # noqa: F401,F403
from dataclasses import dataclass
from functools import *  # noqa: F401,F403
from heapq import *  # noqa: F401,F403
from itertools import *  # noqa: F401,F403
from math import *  # noqa: F401,F403
from typing import *  # noqa: F401,F403


class ListNode:
    def __init__(self, val: int = 0, next: "ListNode | None" = None):
        self.val = val
        self.next = next


class TreeNode:
    def __init__(
        self,
        val: int = 0,
        left: "TreeNode | None" = None,
        right: "TreeNode | None" = None,
    ):
        self.val = val
        self.left = left
        self.right = right


class Node:
    def __init__(
        self,
        val: int = 0,
        neighbors: "list[Node] | None" = None,
        left: "Node | None" = None,
        right: "Node | None" = None,
        next: "Node | None" = None,
        random: "Node | None" = None,
    ):
        self.val = val
        self.neighbors = neighbors if neighbors is not None else []
        self.left = left
        self.right = right
        self.next = next
        self.random = random


class Interval:
    def __init__(self, start: int = 0, end: int = 0):
        self.start = start
        self.end = end


LINKED_LIST_ENTRYPOINTS = {
    "reverseList",
    "mergeTwoLists",
    "hasCycle",
    "reorderList",
    "removeNthFromEnd",
    "addTwoNumbers",
    "mergeKLists",
    "reverseKGroup",
}

TREE_ENTRYPOINTS = {
    "invertTree",
    "maxDepth",
    "diameterOfBinaryTree",
    "isBalanced",
    "isSameTree",
    "isSubtree",
    "lowestCommonAncestor",
    "levelOrder",
    "rightSideView",
    "goodNodes",
    "isValidBST",
    "kthSmallest",
    "buildTree",
    "maxPathSum",
    "serialize",
    "deserialize",
}

INTERVAL_ENTRYPOINTS = {
    "canAttendMeetings",
    "minMeetingRooms",
}

GRAPH_ENTRYPOINTS = {
    "cloneGraph",
}

RANDOM_LIST_ENTRYPOINTS = {
    "copyRandomList",
}

HELPER_CLASS_NAMES = {
    "ListNode",
    "TreeNode",
    "Node",
    "Interval",
}


class _Unparsed:
    pass


_UNPARSED = _Unparsed()


@dataclass
class ParsedEntrypoint:
    kind: str
    name: str
    owner: str | None = None
    class_names: list[str] | None = None


def run_suite(source_code: str, suite_json: str) -> str:
    suite = json.loads(suite_json) if suite_json else {"cases": []}
    execution_pipeline = suite.get("executionPipeline", "single_method")
    case_results = []
    suite_stdout: list[str] = []
    suite_stderr: list[str] = []
    enabled_cases = [case for case in suite.get("cases", []) if case.get("enabled", True)]

    if not enabled_cases:
        return json.dumps(
            {
                "status": "error",
                "summary": "Add at least one enabled custom case before running local checks.",
                "cases": [],
                "stdout": "",
                "stderr": "",
            }
        )

    try:
        entrypoint = _parse_entrypoint(source_code)
    except SyntaxError as exc:
        return json.dumps(
            {
                "status": "error",
                "summary": f"Python syntax error on line {exc.lineno}: {exc.msg}",
                "cases": [],
                "stdout": "",
                "stderr": traceback.format_exc(),
            }
        )

    passed_count = 0
    error_count = 0
    for index, case in enumerate(suite.get("cases", []), start=1):
        if not case.get("enabled", True):
            case_results.append(
                {
                    "testCaseId": case.get("id", f"case-{index}"),
                    "label": case.get("label", f"Case {index}"),
                    "status": "skipped",
                    "input": case.get("stdin", ""),
                    "actualOutput": "",
                    "expectedOutput": case.get("expectedOutput", ""),
                    "errorOutput": "",
                    "durationMillis": None,
                }
            )
            continue

        result = _run_case(source_code, entrypoint, execution_pipeline, case, index)
        case_results.append(result)
        if result["status"] == "passed":
            passed_count += 1
        if result["status"] == "error":
            error_count += 1
        if result["actualOutput"]:
            suite_stdout.append(f"{result['label']}: {result['actualOutput']}")
        if result["errorOutput"]:
            suite_stderr.append(f"{result['label']}: {result['errorOutput']}")

    enabled_count = len(enabled_cases)
    if passed_count == enabled_count:
        status = "passed"
        summary = f"All {enabled_count} enabled custom cases passed."
    elif error_count > 0:
        status = "error"
        summary = f"{passed_count} of {enabled_count} enabled custom cases passed before execution errors."
    else:
        status = "failed"
        summary = f"{passed_count} of {enabled_count} enabled custom cases passed."

    return json.dumps(
        {
            "status": status,
            "summary": summary,
            "cases": case_results,
            "stdout": "\n".join(suite_stdout).strip(),
            "stderr": "\n".join(suite_stderr).strip(),
        }
    )


def _parse_entrypoint(source_code: str) -> ParsedEntrypoint | None:
    tree = ast.parse(source_code)
    class_names = [
        node.name
        for node in tree.body
        if isinstance(node, ast.ClassDef) and not node.name.startswith("_")
    ]
    for node in tree.body:
        if isinstance(node, ast.ClassDef) and node.name == "Solution":
            method_name = _select_solution_method_name(node)
            if method_name is not None:
                return ParsedEntrypoint(
                    kind="solution_method",
                    name=method_name,
                    owner="Solution",
                    class_names=class_names,
                )
        if isinstance(node, ast.FunctionDef) and not node.name.startswith("_"):
            return ParsedEntrypoint(kind="function", name=node.name, class_names=class_names)

    primary_class = next(
        (name for name in class_names if name not in HELPER_CLASS_NAMES),
        None,
    )
    if primary_class is not None:
        return ParsedEntrypoint(
            kind="class_only",
            name="",
            owner=primary_class,
            class_names=class_names,
        )

    return None


def _select_solution_method_name(class_node: ast.ClassDef) -> str | None:
    public_methods = [
        child
        for child in class_node.body
        if isinstance(child, ast.FunctionDef) and not child.name.startswith("_")
    ]
    if not public_methods:
        return None
    if len(public_methods) == 1:
        return public_methods[0].name

    public_names = {method.name for method in public_methods}
    referenced_public_names: set[str] = set()

    for method in public_methods:
        for child in ast.walk(method):
            if isinstance(child, ast.Call):
                if (
                    isinstance(child.func, ast.Attribute)
                    and isinstance(child.func.value, ast.Name)
                    and child.func.value.id == "self"
                    and child.func.attr in public_names
                    and child.func.attr != method.name
                ):
                    referenced_public_names.add(child.func.attr)
                elif (
                    isinstance(child.func, ast.Name)
                    and child.func.id in public_names
                    and child.func.id != method.name
                ):
                    referenced_public_names.add(child.func.id)

    candidates = [method for method in public_methods if method.name not in referenced_public_names]
    if candidates:
        return candidates[-1].name
    return public_methods[-1].name


def _run_case(
    source_code: str,
    entrypoint: ParsedEntrypoint | None,
    execution_pipeline: str,
    case: dict[str, Any],
    index: int,
) -> dict[str, Any]:
    label = case.get("label", f"Case {index}")
    case_id = case.get("id", f"case-{index}")
    expected_output = case.get("expectedOutput", "")
    comparison_mode = case.get("comparisonMode", "exact")
    acceptable_outputs = [
        output
        for output in case.get("acceptableOutputs", [])
        if isinstance(output, str)
    ]
    namespace = _build_namespace()
    stdout_buffer = io.StringIO()

    started_at = time.perf_counter()
    try:
        with contextlib.redirect_stdout(stdout_buffer):
            exec(source_code, namespace, namespace)
            actual_value = _execute_case(
                namespace, entrypoint, execution_pipeline, case.get("stdin", "")
            )

        actual_output = _format_output(actual_value, stdout_buffer.getvalue())
        if _matches_expected(
            actual_value,
            actual_output,
            expected_output,
            comparison_mode,
            acceptable_outputs,
            case.get("stdin", ""),
        ):
            status = "passed"
            error_output = ""
        else:
            status = "failed"
            error_output = ""
    except Exception:
        actual_output = stdout_buffer.getvalue().strip()
        status = "error"
        error_output = traceback.format_exc().strip()

    duration_ms = int((time.perf_counter() - started_at) * 1000)
    return {
        "testCaseId": case_id,
        "label": label,
        "status": status,
        "input": case.get("stdin", ""),
        "actualOutput": actual_output,
        "expectedOutput": expected_output,
        "errorOutput": error_output,
        "durationMillis": duration_ms,
    }


def _execute_case(
    namespace: dict[str, Any],
    entrypoint: ParsedEntrypoint | None,
    execution_pipeline: str,
    stdin_text: str,
) -> Any:
    payload = (stdin_text or "").strip()
    if payload.startswith("script:"):
        script = payload.partition(":")[2].lstrip("\n")
        exec(script, namespace, namespace)
        return namespace.get("result")

    if payload.startswith("expr:"):
        expression = payload.partition(":")[2].strip()
        return eval(expression, namespace, namespace)

    if entrypoint is not None:
        explicit_result = _execute_explicit_pipeline(
            namespace, entrypoint, execution_pipeline, payload
        )
        if explicit_result is not _UNPARSED:
            return explicit_result

        round_trip_result = _try_round_trip_case(namespace, entrypoint, payload)
        if round_trip_result is not _UNPARSED:
            return round_trip_result

        class_sequence_result = _try_class_sequence_case(namespace, entrypoint, payload)
        if class_sequence_result is not _UNPARSED:
            return class_sequence_result

    parsed_json = _try_json(payload)
    if (
        parsed_json is not _UNPARSED
        and entrypoint is not None
        and entrypoint.kind in {"solution_method", "function"}
    ):
        return _invoke_entrypoint(namespace, entrypoint, parsed_json)

    parsed_assignments = _try_named_assignments(payload)
    if parsed_assignments is not _UNPARSED:
        if entrypoint is not None and entrypoint.kind in {"solution_method", "function"}:
            return _invoke_entrypoint(namespace, entrypoint, parsed_assignments)
        namespace.update(parsed_assignments)
        return parsed_assignments

    if payload and entrypoint is None:
        return eval(payload, namespace, namespace)

    if payload:
        return eval(payload, namespace, namespace)

    if entrypoint is None:
        return None

    callable_target = _resolve_callable(namespace, entrypoint)
    result = callable_target()
    if result is None:
        return ""
    return result


def _execute_explicit_pipeline(
    namespace: dict[str, Any],
    entrypoint: ParsedEntrypoint,
    execution_pipeline: str,
    payload: str,
) -> Any:
    if execution_pipeline == "single_method":
        return _UNPARSED
    if execution_pipeline == "encode_decode_round_trip":
        return _try_round_trip_case(namespace, entrypoint, payload)
    if execution_pipeline == "serialize_deserialize_round_trip":
        return _try_round_trip_case(namespace, entrypoint, payload)
    if execution_pipeline == "operation_sequence":
        return _try_class_sequence_case(namespace, entrypoint, payload)
    return _UNPARSED


def _try_round_trip_case(
    namespace: dict[str, Any], entrypoint: ParsedEntrypoint, payload: str
) -> Any:
    parsed_assignments = _try_named_assignments(payload)
    if parsed_assignments is _UNPARSED or not isinstance(parsed_assignments, dict):
        return _UNPARSED

    if entrypoint.owner == "Solution":
        owner = namespace.get("Solution")
        if owner is not None and hasattr(owner, "encode") and hasattr(owner, "decode"):
            instance = owner()
            values = parsed_assignments.get("strs", parsed_assignments.get("dummy_input", _UNPARSED))
            if values is _UNPARSED:
                return _UNPARSED
            encoded = instance.encode(values)
            return instance.decode(encoded)

    if entrypoint.kind == "class_only" and entrypoint.owner is not None:
        owner = namespace.get(entrypoint.owner)
        if owner is not None and hasattr(owner, "serialize") and hasattr(owner, "deserialize"):
            instance = owner()
            if "root" in parsed_assignments:
                root = _build_tree(parsed_assignments["root"])
                serialized = instance.serialize(root)
                return instance.deserialize(serialized)
            if "data" in parsed_assignments:
                tree = instance.deserialize(parsed_assignments["data"])
                return instance.serialize(tree)

    return _UNPARSED


def _try_class_sequence_case(
    namespace: dict[str, Any], entrypoint: ParsedEntrypoint, payload: str
) -> Any:
    if entrypoint.kind != "class_only" or entrypoint.owner is None:
        return _UNPARSED

    owner = namespace.get(entrypoint.owner)
    if owner is None:
        return _UNPARSED

    sequence = _try_operation_sequence_payload(owner, payload)
    if sequence is _UNPARSED:
        return _UNPARSED

    operations, arguments = sequence
    if not operations:
        return _UNPARSED

    constructor_args = arguments[0] if arguments else []
    instance = owner(*constructor_args)
    outputs: list[Any] = [None]

    for op_name, op_args in zip(operations[1:], arguments[1:]):
        method = getattr(instance, op_name)
        outputs.append(method(*op_args))

    return outputs


def _invoke_entrypoint(
    namespace: dict[str, Any], entrypoint: ParsedEntrypoint, payload: Any
) -> Any:
    callable_target = _resolve_callable(namespace, entrypoint)
    args, kwargs = _coerce_arguments(callable_target, payload, entrypoint)
    result = callable_target(*args, **kwargs)
    if result is None:
        mutated = _resolve_mutated_output(args, kwargs)
        if mutated is not _UNPARSED:
            return mutated
    return result


def _resolve_callable(namespace: dict[str, Any], entrypoint: ParsedEntrypoint) -> Any:
    if entrypoint.kind == "solution_method":
        owner = namespace[entrypoint.owner]
        instance = owner()
        return getattr(instance, entrypoint.name)
    return namespace[entrypoint.name]


def _coerce_arguments(
    callable_target: Any, payload: Any, entrypoint: ParsedEntrypoint
) -> tuple[list[Any], dict[str, Any]]:
    signature = inspect.signature(callable_target)
    parameters = list(signature.parameters.values())
    if isinstance(payload, list):
        if len(parameters) == 1:
            return [_convert_argument(parameters[0], payload, entrypoint)], {}
        converted_args = [
            _convert_argument(parameter, value, entrypoint)
            for parameter, value in zip(parameters, payload)
        ]
        if len(payload) > len(converted_args):
            converted_args.extend(payload[len(converted_args) :])
        return converted_args, {}
    if isinstance(payload, dict):
        metadata = dict(payload)
        metadata_index = metadata.pop("index", _UNPARSED)
        try:
            signature.bind_partial(**metadata)
            converted_kwargs = {
                parameter.name: _convert_argument(
                    parameter,
                    metadata[parameter.name],
                    entrypoint,
                    metadata_index,
                    metadata,
                )
                for parameter in parameters
                if parameter.name in metadata
            }
            return [], converted_kwargs
        except TypeError:
            converted_values = []
            for parameter, value in zip(parameters, metadata.values()):
                converted_values.append(
                    _convert_argument(parameter, value, entrypoint, metadata_index, metadata)
                )
            if len(metadata) > len(converted_values):
                converted_values.extend(list(metadata.values())[len(converted_values) :])
            return converted_values, {}
    if parameters:
        return [_convert_argument(parameters[0], payload, entrypoint)], {}
    return [payload], {}


def _convert_argument(
    parameter: inspect.Parameter,
    value: Any,
    entrypoint: ParsedEntrypoint,
    linked_list_index: Any = _UNPARSED,
    context: dict[str, Any] | None = None,
) -> Any:
    annotation = parameter.annotation
    annotation_text = "" if annotation is inspect._empty else str(annotation)
    parameter_name = parameter.name.lower()

    if "ListNode" in annotation_text or (
        entrypoint.name in LINKED_LIST_ENTRYPOINTS
        and parameter_name in {"head", "list1", "list2", "l1", "l2", "lists"}
    ):
        if parameter_name == "lists" and _is_sequence_of_sequences(value):
            return [_build_linked_list(item) for item in value]
        cycle_index = linked_list_index if linked_list_index is not _UNPARSED else None
        return _build_linked_list(value, cycle_index=cycle_index)

    if "TreeNode" in annotation_text or (
        entrypoint.name in TREE_ENTRYPOINTS
        and parameter_name in {"root", "subroot", "p", "q"}
    ):
        if parameter_name in {"p", "q"} and context is not None and "root" in context:
            root = context.get("__resolved_root__", _UNPARSED)
            if root is _UNPARSED:
                root = _build_tree(context["root"])
                context["__resolved_root__"] = root
            return _find_tree_node(root, value)
        return _build_tree(value)

    if "Interval" in annotation_text or entrypoint.name in INTERVAL_ENTRYPOINTS:
        if _is_sequence_of_sequences(value):
            return [_build_interval(item) for item in value]
        return _build_interval(value)

    if "Node" in annotation_text or parameter_name in {"node", "head"}:
        if entrypoint.name in GRAPH_ENTRYPOINTS or parameter_name == "node":
            return _build_graph(value)
        if entrypoint.name in RANDOM_LIST_ENTRYPOINTS:
            return _build_random_list(value)

    return value


def _resolve_mutated_output(args: list[Any], kwargs: dict[str, Any]) -> Any:
    candidates = list(args)
    if kwargs:
        candidates.extend(kwargs.values())
    for value in candidates:
        if isinstance(value, (ListNode, TreeNode, Node, Interval)):
            return value
        if isinstance(value, list):
            return value
    return _UNPARSED


def _matches_expected(
    actual_value: Any,
    actual_output: str,
    expected_output: str,
    comparison_mode: str = "exact",
    acceptable_outputs: list[str] | None = None,
    stdin_text: str = "",
) -> bool:
    expected = (expected_output or "").strip()
    acceptable_outputs = acceptable_outputs or []

    if not expected:
        return True

    if comparison_mode == "one_of":
        candidates = [candidate for candidate in [expected, *acceptable_outputs] if candidate is not None]
        return any(
            _matches_expected(
                actual_value,
                actual_output,
                candidate,
                "exact",
                [],
                stdin_text,
            )
            for candidate in candidates
        )

    if comparison_mode == "tree_node_value":
        node_value = getattr(actual_value, "val", _sanitize_value(actual_value))
        parsed_expected = _try_json(expected)
        if parsed_expected is not _UNPARSED:
            return _sanitize_value(node_value) == _sanitize_value(parsed_expected)
        return str(node_value).strip() == expected

    if comparison_mode == "unordered_top_level":
        parsed_expected = _try_json(expected)
        if parsed_expected is _UNPARSED:
            return actual_output.strip() == expected
        return _normalize_unordered_top_level(_sanitize_value(actual_value)) == _normalize_unordered_top_level(
            _sanitize_value(parsed_expected)
        )

    if comparison_mode == "unordered_nested_collections":
        parsed_expected = _try_json(expected)
        if parsed_expected is _UNPARSED:
            return actual_output.strip() == expected
        return _normalize_unordered_nested(_sanitize_value(actual_value)) == _normalize_unordered_nested(
            _sanitize_value(parsed_expected)
        )

    if comparison_mode == "topological_order":
        return _matches_topological_order(actual_value, stdin_text, expected)

    if comparison_mode == "alien_dictionary_order":
        return _matches_alien_dictionary_order(actual_value, actual_output, stdin_text, expected)

    if isinstance(actual_value, str) and actual_output.strip() == expected:
        return True

    parsed_expected = _try_json(expected)
    if parsed_expected is not _UNPARSED:
        return _sanitize_value(actual_value) == _sanitize_value(parsed_expected)

    return actual_output.strip() == expected


def _format_output(actual_value: Any, captured_stdout: str) -> str:
    if actual_value is None:
        return captured_stdout.strip()
    if isinstance(actual_value, str):
        return actual_value
    return json.dumps(_sanitize_value(actual_value), ensure_ascii=False)


def _sanitize_value(value: Any) -> Any:
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value
    if isinstance(value, list):
        return [_sanitize_value(item) for item in value]
    if isinstance(value, tuple):
        return [_sanitize_value(item) for item in value]
    if isinstance(value, dict):
        return {str(key): _sanitize_value(item) for key, item in value.items()}
    if isinstance(value, set):
        return sorted(_sanitize_value(item) for item in value)
    if isinstance(value, ListNode):
        return _serialize_linked_list(value)
    if isinstance(value, TreeNode):
        return _serialize_tree(value)
    if isinstance(value, Interval):
        return [value.start, value.end]
    if isinstance(value, Node):
        if value.random is not None or value.next is not None:
            return _serialize_random_list(value)
        return _serialize_graph(value)
    if hasattr(value, "__dict__"):
        return {key: _sanitize_value(item) for key, item in vars(value).items()}
    return str(value)


def _normalize_unordered_top_level(value: Any) -> Any:
    if not isinstance(value, list):
        return value
    canonical_items = [_canonicalize_exact(item) for item in value]
    return sorted(canonical_items, key=_stable_json)


def _normalize_unordered_nested(value: Any) -> Any:
    if isinstance(value, list):
        canonical_items = [_normalize_unordered_nested(item) for item in value]
        return sorted(canonical_items, key=_stable_json)
    if isinstance(value, dict):
        return {key: _normalize_unordered_nested(item) for key, item in value.items()}
    return value


def _canonicalize_exact(value: Any) -> Any:
    if isinstance(value, list):
        return [_canonicalize_exact(item) for item in value]
    if isinstance(value, dict):
        return {key: _canonicalize_exact(item) for key, item in value.items()}
    return value


def _stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def _matches_topological_order(actual_value: Any, stdin_text: str, expected_output: str) -> bool:
    payload = _parse_validation_payload(stdin_text)
    if not isinstance(payload, dict):
        return False

    actual_order = _sanitize_value(actual_value)
    if not isinstance(actual_order, list):
        return False

    expected = expected_output.strip()
    if expected == "[]":
        return actual_order == []

    num_courses = payload.get("numCourses")
    prerequisites = payload.get("prerequisites", [])
    if not isinstance(num_courses, int):
        return False
    if len(actual_order) != num_courses or len(set(actual_order)) != num_courses:
        return False

    positions = {course: index for index, course in enumerate(actual_order)}
    if any(course not in positions for course in range(num_courses)):
        return False

    for course, prerequisite in prerequisites:
        if positions[prerequisite] > positions[course]:
            return False
    return True


def _matches_alien_dictionary_order(
    actual_value: Any,
    actual_output: str,
    stdin_text: str,
    expected_output: str,
) -> bool:
    payload = _parse_validation_payload(stdin_text)
    if not isinstance(payload, dict):
        return False

    words = payload.get("words", [])
    if not isinstance(words, list):
        return False

    actual = actual_value if isinstance(actual_value, str) else actual_output.strip()
    if not isinstance(actual, str):
        return False

    invalid_prefix = any(
        len(first) > len(second) and first.startswith(second)
        for first, second in zip(words, words[1:])
    )
    if invalid_prefix:
        return actual == expected_output.strip()

    characters = {char for word in words for char in word}
    if len(actual) != len(characters) or set(actual) != characters:
        return False

    positions = {char: index for index, char in enumerate(actual)}
    for first, second in zip(words, words[1:]):
        for left, right in zip(first, second):
            if left == right:
                continue
            if positions[left] > positions[right]:
                return False
            break
    return True


def _parse_validation_payload(stdin_text: str) -> Any:
    payload = (stdin_text or "").strip()
    parsed_json = _try_json(payload)
    if parsed_json is not _UNPARSED:
        return parsed_json
    parsed_assignments = _try_named_assignments(payload)
    if parsed_assignments is not _UNPARSED:
        return parsed_assignments
    return _UNPARSED


def _serialize_linked_list(node: ListNode | None) -> list[Any]:
    values = []
    current = node
    seen: set[int] = set()
    while current is not None and id(current) not in seen:
        seen.add(id(current))
        values.append(current.val)
        current = current.next
    return values


def _serialize_tree(root: TreeNode | None) -> list[Any]:
    if root is None:
        return []
    values: list[Any] = []
    queue: list[TreeNode | None] = [root]
    while queue:
        node = queue.pop(0)
        if node is None:
            values.append(None)
            continue
        values.append(node.val)
        queue.append(node.left)
        queue.append(node.right)
    while values and values[-1] is None:
        values.pop()
    return values


def _serialize_graph(node: Node | None) -> list[list[int]]:
    if node is None:
        return []
    seen: dict[Node, int] = {}
    ordered: list[Node] = []
    queue = [node]
    while queue:
        current = queue.pop(0)
        if current in seen:
            continue
        seen[current] = current.val
        ordered.append(current)
        for neighbor in current.neighbors:
            if neighbor not in seen:
                queue.append(neighbor)
    ordered.sort(key=lambda item: item.val)
    adjacency = []
    for current in ordered:
        adjacency.append(sorted(neighbor.val for neighbor in current.neighbors))
    return adjacency


def _serialize_random_list(head: Node | None) -> list[list[Any]]:
    if head is None:
        return []
    nodes: list[Node] = []
    index_by_node: dict[Node, int] = {}
    current = head
    seen: set[int] = set()
    while current is not None and id(current) not in seen:
        seen.add(id(current))
        index_by_node[current] = len(nodes)
        nodes.append(current)
        current = current.next
    serialized = []
    for node in nodes:
        random_index = index_by_node.get(node.random) if node.random is not None else None
        serialized.append([node.val, random_index])
    return serialized


def _build_linked_list(values: Any, cycle_index: Any = None) -> ListNode | None:
    if values in (None, []):
        return None
    dummy = ListNode(0)
    current = dummy
    nodes: list[ListNode] = []
    for value in values:
        node = ListNode(value)
        nodes.append(node)
        current.next = node
        current = node
    if cycle_index not in (None, _UNPARSED, -1) and nodes:
        current.next = nodes[int(cycle_index)]
    return dummy.next


def _build_tree(values: Any) -> TreeNode | None:
    if values in (None, []):
        return None
    nodes = [None if value is None else TreeNode(value) for value in values]
    children = nodes[::-1]
    root = children.pop()
    for node in nodes:
        if node is not None:
            if children:
                node.left = children.pop()
            if children:
                node.right = children.pop()
    return root


def _find_tree_node(root: TreeNode | None, target: Any) -> TreeNode | None:
    if root is None:
        return None
    queue = [root]
    while queue:
        node = queue.pop(0)
        if node is None:
            continue
        if node.val == target:
            return node
        queue.append(node.left)
        queue.append(node.right)
    return None


def _build_interval(value: Any) -> Interval:
    if isinstance(value, dict):
        return Interval(value.get("start", 0), value.get("end", 0))
    return Interval(value[0], value[1])


def _build_graph(adjacency_list: Any) -> Node | None:
    if adjacency_list in (None, []):
        return None
    nodes = [Node(index + 1) for index in range(len(adjacency_list))]
    for index, neighbors in enumerate(adjacency_list):
        nodes[index].neighbors = [nodes[neighbor - 1] for neighbor in neighbors]
    return nodes[0]


def _build_random_list(pairs: Any) -> Node | None:
    if pairs in (None, []):
        return None
    nodes = [Node(pair[0]) for pair in pairs]
    for index, node in enumerate(nodes):
        if index + 1 < len(nodes):
            node.next = nodes[index + 1]
    for node, pair in zip(nodes, pairs):
        random_index = pair[1] if len(pair) > 1 else None
        if random_index is not None:
            node.random = nodes[random_index]
    return nodes[0]


def _is_sequence_of_sequences(value: Any) -> bool:
    return (
        isinstance(value, (list, tuple))
        and bool(value)
        and all(isinstance(item, (list, tuple)) for item in value)
    )


def _build_namespace() -> dict[str, Any]:
    namespace = {"__builtins__": __builtins__}
    for name, value in globals().items():
        if name.startswith("_"):
            continue
        namespace[name] = value
    return namespace


def _try_json(raw_text: str) -> Any:
    if not raw_text:
        return _UNPARSED
    try:
        return json.loads(raw_text)
    except Exception:
        return _UNPARSED


def _try_named_assignments(raw_text: str) -> Any:
    if not raw_text or "=" not in raw_text:
        return _UNPARSED

    normalized = re.sub(r"=\s*\n\s*", "= ", raw_text.strip())
    assignments: dict[str, Any] = {}

    for segment in _split_top_level_assignments(normalized):
        if "=" not in segment:
            return _UNPARSED
        name, value_text = segment.split("=", 1)
        name = name.strip()
        value_text = value_text.strip()
        if not name or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name) or not value_text:
            return _UNPARSED
        parsed_value = _parse_assignment_value(value_text)
        if parsed_value is _UNPARSED:
            return _UNPARSED
        assignments[name] = parsed_value

    return assignments if assignments else _UNPARSED


def _split_top_level_assignments(raw_text: str) -> list[str]:
    parts: list[str] = []
    current: list[str] = []
    depth = 0
    quote_char = ""
    escape = False

    for char in raw_text:
        if quote_char:
            current.append(char)
            if escape:
                escape = False
            elif char == "\\":
                escape = True
            elif char == quote_char:
                quote_char = ""
            continue

        if char in {'"', "'"}:
            quote_char = char
            current.append(char)
            continue

        if char in "([{":
            depth += 1
            current.append(char)
            continue

        if char in ")]}":
            depth = max(0, depth - 1)
            current.append(char)
            continue

        if char == "," and depth == 0:
            parts.append("".join(current).strip())
            current.clear()
            continue

        if char == "\n" and depth == 0:
            segment = "".join(current).strip()
            if segment:
                parts.append(segment)
                current.clear()
            continue

        current.append(char)

    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def _parse_assignment_value(value_text: str) -> Any:
    normalized = value_text.strip()
    if re.fullmatch(r"[01]{8,}", normalized):
        return int(normalized, 2)
    try:
        return ast.literal_eval(normalized)
    except Exception:
        try:
            return eval(
                normalized,
                {
                    "__builtins__": {},
                    "null": None,
                    "true": True,
                    "false": False,
                    "True": True,
                    "False": False,
                    "None": None,
                },
                {},
            )
        except Exception:
            return _UNPARSED


def _try_operation_sequence_payload(owner: type, raw_text: str) -> Any:
    parsed_values = _try_json_values(raw_text)
    if parsed_values is _UNPARSED or not parsed_values:
        return _UNPARSED

    if len(parsed_values) == 1 and isinstance(parsed_values[0], list):
        return _build_operation_sequence_from_flat_list(owner, parsed_values[0])

    if (
        len(parsed_values) == 2
        and isinstance(parsed_values[0], list)
        and isinstance(parsed_values[1], list)
    ):
        operations = parsed_values[0]
        raw_arguments = parsed_values[1]
        if len(operations) != len(raw_arguments) or not all(isinstance(op, str) for op in operations):
            return _UNPARSED
        arguments = [_normalize_operation_args(args) for args in raw_arguments]
        return operations, arguments

    return _UNPARSED


def _try_json_values(raw_text: str) -> Any:
    decoder = json.JSONDecoder()
    values: list[Any] = []
    index = 0
    text = raw_text.strip()

    while index < len(text):
        while index < len(text) and text[index].isspace():
            index += 1
        if index >= len(text):
            break
        try:
            value, next_index = decoder.raw_decode(text, index)
        except Exception:
            return _UNPARSED
        values.append(value)
        index = next_index

    return values if values else _UNPARSED


def _build_operation_sequence_from_flat_list(owner: type, items: list[Any]) -> Any:
    if not items or not isinstance(items[0], str):
        return _UNPARSED

    operations = [items[0]]
    arguments: list[list[Any]] = []
    index = 1

    constructor_args, index = _consume_operation_args(items, index, _callable_arity(owner.__init__))
    if constructor_args is _UNPARSED:
        return _UNPARSED
    arguments.append(constructor_args)

    while index < len(items):
        op_name = items[index]
        if not isinstance(op_name, str):
            return _UNPARSED
        method = getattr(owner, op_name, None)
        if method is None:
            return _UNPARSED
        index += 1
        method_args, index = _consume_operation_args(items, index, _callable_arity(method))
        if method_args is _UNPARSED:
            return _UNPARSED
        operations.append(op_name)
        arguments.append(method_args)

    return operations, arguments


def _normalize_operation_args(value: Any) -> list[Any]:
    if value in (None, []):
        return []
    if isinstance(value, list):
        return value
    return [value]


def _callable_arity(callable_obj: Any) -> int:
    signature = inspect.signature(callable_obj)
    parameters = [
        parameter
        for parameter in signature.parameters.values()
        if parameter.kind in (
            inspect.Parameter.POSITIONAL_ONLY,
            inspect.Parameter.POSITIONAL_OR_KEYWORD,
        )
    ]
    if parameters and parameters[0].name == "self":
        parameters = parameters[1:]
    return len(parameters)


def _consume_operation_args(items: list[Any], index: int, arity: int) -> tuple[Any, int]:
    if arity == 0:
        return [], index
    if index >= len(items):
        return _UNPARSED, index

    candidate = items[index]
    if arity == 1:
        if isinstance(candidate, list) and len(candidate) == 1:
            return [candidate[0]], index + 1
        return [candidate], index + 1

    if isinstance(candidate, list) and len(candidate) == arity:
        return candidate, index + 1
    if index + arity <= len(items):
        return items[index : index + arity], index + arity
    return _UNPARSED, index
