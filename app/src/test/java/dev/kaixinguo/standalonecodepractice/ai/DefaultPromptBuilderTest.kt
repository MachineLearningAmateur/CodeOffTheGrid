package dev.kaixinguo.standalonecodepractice.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPromptBuilderTest {
    private val subject = DefaultPromptBuilder()

    @Test
    fun build_hint_prompt_with_code() {
        val prompt = subject.build(
            mode = PromptMode.HINT,
            problem = "Two Sum",
            code = "class Solution:\n    pass",
            userRequest = null
        )

        assertTrue(prompt.contains("Give small hints only"))
        assertTrue(prompt.contains("Problem:\nTwo Sum"))
        assertTrue(prompt.contains("class Solution:\n    pass"))
        assertTrue(prompt.contains("Do not reveal the full solution"))
        assertTrue(prompt.contains("Do not provide the full algorithm"))
        assertTrue(prompt.contains("Keep the response under 80 words"))
    }

    @Test
    fun build_hint_prompt_without_code() {
        val prompt = subject.build(
            mode = PromptMode.HINT,
            problem = "Valid Parentheses",
            code = "   ",
            userRequest = null
        )

        assertTrue(prompt.contains("Problem:\nValid Parentheses"))
        assertTrue(prompt.contains("User code:\nNo code provided."))
    }

    @Test
    fun build_explain_prompt_with_code() {
        val prompt = subject.build(
            mode = PromptMode.EXPLAIN,
            problem = "Binary Search",
            code = "left = 0",
            userRequest = null
        )

        assertTrue(prompt.contains("Explain the solution in plain beginner-friendly English"))
        assertTrue(prompt.contains("Use very simple language and short sentences"))
        assertTrue(prompt.contains("Use one tiny concrete example when it helps the explanation click"))
        assertTrue(prompt.contains("Keep the tone simple and approachable, not childish"))
        assertTrue(prompt.contains("If you provide code, it must be Python 3 only"))
        assertTrue(prompt.contains("Mention time and space complexity at the end"))
        assertTrue(prompt.contains("Use the provided custom tests, local submission tests, and recorded run results when they are available"))
        assertTrue(prompt.contains("If the current draft fails specific cases, call out the failing cases directly and explain why they fail"))
        assertTrue(prompt.contains("User request:\nExplain the solution in plain beginner-friendly English."))
        assertTrue(prompt.contains("left = 0"))
    }

    @Test
    fun build_reviewCode_prompt_with_code() {
        val prompt = subject.build(
            mode = PromptMode.REVIEW_CODE,
            problem = "Contains Duplicate",
            code = "return len(nums) != len(set(nums))",
            userRequest = null
        )

        assertTrue(prompt.contains("Review the user's current code"))
        assertTrue(prompt.contains("Return exactly one ```python fenced block and no prose outside it"))
        assertTrue(prompt.contains("Reproduce the current draft and add inline comments only"))
        assertTrue(prompt.contains("Do not change the logic, variable names, control flow, or return value"))
        assertTrue(prompt.contains("Ignore the known or ideal solution when discussing runtime or space"))
        assertTrue(prompt.contains("Do not give the full solution"))
        assertTrue(prompt.contains("Use the problem statement only to judge correctness, not to guess complexity"))
        assertTrue(prompt.contains("Literal draft facts:"))
        assertTrue(prompt.contains("- Visible loops: no"))
        assertTrue(prompt.contains("- Visible set or dict construction: yes"))
        assertTrue(prompt.contains("return len(nums) != len(set(nums))"))
        assertTrue(prompt.contains("If no complexity-driving operations are visible, keep the review at O(1) time and O(1) auxiliary space."))
    }

    @Test
    fun build_reviewCode_prompt_marksSimpleReturnAsConstantTime() {
        val prompt = subject.build(
            mode = PromptMode.REVIEW_CODE,
            problem = "Two Sum",
            code = "return member",
            userRequest = null
        )

        assertTrue(prompt.contains("If the draft is a simple placeholder like `return [2,3]`, `return member`, or `return nums[0]`, comment that the current draft is O(1) time and O(1) auxiliary space"))
        assertTrue(prompt.contains("- Visible loops: no"))
        assertTrue(prompt.contains("- Visible direct return lines:"))
        assertTrue(prompt.contains("return member"))
    }

    @Test
    fun build_solve_prompt_without_code() {
        val prompt = subject.build(
            mode = PromptMode.SOLVE,
            problem = "Merge Intervals",
            code = null,
            userRequest = null
        )

        assertTrue(prompt.contains("Start with a brief brute-force baseline solution"))
        assertTrue(prompt.contains("Explain why the brute-force version is too slow or too costly"))
        assertTrue(prompt.contains("Provide the final code for the optimal approach"))
        assertTrue(prompt.contains("All code must be Python 3 only"))
        assertTrue(prompt.contains("Use ```python code fences for both brute-force and final code"))
        assertTrue(prompt.contains("Brute Force"))
        assertTrue(prompt.contains("Optimal Approach"))
        assertTrue(prompt.contains("Final Code"))
        assertTrue(prompt.contains("Include time and space complexity"))
        assertTrue(prompt.contains("User code:\nNo code provided."))
    }

    @Test
    fun build_testCases_prompt_without_code() {
        val prompt = subject.build(
            mode = PromptMode.TEST_CASES,
            problem = "Contains Duplicate",
            code = null,
            userRequest = null
        )

        assertTrue(prompt.contains("Return JSON only"))
        assertTrue(prompt.contains("\"cases\""))
        assertTrue(prompt.contains("Contains Duplicate"))
        assertTrue(prompt.contains("No code provided."))
    }

    @Test
    fun build_hint_prompt_with_explicit_request() {
        val prompt = subject.build(
            mode = PromptMode.HINT,
            problem = "Two Sum",
            code = null,
            userRequest = "only give me a very tiny hint"
        )

        assertTrue(prompt.contains("User request:\nonly give me a very tiny hint"))
        assertTrue(!prompt.contains("User request:\nGive me a hint."))
    }
}
