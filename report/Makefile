.PHONY: clean distclean

all: report.pdf

report.pdf: report.tex report.bib
	echo repomake 
	cd ../benchmark ; \
	gnuplot < create_graph.gp ; \
	cp benchmark.eps ../report/benchmark.eps
	latexmk -pdf -e '$$pdflatex=q/pdflatex --shell-escape %O %S/' $*

clean:
	for ext in aux log toc lof lot dlog bbl blg out tpt fdb_latexmk nav snm vrb cache; do \
		$(RM) *.$$ext; \
	done

distclean: clean
	for ext in dvi ps ps.gz pdf; do \
		$(RM) *.$$ext; \
	done
