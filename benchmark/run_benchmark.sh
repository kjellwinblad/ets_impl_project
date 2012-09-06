#!/bin/sh
LOG_DIR=~/test/moves/logs/
erl -eval 'c(multi_4, native)'
run_erl -daemon $LOG_DIR $LOG_DIR "erl -eval 'multi_4:start()'"
