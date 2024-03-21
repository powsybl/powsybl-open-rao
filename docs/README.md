These are the documentation sources for OpenRAO.  
Please keep them up-to-date with your developments.  
They are published on powsybl-openrao.readthedocs.io and pull requests are built and previewed automatically.  

In order to build the docs locally, run the following commands:  
~~~bash
pip install -r docs/requirements.txt
sphinx-build -a docs ./build-docs
~~~
Then open `build-docs/index.html` in your browser.