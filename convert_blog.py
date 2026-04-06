#!/usr/bin/env python3
import argparse
import os
import re
import shutil
from pathlib import Path


DEFAULT_SRC = "/Users/stefan/sources/sogeo.services.ng"
DEFAULT_OUT = "/Users/stefan/tmp/sogeo.services.thoth"

IMAGE_RE = re.compile(r"image::([^\[]+)\[([^\]]*)\]")
INCLUDE_RE = re.compile(r"include::([^\[]+)\[([^\]]*)\]")


def transform_header(text: str) -> str:
    if text.lstrip().startswith("---"):
        return text

    lines = text.splitlines()
    new_lines = ["---"]
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith(":jbake-"):
            line = ":thoth-" + line[len(":jbake-") :]
        new_lines.append(line)
        if line.strip() == ":idprefix:":
            # Remove exactly one empty line after :idprefix: if present
            if i + 1 < len(lines) and lines[i + 1].strip() == "":
                i += 1
            new_lines.append("---")
        i += 1

    out = "\n".join(new_lines)
    if text.endswith("\n"):
        out += "\n"
    return out


def localize_images(text: str, post_dir: Path, assets_images: Path, dry_run: bool, counters, warnings):
    copied = set()

    def repl(match):
        path = match.group(1).strip()
        attrs = match.group(2)
        if "/images/" not in path:
            return match.group(0)

        sub = path.split("/images/", 1)[1]
        src = assets_images / sub
        dest = post_dir / Path(sub).name

        if not src.exists():
            warnings.append(f"Missing image source: {src}")
        else:
            if dest not in copied:
                if not dry_run:
                    shutil.copy2(src, dest)
                copied.add(dest)
                counters["images"] += 1

        return f"image::{dest.name}[{attrs}]"

    return IMAGE_RE.sub(repl, text)


def localize_includes(text: str, post_dir: Path, assets_data: Path, dry_run: bool, counters, warnings):
    copied = set()

    def repl(match):
        path = match.group(1).strip()
        attrs = match.group(2)
        if "/assets/data/" not in path:
            return match.group(0)

        sub = path.split("/assets/data/", 1)[1]
        src = assets_data / sub
        dest = post_dir / Path(sub).name

        if not src.exists():
            warnings.append(f"Missing include source: {src}")
        else:
            if dest not in copied:
                if not dry_run:
                    shutil.copy2(src, dest)
                copied.add(dest)
                counters["includes"] += 1

        return f"include::{dest.name}[{attrs}]"

    return INCLUDE_RE.sub(repl, text)


def write_thoth_properties(out_root: Path, dry_run: bool):
    content = "\n".join(
        [
            "site.title=Thoth Blog",
            "site.description=My notes and projects",
            "site.baseUrl=https://example.com",
            "site.language=en-gb",
            "site.dateFormat=yyyy-MM-dd",
            "dev.port=8080",
            "",
        ]
    )
    target = out_root / "thoth.properties"
    if dry_run:
        print(f"[dry-run] write {target}")
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Convert JBake blog to Thoth-style layout.")
    parser.add_argument("--src", default=DEFAULT_SRC, help="Source repo root")
    parser.add_argument("--out", default=DEFAULT_OUT, help="Output directory")
    parser.add_argument("--dry-run", action="store_true", help="Log actions without writing files")
    args = parser.parse_args()

    src_root = Path(args.src)
    out_root = Path(args.out)

    content_blog = src_root / "content" / "blog"
    assets_images = src_root / "assets" / "images"
    assets_data = src_root / "assets" / "data"

    if not content_blog.exists():
        raise SystemExit(f"Missing content/blog directory: {content_blog}")

    counters = {"posts": 0, "images": 0, "includes": 0}
    warnings = []

    for adoc in sorted(content_blog.rglob("*.adoc")):
        rel = adoc.relative_to(content_blog)
        out_path = out_root / rel
        post_dir = out_path.parent

        counters["posts"] += 1

        text = adoc.read_text(encoding="utf-8")
        text = transform_header(text)

        if not args.dry_run:
            post_dir.mkdir(parents=True, exist_ok=True)

        text = localize_images(text, post_dir, assets_images, args.dry_run, counters, warnings)
        text = localize_includes(text, post_dir, assets_data, args.dry_run, counters, warnings)

        if args.dry_run:
            print(f"[dry-run] write {out_path}")
        else:
            out_path.write_text(text, encoding="utf-8")

    write_thoth_properties(out_root, args.dry_run)

    print(f"Posts processed: {counters['posts']}")
    print(f"Images copied:  {counters['images']}")
    print(f"Includes copied:{counters['includes']}")
    if warnings:
        print("Warnings:")
        for w in warnings:
            print(f"- {w}")


if __name__ == "__main__":
    main()
