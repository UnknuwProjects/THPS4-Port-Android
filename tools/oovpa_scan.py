#!/usr/bin/env python3
"""Scan an XBE with XbSymbolDatabase OOVPA byte patterns (byte entries only,
XREF_ENTRY checks ignored). A name is reported as a solid hit only when:
  - exactly one distinct address matches, across all signature versions
  - the address is inside the expected library section
  - the pattern has >= MIN_CHECKS byte checks
  - there is function-start evidence: padding/ret before it, or a rel32
    call/jump anywhere in the image targets it
Usage: oovpa_scan.py <xbe> <XbSymbolDatabase/src/OOVPADatabase> <config.toml>
Prints hits for names that are 0/missing in the given SDK config.
"""
import re, struct, sys, os
from collections import defaultdict

MIN_CHECKS = 8

def parse_xbe(path):
    d = open(path, 'rb').read()
    u32 = lambda o: struct.unpack_from('<I', d, o)[0]
    base = u32(0x104)
    secs = []
    so = u32(0x120) - base
    for i in range(u32(0x11c)):
        o = so + i * 56
        flags, vaddr, vsize, roff, rsize, name_addr = struct.unpack_from('<IIIIII', d, o)
        no = name_addr - base
        end = d.find(b'\x00', no)
        name = d[no:end].decode('ascii', 'replace')
        secs.append(dict(name=name, vaddr=vaddr, vsize=vsize, roff=roff, rsize=rsize, flags=flags))
    return d, base, secs

def parse_inl(path):
    """Yield (name, version, [(off, byte), ...]) for each SIG_HEADER with a byte match."""
    txt = open(path, encoding='utf-8', errors='replace').read()
    txt = re.sub(r'//[^\n]*', '', txt)
    out = []
    for m in re.finditer(r'OOVPA_SIG_HEADER(?:_XREF|_NO_XREF)?(?:_DETECT|_EXTEND)?\(\s*([A-Za-z0-9_]+)\s*,\s*(\d+)', txt):
        name, ver = m.group(1), int(m.group(2))
        mm = re.search(r'OOVPA_SIG_MATCH\((.*?)\);', txt[m.end():], re.S)
        if not mm:
            continue
        checks = [(int(a, 16), int(b, 16)) for a, b in
                  re.findall(r'\{\s*0x([0-9A-Fa-f]+)\s*,\s*0x([0-9A-Fa-f]+)\s*\}', mm.group(1))]
        if checks:
            out.append((name, ver, checks))
    return out

def main():
    xbe_path, db_root, cfg_path = sys.argv[1], sys.argv[2], sys.argv[3]
    d, base, secs = parse_xbe(xbe_path)
    sec_by_name = {}
    for s in secs:
        sec_by_name.setdefault(s['name'], s)
    def va_bytes(va, n):
        for s in secs:
            if s['vaddr'] <= va < s['vaddr'] + s['rsize']:
                o = s['roff'] + (va - s['vaddr'])
                return d[o:o + n]
        return b''
    # rel32 call/jump targets across all executable sections
    targets = set()
    for s in secs:
        if not (s['flags'] & 0x2):  # writable flag bit differs; use exec flag 0x20000000? XBE sec flags: use all non-data
            pass
        data = d[s['roff']:s['roff'] + s['rsize']]
        for i in range(len(data) - 5):
            if data[i] in (0xE8, 0xE9):
                rel = struct.unpack_from('<i', data, i + 1)[0]
                targets.add(s['vaddr'] + i + 5 + rel)
    # names missing in config
    missing = set()
    known = {}
    for line in open(cfg_path):
        m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*)\s+=\s+(0x[0-9A-Fa-f]+|0)\s*$', line.strip())
        if m:
            v = int(m.group(2), 16)
            if v == 0:
                missing.add(m.group(1))
            else:
                known[m.group(1)] = v
    libs = {  # OOVPA folder -> sections to scan
        'D3D8': ['D3D', 'D3DX'], 'D3D8LTCG': ['D3D', 'D3DX'],
        'DSound': ['DSOUND'], 'Xapi': ['.text', 'XPP'],
        'XNet': ['XNET'], 'XOnline': ['XONLINE'], 'XGraphic': ['XGRPH'],
    }
    hits = defaultdict(lambda: defaultdict(set))  # name -> addr -> {(lib, ver, nchecks)}
    for lib, secnames in libs.items():
        folder = os.path.join(db_root, lib)
        if not os.path.isdir(folder):
            continue
        secdata = []
        for sn in secnames:
            s = sec_by_name.get(sn)
            if s:
                secdata.append((s, d[s['roff']:s['roff'] + s['rsize']]))
        for fn in sorted(os.listdir(folder)):
            if not fn.endswith('.inl'):
                continue
            for name, ver, checks in parse_inl(os.path.join(folder, fn)):
                if name not in missing:
                    continue
                anchor_off, anchor_byte = checks[0]
                for s, data in secdata:
                    start = 0
                    while True:
                        i = data.find(bytes([anchor_byte]), start)
                        if i < 0:
                            break
                        start = i + 1
                        pos = i - anchor_off
                        if pos < 0:
                            continue
                        if all(pos + o < len(data) and data[pos + o] == b for o, b in checks):
                            hits[name][s['vaddr'] + pos].add((lib, ver, len(checks)))
    print(f"{'name':48s} {'addr':10s} verdict")
    merged = {}
    for name in sorted(hits):
        addrs = hits[name]
        if len(addrs) != 1:
            print(f"{name:48s} {'':10s} AMBIGUOUS {len(addrs)} addrs: " +
                  ' '.join(hex(a) for a in sorted(addrs)))
            continue
        addr, srcs = next(iter(addrs.items()))
        nchecks = max(c for _, _, c in srcs)
        before = va_bytes(addr - 1, 1)
        prev_ret = va_bytes(addr - 3, 3)[:1] == b'\xc2' or before in (b'\xc3', b'\xcc', b'\x90')
        start_ev = prev_ret or addr in targets or any(addr == s['vaddr'] for s in secs)
        verdict = 'SOLID' if (nchecks >= MIN_CHECKS and start_ev) else f'WEAK checks={nchecks} start_ev={start_ev}'
        print(f"{name:48s} {addr:#010x} {verdict} srcs={sorted(srcs)}")
        if verdict == 'SOLID':
            merged[name] = addr
    print("\nMERGEABLE:", len(merged))
    for k, v in merged.items():
        print(f"{k} = {v:#x}")

if __name__ == '__main__':
    main()
