#!/usr/bin/env python3
"""Generate Markdown per-file technical audit for EcoMap Invest (appendix 8.3)."""
from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Iterable

# Repo module root (`ecomap-invest/`), not the parent `Projet/` folder.
ROOT = Path(__file__).resolve().parent.parent
SKIP_DIRS = {"node_modules", "target", ".git", ".mvn", ".next"}
TEXT_EXT = {
    ".java", ".ts", ".tsx", ".js", ".jsx", ".sql", ".yml", ".yaml",
    ".xml", ".json", ".properties", ".css", ".xsd", ".md", ".sh", ".py", ".csv", ".geojson",
}
SPECIAL_NAMES = {"Dockerfile", ".dockerignore", ".gitignore", "sonar-project.properties"}

INTERNAL_JAVA_PREFIXES = (
    "com.example.",
    "com.ecomap.",
)


def iter_audit_files() -> Iterable[Path]:
    for dirpath, dirnames, filenames in os.walk(ROOT):
        parts = set(Path(dirpath).parts)
        if parts & SKIP_DIRS:
            dirnames[:] = []
            continue
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and d != ".m2"]
        if ".m2" in Path(dirpath).parts:
            continue
        for fn in filenames:
            p = Path(dirpath) / fn
            ext = p.suffix.lower()
            if fn.startswith("Dockerfile") or fn in SPECIAL_NAMES:
                yield p
            elif ext in TEXT_EXT:
                yield p


def read_text_safe(p: Path) -> str:
    try:
        return p.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def java_summary(text: str) -> str:
    m = re.search(r"/\*\*(.*?)\*/", text, re.DOTALL)
    if m:
        s = re.sub(r"\s*\*\s?", " ", m.group(1)).strip()
        s = re.sub(r"\s+", " ", s)
        s = re.sub(r"\{@code\s+([^}]+)\}", r"\1", s)

        def _link_repl(m):
            ref = m.group(1).strip().replace("#", ".")
            return ref.split(".")[-1]

        s = re.sub(r"\{@link\s+([^}]+)\}", _link_repl, s)
        s = re.sub(r"<[^>]+>", " ", s)
        s = re.sub(r"\s+", " ", s).strip()
        if len(s) > 280:
            s = s[:277].rsplit(" ", 1)[0] + "…"
        return s or "Spring / Java compilation unit."
    m2 = re.search(
        r"/\*\s*([^\n*][^\*]*?)\s*\*/\s*(?:@\w+\([^)]*\)\s*)*"
        r"(?:public\s+)?(?:class|interface|enum|record)\s+(\w+)",
        text,
        re.DOTALL,
    )
    if m2:
        one = re.sub(r"\s+", " ", m2.group(1).strip())
        if len(one) > 240:
            one = one[:237] + "…"
        return one
    # Trailing line comments after package / imports
    body = re.split(
        r"\n(?=public |private |protected |class |interface |enum |record |@)",
        text,
        maxsplit=1,
    )[-1]
    for line in body.splitlines()[:12]:
        ls = line.strip()
        if ls.startswith("//") and "package" not in ls.lower():
            return ls[2:].strip()[:240]
    # record/class name heuristic
    rm = re.search(r"(?:public\s+)?record\s+(\w+)\s*\(", text)
    if rm:
        return f"Immutable `{rm.group(1)}` record (DTO / API payload)."
    cm = re.search(r"public\s+class\s+(\w+)\b", text)
    if cm:
        return f"Type `{cm.group(1)}` (implementation details in source)."
    return "Java compilation unit."


def java_symbols(text: str) -> list[str]:
    out = []
    for m in re.finditer(
        r"^\s*(?:public|private|protected)?\s*(?:abstract\s+|static\s+|sealed\s+|non-sealed\s+|final\s+|strictfp\s+)*"
        r"(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)\b",
        text,
        re.MULTILINE,
    ):
        out.append(f"`{m.group(2)}` ({m.group(1)})")
    # de-dupe preserve order
    seen = set()
    uniq = []
    for x in out:
        if x not in seen:
            seen.add(x)
            uniq.append(x)
    return uniq[:25]


def java_imports(text: str) -> tuple[list[str], list[str]]:
    internal, external = [], []

    def classify(stmt: str) -> None:
        stmt = stmt.strip().rstrip(";")
        if stmt.startswith(INTERNAL_JAVA_PREFIXES) or stmt.startswith(
            ("com.example.", "com.ecomap.")
        ):
            internal.append(stmt)
        else:
            external.append(stmt)

    for line in text.splitlines():
        line = line.strip()
        if line.startswith("import static "):
            rest = line[len("import static ") :].rstrip(";")
            # e.g. com.foo.Bar.BAZ or com.foo.Bar.method
            classify(rest.split()[0])
        elif line.startswith("import "):
            classify(line[len("import ") :])
    return sorted(set(internal)), sorted(set(external))


def detect_java_patterns(text: str) -> list[str]:
    hits = []
    checks = [
        ("@CircuitBreaker", "Resilience4j `@CircuitBreaker`"),
        ("@Retry", "Resilience4j `@Retry`"),
        ("@TimeLimiter", "Resilience4j `@TimeLimiter`"),
        ("@RateLimiter", "Resilience4j `@RateLimiter`"),
        ("@Cacheable", "Spring Cache `@Cacheable` / cache abstraction"),
        ("@Scheduled", "Spring `@Scheduled`"),
        ("@Transactional", "Spring `@Transactional`"),
        ("@Aspect", "AspectJ `@Aspect` (AOP)"),
        ("@Audited", "Custom audit aspect `@Audited`"),
        ("@RestController", "`@RestController`"),
        ("@Controller", "`@Controller`"),
        ("@Service", "`@Service`"),
        ("@Repository", "`@Repository`"),
        ("@Configuration", "Spring `@Configuration`"),
        ("@Bean", "Spring `@Bean` definitions"),
        ("@SpringBootApplication", "Spring Boot entry (`@SpringBootApplication`)"),
        ("@Entity", "JPA `@Entity`"),
        ("@Table(", "JPA `@Table` mapping"),
        ("@EnableMethodSecurity", "Method security"),
        ("@PreAuthorize", "`@PreAuthorize`"),
        ("@JmsListener", "JMS `@JmsListener` (message consumer)"),
        ("@ConditionalOnProperty", "Conditional configuration"),
        ("OncePerRequestFilter", "Servlet filter (e.g. JWT pipeline)"),
        ("SseEmitter", "SSE streaming"),
        ("CompletableFuture", "Async `CompletableFuture`"),
        ("implements CommandLineRunner", "`CommandLineRunner` bootstrap"),
        ("implements ApplicationRunner", "`ApplicationRunner` bootstrap"),
        ("ItemReader", "Spring Batch reader"),
        ("ItemProcessor", "Spring Batch processor"),
        ("ItemWriter", "Spring Batch writer"),
        ("RmiSaturationInvoker", "RMI scoring invocation"),
        ("extends Remote", "Java RMI `Remote`"),
        ("HexScoringConfig", "Hex scoring configuration object"),
    ]
    for needle, label in checks:
        if needle in text:
            hits.append(label)
    return hits


def java_value_keys(text: str) -> list[str]:
    keys = []
    for m in re.finditer(r'@Value\s*\(\s*"\$\{([^}]+)\}"', text):
        k = m.group(1).split(":")[0].strip()
        if k and k not in keys:
            keys.append(k)
        if len(keys) >= 10:
            break
    return keys


def java_constants(text: str) -> list[str]:
    lines = []
    for m in re.finditer(
        r"^\s*(?:public|private|protected)\s+static\s+final\s+[^=]+=\s*[^;]+;",
        text,
        re.MULTILINE,
    ):
        s = " ".join(m.group(0).split())
        if len(s) > 120:
            s = s[:117] + "…"
        lines.append(s)
        if len(lines) >= 6:
            break
    return lines


def ts_summary(text: str, path: str) -> str:
    m = re.search(r"/\*\*?([\s\S]*?)\*/", text[:1500])
    if m:
        s = re.sub(r"\s+", " ", m.group(1)).strip()
        if len(s) > 240:
            s = s[:237] + "…"
        return s
    if path.endswith("page.tsx"):
        return "Next.js App Router page component for this route segment."
    return "TypeScript/JavaScript module."


def ts_imports(text: str, rel: str) -> tuple[list[str], list[str]]:
    internal, external = [], []
    pat = re.compile(
        r"""^import\s+(?:type\s+)?(?:(?:\{[^}]+\}|\*\s+as\s+\w+|\w+)(?:\s*,\s*(?:\{[^}]+\}|\w+))*\s+from\s+)?['"]([^'"]+)['"]""",
        re.MULTILINE,
    )
    for m in pat.finditer(text):
        spec = m.group(1)
        if spec.startswith("@/") or spec.startswith(".") or spec.startswith(".."):
            internal.append(spec)
        else:
            external.append(spec)
    return sorted(set(internal)), sorted(set(external))


def ts_exports(text: str) -> list[str]:
    syms = []
    for m in re.finditer(
        r"^export\s+default\s+function\s+(\w+)\b", text, re.MULTILINE
    ):
        syms.append(f"`{m.group(1)}` (default export function)")
    for m in re.finditer(
        r"^export\s+(?:async\s+)?function\s+(\w+)\b", text, re.MULTILINE
    ):
        syms.append(f"`{m.group(1)}` (export function)")
    for m in re.finditer(r"^export\s+const\s+(\w+)\s*=", text, re.MULTILINE):
        syms.append(f"`{m.group(1)}` (const)")
    for m in re.finditer(r"^export\s+interface\s+(\w+)\b", text, re.MULTILINE):
        syms.append(f"`{m.group(1)}` (interface)")
    for m in re.finditer(r"^export\s+type\s+(\w+)\b", text, re.MULTILINE):
        syms.append(f"`{m.group(1)}` (type)")
    seen = set()
    out = []
    for s in syms:
        if s not in seen:
            seen.add(s)
            out.append(s)
    return out[:30]


def ts_patterns(text: str) -> list[str]:
    p = []
    if '"use client"' in text or "'use client'" in text:
        p.append("Next.js client component (`" "use client" "`)")
    if "create(" in text and "zustand" in text:
        p.append("Zustand store")
    if "useEffect" in text or "useState" in text:
        p.append("React hooks")
    if "react-leaflet" in text or "/leaflet" in text:
        p.append("Leaflet / react-leaflet")
    if "EventSource" in text or "text/event-stream" in text:
        p.append("SSE client")
    if "describe(" in text and "it(" in text:
        p.append("Vitest / test suite")
    return p


def sql_brief(text: str) -> str:
    c_lines = [ln.strip() for ln in text.splitlines() if ln.strip().startswith("--")]
    if c_lines:
        first = c_lines[0].lstrip("-").strip()
        if len(first) > 260:
            first = first[:257] + "…"
        return first
    actions = []
    for kw in ("CREATE TABLE", "ALTER TABLE", "INSERT INTO", "DELETE FROM", "CREATE INDEX"):
        if kw in text.upper():
            actions.append(kw)
    base = "; ".join(dict.fromkeys(actions)) or "SQL migration / script."
    return f"{base} ({len(text)} chars)"


def yaml_keys(text: str) -> list[str]:
    keys = []
    for line in text.splitlines()[:80]:
        m = re.match(r"^([a-zA-Z0-9_.-]+):\s*", line)
        if m and not line.strip().startswith("#"):
            keys.append(m.group(1))
    return keys[:25]


def pom_brief(text: str) -> tuple[str, list[str]]:
    sans_parent = re.sub(r"<parent>[\s\S]*?</parent>\s*", "", text, count=1)
    aid = re.search(r"<artifactId>([^<]+)</artifactId>", sans_parent)
    par_m = re.search(r"<parent>[\s\S]*?</parent>", text, re.DOTALL)
    par_aid = (
        re.search(r"<artifactId>([^<]+)</artifactId>", par_m.group(0))
        if par_m
        else None
    )
    deps = re.findall(r"<dependency>[\s\S]*?<artifactId>([^<]+)</artifactId>", text)
    deps_unique = list(dict.fromkeys(deps))[:24]
    summary = "Maven POM"
    if aid:
        summary += f" (`artifactId`: `{aid.group(1).strip()}`)"
    if par_aid:
        summary += f"; inherits `{par_aid.group(1).strip()}`"
    return summary, deps_unique


def dockerfile_brief(text: str) -> list[str]:
    lines = []
    for ln in text.splitlines()[:40]:
        if re.match(r"^(FROM|EXPOSE|ENV|ARG|RUN|CMD|ENTRYPOINT|HEALTHCHECK)\s", ln):
            lines.append(ln.strip())
    return lines[:12]


def audit_file(p: Path) -> str:
    rel = p.relative_to(ROOT).as_posix()
    text = read_text_safe(p)
    ext = p.suffix.lower()
    fn = p.name

    parts = [f"#### `{rel}`\n"]

    if ext == ".java":
        parts.append(f"**Summary:** {java_summary(text)}\n")
        syms = java_symbols(text)
        parts.append(
            "**Symbols:** "
            + (", ".join(syms) if syms else "*(none detected — package-private or only nested types)*")
            + "\n"
        )
        inn, outt = java_imports(text)
        parts.append(
            "**Internal dependencies (imports):** "
            + ("; ".join(f"`{x}`" for x in inn) if inn else "—")
            + "\n"
        )
        parts.append(
            "**External dependencies (imports):** "
            + ("; ".join(f"`{x}`" for x in outt[:40]) + ("…" if len(outt) > 40 else ""))
            + "\n"
        )
        pat = detect_java_patterns(text)
        parts.append(
            "**Patterns / algorithms:** "
            + ("; ".join(pat) if pat else "—")
            + "\n"
        )
        consts = java_constants(text)
        vkeys = java_value_keys(text)
        kc = []
        if consts:
            kc.extend(f"`{c}`" for c in consts)
        if vkeys:
            kc.append("`@Value` keys: " + ", ".join(f"`{v}`" for v in vkeys))
        parts.append(
            "**Key constants / config references:** "
            + ("; ".join(kc) if kc else "—")
            + "\n"
        )
    elif ext in (".ts", ".tsx", ".js", ".jsx"):
        parts.append(f"**Summary:** {ts_summary(text, rel)}\n")
        syms = ts_exports(text)
        parts.append(
            "**Exported symbols (detected):** "
            + (", ".join(syms) if syms else "*(default-only or re-exports — inspect file)*")
            + "\n"
        )
        inn, outt = ts_imports(text, rel)
        parts.append(
            "**Internal dependencies:** "
            + ("; ".join(f"`{x}`" for x in inn[:35]) + ("…" if len(inn) > 35 else "") if inn else "—")
            + "\n"
        )
        parts.append(
            "**External dependencies:** "
            + ("; ".join(f"`{x}`" for x in sorted(set(outt))[:35]) + ("…" if len(set(outt)) > 35 else ""))
            + "\n"
        )
        pat = ts_patterns(text)
        parts.append(
            "**Patterns / notes:** " + ("; ".join(pat) if pat else "—") + "\n"
        )
        parts.append("**Key configuration / constants:** — *(see literals in file)*\n")
    elif ext == ".sql":
        parts.append(f"**Summary:** {sql_brief(text)}\n")
        parts.append("**Symbols:** — *(SQL DDL/DML)*\n")
        tabs = re.findall(
            r"CREATE TABLE\s+(?:IF NOT EXISTS\s+)?([a-zA-Z0-9_]+)", text, re.I
        )
        parts.append(
            "**Internal dependencies:** — *(database objects)*\n"
            "**External dependencies:** PostGIS / PostgreSQL\n"
        )
        parts.append(
            "**Patterns / notes:** Flyway versioned migration"
            if "migration" in rel or rel.startswith("backend/")
            else "**Patterns / notes:** SQL batch script\n"
        )
        tab_u = list(dict.fromkeys(tabs))[:15]
        parts.append(
            f"**Key configuration / constants:** tables/objects: {', '.join(f'`{t}`' for t in tab_u) or '—'}\n"
        )
    elif ext in (".yml", ".yaml"):
        is_gha = ".github/workflows" in rel
        parts.append(
            "**Summary:** GitHub Actions CI workflow (`on`, `jobs`).\n"
            if is_gha
            else f"**Summary:** YAML configuration for `{rel.split('/')[0]}` stack layer.\n"
        )
        parts.append("**Symbols:** —\n")
        keys = yaml_keys(text)
        parts.append(
            "**Internal dependencies:** —\n"
            "**External dependencies:** "
            + (
                "GitHub-hosted runners; actions/checkout, etc.\n"
                if is_gha
                else "Spring Boot / runtime env interpolation\n"
            )
        )
        parts.append(
            "**Patterns / notes:** "
            + (
                "CI pipeline (Maven, tests)\n"
                if is_gha
                else "`${...}` placeholders for env-backed settings\n"
            )
        )
        parts.append(
            f"**Key top-level keys:** {', '.join(f'`{k}`' for k in keys)}\n"
        )
    elif ext == ".xml":
        if "pom.xml" in fn or fn.endswith(".xml") and p.parent.name in (
            "build-support", "docker-stub-poms"
        ):
            sm, deps = pom_brief(text)
            parts.append(f"**Summary:** {sm}\n")
            parts.append(
                f"**Symbols:** — *(Maven coordinates)*\n"
                f"**Internal dependencies:** `{ROOT.name}` modules via `<module>` / project refs\n"
                f"**External dependencies (artifactIds sampled):** {', '.join(f'`{d}`' for d in deps[:25])}\n"
                f"**Patterns / notes:** Maven reactor / dependency graph\n"
                f"**Key configuration / constants:** see `<properties>` / `<dependencies>`\n"
            )
        elif "xsd" in rel:
            parts.append("**Summary:** XML Schema for SOAP contract generation.\n")
            els = re.findall(r'<xs:element\s+name="([^"]+)"', text)
            parts.append(
                f"**Symbols:** schema elements (sample): {', '.join(f'`{e}`' for e in els[:20])}\n"
                f"**Internal dependencies:** —\n**External dependencies:** W3C XSD / JAXB toolchain\n"
            )
            parts.append(
                "**Patterns / notes:** contract-first WSDL/XSD\n"
                "**Key configuration / constants:** namespace / element definitions in file\n"
            )
        else:
            parts.append("**Summary:** XML resource.\n**Symbols:** —\n**Internal dependencies:** —\n**External dependencies:** XML stack\n")
    elif ext == ".json" and fn == "package-lock.json":
        parts.append(
            "**Summary:** npm lockfile (exact dependency tree for reproducible installs).\n"
            "**Symbols:** `packages`, `dependencies` pins\n"
            "**Internal dependencies:** —\n**External dependencies:** locked npm packages\n"
            "**Patterns / notes:** consumed by `npm ci`\n"
        )
    elif ext == ".json" and fn == "package.json":
        try:
            data = json.loads(text)
        except json.JSONDecodeError:
            data = {}
        parts.append("**Summary:** npm package manifest (frontend).\n")
        parts.append(
            "**Symbols:** `name`, `scripts`, `dependencies`, `devDependencies`\n"
            f"**Internal dependencies:** —\n**External dependencies:** "
            f"{', '.join(f'`{k}@{v}`' for k, v in list((data.get('dependencies') or {}).items())[:22])}\n"
        )
        parts.append(
            "**Patterns / notes:** Next.js 16 / React 19 stack\n"
            "**Key configuration:** `scripts.build`, `overrides` for postcss\n"
        )
    elif ext == ".json" and fn == "tsconfig.json":
        parts.append("**Summary:** TypeScript compiler options for Next.js.\n")
        parts.append("**Symbols:** `compilerOptions`, `paths` (`@/*`)\n**Internal dependencies:** `@/*` → `./`\n**External dependencies:** TypeScript\n")
    elif ext == ".properties":
        parts.append("**Summary:** Java `.properties` configuration.\n")
        sample = [ln.strip() for ln in text.splitlines() if ln.strip() and not ln.strip().startswith("#")][:8]
        parts.append(f"**Key lines:** {'; '.join(sample)}\n")
    elif ext == ".css":
        parts.append("**Summary:** Global or design-system CSS.\n**Symbols:** — **Patterns:** Tailwind `@import` / layers if present\n")
    elif fn.startswith("Dockerfile"):
        lines = dockerfile_brief(text)
        parts.append("**Summary:** OCI image build instructions.\n")
        parts.append(
            "**Symbols:** —\n**Internal dependencies:** copied project paths\n"
            "**External dependencies:** base image (`FROM`)\n"
        )
        parts.append(
            "**Patterns / notes:** multi-stage / JAR or Node build\n"
            f"**Key lines:** {'; '.join(f'`{ln}`' for ln in lines)}\n"
        )
    elif fn == ".dockerignore" or fn == ".gitignore":
        parts.append(f"**Summary:** Excludes paths from Docker build or Git.\n**Patterns:** ignore lists\n")
    elif ext == ".md":
        parts.append(f"**Summary:** Documentation — {text.splitlines()[0][:120] if text else 'empty'}…\n")
    elif ext == ".sh":
        parts.append("**Summary:** Shell script for dev/CI checks.\n")
    elif ext == ".py":
        parts.append("**Summary:** Python utility (OSM bbox extraction).\n")
    elif ext == ".csv":
        parts.append("**Summary:** Seed or mock CSV data.\n")
    elif ext == ".geojson":
        parts.append("**Summary:** GeoJSON spatial reference data.\n")
    elif fn == "sonar-project.properties":
        parts.append("**Summary:** SonarQube scanner properties.\n")
    elif ext == ".tsx" or ext == ".ts":
        pass  # handled above
    else:
        parts.append(f"**Summary:** Project file (`{ext or fn}`).\n")

    parts.append("\n")
    return "".join(parts)


def main() -> None:
    files = sorted(iter_audit_files(), key=lambda p: p.relative_to(ROOT).as_posix())
    out_lines = [
        "### 8.3 Per-File Technical Audit\n",
        "\n",
        "This section expands **§8.2** with structured entries for every audited path under `ecomap-invest/`. "
        "**Internal** TypeScript imports use `@/` or relative paths. "
        "**Internal** Java imports use `com.example.*` or `com.ecomap.*`. "
        "Automated extraction may miss dynamic imports or nested types; verify symbols in IDE if needed.\n",
        "\n",
    ]
    for p in files:
        try:
            out_lines.append(audit_file(p))
        except Exception as ex:  # noqa: BLE001
            rel = p.relative_to(ROOT).as_posix()
            out_lines.append(f"#### `{rel}`\n**Error during audit:** `{ex}`\n\n")

    # Place next to main technical report at workspace root (`Projet/`).
    dest = ROOT.parent / "ECOMAP_APPENDIX_8_3_AUDIT.md"
    dest.write_text("".join(out_lines), encoding="utf-8")
    print(f"Wrote {len(files)} entries -> {dest}")


if __name__ == "__main__":
    main()
