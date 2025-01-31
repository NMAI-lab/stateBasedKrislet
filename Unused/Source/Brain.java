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
    // current situation. The rules are:
    //
    //	1. If you don't know where ball is then turn right and wait for new info
    //
    //	2. If ball is too far to kick it then
    //		2.1. If we are directed towards the ball then go to the ball
    //		2.2. else turn to the ball
    //
    //	3. If we don't know where opponent goal is then turn
    //				and wait for new info
    //
    //	4. Kick ball
    //
    //	To ensure that we don't send commands too often after each cycle
    //	we wait one simulator step. (This of course should be done better)

    // ***************  Improvements ******************
    // Always know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    public void run()
    {
	ObjectInfo object;
	boolean kickedBall = false;
	ObjectInfo goal = null;
	int n = 0;
	int spinned = 0;
	
	// first put it somewhere on my side
	if(Pattern.matches("^before_kick_off.*",m_playMode))
	    m_krislet.move( -Math.random()*52.5 , 34 - Math.random()*68.0 );

	while( !m_timeOver )
	    {
		if( !kickedBall )
			{
			object = m_memory.getObject("ball");
			// Figure out which goal to aim at and remember
			// where it is if visible
			if ( m_side == 'l' )
				{
				if ( m_memory.getObject("goal r") != null)
					goal = m_memory.getObject("goal r");
				}
			else 
				{
				if ( m_memory.getObject("goal l") != null)
					goal = m_memory.getObject("goal l");
				}
			
			if( object == null )
			    {
				// If you don't know where ball is then find it
				m_krislet.turn(40);
				m_memory.waitForNewInfo();
			    }
			else if( object.m_distance > 1.0 )
			    {
				// If ball is too far then
				// turn to ball or 
				// if we have correct direction then go to ball
				if( object.m_direction != 0 )
				    m_krislet.turn(object.m_direction);
				else
				    m_krislet.dash(10*object.m_distance); 
			    }
			else 
			    {
				// We know where ball is and we can kick it
				// so look for goal
				if( m_side == 'l' )
				    object = m_memory.getObject("goal r");
				else
				    object = m_memory.getObject("goal l");
				
				// Turn towards where you last saw the goal
				if( object == null )
				    {
					if ( goal.m_direction < 0)
						{
						m_krislet.turn(-25);
						m_memory.waitForNewInfo();
						}
					else if (goal.m_direction > 0)
						{
						m_krislet.turn(25);
						m_memory.waitForNewInfo();
						}
				    }
				else
					{
				    m_krislet.kick(100, object.m_direction);
				    kickedBall = true;
					}
			    }
			}
		// Do a spin if you just kicked the ball
		else
			{
			while ( spinned < 360 )
				{
				m_krislet.turn(Math.pow(-1, n)*-40);
				spinned += 40;
				try{
				    Thread.sleep(1*SoccerParams.simulator_step);
				}catch(Exception e){}
				}
			kickedBall = false;
			n++;
			spinned = 0;
			}
		
		// sleep one step to ensure that we will not send
		// two commands in one cycle.
		try{
		    Thread.sleep(2*SoccerParams.simulator_step);
		}catch(Exception e){}
	    }
	m_krislet.bye();
    }


    //===========================================================================
    // Here are supporting functions for implement logic


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
    private SendCommand	                m_krislet;			// robot which is controlled by this brain
    private Memory			m_memory;				// place where all information is stored
    private char			m_side;
    volatile private boolean		m_timeOver;
    private String                      m_playMode;
    
}
