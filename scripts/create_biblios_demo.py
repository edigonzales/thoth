#!/usr/bin/env python3
"""
Create a local thoth-biblios demo setup with two local Git repositories.

The script creates:
- two repositories (docs-a, docs-b) with branches main and v1.x
- AsciiDoc content and nav.yml for each branch
- a biblios.yml that points to these repositories via file:// URLs
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import textwrap
from pathlib import Path


DEFAULT_BASE_DIR = Path("/Users/stefan/sources/thoth/.demo/biblios")


def run_git(repo_dir: Path, *args: str) -> str:
    cmd = ["git", *args]
    result = subprocess.run(
        cmd,
        cwd=repo_dir,
        check=True,
        text=True,
        capture_output=True,
    )
    return result.stdout.strip()


def init_git_repo(repo_dir: Path) -> None:
    repo_dir.mkdir(parents=True, exist_ok=True)
    try:
        run_git(repo_dir, "init", "--initial-branch", "main")
    except subprocess.CalledProcessError:
        run_git(repo_dir, "init")
        run_git(repo_dir, "branch", "-M", "main")

    run_git(repo_dir, "config", "user.name", "Biblios Demo")
    run_git(repo_dir, "config", "user.email", "biblios-demo@example.org")


def write_docs(repo_dir: Path, display_name: str, branch_name: str, token: str) -> None:
    docs_dir = repo_dir / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)

    nav_content = textwrap.dedent(
        """
        items:
          - title: Welcome
            page: index.adoc
          - title: User Guide
            page: guide.adoc
          - title: Search Notes
            page: search.adoc
        """
    ).strip() + "\n"

    index_content = textwrap.dedent(
        f"""
        = {display_name} ({branch_name})
        :doctype: book

        This page belongs to branch {branch_name}.

        Search token: {token}

        == Introduction

        This demo repository is used for a local biblios setup.
        """
    ).strip() + "\n"

    guide_content = textwrap.dedent(
        f"""
        = User Guide ({branch_name})
        :doctype: book

        This guide is specific to branch {branch_name}.

        == Install

        Install steps for {display_name} on branch {branch_name}.
        """
    ).strip() + "\n"

    search_content = textwrap.dedent(
        f"""
        = Search Notes ({branch_name})
        :doctype: book

        This page adds searchable text to the global index.

        Terms:
        - biblios demo
        - {display_name}
        - {branch_name}
        - {token}
        """
    ).strip() + "\n"

    (docs_dir / "nav.yml").write_text(nav_content, encoding="utf-8")
    (docs_dir / "index.adoc").write_text(index_content, encoding="utf-8")
    (docs_dir / "guide.adoc").write_text(guide_content, encoding="utf-8")
    (docs_dir / "search.adoc").write_text(search_content, encoding="utf-8")


def create_repo(repo_path: Path, source_id: str, display_name: str) -> None:
    token_prefix = source_id.upper().replace("-", "_")
    main_token = f"TOKEN_{token_prefix}_MAIN"
    v1x_token = f"TOKEN_{token_prefix}_V1X"

    init_git_repo(repo_path)

    write_docs(repo_path, display_name, "main", main_token)
    run_git(repo_path, "add", "docs")
    run_git(repo_path, "commit", "-m", "Initial docs on main")

    run_git(repo_path, "checkout", "-b", "v1.x")
    write_docs(repo_path, display_name, "v1.x", v1x_token)
    run_git(repo_path, "add", "docs")
    run_git(repo_path, "commit", "-m", "Branch-specific docs for v1.x")

    run_git(repo_path, "checkout", "main")


def write_biblios_config(base_dir: Path, repo_a: Path, repo_b: Path) -> Path:
    output_dir = base_dir / "site"
    config_path = base_dir / "biblios.yml"

    yaml_content = textwrap.dedent(
        f"""
        site:
          title: Biblios Local Demo
          url: https://example.org/biblios-demo
          default_language: en

        output:
          dir: {output_dir}
          clean: true

        content:
          sources:
            - id: docs-a
              display_name: Documentation A
              url: {repo_a.resolve().as_uri()}
              branches:
                - name: main
                  display_version: Latest
                - name: v1.x
                  display_version: Version 1.x
              start_path: docs
              default_version: main
              navigation:
                file: nav.yml

            - id: docs-b
              display_name: Documentation B
              url: {repo_b.resolve().as_uri()}
              branches:
                - name: main
                  display_version: Latest
                - name: v1.x
                  display_version: Version 1.x
              start_path: docs
              default_version: main
              navigation:
                file: nav.yml
        """
    ).strip() + "\n"

    config_path.write_text(yaml_content, encoding="utf-8")
    return config_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create a local thoth-biblios demo with two Git repositories."
    )
    parser.add_argument(
        "--base-dir",
        type=Path,
        default=DEFAULT_BASE_DIR,
        help=f"Base directory for demo setup (default: {DEFAULT_BASE_DIR})",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Delete base directory first if it already exists.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if shutil.which("git") is None:
        print("Error: git is required but was not found in PATH.", file=sys.stderr)
        return 1

    base_dir = args.base_dir.expanduser().resolve()
    repos_dir = base_dir / "repos"
    repo_a = repos_dir / "docs-a"
    repo_b = repos_dir / "docs-b"

    if base_dir.exists():
        if args.force:
            shutil.rmtree(base_dir)
        else:
            print(
                f"Error: base directory already exists: {base_dir}\n"
                "Use --force to recreate it.",
                file=sys.stderr,
            )
            return 1

    try:
        repos_dir.mkdir(parents=True, exist_ok=True)
        create_repo(repo_a, "docs-a", "Documentation A")
        create_repo(repo_b, "docs-b", "Documentation B")
        config_path = write_biblios_config(base_dir, repo_a, repo_b)
    except subprocess.CalledProcessError as exc:
        stderr = (exc.stderr or "").strip()
        print(
            f"Git command failed: {' '.join(exc.cmd)}\n{stderr}",
            file=sys.stderr,
        )
        return 1

    print("Biblios demo setup created.")
    print(f"Base directory: {base_dir}")
    print(f"Repo A: {repo_a}")
    print(f"Repo B: {repo_b}")
    print(f"Config: {config_path}")
    print(f"Output dir (from config): {base_dir / 'site'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
