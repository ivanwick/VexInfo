package com.warrenrobotics;

public class Tester {
	public static void main(String[] args) throws Exception{
		//Time runtime of whole program
		long curTime = System.currentTimeMillis();
		//Set event link(s)
		String eventLink = "https://www.robotevents.com/robot-competitions/vex-robotics-competition/RE-VRC-17-3805.html"; //Worlds 2018
		String eventLink2 = "https://www.robotevents.com/robot-competitions/vex-robotics-competition/RE-VRC-17-4583.html"; //Central California States 2018
		String eventLink3 = "https://www.robotevents.com/robot-competitions/vex-robotics-competition/RE-VRC-17-4125.html"; //McBride HS #1 2017-2018
		String eventLink4 = "https://www.robotevents.com/robot-competitions/vex-robotics-competition/RE-VRC-17-4185.html"; //VRC Howl in the Hills
		//Set email for user to transfer ownership to
		String usrEmail = "iragequitwup123zz@gmail.com";
		//Essentially grab all data and set it(constructor body calls create and write methods)
		@SuppressWarnings("unused")
		TeamAPI api = new TeamAPI(eventLink2, usrEmail);
		//Time taken
		long timeTaken = System.currentTimeMillis() - curTime; 
		//Print out message
		System.out.printf("Program runtime - %f seconds", ((double)timeTaken/1000));
		System.exit(0);
	}
}
