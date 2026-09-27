#!/usr/bin/env python3
"""The English words the home screen shows, gathered from its sources.

Every string literal in the sources is read, with literals joined by + into
one (as the compiler joins them); what looks like words to be read — not
keys, formats, queries or markup — is kept. The list is written as the
English template of a language module, one line a phrase:

    English words = English words

Run from the project's folder: python3 tools/words.py
"""
import glob
import re
import sys

SKIP_FILES = {"Folio.java", "Paper.java", "Foreign.java", "Pack.java", "Sky.java", "Copy.java", "Looks.java", "Keep.java"}
KEEP_IN_SKIPPED = set()


def literals(src):
    """Every string literal, with literals joined by + into one, comments and characters passed over."""
    tokens = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith('//', i):
            i = src.find('\n', i)
            i = n if i < 0 else i
        elif src.startswith('/*', i):
            i = src.find('*/', i + 2)
            i = n if i < 0 else i + 2
        elif c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == '\\' else 1
            i = j + 1
            tokens.append(('other', ''))
        elif c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == '\\' else 1
            tokens.append(('str', src[i + 1:j]))
            i = j + 1
        elif c == '+':
            tokens.append(('plus', ''))
            i += 1
        elif c.isspace():
            i += 1
        else:
            tokens.append(('other', c))
            i += 1
    k = 0
    while k < len(tokens):
        if tokens[k][0] != 'str':
            k += 1
            continue
        parts = [tokens[k][1]]
        k += 1
        while k + 1 < len(tokens) and tokens[k][0] == 'plus' and tokens[k + 1][0] == 'str':
            parts.append(tokens[k + 1][1])
            k += 2
        text = "".join(parts)
        if '\\u' in text:
            text = text.encode().decode('unicode_escape')
        yield text.replace('\\"', '"')


def readable(s):
    # A phrase that counts gives its forms apart by " | "; each form is read as words.
    s = s.replace(' | ', ' ')
    if not re.search(r'[A-Za-z]{2}', s) or '\n' in s or '(' in s:
        return False
    if re.search(r'[{}<>=\[\]\\|^$*]', s):
        return False
    if re.fullmatch(r'[a-z0-9_.#:/%\-]+', s):
        return False
    if re.match(r'(android|com\.|org\.|io\.|http|sans-serif|#)', s):
        return False
    if re.fullmatch(r'[A-Za-z]+[A-Z][a-zA-Z]*', s) and not s[0].isupper():
        return False
    if re.fullmatch(r'[EMdHhmsyaLk:, \-]+', s):
        return False
    if '/' in s and ' ' not in s:
        return False
    if s.endswith(('.txt', '.json', '.png', '.xml', '.db')):
        return False
    if s.startswith((' ', '.', ',')) or s.endswith(' '):
        return False
    if s in ('UTF-8', 'HORIZONTAL', 'NONE', 'NULL', 'EE', 'EE d', 'Aa', 'Meno', 'Ellipse'):
        return False
    return True


def page_words(folder):
    """The words of the pages drawn as pages, as their table keeps them in English."""
    src = open(folder + '/Words.java', encoding='utf-8').read()
    body = src[src.index('String[] EN'):]
    body = body[:body.index('};')]
    return [w.encode().decode('unicode_escape') for w in re.findall(r'"((?:[^"\\]|\\.)*)"', body)]


EXTRA = ['feels like', 'humidity', 'rain', 'm/s', 'h', 'min', 'widgets', 'Category',
         # The phone's state page, written in a file the gatherer passes by.
         'State', 'charging', 'on battery', 'good', 'too warm', 'worn', 'too cold', 'not told',
         'battery health', '%1 cycle | %1 cycles', 'warm', 'volts', 'free of %1 memory', 'free of %1',
         'Security update', 'System update', 'Running for', 'GB', '%1 day | %1 days', '%1 hour | %1 hours',
         'Screen today', 'Charge today', 'Warmest today']


def gather(folder):
    found = []
    for w in page_words(folder) + EXTRA:
        if w not in found and not re.search(r'[{}]', w):
            found.append(w)
    for path in sorted(glob.glob(folder + '/*.java')):
        name = path.rsplit('/', 1)[-1]
        if name in SKIP_FILES:
            if name == 'Looks.java':
                # The presets' names and what each is, shown in the settings, are words of the screen.
                text = open(path, encoding='utf-8').read()
                for start in ('READY = {', 'READY_ABOUT = {'):
                    at = text.find(start)
                    if at >= 0:
                        for s in literals(text[at:text.find('};', at)]):
                            joined = s
                            if readable(joined) and joined not in found:
                                found.append(joined)
            continue
        for s in literals(open(path, encoding='utf-8').read()):
            if readable(s) and s not in found:
                found.append(s)
    return found


if __name__ == '__main__':
    words = gather('src/io/github/shumtugle/ellipse')
    out = sys.argv[1] if len(sys.argv) > 1 else 'assets/lang/template.txt'
    with open(out, 'w', encoding='utf-8') as f:
        f.write('# Ellipse language module\n')
        f.write('# name: English\n')
        f.write('# code: en\n')
        f.write('#\n')
        f.write('# One phrase a line: the English as the home screen says it, " = ", and the same\n')
        f.write('# words in the language of the module. Keep the left side exactly as it is;\n')
        f.write('# write only after " = ". Change "name" and "code" above to the language\'s own.\n')
        f.write('# A line left out, or left in English, is shown in English.\n')
        f.write('\n')
        for w in words:
            f.write(w + ' = ' + w + '\n')
    print(len(words), 'phrases ->', out)
