package moves

import java.util.HashMap;
import java.util.AbstractMap;

object SingleMoves {

	private val BLACK = 'b'
	private val WHITE = 'w'
	private val EMPTY = 'e'

	private val LEFT_DIRECTION = -1

	private val RIGHT_DIRECTION = 1

	private abstract sealed class AnalyzeResult
	private case object Solution extends AnalyzeResult
	private case object SearchFurther extends AnalyzeResult
	private case object HasBlocker extends AnalyzeResult

	def moves(str:String):Int = {
		moves(List(str), Nil, 0, new HashMap[String, Boolean]())
	}

	private def moves(stepsOnCurrentLevel:List[String], 
						stepsOnNextLevel:List[String], 
						level:Int, 
						cache:AbstractMap[String, Boolean]): Int = (stepsOnCurrentLevel, stepsOnNextLevel) match {
		case (Nil, Nil) =>
			-1
		case (Nil, stepsOnNextLevel) =>
			moves(stepsOnNextLevel, Nil, level + 1, cache)
		case (step::remainingStepsOnLevel, stepsOnNextLevel) => { 
			analyze(step) match {
				case Solution =>
					level
				case SearchFurther => {
					val newStepsOnNextLevel = allNextStepArrays(step, cache) ++ stepsOnNextLevel
					moves(remainingStepsOnLevel, newStepsOnNextLevel, level, cache)
				}
				case HasBlocker => {
					moves(remainingStepsOnLevel, stepsOnNextLevel, level, cache)
				}		
			}
		}	
	}

	private def analyze(array:String):AnalyzeResult = {
		if(hasStartBlocker(array) || hasEndBlocker(array)) HasBlocker else analyze(array, 0, true) 
	}

abstract sealed class AnalyzePosResult
private case object SolutionPos  extends AnalyzePosResult
private case object NotSolutionPos  extends AnalyzePosResult
private case object BlockerPos  extends AnalyzePosResult

	private def analyze(array:String, fromPos:Int, isSolutionToPos:Boolean):AnalyzeResult  = {
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

	private def analyzePos(array:String, pos:Int) = {
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

	private def solutionPos(array:String, pos:Int) = (array.charAt(pos), array.charAt(pos + 1)) match {
		case (BLACK, EMPTY) =>
			NotSolutionPos
		case (BLACK, WHITE) =>
			NotSolutionPos
		case (EMPTY, WHITE) =>
			NotSolutionPos
		case _ =>
			SolutionPos
	}

	private def hasBlocker(array:String, pos:Int) = array.charAt(pos) match {
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

	private def hasStartBlocker(array:String) =
		if (array.size < 3)
			false
		else
	  	(array.charAt(0) == BLACK) &&
			(array.charAt(1) == WHITE) &&
			(array.charAt(2) == WHITE)			


	private def hasEndBlocker(array:String) = {
		val size = array.size
		if (size < 3)
			false
		else
	  	(array.charAt(size -3) == BLACK) &&
			(array.charAt(size -2) == BLACK) &&
			(array.charAt(size -1) == WHITE)	
	}

	private def allNextStepArrays(array:String, cache:AbstractMap[String, Boolean]):List[String] =
		allNextStepArrays(array, 0, cache)


	private def allNextStepArrays(array:String, currentPos:Int, cache:AbstractMap[String, Boolean]):List[String] =
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


	private def moveInDirection(array:String, currentPos:Int, direction:Int) = {
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

	private def move(currentPos:Int, moveToPos:Int, array:String) =
		if((moveToPos < 0) || (moveToPos >= array.size))
			null
		else if (array.charAt(moveToPos) ==EMPTY){
		    val currentPosOccupant = array.charAt(currentPos)
		    val str = new StringBuffer(array)
		    str.setCharAt(moveToPos, currentPosOccupant)
		    str.setCharAt(currentPos, EMPTY)
				str.toString			
		} else null

	def mainA(args:Array[String]){
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