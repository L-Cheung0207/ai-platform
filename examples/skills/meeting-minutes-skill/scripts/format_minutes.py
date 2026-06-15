#!/usr/bin/env python3
"""Print a compact markdown template for meeting minutes."""

TEMPLATE = """## 会议摘要

## 关键决策

## 行动项
| 事项 | 负责人 | 截止时间 | 状态 |
| --- | --- | --- | --- |

## 风险与阻塞

## 待确认问题
"""


def main() -> int:
    print(TEMPLATE)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
