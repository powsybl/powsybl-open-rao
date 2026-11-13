### Introduction

These are the documentation sources for OpenRAO.  
Please keep them up-to-date with your developments.  
They are published on https://powsybl.readthedocs.org/projects/openrao and pull requests are built and previewed automatically.

### Readthedocs & Sphinx
The website is hosted on [readthedocs](https://readthedocs.org/). The build workflow requires a configuration file: 
[.readthedocs.yml](./.readthedocs.yaml). This platform presents many advantages,
thanks to its workflow of automatic branch/tag building & publication:
- Multiple versions are activated: you can browse different versions of the documentation for different releases of OpenRAO
- Pull requests are built automatically and the build status is reported in the PR's checks (["Build documentation" workflow](../.github/workflows/build_doc.yml)).  
  Reviewers have access to the resulting documentation preview in order to make reviewing it easier.

HTML content of the website is built automatically from source files written in [Markdown](https://fr.wikipedia.org/wiki/Markdown),
using [Sphinx](https://www.sphinx-doc.org/).  
Sphinx build needs a configuration file: [conf.py](./conf.py). 
It contains all customization options for the website (for example, the theme used is [furo](https://pradyunsg.me/furo/)).
Here is the most common cases when you'll need to tweak this file:
- If you want to customize the theme's behaviour, you will probably have to change the conf.py file (refer to the
  used theme's documentation first)
- If you want to customize sphinx's behaviour, add plugins, css, etc., you need to change this file (refer to the
  Sphinx doc first)

### Customization options

Sometimes, the possibilities offered by Sphinx & the used theme can be limiting. Sphinx offers the possibility to use
custom html & css code. Currently, two methods are used:
- Custom CSS: the [styles.css](_static/styles/styles.css) files contains custom CSS classes (read more about this [here](https://docs.readthedocs.io/en/stable/guides/adding-custom-css.html)).
  Feel free to add classes to it if you find it necessary.
- HTML overrides: the [_templates](_templates) directory contains HTML files that override objects inherited from Sphinx
  or its theme (read more about this [here](https://www.sphinx-doc.org/en/master/development/theming.html#templating)).
  You can use this method when other, lighter methods (i.e. tweaking mkdocs.yml & CSS styles) are not enough.

### Using Mermaid
You can draw diagrams using the Mermaid plugin.  
In markdown, use the '```{mermaid}' fields.  
~~~mermaid
graph TD
    A[Hard] -->|Text| B(Round)
    B --> C{Decision}
    C -->|One| D[Result 1]
    C -->|Two| E[Result 2]
~~~

### Linking to source code

Links from the documentation to Java sources must follow the **published version** of the
website: the `v7.5.0` doc must link to the `v7.5.0` sources, not to `main`.  
`conf.py` computes the GitHub reference from the `READTHEDOCS_VERSION` environment variable
(set by readthedocs): tag builds use the exact tag (`v7.5.0`, `v7.4.0`, …), while
`latest` / `stable` and pull-request previews (`pr/<N>`) fall back to `main`.  
On a local build (no `READTHEDOCS_VERSION`), links point to `main`.

To link a source file (or a directory), use the `{src}` MyST role (registered in
`conf.py`) with a path **relative to the root of the repository**:
~~~markdown
See {src}`data/crac/crac-api/src/main/java/com/powsybl/openrao/data/crac/api/parameters/CracCreationParameters.java`.

With a display label:
OpenRAO's {src}`CracCreationParameters<data/crac/crac-api/src/main/java/com/powsybl/openrao/data/crac/api/parameters/CracCreationParameters.java>`.

For a directory (GitHub `tree` URL):
The {src}`Monitoring<monitoring>` package.
~~~

Rules:
- **Do not hardcode** `https://github.com/powsybl/powsybl-open-rao/blob/main/...` in the
docs: it drifts after the next release.
- **Do not use relative links** (`../../data/...`): the file exists in the repository, so
the `broken_links_check` CI job passes, but the source tree is *not* published on
readthedocs and the link 404s on the website.
- `{src}` links are resolved **at build time**, so the `broken_links_check` CI job does
not see them: verify links in the pull-request's readthedocs preview ("Build documentation"
check).
- A plain `[label](url)` Markdown link cannot be versioned: neither `myst_substitutions`
nor `rst_epilog` substitute inside a link's URL, which is why the `{src}` role exists.
To test a specific version locally, prefix the build, e.g.
`READTHEDOCS_VERSION=v7.5.0 sphinx-build -a docs ./build-docs`.

### Building the website locally

When modifying the website content, you can easily preview the result on your PC by navigating to the root of the
project and running:
~~~bash
pip install -r docs/requirements.txt
sphinx-build -a docs ./build-docs
~~~
Then open `build-docs/index.html` in your browser.
  
### External links
- [Readthedocs documentation](https://docs.readthedocs.io/en/stable/index.html)
- [Sphinx documentation](https://www.sphinx-doc.org/en/master/)
- Sphinx uses MyST to parse MD & RST files: [MyST documentation](https://mystmd.org/guide)
- [Furo documentation](https://pradyunsg.me/furo/)
