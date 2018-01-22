package com.aubet.francois.VacumBotControler.model;

/**
 * Created by root on 31.10.17.
 */

public class State {
	public static boolean connectedPi = false;
    public static boolean connectedWifi = false;
	public static double lightValue = 0.0;


	public State(){
		connectedPi = false;
		connectedWifi = false;
	}


	public static void connectionLost(){
		connectedPi = false;
		connectedWifi = false;
	}

}
