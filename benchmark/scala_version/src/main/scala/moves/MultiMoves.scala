import java.util.HashMap;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import scala.actors.Actor
import scala.actors.Actor._

object MultiMoves {

	val BLACK = 'b'
	val WHITE = 'w'
	val EMPTY = 'e'

	val LEFT_DIRECTION = -1

	val RIGHT_DIRECTION = 1

	abstract class AnalyzeResult
	case object Solution extends AnalyzeResult
	case object SearchFurther extends AnalyzeResult
	case object HasBlocker extends AnalyzeResult

	def moves(str:String):Int = {
		moves(str, Runtime.getRuntime().availableProcessors())
	}

	def moves(array:String, nrOfWorkers:Int) =    
    moves(array, nrOfWorkers, new ConcurrentHashMap[String, Boolean]())


	def moves(array:String, nrOfWorkers:Int, cache:AbstractMap[String, Boolean]) = {
    val workers = (1 to nrOfWorkers).map(startWorker(cache)).toArray
    moves(List(array), -1, workers, nrOfWorkers, 0, cache)
	}


	abstract sealed class StrinContainer
	case object Empty extends StrinContainer
	case class ConsL(sc:StrinContainer, l:List[String]) extends StrinContainer

	def moves(arrays:StrinContainer, 
						movesSoFar:Int, 
						workers:Actor, 
						nrOfWorkers:Int, 
						workPackagesLeft:Int, 
						cache:AbstractMap[String, Boolean]) =  
		if(workPackagesLeft==0) {
    	if (isEmpty(arrays)) {
	    	sendMsgToWorkers(workers, nrOfWorkers, 0, "stop")
 	    	reciveMsgFromWorkers(nrOfWorkers, "stopped")
	    	-1;
			else {
	    	divideWorkToWorkers(arrays, workers, nrOfWorkers, cache)
	    	moves([], movesSoFar + 1, workers, nrOfWorkers, nrOfWorkers, cache)
			}
		} else {
    	receive {
				case "solution_found" => {
	    		sendMsgToWorkers(workers, nrOfWorkers, 0, "stop")
 	    		reciveMsgFromWorkers(NrOfWorkers, "stopped")
	    		movesSoFar
				}
				case ("work_done", newWork) =>
	    		calculateMoves(ConsL(newWork, arrays), movesSoFar, workers, nrOfWorkers, workPackagesLeft - 1, cache)
			}
		}
    

	def divideWorkToWorkers(arrays:StrinContainer, Workers, NrOfWorkers, Cache) ->
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

	

	def analyze(array:String):AnalyzeResult = {
		if(hasStartBlocker(array) || hasEndBlocker(array)) HasBlocker else analyze(array, 0, true) 
	}

abstract class AnalyzePosResult
case object SolutionPos  extends AnalyzeResult
case object NotSolutionPos  extends AnalyzeResult
case object BlockerPos  extends AnalyzeResult

	def analyze(array:String, fromPos:Int, isSolutionToPos:Boolean):AnalyzeResult  = {
		if(fromPos == array.size)
			if(isSolutionToPos) Solution else SearchFurther
		else 
			analyzePos(array, fromPos) match {
				case SolutionPos =>
					analyze(array, fromPos + 1, isSolutionToPos)
				case NotSolutionPos =>
					analyze(array, fromPos + 1, false)
				case BlockerPos =>
					HasBlocker
			}
	}

	def analyzePos(array:String, pos:Int) = {
		val end = array.size - 1
		(pos, end) match {
			case (n1, n2) if(n1 == n2) =>
				SolutionPos
			case (0, _) =>
				solutionPos(array, pos)
			case (_,_) =>
				if(hasBlocker(array, pos)) BlockerPos else solutionPos(array, pos)				
		}
	}

	def solutionPos(array:String, pos:Int) = (array.charAt(pos), array.charAt(pos + 1)) match {
		case (BLACK, EMPTY) =>
			NotSolutionPos
		case (BLACK, WHITE) =>
			NotSolutionPos
		case (EMPTY, WHITE) =>
			NotSolutionPos
		case _ =>
			SolutionPos
	}

	def hasBlocker(array:String, pos:Int) = array.charAt(pos) match {
		case BLACK =>
			if ((array.size - pos) < 3) 
				false 
			else
				(array.charAt(pos -1) == BLACK) &&
		    (array.charAt(pos + 1) == WHITE) &&
		    (array.charAt(pos + 2) == WHITE)
		case _ =>
			false
				  
	}

	def hasStartBlocker(array:String) =
		if (array.size < 3)
			false
		else
	  	(array.charAt(0) == BLACK) &&
			(array.charAt(1) == WHITE) &&
			(array.charAt(2) == WHITE)			


	def hasEndBlocker(array:String) = {
		val size = array.size
		if (size < 3)
			false
		else
	  	(array.charAt(size -3) == BLACK) &&
			(array.charAt(size -2) == BLACK) &&
			(array.charAt(size -1) == WHITE)	
	}

	def allNextStepArrays(array:String, cache:AbstractMap[String, Boolean]):List[String] =
		allNextStepArrays(array, 0, cache)


	def allNextStepArrays(array:String, currentPos:Int, cache:AbstractMap[String, Boolean]):List[String] =
		if(currentPos == array.size)
			Nil
		else {
			val moveArray = array.charAt(currentPos) match {
				 case WHITE =>
				 	moveInDirection(array, currentPos, LEFT_DIRECTION)
				 case BLACK =>
				 	moveInDirection(array, currentPos, RIGHT_DIRECTION)
				 case EMPTY=>
				 	null
			}
			if(moveArray == null)
				allNextStepArrays(array, currentPos + 1, cache)
			else {
				val alreadyInserted = cache.put(moveArray, true)
				if(alreadyInserted)
					allNextStepArrays(array, currentPos + 1, cache)
				else{
					moveArray::allNextStepArrays(array, currentPos + 1, cache)
				}
			}
		}


	def moveInDirection(array:String, currentPos:Int, direction:Int) = {
		val nextPos = currentPos + 1*direction
		move(currentPos, nextPos, array) match {
			case null =>{
				val jumpPos = currentPos + 2*direction
				move(currentPos, jumpPos, array)
			}
			case moveArray =>
				moveArray
		}
	}

	def move(currentPos:Int, moveToPos:Int, array:String) =
		if((moveToPos < 0) || (moveToPos >= array.size))
			null
		else if (array.charAt(moveToPos) ==EMPTY){
		    val currentPosOccupant = array.charAt(currentPos)
		    val str = new StringBuffer(array)
		    str.setCharAt(moveToPos, currentPosOccupant)
		    str.setCharAt(currentPos, EMPTY)
				str.toString			
		} else null

	def main(args:Array[String]){
     println(moves(""))
     println(moves("www"))
     println(moves("bee"))
     println(moves("bebw"))
     println(moves("beebw"))
     println(moves("wewebbw"))
     println(moves("bbeww"))
     println(moves("bebwew"))
     println(moves("ebbeewwb"))
     println(moves("bebbeww"))
     println(moves("bebbewww"))
     println(moves("bebwwebww"))
     println(moves("bebwbewbbwew"))
     println(moves("bebbebeweewew"))
     println(moves("bebbwbwbbwew"))
     println(moves("bebebeewewewew"))
     println(moves("bewebeeewewebewwe"))
     println(moves("bebebeeewewewew"))
     println(moves("bewebeeewbwebewwe"))
     println(moves("bebebbeeeewwwbw"))
     println(moves("bwwebbewwebbbwbwwwebbw"))
     println(moves("bebebeeewewewewwe"))
     println(moves("bebebeeewewbewwewe"))
	}

}