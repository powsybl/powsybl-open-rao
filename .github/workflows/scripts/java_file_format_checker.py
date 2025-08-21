import os
import re
from pathlib import Path
from typing import List

NEW_LINE_REGEX = r"\n"
SKIP_LINE_REGEX = r"\n{2}"
TOO_MANY_LINES_SKIPPED = r"\n{2}\n+"

COPYRIGHT_REGEX = r"\*\n \* Copyright \(c\) \d{4}, RTE \(http://www\.rte-france\.com\)\n \* This Source Code Form is subject to the terms of the Mozilla Public\n \* License, v\. 2\.0\. If a copy of the MPL was not distributed with this\n \* file, You can obtain one at http://mozilla\.org/MPL/2\.0/\.\n \*/"

PACKAGE_REGEX = r"package com\.powsybl\.openrao\.[a-z\._]+;"

IMPORT_LINE_REGEX = r"import [a-zA-Z0-9\._\* ]+;"
IMPORT_BLOCK_REGEX = r"(?:" + IMPORT_LINE_REGEX + NEW_LINE_REGEX + r")+"
IMPORT_REGEX = r"(?:" + IMPORT_BLOCK_REGEX + NEW_LINE_REGEX + r")+"

DESCRIPTION_REGEX = r"/\*{2}\n(?:(?: \* .*\n)|(?: \*\n))*(?: \* @author [A-Za-z -]+ \{@literal <[a-z-_]+\.[a-z-_]+ at [a-z-_.]+\.[a-z]+>\}\n)+ \*/"

FILE_REGEX = (
    COPYRIGHT_REGEX
    + SKIP_LINE_REGEX
    + PACKAGE_REGEX
    + SKIP_LINE_REGEX
    + IMPORT_REGEX
    + DESCRIPTION_REGEX
    + NEW_LINE_REGEX
    + r"[a-z@]"
)

FILE_NO_IMPORT_REGEX = (
    COPYRIGHT_REGEX
    + SKIP_LINE_REGEX
    + PACKAGE_REGEX
    + SKIP_LINE_REGEX
    + DESCRIPTION_REGEX
    + NEW_LINE_REGEX
    + r"[a-z@]"
)


def check_file_structure(filename: str, file_content: str, report: List[str]):
    # Bad copyright section
    if not re.compile(COPYRIGHT_REGEX).findall(file_content):
        report.append(
            f"{filename}: Copyright section is either missing or incorrect. Make sure it is located at the top of the file and that it follows the same pattern as other files."
        )

    # No blank line between copyright and package definition
    elif re.compile(COPYRIGHT_REGEX + NEW_LINE_REGEX + PACKAGE_REGEX).findall(
        file_content
    ):
        report.append(
            f"{filename}: Copyright section must be separated from the package declaration by a blank line."
        )

    # Several blank lines between copyright and package definition
    elif re.compile(COPYRIGHT_REGEX + TOO_MANY_LINES_SKIPPED + PACKAGE_REGEX).findall(
        file_content
    ):
        report.append(
            f"{filename}: Copyright section must be separated from the package declaration by a single blank line, too many lines were skipped."
        )

    # No blank line between package and imports
    elif re.compile(IMPORT_REGEX).findall(file_content) and re.compile(
        PACKAGE_REGEX + NEW_LINE_REGEX + IMPORT_REGEX
    ).findall(file_content):
        report.append(
            f"{filename}: Package declaration must be separated from the imports by exactly one blank line."
        )

    # Bad description section
    elif not re.compile(DESCRIPTION_REGEX).findall(file_content):
        report.append(
            f"{filename}: Description section is either missing or incorrect. Make sure it is located right before the class definition and that at least one author was provided using the pattern `@author Firstname Lastname "
            + "{"
            + "@literal <firstname.lastname at company.com>}`. If a supplementary description is present, make sure a blank line separates it from the authors."
        )

    # Blank line between description and class definition
    elif re.compile(DESCRIPTION_REGEX + r"\n\n+[a-z@]").findall(file_content):
        report.append(
            f"{filename}: There must not be blank lines between the description section and the class definition."
        )

    # Missing blank line at the end of the file
    elif file_content.endswith("}"):
        report.append(f"{filename}: File is missing a blank line at the end.")

    # Several blank lines at the end of the file
    elif not file_content.endswith("}\n"):
        report.append(f"{filename}: File must finish with a single blank line.")

    # Too many consecutive blank lines
    elif re.compile(TOO_MANY_LINES_SKIPPED).findall(file_content):
        report.append(f"{filename}: At least two consecutive blank lines found in file. This is not allowed.")

    # Bad overall structure
    elif not re.compile(FILE_REGEX).findall(file_content) and not re.compile(
        FILE_NO_IMPORT_REGEX
    ).findall(file_content):
        report.append(
            f"{filename}: File does not follow the required pattern. The error was not precisely located, please manually check the file."
        )


def check_all_files(root_dir: Path):
    report = []
    for directory, _, files in os.walk(root_dir):
        if "generated-sources" not in directory:  # Skip automatically generated files
            for element in files:
                if (
                    element.endswith(".java") and element != "package-info.java"
                ):  # Skip non-Java files
                    file = root_dir / directory / element
                    with open(file, "r") as infile:
                        content = infile.read()
                        check_file_structure(element, content, report)
    if report:
        raise ValueError(
            "Some files are not properly formatted:\n ❌ " + "\n ❌ ".join(report)
        )


if __name__ == "__main__":
    root_dir = Path(__file__).parent.parent.parent.parent
    check_all_files(root_dir)
