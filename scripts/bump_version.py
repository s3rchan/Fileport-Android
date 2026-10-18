"""Fileport Android stable version manager.

python scripts/bump_version.py patch  # 3.0.1 -> 3.0.2
python scripts/bump_version.py minor  # 3.0.1 -> 3.1.0
python scripts/bump_version.py --check

Always increment Android versionCode as well as versionName for upgrade support.
"""
import re
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
GRADLE=ROOT/"app/build.gradle"
HTML=ROOT/"app/src/main/assets/index.html"
README=ROOT/"README.md"
PATTERN=r"versionCode\s+(\d+);\s*versionName\s+'(\d+\.\d+\.\d+)'"

def read():
    match=re.search(PATTERN,GRADLE.read_text(encoding="utf-8"))
    if not match:raise SystemExit("Expected versionCode and versionName together in Gradle")
    return int(match.group(1)),match.group(2)

def check():
    code,version=read()
    if code<1:raise SystemExit("versionCode must be positive")
    page=HTML.read_text(encoding="utf-8")
    about=f'<span>Version</span><strong>{version}</strong>'
    marker=f'**Source version:** v{version}'
    if about not in page or marker not in README.read_text(encoding="utf-8"):
        raise SystemExit(f"Version mismatch for Android v{version}: Gradle, About and README must agree")
    print(f"Fileport Android v{version} (code {code}): consistent")

def bump(kind):
    code,version=read()
    if not re.fullmatch(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)",version):
        raise SystemExit("Invalid versionName")
    first,middle,last=map(int,version.split("."))
    if kind=="patch":last+=1
    elif kind=="minor":middle+=1;last=0
    else:raise SystemExit("Use patch, minor or --check")
    following=f"{first}.{middle}.{last}"
    old=GRADLE.read_text(encoding="utf-8")
    updated,n=re.subn(PATTERN,f"versionCode {code+1}; versionName '{following}'",old,count=1)
    if n!=1:raise SystemExit("Gradle version match not unique")
    GRADLE.write_text(updated,encoding="utf-8")
    page=HTML.read_text(encoding="utf-8")
    page,n=re.subn(r'(<span>Version</span><strong>)[^<]+(</strong>)',
        lambda m:m.group(1)+following+m.group(2),page,count=1)
    if n!=1:raise SystemExit("Android About version missing")
    HTML.write_text(page,encoding="utf-8")
    text=README.read_text(encoding="utf-8")
    text,n=re.subn(r'(\*\*Source version:\*\* v)\d+\.\d+\.\d+',
        lambda m:m.group(1)+following,text,count=1)
    if n!=1:raise SystemExit("README version marker missing")
    README.write_text(text,encoding="utf-8")
    print(f"Next Android build: v{following} (versionCode {code+1}); update changelog before publishing")

if __name__=="__main__":
    args=sys.argv[1:]
    if args==["--check"]:check()
    elif len(args)==1 and args[0] in ("patch","minor"):bump(args[0])
    else:raise SystemExit(__doc__)
