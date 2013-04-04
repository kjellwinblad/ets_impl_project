.PHONY: clean distclean

all: report.pdf

report.pdf: report.tex report.bib
	echo repomake 
	cd ../benchmark ; \
	gnuplot < create_graph.gp ; \
	cp benchmark.eps ../report/benchmark.eps
	cd ../locking_bench  ; \
	gnuplot < create_etsmp_graph.gp ; \
	gnuplot < create_lock_level_graph.gp ; \
	gnuplot < create_lock_level_graph_sandy.gp ; \
	cp etsmp_graph.eps ../report/etsmp_graph.eps ; \
	cp lock_level_graph.eps ../report/lock_level_graph.eps ; \
	cp lock_level_graph_sandy.eps ../report/lock_level_graph_sandy.eps
	latexmk -pdf -e '$$pdflatex=q/pdflatex --shell-escape %O %S/' $*

clean:
	for ext in aux log toc lof lot dlog bbl blg out tpt fdb_latexmk nav snm vrb cache; do \
		$(RM) *.$$ext; \
	done

distclean: clean
	for ext in dvi ps ps.gz pdf; do \
		$(RM) *.$$ext; \
	done
