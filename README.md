Scalable ETS Investigation Project
==================================

This git repository contains files related to an investigation about the current Erlang ETS implementation.


Report Build Instructions
-------------------------

### Requirements

* git
* gnu make
* gnuplot
* latexmk
* pdflatex
* [revtex4 latex package](https://authors.aps.org/revtex4/)

On Debian and Ubuntu based systems it should be enough to run the following command:

`apt-get install texlive-full texlive-publishers latex-make gnuplot`

### Instructions

1. Run: `git clone git://github.com/kjellwinblad/ets_impl_project.git` 
2. Run: `cd ets_impl_project/report`
3. Run: `make`
4. The path to the compiled report should be `ets_impl_project/report/report.pdf`
