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


class _Unparsed:
    pass


_UNPARSED = _Unparsed()


@dataclass
class ParsedEntrypoint:
    kind: str
    name: str
    owner: str | None = None


def run_suite(source_code: str, suite_json: str) -> str:
    suite = json.loads(suite_json) if suite_json else {"cases": []}
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
                    "actualOutput": "",
                    "expectedOutput": case.get("expectedOutput", ""),
                    "errorOutput": "",
                    "durationMillis": None,
                }
            )
            continue

        result = _run_case(source_code, entrypoint, case, index)
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
    for node in tree.body:
        if isinstance(node, ast.ClassDef) and node.name == "Solution":
            for child in node.body:
                if isinstance(child, ast.FunctionDef) and not child.name.startswith("_"):
                    return ParsedEntrypoint(kind="solution_method", name=child.name, owner="Solution")
        if isinstance(node, ast.FunctionDef) and not node.name.startswith("_"):
            return ParsedEntrypoint(kind="function", name=node.name)
    return None


def _run_case(
    source_code: str, entrypoint: ParsedEntrypoint | None, case: dict[str, Any], index: int
) -> dict[str, Any]:
    label = case.get("label", f"Case {index}")
    case_id = case.get("id", f"case-{index}")
    expected_output = case.get("expectedOutput", "")
    namespace = _build_namespace()
    stdout_buffer = io.StringIO()

    started_at = time.perf_counter()
    try:
        with contextlib.redirect_stdout(stdout_buffer):
            exec(source_code, namespace, namespace)
            actual_value = _execute_case(namespace, entrypoint, case.get("stdin", ""))

        actual_output = _format_output(actual_value, stdout_buffer.getvalue())
        if _matches_expected(actual_value, actual_output, expected_output):
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
        "actualOutput": actual_output,
        "expectedOutput": expected_output,
        "errorOutput": error_output,
        "durationMillis": duration_ms,
    }


def _execute_case(namespace: dict[str, Any], entrypoint: ParsedEntrypoint | None, stdin_text: str) -> Any:
    payload = (stdin_text or "").strip()
    if payload.startswith("script:"):
        script = payload.partition(":")[2].lstrip("\n")
        exec(script, namespace, namespace)
        return namespace.get("result")

    if payload.startswith("expr:"):
        expression = payload.partition(":")[2].strip()
        return eval(expression, namespace, namespace)

    parsed_json = _try_json(payload)
    if parsed_json is not _UNPARSED and entrypoint is not None:
        callable_target = _resolve_callable(namespace, entrypoint)
        args, kwargs = _coerce_arguments(callable_target, parsed_json, entrypoint)
        result = callable_target(*args, **kwargs)
        if result is None:
            mutated = _resolve_mutated_output(args, kwargs)
            if mutated is not _UNPARSED:
                return mutated
        return result

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
                parameter.name: _convert_argument(parameter, metadata[parameter.name], entrypoint, metadata_index)
                for parameter in parameters
                if parameter.name in metadata
            }
            return [], converted_kwargs
        except TypeError:
            converted_values = []
            for parameter, value in zip(parameters, metadata.values()):
                converted_values.append(
                    _convert_argument(parameter, value, entrypoint, metadata_index)
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
) -> Any:
    annotation = parameter.annotation
    annotation_text = "" if annotation is inspect._empty else str(annotation)
    parameter_name = parameter.name.lower()

    if "ListNode" in annotation_text or entrypoint.name in LINKED_LIST_ENTRYPOINTS:
        if _is_sequence_of_sequences(value):
            return [_build_linked_list(item) for item in value]
        cycle_index = linked_list_index if linked_list_index is not _UNPARSED else None
        return _build_linked_list(value, cycle_index=cycle_index)

    if "TreeNode" in annotation_text or entrypoint.name in TREE_ENTRYPOINTS:
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


def _matches_expected(actual_value: Any, actual_output: str, expected_output: str) -> bool:
    expected = (expected_output or "").strip()
    if not expected:
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
        isinstance(value, list)
        and bool(value)
        and all(isinstance(item, list) for item in value)
    )


def _build_namespace() -> dict[str, Any]:
    return {
        "__builtins__": __builtins__,
        "ListNode": ListNode,
        "TreeNode": TreeNode,
        "Node": Node,
        "Interval": Interval,
        "bisect": bisect,
        "collections": collections,
        "functools": functools,
        "heapq": heapq,
        "itertools": itertools,
        "math": math,
        "random": random,
        "string": string,
    }


def _try_json(raw_text: str) -> Any:
    if not raw_text:
        return _UNPARSED
    try:
        return json.loads(raw_text)
    except Exception:
        return _UNPARSED
