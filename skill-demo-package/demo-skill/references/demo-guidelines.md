# Demo Skill Guidelines

This reference file exists to show how a skill can keep detailed guidance out of
`SKILL.md` until the agent actually needs it.

## What This Demo Shows

- `SKILL.md` contains trigger metadata and a short workflow.
- `scripts/greet.py` contains repeatable executable behavior.
- `references/demo-guidelines.md` contains optional explanatory context.
- `agents/openai.yaml` provides UI-facing metadata for the skill list.

## Adapting It

To turn this demo into a real skill, replace the greeting workflow with the
smallest reliable procedure for the target task. Add scripts only when repeated
or fragile work benefits from deterministic code. Add references only when the
agent needs domain-specific details that would make `SKILL.md` too large.
