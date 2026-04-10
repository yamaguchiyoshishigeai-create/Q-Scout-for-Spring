# Controller To Repository Direct Access

## One-line Summary
Controller code is touching repository concerns directly, which can blur the boundary between HTTP handling and application use cases.

## What This Rule Looks For
This rule checks whether a controller reaches a repository directly instead of going through a service boundary.

## Why It Often Becomes a Problem
When persistence access lives in controllers, HTTP concerns, use-case decisions, and storage details start mixing together. That makes changes harder to reason about and encourages responsibility leakage over time.

## General Improvement Direction
Move the use-case entry point behind a service and keep controllers focused on request mapping, validation handoff, and response shaping.

## Conditionally Acceptable Cases
Simple read-only endpoints, tutorials, PoCs, or narrowly scoped reference APIs can sometimes accept direct repository access temporarily.

## Interpretation Notes
This finding should be read as a structural warning signal, not as an absolute statement that every instance is equally harmful.

## Why Q-Scout Cares
Q-Scout emphasizes this rule because controller-level persistence access often begins as a small shortcut and later grows into a larger architectural maintenance cost.
