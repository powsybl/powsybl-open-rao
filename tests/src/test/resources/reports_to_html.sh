#!/bin/bash

# If number of arguments is not one, fail
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <reports_file>"
    exit 1
fi

INPUT_FILE="$1"
# If the argument does not refer to a file, fail
if [ ! -f "$INPUT_FILE" ]; then
    echo "$INPUT_FILE does not exist"
    exit 1
fi

# Copy the input file and append ".html" to its name (the original file won't be modified, the script only works on the html file)
HTML_FILE="$INPUT_FILE.html"
cp "$INPUT_FILE" "$HTML_FILE"

# While the file contains at least one line that should be foldable (the line begins with 3-spaces indentation and a "+")
while [ $(grep -E '(   )*\+ ' "$HTML_FILE" | wc -l) -gt 0 ]
do
  # Remove the "+" at the beginning and add the required HTML markup
  perl -0777 -pi -e 's;( *)\+ (.*)\n((\1   .*\n)*);$1<table><tr><td><details><summary>$2</summary>\n$3$1</details></td></tr></table>\n;' "$HTML_FILE"
done

# Add a <p> markup at the beginning of the lines that are not foldable
sed -i -E 's;^( *)([^< ].*);\1<p>\2</p>;' "$HTML_FILE"

# Add page style at beginning of file (use @ separator because ; is used in CSS and / or ~ may appear in $INPUT_FILE
sed -i '1s@^@<!DOCTYPE html>\n<html>\n<head>\n   <title>'"$INPUT_FILE"'</title>\n   <style>\n   td {\n      padding-left: 1em;\n   }\n   td p {\n      padding-left: 1em;\n      margin: 0.2em 0;\n   }\n   summary {\n      color: blue;\n      margin-left: -1em;\n   }\n   summary::marker {\n      color: transparent;\n   }\n   </style>\n</head>\n<body>\n@' "$HTML_FILE"

# Add HTML markups at end of file
echo '</body></html>' >> "$HTML_FILE"