#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import sys
import re
import io

# Windows에서 UTF-8 출력 강제
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

try:
    data = json.load(sys.stdin)
    cmd = data.get('tool_input', {}).get('command', '')

    # main 브랜치로의 push만 차단 (브랜치 이름에 main이 포함된 경우는 허용)
    # 패턴: "git push origin main" 또는 "git push main"
    if re.search(r'git push\s+(-[^\s]+\s+)*origin\s+main(\s|$)|git push\s+main(\s|$)', cmd):
        print("!! main 브랜치에 직접 push 금지!", file=sys.stderr)
        print("-> feature 브랜치 생성 후 PR을 통해 머지하세요.", file=sys.stderr)
        sys.exit(2)

    sys.exit(0)
except Exception as e:
    # 에러 발생 시에도 허용 (fail-safe)
    sys.exit(0)
