//
//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

import java.lang.Math;
import java.util.regex.*;

class Brain extends Thread implements SensorInput
{
    //---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to krislet
    // - starts thread for this object
    public Brain(SendCommand krislet, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode)
    {
	m_timeOver = false;
	m_krislet = krislet;
	m_memory = new Memory();
	//m_team = team;
	m_side = side;
	// m_number = number;
	m_playMode = playMode;
	start();
    }


    //---------------------------------------------------------------------------
    // This is main brain function used to make decision
    // In each cycle we decide which command to issue based on
    // current situation. the rules are:
    //
    //	1. If you don't know where is ball then turn right and wait for new info
    //
    //	2. If ball is too far to kick it then
    //		2.1. If we are directed towards the ball then go to the ball
    //		2.2. else turn to the ball
    //
    //	3. If we dont know where is opponent goal then turn wait 
    //				and wait for new info
    //
    //	4. Kick ball
    //
    //	To ensure that we don't send commands to often after each cycle
    //	we waits one simulator steps. (This of course should be done better)

    // ***************  Improvements ******************
    // Allways know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    public void run() {
    	ObjectInfo object;
    	int lastGoalDirection = -1;
    	int lastBallDirection = -1;
    	boolean seeGoal = false;
    	boolean seeBall = false;
    	boolean ballClose = false;
    	int turnAngle = 10;
    	float ballDistance = 0;
    	float ballDirection = 0;
    	float goalDirection = 0;

    	// first put it somewhere on my side
    	if (Pattern.matches("^before_kick_off.*",m_playMode)) {
    		m_krislet.move( -Math.random()*52.5 , 34 - Math.random()*68.0 );
    	}

    	while(!m_timeOver) {
    		// Check to see if the goal is visible. 
    		// If so, record if the direction is positive or negative
    		// Used for the state based behaviour.
    		// Also record if goal was visible.
    		if( m_side == 'l' ) {
    			object = m_memory.getObject("goal r");
    		} else {
    			object = m_memory.getObject("goal l");
    		}

    		if (object != null) {
    			seeGoal = true;
    			goalDirection = object.m_direction;
    			if (goalDirection > 0) {
    				lastGoalDirection = 1;
    			} else {
    				lastGoalDirection = -1;
    			}
    		} else {
    			seeGoal = false;
    		}
    		
    		// Check to see if the ball is visible. 
    		// If so, record if the direction is positive or negative
    		// Used for the state based behaviour.
    		// Also record if ball is visible, far or close.
    		object = m_memory.getObject("ball");
    		if (object != null) {
    			seeBall = true;
    			
    			// Check the distance to the ball
    			ballDistance = object.m_distance;
    			if (ballDistance > 1.0) {
    				ballClose = false;
    			} else {
    				ballClose = true;
    			}
    			
    			ballDirection = object.m_direction;
    			if (ballDirection > 0) {
    				lastBallDirection = 1;
    			} else {
    				lastBallDirection = -1;
    			}
    		} else {
    			seeBall = false;
    			ballClose = false;
    		}
    		

    		// Perform the action
    		if (seeBall) {
    			if (ballClose) {
    				if (seeGoal) {
    					// seeBall && ballClose && seeGoal -> kick
    					m_krislet.kick(100, goalDirection);
    				} else {
    					// seeBall && ballClose && !seeGoal -> turn last known goal direction
    					m_krislet.turn(lastGoalDirection * turnAngle);
    				}
    			} else {
    				// seeBall && !ballClose -> dash
    				m_krislet.dash(10 * ballDistance);
    			}
    		} else {
    			// !seeBall -> turn last known ball direction
    			m_krislet.turn(lastBallDirection * turnAngle);
    		}
    		m_memory.waitForNewInfo();

		
    		// sleep one step to ensure that we will not send
    		// two commands in one cycle.
    		try {
    			Thread.sleep(2*SoccerParams.simulator_step);
    		} catch(Exception e) {}
	    }
    	m_krislet.bye();
    }


    //===========================================================================
    // Here are suporting functions for implement logic


    //===========================================================================
    // Implementation of SensorInput Interface

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info)
    {
	m_memory.store(info);
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message)
    {
    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message)
    {						 
	if(message.compareTo("time_over") == 0)
	    m_timeOver = true;

    }


    //===========================================================================
    // Private members
    private SendCommand	                m_krislet;			// robot which is controled by this brain
    private Memory			m_memory;				// place where all information is stored
    private char			m_side;
    volatile private boolean		m_timeOver;
    private String                      m_playMode;
    
}
