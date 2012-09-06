#set term x11
#set output
set term post eps
set output 'benchmark.eps'
#set terminal png
#set output 'benchmark.png'
set title "ETS Moves Benchmark on Bulldozer (64 Cores)"
f(x)=49369567
plot f(x) title 'serial',\
     "data/benchmark_set_w_r.dat" using 2 title 'set,w,r' with linespoints,\
     "data/benchmark_set_w.dat" using 2 title 'set,w' with linespoints,\
     "data/benchmark_set_r.dat" using 2 title 'set,r' with linespoints,\
     "data/benchmark_set.dat" using 2 title 'set' with linespoints,\
     "data/benchmark_oset_w_r.dat" using 2 title 'ordered_set,w,r' with linespoints,\
     "data/benchmark_oset_w.dat" using 2 title 'ordered_set,w' with linespoints,\
     "data/benchmark_oset_r.dat" using 2 title 'ordered_set,r' with linespoints,\
     "data/benchmark_oset.dat" using 2 title 'ordered_set' with linespoints
set xlabel "Number of Workers"
set ylabel "Benchmark Time"