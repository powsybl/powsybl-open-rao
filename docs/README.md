### Introduction

These are the documentation sources for OpenRAO.  
Please keep them up-to-date with your developments.  
They are published on https://powsybl.readthedocs.org/projects/openrao and pull requests are built and previewed automatically.  

### Building the website locally

In order to build the docs locally, run the following commands:  
~~~bash
pip install -r docs/requirements.txt
sphinx-build -a docs ./build-docs
~~~
Then open `build-docs/index.html` in your browser.  
  
### Other information

Relevant links:
- We use Sphinx to build HTML pages: [Sphinx documentation](https://www.sphinx-doc.org/en/master/)
- Sphinx uses MyST to parse MD & RST files: [MyST documentation](https://mystmd.org/guide)
- We Furo theme for Sphinx: [Furo documentation](https://pradyunsg.me/furo/)
