-module(multi_4).

-compile(export_all).
%-export([moves/1]).

-define(WHITE, 119).
-define(EMPTY, 101).
-define(BLACK, 98).
-define(LEFT_DIRECTION, -1).
-define(RIGHT_DIRECTION, 1).


moves(String) ->    
    moves(String, erlang:system_info(schedulers)).

moves(String, NrOfWorkers) ->    
    Array = array:from_list(String),
    calculate_moves(Array, NrOfWorkers, [set, public, {write_concurrency,true}, {read_concurrency,true}]).

moves(String, NrOfWorkers, ETSOptions) ->    
    Array = array:from_list(String),
    calculate_moves(Array, NrOfWorkers, ETSOptions).


calculate_moves(Array, NrOfWorkers, ETSOptions) ->
    Cache = ets:new(cache, ETSOptions),
    NrOfWorkers = NrOfWorkers,
    Workers = array:from_list([start_worker(Cache) || _ <- lists:seq(1, NrOfWorkers)]),
    Result = calculate_moves([Array], -1, Workers, NrOfWorkers, 0, Cache),
    ets:delete(Cache), 
    Result.

calculate_moves(Arrays, MovesSoFar, Workers, NrOfWorkers, 0, Cache) ->
    IsEmpty = is_empty(Arrays),
    case IsEmpty of
	true ->
	    send_msg_to_workers(Workers, NrOfWorkers, 0, stop),
 	    recive_msg_from_workers(NrOfWorkers, stopped),
	    -1;
	false ->
	    divide_work_to_workers(Arrays, Workers, NrOfWorkers, Cache),
	    calculate_moves([], MovesSoFar + 1, Workers, NrOfWorkers, NrOfWorkers, Cache)
    end;
calculate_moves(Arrays, MovesSoFar, Workers, NrOfWorkers, WorkPackagesLeft, Cache) ->
    receive
	solution_found ->
	    send_msg_to_workers(Workers, NrOfWorkers, 0, stop),
 	    recive_msg_from_workers(NrOfWorkers, stopped),
	    MovesSoFar;
	{work_done, NewWork} ->
	    calculate_moves([NewWork, Arrays], MovesSoFar, Workers, NrOfWorkers, WorkPackagesLeft - 1, Cache)
    end.


divide_work_to_workers(WorkList, Workers, NrOfWorkers, Cache) ->
    divide_work_to_workers(WorkList, Workers, NrOfWorkers, 0, Cache).

divide_work_to_workers(Arrays, Workers, NrOfWorkers, CurrentWorker, Cache) ->
    case NrOfWorkers =:= CurrentWorker of
	true ->
	    divide_work_to_workers(Arrays, Workers, NrOfWorkers, 0, Cache);
	false ->
	    R = get_next_and_rest(Arrays),
	    case R of
		empty ->
		    send_msg_to_workers(Workers, NrOfWorkers, 0, level_completed);
		{Work, RemainingWork} ->
		    array:get(CurrentWorker, Workers) ! {do_work, Work},
		    divide_work_to_workers(RemainingWork, Workers, NrOfWorkers, CurrentWorker + 1, Cache)
	    end
    end.
	
	
send_msg_to_workers(Workers, NrOfWorkers, CurrentWorker, Msg) ->
     case NrOfWorkers =:= CurrentWorker of
	true ->
	    ok;
	false ->
	    array:get(CurrentWorker, Workers) ! Msg,
	    send_msg_to_workers(Workers, NrOfWorkers, CurrentWorker + 1, Msg)
    end.


recive_msg_from_workers(NrOfWorkers, Msg) ->
    receive
	Msg ->
	    recive_msg_from_workers(NrOfWorkers -1, Msg);
	_ ->
	    recive_msg_from_workers(NrOfWorkers, Msg)
    after 0 ->
	    case NrOfWorkers =< 0 of
		true ->
		    ok;
		false ->
		    recive_msg_from_workers(NrOfWorkers, Msg)
	    end	    
    end.


is_empty(Arrays) ->
    get_next_and_rest(Arrays) == empty.


get_next_and_rest([]) ->
    empty;
get_next_and_rest([[]|Rest]) ->
    get_next_and_rest(Rest);
get_next_and_rest([[E|RestIn]|RestO]) ->
    get_next_and_rest([E, RestIn|RestO]);
get_next_and_rest([E|Rest]) ->
    {E, Rest}.
    

start_worker(Cache) ->
    spawn(multi_4, worker, [Cache, [], self()]).


worker(Cache, WorkDoneSoFar, ResultReciver) ->
    receive
	stop ->
	    ResultReciver ! stopped;
	{do_work, Step} ->
	    Work = do_work(Step, Cache),
	    case Work of
		solution_found ->
		    ResultReciver ! solution_found,
		    worker(Cache, [], ResultReciver);
		blocker ->
		    worker(Cache, WorkDoneSoFar, ResultReciver);
		_ ->
		    worker(Cache, [Work, WorkDoneSoFar], ResultReciver)
	    end;
	level_completed ->
	    ResultReciver ! {work_done, WorkDoneSoFar},
	    worker(Cache, [], ResultReciver)
    end.

do_work(Step, Cache) ->
    AnalyzeResult = analyze(Step),
    case AnalyzeResult of
	solution ->
	    solution_found;
	search_further ->
	    all_next_step_arrays(Step, Cache);
	has_blocker ->
	    blocker
    end.
	    

analyze(Array) ->
    HasStartOrEndBlocker = has_start_blocker(Array) 
	orelse has_end_blocker(Array),
    case HasStartOrEndBlocker of
	true ->
	    has_blocker;
	false ->
	    analyze(Array, 0, true)
    end.

analyze(Array, FromPos, IsSolutionToPos) ->
    Size = array:size(Array),
    case FromPos =:= Size of
	true ->
	    case IsSolutionToPos of
		true ->
		    solution;
		false ->
		    search_further
		end;
	false ->
	    case analyze_pos(Array, FromPos) of
		solution_pos ->
		    analyze(Array, FromPos + 1, IsSolutionToPos);
		not_solution_pos ->
		    analyze(Array, FromPos + 1, false);
		blocker_pos ->
		    has_blocker
	    end	
    end.

analyze_pos(Array, Pos) ->
    End = array:size(Array) - 1,
    case {Pos, End} of
	{N, N} ->
	    solution_pos;
	{0, _} ->
	    solution_pos(Array, Pos);
	{_, _} ->
	    HasBlocker = has_blocker(Array,Pos),
	    case HasBlocker of
		true ->
		    blocker_pos;
		false ->
		    solution_pos(Array, Pos)
	    end
    end.

solution_pos(Array, Pos) ->
    O1 = array:get(Pos, Array),
    O2 = array:get(Pos + 1, Array),
    case {O1, O2} of
	{?BLACK, ?EMPTY} ->
	    not_solution_pos;
	{?BLACK, ?WHITE} ->
	    not_solution_pos;
	{?EMPTY, ?WHITE} ->
	    not_solution_pos;
	_ ->
	    solution_pos
    end.
			
has_blocker(Array,Pos) ->
    Occupant = array:get(Pos, Array),
    case Occupant of
	?BLACK ->
	    Size = array:size(Array),
	    case (Size - Pos) < 3 of
		true ->
		    false;
		false ->
		    (array:get(Pos -1, Array) =:= ?BLACK) and
		    (array:get(Pos + 1, Array) =:= ?WHITE) and
		    (array:get(Pos + 2, Array) =:= ?WHITE)
	    end;
	_ ->
	    false
    end.

has_start_blocker(Array) ->
    Size = array:size(Array),
    case Size < 3 of
	true ->
	    false;
	false ->
	    (array:get(0, Array) =:= ?BLACK) and
		(array:get(1, Array) =:= ?WHITE) and
		(array:get(2, Array) =:= ?WHITE)
    end.

has_end_blocker(Array) ->
    Size = array:size(Array),
    case Size < 3 of
	true ->
	    false;
	false ->
	    (array:get(Size -3, Array) =:= ?BLACK) and
		(array:get(Size -2, Array) =:= ?BLACK) and
		(array:get(Size -1, Array) =:= ?WHITE)
    end.


all_next_step_arrays(Array, Cache) ->
    all_next_step_arrays(Array, 0, Cache).

all_next_step_arrays(Array, CurrentPos, Cache) ->
    IsEnd = CurrentPos =:= array:size(Array),
    case IsEnd of
	true ->
	    [];
	false ->
	    Occupant = array:get(CurrentPos, Array),
	    MoveArray = 
		case Occupant of 
		    ?WHITE -> 
			move_in_direction(Array, CurrentPos, ?LEFT_DIRECTION);
		    ?BLACK -> 
			move_in_direction(Array, CurrentPos, ?RIGHT_DIRECTION);
		    ?EMPTY -> 
			none
		end,
	    case MoveArray of
		none ->
		    all_next_step_arrays(Array, CurrentPos + 1, Cache);
		_ ->
		    IsFound = ets:member(Cache, MoveArray),
		    case IsFound of
			false ->
			    Inserted = ets:insert_new(Cache, {MoveArray}),
			    case Inserted of
				true ->
				    [MoveArray|all_next_step_arrays(Array, CurrentPos + 1, Cache)];
				false ->
				    all_next_step_arrays(Array, CurrentPos + 1, Cache)
			    end;
			true ->
			    all_next_step_arrays(Array, CurrentPos + 1, Cache)
		    end
	    end	    
    end.


move_in_direction(Array, CurrentPos, Direction) ->
    NextPos = CurrentPos + 1*Direction,
    case move(CurrentPos, NextPos, Array) of
	none ->
	    JumpPos = CurrentPos + 2*Direction,
	    move(CurrentPos, JumpPos, Array);
	MoveArray ->
	    MoveArray
    end.    

move(CurrentPos, MoveToPos, Array) ->
    IsMoveOut = (MoveToPos < 0) or (MoveToPos >= array:size(Array)),
    case IsMoveOut of
	true ->
	    none;
	false ->
	    MoveToPosOccupant = array:get(MoveToPos, Array),
	    case MoveToPosOccupant =:= ?EMPTY of
		true ->
		    CurrentPosOccupant = array:get(CurrentPos, Array),
		    Array2 = array:set(MoveToPos, CurrentPosOccupant, Array),
		    array:set(CurrentPos, ?EMPTY, Array2);
		false ->
		    none
	    end	
    end.	    


test() ->
     0 = moves(""),
     0 = moves("www"),
     2 = moves("bee"),
     4 = moves("bebw"),
     5 = moves("beebw"),
    -1 = moves("wewebbw"),
     8 = moves("bbeww"),
     9 = moves("bebwew"),
    11 = moves("ebbeewwb"),
    12 = moves("bebbeww"),
    17 = moves("bebbewww"),
    17 = moves("bebwwebww"),
    27 = moves("bebwbewbbwew"),
    32 = moves("bebbebeweewew"),
    -1 = moves("bebbwbwbbwew"),
    36 = moves("bebebeewewewew"),
    38 = moves("bewebeeewewebewwe"),
    40 = moves("bebebeeewewewew"),
    40 = moves("bewebeeewbwebewwe"),
    42 = moves("bebebbeeeewwwbw"),
    %-1 = moves("bwwebbewwebbbwbwwwebbw"),
    %49 = moves("bebebeeewewewewwe"),
    %53 = moves("bebebeeewewbewwewe"),
    ok.


speed_test(NumberOfWorkers, ETSOptions) ->
    ThreeTests = ["bebebbeeeewwwbw", "bebebeeewewewewwe"], %"bebebeeewewbewwewe"],
    {Times, _} = lists:unzip([timer:tc(multi_4, moves, [S, NumberOfWorkers, ETSOptions]) || S <- ThreeTests]),
    lists:sum(Times).

benchmark(FromNumberOfWorkers, ToNumberOfWorkers, ETSOptionsDesc, NrOfRunsPerTest) ->
    {ETSOptions, Desc} = ETSOptionsDesc,
    {ok, FileId} = file:open(lists:flatten(io_lib:format("benchmark_~p.dat", [Desc])), [append]),
    io:fwrite(FileId, "#Benchmark: ~w to ~w workers~n",[FromNumberOfWorkers, ToNumberOfWorkers]),
    io:fwrite(FileId, "#ETS Options ~p ~n",[ETSOptions]),
    lists:foreach(
      fun(NrOfWorkers) ->
	      Times = [speed_test(NrOfWorkers, ETSOptions) || _ <- lists:seq(1,NrOfRunsPerTest)],
	      Time = lists:sum(Times) / NrOfRunsPerTest,
	      io:fwrite(FileId,"~w ~w~n", [NrOfWorkers, Time])
      end, lists:seq(FromNumberOfWorkers, ToNumberOfWorkers)),
    file:close(FileId).

benchmark_ets_options(FromNumberOfWorkers, ToNumberOfWorkers, ETSOptionsList, NrOfRunsPerTest) ->
    lists:foreach(fun(ETSOptions) -> 
			  benchmark(FromNumberOfWorkers, ToNumberOfWorkers, ETSOptions, NrOfRunsPerTest)	  
		  end, ETSOptionsList).

bulldozer_benchmark() ->
    ETSOptionsList = [{[set,public,{write_concurrency,true},{read_concurrency,true}], set_w_r},
		      {[set,public,{write_concurrency,true},{read_concurrency,false}], set_w},
		      {[set,public,{write_concurrency,false},{read_concurrency,true}], set_r},
		      {[set,public,{write_concurrency,false},{read_concurrency,false}], set},
		      {[ordered_set,public,{write_concurrency,true},{read_concurrency,true}], oset_w_r},
		      {[ordered_set,public,{write_concurrency,true},{read_concurrency,false}], oset_w},
		      {[ordered_set,public,{write_concurrency,false},{read_concurrency,true}], oset_r},
		      {[ordered_set,public,{write_concurrency,false},{read_concurrency,false}], oset}],
    benchmark_ets_options(1, 30, ETSOptionsList, 1).

start() ->
    bulldozer_benchmark().
    
    

    
