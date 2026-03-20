import json
from pathlib import Path


CATALOG_PATH = Path(r"D:\Code Projects\StandaloneCodePractice\app\src\main\assets\neetcode150_catalog.json")


EDGE_CASE_OVERRIDES = {
    "Contains Duplicate": [
        {"label": "Edge 1", "stdin": "nums = []", "expectedOutput": "false"},
        {"label": "Edge 2", "stdin": "nums = [1]", "expectedOutput": "false"},
    ],
    "Valid Anagram": [
        {"label": "Edge 1", "stdin": 's = "", t = ""', "expectedOutput": "true"},
    ],
    "Two Sum": [
        {"label": "Edge 1", "stdin": "nums = [3, 3], target = 6", "expectedOutput": "[0,1]"},
    ],
    "Group Anagrams": [
        {"label": "Edge 1", "stdin": 'strs = [""]', "expectedOutput": '[[""]]'},
    ],
    "Top K Frequent Elements": [
        {"label": "Edge 1", "stdin": "nums = [1], k = 1", "expectedOutput": "[1]"},
    ],
    "Encode and Decode Strings": [
        {"label": "Edge 1", "stdin": 'strs = ["", "a#b", ""]', "expectedOutput": '["","a#b",""]'},
    ],
    "Product of Array Except Self": [
        {"label": "Edge 1", "stdin": "nums = [0, 1, 2, 3]", "expectedOutput": "[6,0,0,0]"},
    ],
    "Valid Sudoku": [
        {
            "label": "Edge 1",
            "stdin": 'board = [["5","3",".",".","7",".",".",".","."],["6","5",".","1","9","5",".",".","."],[".","9","8",".",".",".",".","6","."],["8",".",".",".","6",".",".",".","3"],["4",".",".","8",".","3",".",".","1"],["7",".",".",".","2",".",".",".","6"],[".","6",".",".",".",".","2","8","."],[".",".",".","4","1","9",".",".","5"],[".",".",".",".","8",".",".","7","9"]]',
            "expectedOutput": "false",
        },
    ],
    "Longest Consecutive Sequence": [
        {"label": "Edge 1", "stdin": "nums = [1, 2, 0, 1]", "expectedOutput": "3"},
    ],
    "Valid Palindrome": [
        {"label": "Edge 1", "stdin": 's = " "', "expectedOutput": "true"},
    ],
    "Two Sum II Input Array Is Sorted": [
        {"label": "Edge 1", "stdin": "numbers = [2, 3, 4], target = 6", "expectedOutput": "[1,3]"},
    ],
    "3Sum": [
        {"label": "Edge 1", "stdin": "nums = [0, 0, 0]", "expectedOutput": "[[0,0,0]]"},
    ],
    "Container With Most Water": [
        {"label": "Edge 1", "stdin": "height = [1, 1]", "expectedOutput": "1"},
    ],
    "Trapping Rain Water": [
        {"label": "Edge 1", "stdin": "height = [4, 2, 3]", "expectedOutput": "1"},
    ],
    "Best Time to Buy And Sell Stock": [
        {"label": "Edge 1", "stdin": "prices = [7, 6, 4, 3, 1]", "expectedOutput": "0"},
    ],
    "Longest Substring Without Repeating Characters": [
        {"label": "Edge 1", "stdin": 's = ""', "expectedOutput": "0"},
    ],
    "Longest Repeating Character Replacement": [
        {"label": "Edge 1", "stdin": 's = "AAAA", k = 0', "expectedOutput": "4"},
    ],
    "Permutation In String": [
        {"label": "Edge 1", "stdin": 's1 = "ab", s2 = "eidboaoo"', "expectedOutput": "false"},
    ],
    "Minimum Window Substring": [
        {"label": "Edge 1", "stdin": 's = "a", t = "aa"', "expectedOutput": '""'},
    ],
    "Sliding Window Maximum": [
        {"label": "Edge 1", "stdin": "nums = [1], k = 1", "expectedOutput": "[1]"},
    ],
    "Valid Parentheses": [
        {"label": "Edge 1", "stdin": 's = "("', "expectedOutput": "false"},
    ],
    "Min Stack": [
        {
            "label": "Edge 1",
            "stdin": '["MinStack","push",2,"push",0,"push",3,"push",0,"getMin","pop","getMin","pop","getMin","pop","getMin"]',
            "expectedOutput": "[null,null,null,null,null,0,null,0,null,0,null,2]",
        },
    ],
    "Evaluate Reverse Polish Notation": [
        {"label": "Edge 1", "stdin": 'tokens = ["2","1","+","3","*"]', "expectedOutput": "9"},
    ],
    "Daily Temperatures": [
        {"label": "Edge 1", "stdin": "temperatures = [90, 80, 70]", "expectedOutput": "[0,0,0]"},
    ],
    "Car Fleet": [
        {"label": "Edge 1", "stdin": "target = 10, position = [3], speed = [3]", "expectedOutput": "1"},
    ],
    "Largest Rectangle In Histogram": [
        {"label": "Edge 1", "stdin": "heights = [2, 4]", "expectedOutput": "4"},
    ],
    "Binary Search": [
        {"label": "Edge 1", "stdin": "nums = [1, 3, 5], target = 2", "expectedOutput": "-1"},
    ],
    "Search a 2D Matrix": [
        {"label": "Edge 1", "stdin": "matrix = [[1]], target = 0", "expectedOutput": "false"},
    ],
    "Koko Eating Bananas": [
        {"label": "Edge 1", "stdin": "piles = [1], h = 1", "expectedOutput": "1"},
    ],
    "Find Minimum In Rotated Sorted Array": [
        {"label": "Edge 1", "stdin": "nums = [11, 13, 15, 17]", "expectedOutput": "11"},
    ],
    "Search In Rotated Sorted Array": [
        {"label": "Edge 1", "stdin": "nums = [1], target = 0", "expectedOutput": "-1"},
    ],
    "Time Based Key Value Store": [
        {
            "label": "Edge 1",
            "stdin": '["TimeMap","set","foo","bar",1,"get","foo",1,"get","foo",3]',
            "expectedOutput": '[null,null,"bar","bar"]',
        },
    ],
    "Median of Two Sorted Arrays": [
        {"label": "Edge 1", "stdin": "nums1 = [], nums2 = [1]", "expectedOutput": "1.0"},
    ],
    "Reverse Linked List": [
        {"label": "Edge 1", "stdin": "head = []", "expectedOutput": "[]"},
    ],
    "Merge Two Sorted Lists": [
        {"label": "Edge 1", "stdin": "list1 = [], list2 = []", "expectedOutput": "[]"},
    ],
    "Linked List Cycle": [
        {"label": "Edge 1", "stdin": "head = [1], index = -1", "expectedOutput": "false"},
    ],
    "Reorder List": [
        {"label": "Edge 1", "stdin": "head = [1, 2]", "expectedOutput": "[1,2]"},
    ],
    "Remove Nth Node From End of List": [
        {"label": "Edge 1", "stdin": "head = [1], n = 1", "expectedOutput": "[]"},
    ],
    "Copy List With Random Pointer": [
        {"label": "Edge 1", "stdin": "head = []", "expectedOutput": "[]"},
    ],
    "Add Two Numbers": [
        {"label": "Edge 1", "stdin": "l1 = [0], l2 = [0]", "expectedOutput": "[0]"},
    ],
    "Find The Duplicate Number": [
        {"label": "Edge 1", "stdin": "nums = [1, 1]", "expectedOutput": "1"},
    ],
    "LRU Cache": [
        {
            "label": "Edge 1",
            "stdin": '["LRUCache",2,"put",1,1,"put",2,2,"get",1,"put",3,3,"get",2,"put",4,4,"get",1,"get",3,"get",4]',
            "expectedOutput": "[null,null,null,1,null,-1,null,-1,3,4]",
        },
    ],
    "Merge K Sorted Lists": [
        {"label": "Edge 1", "stdin": "lists = []", "expectedOutput": "[]"},
    ],
    "Reverse Nodes In K Group": [
        {"label": "Edge 1", "stdin": "head = [1, 2], k = 3", "expectedOutput": "[1,2]"},
    ],
    "Invert Binary Tree": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "[]"},
    ],
    "Maximum Depth of Binary Tree": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "0"},
    ],
    "Diameter of Binary Tree": [
        {"label": "Edge 1", "stdin": "root = [1]", "expectedOutput": "0"},
    ],
    "Balanced Binary Tree": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "true"},
    ],
    "Same Tree": [
        {"label": "Edge 1", "stdin": "p = [1], q = [1]", "expectedOutput": "true"},
    ],
    "Lowest Common Ancestor of a Binary Search Tree": [
        {"label": "Edge 1", "stdin": "root = [2, 1, 3], p = 1, q = 3", "expectedOutput": "2"},
    ],
    "Binary Tree Level Order Traversal": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "[]"},
    ],
    "Binary Tree Right Side View": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "[]"},
    ],
    "Count Good Nodes In Binary Tree": [
        {"label": "Edge 1", "stdin": "root = [3, 1, 4, 3, null, 1, 5]", "expectedOutput": "4"},
    ],
    "Validate Binary Search Tree": [
        {"label": "Edge 1", "stdin": "root = [2, 1, 3]", "expectedOutput": "true"},
    ],
    "Kth Smallest Element In a Bst": [
        {"label": "Edge 1", "stdin": "root = [1], k = 1", "expectedOutput": "1"},
    ],
    "Construct Binary Tree From Preorder And Inorder Traversal": [
        {"label": "Edge 1", "stdin": "preorder = [], inorder = []", "expectedOutput": "[]"},
    ],
    "Binary Tree Maximum Path Sum": [
        {"label": "Edge 1", "stdin": "root = [-3]", "expectedOutput": "-3"},
    ],
    "Serialize And Deserialize Binary Tree": [
        {"label": "Edge 1", "stdin": "root = []", "expectedOutput": "[]"},
    ],
    "Kth Largest Element In a Stream": [
        {
            "label": "Edge 1",
            "stdin": '["KthLargest",3,[4,5,8,2],"add",3,"add",5,"add",10,"add",9,"add",4]',
            "expectedOutput": "[null,4,5,5,8,8]",
        },
    ],
    "Last Stone Weight": [
        {"label": "Edge 1", "stdin": "stones = [2, 7, 4, 1, 8, 1]", "expectedOutput": "1"},
    ],
    "K Closest Points to Origin": [
        {"label": "Edge 1", "stdin": "points = [[1, 3], [-2, 2]], k = 1", "expectedOutput": "[[-2,2]]"},
    ],
    "Kth Largest Element In An Array": [
        {"label": "Edge 1", "stdin": "nums = [3, 2, 1, 5, 6, 4], k = 2", "expectedOutput": "5"},
    ],
    "Task Scheduler": [
        {"label": "Edge 1", "stdin": 'tasks = ["A","A","A","B","B","B"], n = 0', "expectedOutput": "6"},
    ],
    "Design Twitter": [
        {
            "label": "Edge 1",
            "stdin": '["Twitter","postTweet",1,5,"getNewsFeed",1,"follow",1,2,"postTweet",2,6,"getNewsFeed",1,"unfollow",1,2,"getNewsFeed",1]',
            "expectedOutput": "[null,null,[5],null,null,[6,5],null,[5]]",
        },
    ],
    "Find Median From Data Stream": [
        {
            "label": "Edge 1",
            "stdin": '["MedianFinder","addNum",1,"addNum",2,"findMedian","addNum",3,"findMedian"]',
            "expectedOutput": "[null,null,null,1.5,null,2.0]",
        },
    ],
    "Subsets": [
        {"label": "Edge 1", "stdin": "nums = []", "expectedOutput": "[[]]"},
    ],
    "Combination Sum": [
        {"label": "Edge 1", "stdin": "candidates = [2], target = 1", "expectedOutput": "[]"},
    ],
    "Combination Sum II": [
        {"label": "Edge 1", "stdin": "candidates = [1, 1], target = 1", "expectedOutput": "[[1]]"},
    ],
    "Permutations": [
        {"label": "Edge 1", "stdin": "nums = [1]", "expectedOutput": "[[1]]"},
    ],
    "Subsets II": [
        {"label": "Edge 1", "stdin": "nums = [0]", "expectedOutput": "[[],[0]]"},
    ],
    "Generate Parentheses": [
        {"label": "Edge 1", "stdin": "n = 1", "expectedOutput": '["()"]'},
    ],
    "Word Search": [
        {"label": "Edge 1", "stdin": 'board = [["A"]], word = "A"', "expectedOutput": "true"},
    ],
    "Palindrome Partitioning": [
        {"label": "Edge 1", "stdin": 's = "a"', "expectedOutput": '[["a"]]'},
    ],
    "Letter Combinations of a Phone Number": [
        {"label": "Edge 1", "stdin": 'digits = "2"', "expectedOutput": '["a","b","c"]'},
    ],
    "N Queens": [
        {"label": "Edge 1", "stdin": "n = 1", "expectedOutput": '[["Q"]]'},
    ],
    "Implement Trie Prefix Tree": [
        {
            "label": "Edge 1",
            "stdin": '["Trie","insert","apple","search","apple","search","app","startsWith","app","insert","app","search","app"]',
            "expectedOutput": "[null,null,true,false,true,null,true]",
        },
    ],
    "Design Add And Search Words Data Structure": [
        {
            "label": "Edge 1",
            "stdin": '["WordDictionary","addWord","bad","addWord","dad","addWord","mad","search","pad","search","bad","search",".ad","search","b.."]',
            "expectedOutput": "[null,null,null,null,false,true,true,true]",
        },
    ],
    "Word Search II": [
        {"label": "Edge 1", "stdin": 'board = [["a"]], words = ["a"]', "expectedOutput": '["a"]'},
    ],
    "Number of Islands": [
        {"label": "Edge 1", "stdin": 'grid = [["1"]]', "expectedOutput": "1"},
    ],
    "Max Area of Island": [
        {"label": "Edge 1", "stdin": "grid = [[1]]", "expectedOutput": "1"},
    ],
    "Clone Graph": [
        {"label": "Edge 1", "stdin": "node = [[]]", "expectedOutput": "[[]]"},
    ],
    "Walls And Gates": [
        {"label": "Edge 1", "stdin": "rooms = [[0]]", "expectedOutput": "[[0]]"},
    ],
    "Rotting Oranges": [
        {"label": "Edge 1", "stdin": "grid = [[0]]", "expectedOutput": "0"},
    ],
    "Pacific Atlantic Water Flow": [
        {"label": "Edge 1", "stdin": "heights = [[1]]", "expectedOutput": "[[0,0]]"},
    ],
    "Surrounded Regions": [
        {"label": "Edge 1", "stdin": 'board = [["X"]]', "expectedOutput": '[["X"]]'},
    ],
    "Course Schedule": [
        {"label": "Edge 1", "stdin": "numCourses = 2, prerequisites = [[1, 0]]", "expectedOutput": "true"},
    ],
    "Course Schedule II": [
        {"label": "Edge 1", "stdin": "numCourses = 1, prerequisites = []", "expectedOutput": "[0]"},
    ],
    "Graph Valid Tree": [
        {"label": "Edge 1", "stdin": "n = 1, edges = []", "expectedOutput": "true"},
    ],
    "Number of Connected Components In An Undirected Graph": [
        {"label": "Edge 1", "stdin": "n = 5, edges = []", "expectedOutput": "5"},
    ],
    "Redundant Connection": [
        {"label": "Edge 1", "stdin": "edges = [[1, 2], [1, 3], [2, 3]]", "expectedOutput": "[2,3]"},
    ],
    "Network Delay Time": [
        {"label": "Edge 1", "stdin": "times = [[1, 2, 1]], n = 2, k = 2", "expectedOutput": "-1"},
    ],
    "Reconstruct Itinerary": [
        {"label": "Edge 1", "stdin": 'tickets = [["JFK","SFO"]]', "expectedOutput": '["JFK","SFO"]'},
    ],
    "Min Cost to Connect All Points": [
        {"label": "Edge 1", "stdin": "points = [[0, 0]]", "expectedOutput": "0"},
    ],
    "Swim In Rising Water": [
        {"label": "Edge 1", "stdin": "grid = [[0]]", "expectedOutput": "0"},
    ],
    "Alien Dictionary": [
        {"label": "Edge 1", "stdin": 'words = ["z"]', "expectedOutput": '"z"'},
    ],
    "Cheapest Flights Within K Stops": [
        {
            "label": "Edge 1",
            "stdin": "n = 3, flights = [[0, 1, 100], [1, 2, 100], [0, 2, 500]], src = 0, dst = 2, k = 1",
            "expectedOutput": "200",
        },
    ],
    "Climbing Stairs": [
        {"label": "Edge 1", "stdin": "n = 1", "expectedOutput": "1"},
    ],
    "Min Cost Climbing Stairs": [
        {"label": "Edge 1", "stdin": "cost = [10, 15, 20]", "expectedOutput": "15"},
    ],
    "House Robber": [
        {"label": "Edge 1", "stdin": "nums = [2, 1, 1, 2]", "expectedOutput": "4"},
    ],
    "House Robber II": [
        {"label": "Edge 1", "stdin": "nums = [1]", "expectedOutput": "1"},
    ],
    "Longest Palindromic Substring": [
        {"label": "Edge 1", "stdin": 's = "cbbd"', "expectedOutput": '"bb"'},
    ],
    "Palindromic Substrings": [
        {"label": "Edge 1", "stdin": 's = "aaa"', "expectedOutput": "6"},
    ],
    "Decode Ways": [
        {"label": "Edge 1", "stdin": 's = "06"', "expectedOutput": "0"},
    ],
    "Coin Change": [
        {"label": "Edge 1", "stdin": "coins = [2], amount = 3", "expectedOutput": "-1"},
    ],
    "Maximum Product Subarray": [
        {"label": "Edge 1", "stdin": "nums = [-2, 0, -1]", "expectedOutput": "0"},
    ],
    "Word Break": [
        {
            "label": "Edge 1",
            "stdin": 's = "catsandog", wordDict = ["cats","dog","sand","and","cat"]',
            "expectedOutput": "false",
        },
    ],
    "Longest Increasing Subsequence": [
        {"label": "Edge 1", "stdin": "nums = [0, 1, 0, 3, 2, 3]", "expectedOutput": "4"},
    ],
    "Partition Equal Subset Sum": [
        {"label": "Edge 1", "stdin": "nums = [1, 2, 3, 5]", "expectedOutput": "false"},
    ],
    "Unique Paths": [
        {"label": "Edge 1", "stdin": "m = 1, n = 1", "expectedOutput": "1"},
    ],
    "Longest Common Subsequence": [
        {"label": "Edge 1", "stdin": 'text1 = "abc", text2 = "def"', "expectedOutput": "0"},
    ],
    "Best Time to Buy And Sell Stock With Cooldown": [
        {"label": "Edge 1", "stdin": "prices = [1]", "expectedOutput": "0"},
    ],
    "Coin Change II": [
        {"label": "Edge 1", "stdin": "amount = 3, coins = [2]", "expectedOutput": "0"},
    ],
    "Target Sum": [
        {"label": "Edge 1", "stdin": "nums = [1], target = 1", "expectedOutput": "1"},
    ],
    "Interleaving String": [
        {"label": "Edge 1", "stdin": 's1 = "", s2 = "", s3 = ""', "expectedOutput": "true"},
    ],
    "Longest Increasing Path In a Matrix": [
        {"label": "Edge 1", "stdin": "matrix = [[7]]", "expectedOutput": "1"},
    ],
    "Distinct Subsequences": [
        {"label": "Edge 1", "stdin": 's = "rabbbit", t = "rabbit"', "expectedOutput": "3"},
    ],
    "Edit Distance": [
        {"label": "Edge 1", "stdin": 'word1 = "", word2 = "abc"', "expectedOutput": "3"},
    ],
    "Burst Balloons": [
        {"label": "Edge 1", "stdin": "nums = [1]", "expectedOutput": "1"},
    ],
    "Regular Expression Matching": [
        {"label": "Edge 1", "stdin": 's = "aa", p = "a"', "expectedOutput": "false"},
    ],
    "Maximum Subarray": [
        {"label": "Edge 1", "stdin": "nums = [-1]", "expectedOutput": "-1"},
    ],
    "Jump Game": [
        {"label": "Edge 1", "stdin": "nums = [0]", "expectedOutput": "true"},
    ],
    "Jump Game II": [
        {"label": "Edge 1", "stdin": "nums = [0]", "expectedOutput": "0"},
    ],
    "Gas Station": [
        {"label": "Edge 1", "stdin": "gas = [2], cost = [2]", "expectedOutput": "0"},
    ],
    "Hand of Straights": [
        {"label": "Edge 1", "stdin": "hand = [1], groupSize = 1", "expectedOutput": "true"},
    ],
    "Merge Triplets to Form Target Triplet": [
        {
            "label": "Edge 1",
            "stdin": "triplets = [[2, 5, 3], [1, 8, 4], [1, 7, 5]], target = [2, 7, 5]",
            "expectedOutput": "true",
        },
    ],
    "Partition Labels": [
        {"label": "Edge 1", "stdin": 's = "eccbbbbdec"', "expectedOutput": "[10]"},
    ],
    "Valid Parenthesis String": [
        {"label": "Edge 1", "stdin": 's = "*"', "expectedOutput": "true"},
    ],
    "Insert Interval": [
        {"label": "Edge 1", "stdin": "intervals = [], newInterval = [5, 7]", "expectedOutput": "[[5,7]]"},
    ],
    "Merge Intervals": [
        {"label": "Edge 1", "stdin": "intervals = [[1, 4], [4, 5]]", "expectedOutput": "[[1,5]]"},
    ],
    "Non Overlapping Intervals": [
        {"label": "Edge 1", "stdin": "intervals = [[1, 2], [1, 2], [1, 2]]", "expectedOutput": "2"},
    ],
    "Meeting Rooms": [
        {"label": "Edge 1", "stdin": "intervals = [(4, 9)]", "expectedOutput": "true"},
    ],
    "Minimum Interval to Include Each Query": [
        {"label": "Edge 1", "stdin": "intervals = [[1, 4]], queries = [2, 3, 5]", "expectedOutput": "[4,4,-1]"},
    ],
    "Rotate Image": [
        {"label": "Edge 1", "stdin": "matrix = [[1]]", "expectedOutput": "[[1]]"},
    ],
    "Spiral Matrix": [
        {"label": "Edge 1", "stdin": "matrix = [[1], [2], [3]]", "expectedOutput": "[1,2,3]"},
    ],
    "Set Matrix Zeroes": [
        {"label": "Edge 1", "stdin": "matrix = [[1, 0, 3]]", "expectedOutput": "[[0,0,0]]"},
    ],
    "Happy Number": [
        {"label": "Edge 1", "stdin": "n = 2", "expectedOutput": "false"},
    ],
    "Plus One": [
        {"label": "Edge 1", "stdin": "digits = [9]", "expectedOutput": "[1,0]"},
    ],
    "Pow(x, n)": [
        {"label": "Edge 1", "stdin": "x = 2.0, n = -2", "expectedOutput": "0.25"},
    ],
    "Multiply Strings": [
        {"label": "Edge 1", "stdin": 'num1 = "0", num2 = "52"', "expectedOutput": '"0"'},
    ],
    "Detect Squares": [
        {
            "label": "Edge 1",
            "stdin": '["DetectSquares","add",[3,10],"add",[11,2],"add",[3,2],"count",[11,10],"count",[14,8],"add",[11,2],"count",[11,10]]',
            "expectedOutput": "[null,null,null,null,1,0,null,2]",
        },
    ],
    "Single Number": [
        {"label": "Edge 1", "stdin": "nums = [4, 1, 2, 1, 2]", "expectedOutput": "4"},
    ],
    "Number of 1 Bits": [
        {"label": "Edge 1", "stdin": "n = 00000000000000000000000000001011", "expectedOutput": "3"},
    ],
    "Counting Bits": [
        {"label": "Edge 1", "stdin": "n = 2", "expectedOutput": "[0,1,1]"},
    ],
    "Reverse Bits": [
        {"label": "Edge 1", "stdin": "n = 00000010100101000001111010011100", "expectedOutput": "964176192"},
    ],
    "Missing Number": [
        {"label": "Edge 1", "stdin": "nums = [0, 1]", "expectedOutput": "2"},
    ],
    "Sum of Two Integers": [
        {"label": "Edge 1", "stdin": "a = 1, b = 2", "expectedOutput": "3"},
    ],
    "Reverse Integer": [
        {"label": "Edge 1", "stdin": "x = 1534236469", "expectedOutput": "0"},
    ],
    "Subtree of Another Tree": [
        {"label": "Edge 1", "stdin": "root = [1], subRoot = [1]", "expectedOutput": "true"},
    ],
    "Word Ladder": [
        {
            "label": "Edge 1",
            "stdin": 'beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log","cog"]',
            "expectedOutput": "5",
        },
    ],
    "Meeting Rooms II": [
        {"label": "Edge 1", "stdin": "intervals = [(1, 5), (8, 9), (8, 9)]", "expectedOutput": "2"},
    ],
}

CASE_COMPARISON_MODES = {
    "Group Anagrams": "unordered_nested_collections",
    "Top K Frequent Elements": "unordered_top_level",
    "3Sum": "unordered_nested_collections",
    "Lowest Common Ancestor of a Binary Search Tree": "tree_node_value",
    "K Closest Points to Origin": "unordered_top_level",
    "Subsets": "unordered_nested_collections",
    "Combination Sum": "unordered_nested_collections",
    "Combination Sum II": "unordered_nested_collections",
    "Permutations": "unordered_top_level",
    "Subsets II": "unordered_nested_collections",
    "Palindrome Partitioning": "unordered_top_level",
    "Letter Combinations of a Phone Number": "unordered_top_level",
    "N Queens": "unordered_top_level",
    "Word Search II": "unordered_top_level",
    "Course Schedule II": "topological_order",
    "Pacific Atlantic Water Flow": "unordered_top_level",
    "Alien Dictionary": "alien_dictionary_order",
    "Longest Palindromic Substring": "one_of",
}

CASE_ACCEPTABLE_OUTPUTS = {
    ("Longest Palindromic Substring", "Example 1"): ["aba"],
}


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


def main() -> int:
    root = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    updated = 0
    total_extra_cases = 0

    for folder in root.get("folders", []):
        for problem_set in folder.get("sets", []):
            for problem in problem_set.get("problems", []):
                overrides = EDGE_CASE_OVERRIDES.get(problem["title"], [])
                if not overrides:
                    continue
                suite = problem.setdefault("submissionTestSuite", {"draft": "", "cases": []})
                comparison_mode = CASE_COMPARISON_MODES.get(problem["title"], "exact")
                existing_ids = {case.get("id") for case in suite.get("cases", [])}
                existing_keys = {normalize_case_key(case) for case in suite.get("cases", [])}
                for case in suite.get("cases", []):
                    case["comparisonMode"] = comparison_mode
                    acceptable_outputs = CASE_ACCEPTABLE_OUTPUTS.get(
                        (problem["title"], case.get("label", ""))
                    )
                    if acceptable_outputs:
                        case["acceptableOutputs"] = acceptable_outputs
                for index, override in enumerate(overrides, start=1):
                    case_id = f"edge-{index}"
                    candidate = {
                        "id": case_id,
                        "label": override["label"],
                        "stdin": override["stdin"],
                        "expectedOutput": override["expectedOutput"],
                        "enabled": True,
                        "comparisonMode": comparison_mode,
                        "acceptableOutputs": CASE_ACCEPTABLE_OUTPUTS.get(
                            (problem["title"], override["label"]),
                            [],
                        ),
                    }
                    if case_id in existing_ids or normalize_case_key(candidate) in existing_keys:
                        continue
                    suite["cases"].append(candidate)
                    existing_keys.add(normalize_case_key(candidate))
                    total_extra_cases += 1
                suite["cases"] = dedupe_suite_cases(suite.get("cases", []))
                updated += 1

    CATALOG_PATH.write_text(json.dumps(root, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Updated {updated} problems with {total_extra_cases} extra submit cases.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
