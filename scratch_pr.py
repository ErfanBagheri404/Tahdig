#!/usr/bin/env python3
"""Open a PR for the current branch. Usage: open_pr.py <title> <body-file> <closes_issue>"""
import json, os, sys, urllib.request, ssl

REPO = "ErfanBagheri404/Tahdig"
TOKEN = open(os.path.expanduser("~/AppData/Local/Temp/tok.txt")).read().strip()
API = "https://api.github.com"
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE


def req(method, path, payload=None):
    data = json.dumps(payload).encode() if payload else None
    r = urllib.request.Request(f"{API}{path}", data=data, method=method)
    r.add_header("Authorization", f"token {TOKEN}")
    r.add_header("Accept", "application/vnd.github+json")
    r.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(r, context=CTX) as resp:
        return json.loads(resp.read().decode() or "{}")


def branch():
    return subprocess_check(["git", "branch", "--show-current"])


def subprocess_check(cmd):
    import subprocess
    return subprocess.run(cmd, capture_output=True, text=True).stdout.strip()


def main():
    title = sys.argv[1]
    body = open(sys.argv[2]).read().strip()
    close = sys.argv[3] if len(sys.argv) > 3 else None
    if close:
        body += f"\n\nCloses #{close}"
    head = branch()
    body += f"\n\nBranch: `{head}`"
    out = req("POST", f"/repos/{REPO}/pulls", {
        "title": title, "head": head, "base": "main", "body": body,
    })
    print(f"PR #{out['number']}: {out['html_url']}")


if __name__ == "__main__":
    main()
