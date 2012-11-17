#set term x11
#set output
set term post eps
set output 'lock_level_graph.eps'
#set terminal png
#set output 'benchmark.png'
set title "ETS Lock Level Benchmark on Bulldozer (64 Cores)"
set xlabel "Number of Schedulers"
set ylabel "Benchmark Time"
set key below
set xrange [1:64]
#f(x)=49369567
plot "DEFARGS.sched.time" using 35 title '16 locks' with linespoints,\
     "DEFARGS.sched.time" using 19 title '32 locks' with linespoints,\
     "DEFARGS.sched.time" using 27 title '64 locks' with linespoints,\
     "DEFARGS.sched.time" using 3 title '128 locks' with linespoints,\
     "DEFARGS.sched.time" using 11 title '256 locks' with linespoints