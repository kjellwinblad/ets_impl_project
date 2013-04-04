#set term x11
#set output
set term post eps color
set output 'lock_level_graph_sandy.eps'
#set terminal png
#set output 'benchmark.png'
set title "ETS Lock Level Benchmark on Sandy (32 cores, 2 hyperthreads/core)"
set xlabel "Number of Schedulers"
set ylabel "Benchmark Time"
set key below
set xrange [0:65]
#f(x)=49369567
plot "DEFARGS.sched.time.sandy" using 1:11 title '16 locks' with linespoints,\
     "DEFARGS.sched.time.sandy" using 1:3 title '32 locks' with linespoints,\
     "DEFARGS.sched.time.sandy" using 1:9 title '64 locks' with linespoints,\
     "DEFARGS.sched.time.sandy" using 1:5 title '128 locks' with linespoints,\
     "DEFARGS.sched.time.sandy" using 1:7 title '256 locks' with linespoints
