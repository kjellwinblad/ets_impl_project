-module(single_1).

-compile(export_all).
%-export([moves/1]).

-define(WHITE, 119).
-define(EMPTY, 101).
-define(BLACK, 98).
-define(LEFT_DIRECTION, -1).
-define(RIGHT_DIRECTION, 1).


moves(String) ->    
    Array = array:from_list(String),
    Cache = ets:new(cache, [set, private]),
    Result = moves([Array], [], 0, Cache),
    ets:delete(Cache),
    Result.

moves([], [], _, _) ->
    -1;
moves([], StepsOnNextLevel, Level, Cache) ->
    moves(StepsOnNextLevel, [], Level + 1, Cache);
moves([Step|RemainingStepsOnLevel], StepsOnNextLevel, Level, Cache) ->
    AnalyzeResult = analyze(Step),
    case AnalyzeResult of
	solution ->
	    Level;
	search_further ->
	    NewStepsOnNextLevel = all_next_step_arrays(Step, Cache) ++ StepsOnNextLevel,
	    moves(RemainingStepsOnLevel, NewStepsOnNextLevel, Level, Cache);
	has_blocker ->
	    moves(RemainingStepsOnLevel, StepsOnNextLevel, Level, Cache)
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
		    Inserted = ets:insert_new(Cache, {MoveArray, true}),
		    case Inserted of
			true ->			    
			    [MoveArray|all_next_step_arrays(Array, CurrentPos + 1, Cache)];
			false ->
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
