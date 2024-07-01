### Introduction

These are the documentation sources for OpenRAO.  
Please keep them up-to-date with your developments.  
They are published on https://powsybl.readthedocs.org/projects/openrao and pull requests are built and previewed automatically.

### Readthedocs & Sphinx
The website is hosted on [readthedocs](https://readthedocs.org/). The build workflow requires a configuration file: 
[.readthedocs.yml](./.readthedocs.yml). This platform presents many advantages,
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

### Using PlantUML
You can draw diagrams using the PlantUml plugin.  
In markdown, use the '~~~{plantuml}' fields.  
You can preview the diagrams in your markdown editor by removing the '{}'.
> Note: in order to render the diagrams [locally](#building-the-website-locally), you have to install plantuml:
> 'sudo apt install plantuml'

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
