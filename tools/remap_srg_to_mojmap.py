#!/usr/bin/env python3
"""Rename SRG identifiers (m_1234_, f_1234_) in decompiled sources to Mojang names.

Usage: python3 tools/remap_srg_to_mojmap.py src/main/java

Downloads MCPConfig 1.20.1 (obf -> srg) and Mojang client/server mappings
(mojang -> obf), composes them into srg -> mojang and rewrites the sources in place.
"""
import io, json, re, sys, urllib.request, zipfile
from pathlib import Path

MC = "1.20.1"
MCPCONFIG = "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/{v}/mcp_config-{v}.zip"
MCPCONFIG_VERSION = "1.20.1-20230612.114412"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
CACHE = Path(__file__).resolve().parent / ".cache"

PRIMS = {"byte": "B", "char": "C", "double": "D", "float": "F", "int": "I",
         "long": "J", "short": "S", "boolean": "Z", "void": "V"}


def fetch(url, name):
    CACHE.mkdir(exist_ok=True)
    p = CACHE / name
    if not p.exists():
        print("downloading", url)
        p.write_bytes(urllib.request.urlopen(url).read())
    return p.read_bytes()


def mojang_mappings():
    manifest = json.loads(fetch(MANIFEST, "manifest.json"))
    ver_url = next(v["url"] for v in manifest["versions"] if v["id"] == MC)
    ver = json.loads(fetch(ver_url, f"{MC}.json"))
    texts = [fetch(ver["downloads"][k]["url"], f"{k}.txt").decode()
             for k in ("client_mappings", "server_mappings")]
    classes = {}  # obf class -> (moj class, {(obf name, obf desc or None): moj name})
    moj2obf = {}
    parsed = []
    for text in texts:
        cur = None
        for line in text.splitlines():
            if line.startswith("#") or not line.strip():
                continue
            if not line.startswith(" "):
                moj, obf = line.rstrip(":").split(" -> ")
                moj2obf[moj] = obf
                cur = (moj, obf, [])
                parsed.append(cur)
            else:
                cur[2].append(line.strip())
    def to_desc(t):
        dims = t.count("[]")
        t = t.replace("[]", "")
        base = PRIMS.get(t) or "L" + moj2obf.get(t, t).replace(".", "/") + ";"
        return "[" * dims + base
    for moj, obf, members in parsed:
        entry = classes.setdefault(obf, (moj, {}))[1]
        for m in members:
            left, obf_name = m.split(" -> ")
            left = re.sub(r"^\d+:\d+:", "", left)
            ret, rest = left.split(" ", 1)
            if "(" in rest:
                name, args = rest[:-1].split("(")
                args = [a for a in args.split(",") if a]
                desc = "(" + "".join(to_desc(a) for a in args) + ")" + to_desc(ret)
                entry[(obf_name, desc)] = name
            else:
                entry[(obf_name, None)] = rest
    return classes


def srg_to_moj(classes):
    z = zipfile.ZipFile(io.BytesIO(fetch(MCPCONFIG.format(v=MCPCONFIG_VERSION), "mcp_config.zip")))
    tsrg = z.read("config/joined.tsrg").decode().splitlines()
    out, cls = {}, None
    for line in tsrg[1:]:
        if not line.startswith("\t"):
            cls = line.split(" ")[0]
            continue
        if line.startswith("\t\t"):
            continue  # parameters
        parts = line.strip().split(" ")
        members = classes.get(cls, (None, {}))[1]
        if len(parts) >= 3 and parts[1].startswith("("):  # method: obf desc srg id
            obf, desc, srg = parts[0], parts[1], parts[2]
            name = members.get((obf, desc))
        else:  # field: obf srg id
            obf, srg = parts[0], parts[1]
            name = members.get((obf, None))
        if name and re.fullmatch(r"[mf]_\d+_", srg):
            out[srg] = name
    return out


def main(root):
    mapping = srg_to_moj(mojang_mappings())
    print("mapped identifiers:", len(mapping))
    pat = re.compile(r"\b[mf]_\d+_\b")
    missing = set()
    def sub(m):
        name = mapping.get(m.group(0))
        if name is None:
            missing.add(m.group(0))
            return m.group(0)
        return name
    for f in Path(root).rglob("*.java"):
        s = f.read_text(encoding="utf-8")
        t = pat.sub(sub, s)
        if t != s:
            f.write_text(t, encoding="utf-8")
    print("unmapped identifiers left:", len(missing), sorted(missing)[:20])


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "src/main/java")
