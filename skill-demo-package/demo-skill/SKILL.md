---
name: demo-skill
description: Create small greeting artifacts and explain the anatomy of a Codex skill. Use when the user asks for a demo skill, sample skill package, example SKILL.md layout, or a lightweight skill that demonstrates scripts and references.
---

# Demo Skill

## Overview

Use this skill to demonstrate a minimal, installable Codex skill. It shows how a skill can combine short operating instructions, an optional script, and a small reference file without adding unnecessary documentation.

## Quick Workflow

1. Clarify the tiny artifact the user wants, such as a greeting, note, or example output.
2. If deterministic output is useful, run `scripts/greet.py`.
3. If the user asks how the skill is structured, read `references/demo-guidelines.md`.
4. Keep the response brief and include the generated artifact or the relevant file path.

## Using The Script

Run the helper from the skill directory:

```bash
python scripts/greet.py --name "Codex"
```

Use `--style concise` for a shorter message or `--style friendly` for the default demo tone.

## Reference

Read `references/demo-guidelines.md` only when the user asks about the example structure or wants to adapt this skill into a new one.
