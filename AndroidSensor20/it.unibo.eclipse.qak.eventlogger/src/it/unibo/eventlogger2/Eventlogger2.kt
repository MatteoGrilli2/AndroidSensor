/* Generated by AN DISI Unibo */ 
package it.unibo.eventlogger2

import it.unibo.kactor.*
import alice.tuprolog.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
	
class Eventlogger2 ( name: String, scope: CoroutineScope ) : ActorBasicFsm( name, scope){
 	
	override fun getInitialState() : String{
		return "s0"
	}
		
	override fun getBody() : (ActorBasicFsm.() -> Unit){
		return { //this:ActionBasciFsm
				state("s0") { //this:State
					action { //it:State
						println("Starting up event logger 2")
					}
					 transition( edgeName="goto",targetState="waitForEvents", cond=doswitch() )
				}	 
				state("waitForEvents") { //this:State
					action { //it:State
						println("Logger 2 waiting for events...")
					}
					 transition(edgeName="t02",targetState="printEvent",cond=whenEvent("androidsensor"))
				}	 
				state("printEvent") { //this:State
					action { //it:State
						if( checkMsgContent( Term.createTerm("androidsensor(X)"), Term.createTerm("androidsensor(X)"), 
						                        currentMsg.msgContent()) ) { //set msgArgList
								println("Logger 2!")
						}
					}
					 transition(edgeName="t03",targetState="printEvent",cond=whenEvent("androidsensor"))
				}	 
			}
		}
}
