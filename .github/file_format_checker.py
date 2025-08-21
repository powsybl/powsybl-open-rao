import re
import os
from pathlib import Path
import git

import subprocess

def get_first_commit_date(file_path):
    try:
        # Run git log to find the first commit date for the file
        result = subprocess.run(
            ['git', 'log', '--diff-filter=A', '--follow', '--format=%aI', '--', file_path],
            capture_output=True, text=True, check=True
        )

        # The output will have dates, the first line corresponds to the oldest commit where the file was added
        first_commit_date = result.stdout.strip().split('\n')[-1]
        return first_commit_date

    except subprocess.CalledProcessError:
        return None

good_format = r"""\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/\n{2}package com\.powsybl\.openrao\.[a-z\.]+;\n{2}(?:(?:import [a-zA-Z0-9\.\* _]+;\n)*\n)*/\*{2}\n(?: \*.*\n)*(?: \* @author [A-Za-z -]+ \{@literal <[a-z-_]+\.[a-z-_]+ at [a-z-_.]+\.[a-z]+>\}\n)+ \*/\n[a-z@]"""
regex = re.compile(good_format)

HEADER_REGEX = re.compile(r"""\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/""")
HEADER_GOOD_ESCAPE_REGEX = re.compile(r"""\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/\n{2}package com\.powsybl\.openrao\.[a-z\.]+;\n{2}""")
HEADER_NO_ESCAPE_REGEX = re.compile(r"""\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/\npackage com\.powsybl\.openrao\.[a-z\.]+;\n{2}""")
HEADER_TOO_MANY_ESCAPE_REGEX = re.compile(r"""\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/\n{2}\n+package com\.powsybl\.openrao\.[a-z\.]+;\n{2}""")

AUTHOR_REGEX = re.compile("@author [A-Za-z -]+ \{@literal <[a-z-_]+\.[a-z-_]+ at [a-z-_.]+\.[a-z]+>")

def get_missing_header(file):
    first_commit_date = get_first_commit_date(file)
    year = first_commit_date[:4]
    return f"""/*
 * Copyright (c) {year}, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */"""

def get_first_commit_author_for_file(repo_path, file_path):
    # Open the repository
    repo = git.Repo(repo_path)

    # Get the first commit that modified the specified file
    commits = list(repo.iter_commits(paths=file_path))
    if not commits:
        raise ValueError(f"No commits found for the file: {file_path}")

    # Get the first commit (which is the last in the list)
    first_commit = commits[-1]

    # Extract author name and email
    author_name = first_commit.author.name
    author_email = first_commit.author.email

    return author_name, author_email

KNOWN_AUTHORS = {
    "phiedw": {"name": "Philippe Edwards", "email": "philippe.edwards at rte-france.com"},
    "Thomas Bouquet": {"name": "Thomas Bouquet", "email": "thomas.bouquet at rte-france.com"},
    "pjeanmarie": {"name": "Pauline JEAN-MARIE", "email": "pauline.jean-marie at artelys.com"},
    "Roxane Chen": {"name": "Roxane Chen", "email": "roxane.chen at rte-france.com"},
    "Peter Mitri": {"name": "Peter Mitri", "email": "peter.mitri at rte-france.com"},
    "Peter": {"name": "Peter Mitri", "email": "peter.mitri at rte-france.com"},
    "vbochetRTE": {"name": "Vincent Bochet", "email": "vincent.bochet at rte-france.com"},
    "benrejebmoh": {"name": "Mohamed Ben Rejeb", "email": "mohamed.ben-rejeb at rte-france.com"},
}

root_dir = Path(__file__).parent.parent
errors = 0
files = 0
for a, b, c in os.walk(root_dir):
    if "generated-sources" not in a:
        for element in c:
            if element.endswith(".java") and element != "package-info.java":
                file = Path(a) / element
                with open(root_dir / file, "r") as infile:
                    files += 1
                    content = infile.read()
                    if not HEADER_REGEX.findall(content):
                        errors += 1
                        # print(f"{element}: Wrong copyright definition.")
                        # write_missing_header(file, infile, content)
                    elif HEADER_NO_ESCAPE_REGEX.findall(content):
                        print(f"{element}: Missing escape line between copyright and package name.")
                        errors += 1
                    elif HEADER_TOO_MANY_ESCAPE_REGEX.findall(content):
                        print(f"{element}: Too many escape lines between copyright and package name.")
                        errors += 1
                    elif not AUTHOR_REGEX.findall(content):
                        author = get_first_commit_author_for_file(root_dir, file)[0]
                        print(f"{element}: Adding missing author '{author}'.")
                        author_name = KNOWN_AUTHORS[author]['name']
                        author_mail = KNOWN_AUTHORS[author]['email']
                        author_section = """/**
 * @author """ + author_name + " {" + "@literal <" + author_mail + """>}
 */"""
                        print(author_section)
                        errors += 1
                    elif not regex.findall(content):
                        print(f"{element}: Unidentified problem.")
                        errors += 1

print(f"TOTAL FORMAT ERRORS: {errors} / {files}")