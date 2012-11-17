#set term x11
#set output
set term post eps
set output 'etsmp_graph.eps'
#set terminal png
#set output 'benchmark.png'
set title "ETS Sub-Table Benchmark on Bulldozer (64 Cores)"
set xlabel "Number of Schedulers"
set ylabel "Benchmark Time"
#set key below
set xrange [1:64]
#f(x)=49369567
plot "DEFARGS.sched.time" using 11 title 'ets, 256 locks' with linespoints,\
     "DEFARGS.sched.time" using 37 title 'etsm, 16 locks' with linespoints,\
     "DEFARGS.sched.time" using 21 title 'etsm, 32 locks' with linespoints,\
     "DEFARGS.sched.time" using 39 title 'etsmp, 16 locks' with linespoints,\
     "DEFARGS.sched.time" using 23 title 'etsmp, 32 locks' with linespoints