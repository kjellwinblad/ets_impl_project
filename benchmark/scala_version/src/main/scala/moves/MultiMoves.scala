package moves

import java.util.HashMap;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import scala.actors.Actor
import scala.actors.TIMEOUT
import scala.actors.Actor._
import scala.annotation.tailrec

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

	def moves(array:String, nrOfWorkers:Int):Int =    
    moves(array, nrOfWorkers, new ConcurrentHashMap[String, Boolean]())


	def moves(array:String, nrOfWorkers:Int, cache:AbstractMap[String, Boolean]):Int = {
    val workers = Array.fill(nrOfWorkers){startWorker(cache)}
    moves(ConsL(List(array), Empty), -1, workers, nrOfWorkers, 0, cache)
	}


	abstract sealed class StrinContainer
	case object Empty extends StrinContainer
	case class ConsL(l:List[String], sc:StrinContainer) extends StrinContainer
	case class ConsC(sc1:StrinContainer, sc2:StrinContainer) extends StrinContainer

	@tailrec
	def moves(arrays:StrinContainer, 
						movesSoFar:Int, 
						workers:Array[Actor], 
						nrOfWorkers:Int, 
						workPackagesLeft:Int, 
						cache:AbstractMap[String, Boolean]):Int =  
		if(workPackagesLeft==0) {
    	if (isEmpty(arrays)) {
	    	sendMsgToWorkers(workers, nrOfWorkers, 0, "stop")
 	    	reciveMsgFromWorkers(nrOfWorkers, "stopped")
	    	-1;
    	} else {
	    	divideWorkToWorkers(arrays, workers, nrOfWorkers, cache)
	    	moves(Empty, movesSoFar + 1, workers, nrOfWorkers, nrOfWorkers, cache)
			}
		} else 
    	receive { case m => m } match {
				case "solution_found" => {
	    		sendMsgToWorkers(workers, nrOfWorkers, 0, "stop")
 	    		reciveMsgFromWorkers(nrOfWorkers, "stopped")
	    		movesSoFar
				}
				case ("work_done", newWork:StrinContainer) => 
	    		moves(ConsC(newWork, arrays), movesSoFar, workers, nrOfWorkers, workPackagesLeft - 1, cache)
			}
		
    

	def divideWorkToWorkers(arrays:StrinContainer, workers:Array[Actor], nrOfWorkers:Int, cache:AbstractMap[String, Boolean]):Unit =
    divideWorkToWorkers(arrays, workers, nrOfWorkers, 0, cache)

	@tailrec
	def divideWorkToWorkers(arrays:StrinContainer, workers:Array[Actor], nrOfWorkers:Int, currentWorker:Int, cache:AbstractMap[String, Boolean]):Unit =
    if(nrOfWorkers == currentWorker)
			divideWorkToWorkers(arrays, workers, nrOfWorkers, 0, cache)
		else 
			getNextAndRest(arrays) match {
	    	case Empty =>
		    	sendMsgToWorkers(workers, nrOfWorkers, 0, "level_completed")
				case ConsL(List(work), remainingWork) => {
		    	workers(currentWorker) ! ("do_work", work)
		    	divideWorkToWorkers(remainingWork, workers, nrOfWorkers, currentWorker + 1, cache)
				}
				case _  => throw new Exception("Not expected result")
		}

	@tailrec
	def sendMsgToWorkers(workers:Array[Actor], nrOfWorkers:Int, currentWorker:Int, msg:String):Unit =
		if(nrOfWorkers != currentWorker){
	  	workers(currentWorker) ! msg
	    sendMsgToWorkers(workers, nrOfWorkers, currentWorker + 1, msg)
		}

	@tailrec
	def reciveMsgFromWorkers(nrOfWorkers:Int, msg:String):Unit = {
    receiveWithin(0) {case m => m} match {
			case msg:String =>
	    	reciveMsgFromWorkers(nrOfWorkers -1, msg);
	    case TIMEOUT if(nrOfWorkers <= 0) =>
	    	()
	   	case _ =>
	    	reciveMsgFromWorkers(nrOfWorkers, msg)
    }
     
	    
	}


	def isEmpty(arrays:StrinContainer) =
    (getNextAndRest(arrays) == Empty)

	@tailrec
	def getNextAndRest(l:StrinContainer):StrinContainer = l match {
		case Empty =>
			Empty
		case ConsL(Nil, rest) =>
			getNextAndRest(rest)
		case ConsL(e::restIn,rest) =>
			 ConsL(List(e), ConsL(restIn, rest))
		case ConsC(Empty,rest) =>
			getNextAndRest(rest)
		case ConsC(ConsC(c1,c2),rest) =>
			getNextAndRest(ConsC(c1,ConsC(c2, rest)))
		case ConsC(ConsL(l, rest1),rest2) =>
			getNextAndRest(ConsL(l,ConsC(rest1,rest2)))
	}

	

	def startWorker(cache:AbstractMap[String, Boolean]) ={
    val resultReciver = self
    actor {
    	worker(cache, Empty, resultReciver)
    }
	}

	@tailrec
	def worker(cache:AbstractMap[String, Boolean], workDoneSoFar:StrinContainer, resultReciver:Actor):Unit =
    receive {case m => m} match {
			case "stop" =>
	    	resultReciver ! "stopped"
			case ("do_work", step:String) => {
	    	val workResult = doWork(step, cache)
	    	(workResult: @unchecked) match {
					case "solution_found" => {
		    		resultReciver ! "solution_found"
		    		worker(cache, Empty, resultReciver)
					}
					case "blocker" =>  
		    		worker(cache, workDoneSoFar, resultReciver);
					case work:List[String]  =>
		    		worker(cache, ConsL(work, workDoneSoFar), resultReciver)
    		}
    	}
			case "level_completed" => {
	    	resultReciver ! ("work_done", workDoneSoFar)
	    	worker(cache, Empty, resultReciver)
			}
    }

	def doWork(step:String, cache:AbstractMap[String, Boolean]) = 
    analyze(step) match {
    	case Solution =>
	    	"solution_found"
			case SearchFurther =>
	    	allNextStepArrays(step, cache);
			case HasBlocker =>
	    	"blocker"
		}

	

	def analyze(array:String):AnalyzeResult = {
		if(hasStartBlocker(array) || hasEndBlocker(array)) HasBlocker else analyze(array, 0, true) 
	}

abstract class AnalyzePosResult
case object SolutionPos  extends AnalyzeResult
case object NotSolutionPos  extends AnalyzeResult
case object BlockerPos  extends AnalyzeResult

	@tailrec
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
     /*println(moves(""))
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
     println(moves("bebebeeewewbewwewe"))*/

    def time[R](block: => R): R = {
    	val t0 = System.nanoTime()
    	val result = block    // call-by-name
   	 	val t1 = System.nanoTime()
    	println("Elapsed time: " + (t1 - t0) + "ns")
  	  result
		}

		time {MultiMoves.moves("bebebeeewewbewweweebe")}

		
	}

}