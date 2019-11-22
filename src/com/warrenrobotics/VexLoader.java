package com.warrenrobotics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VexLoader {

    //Logger
    private static Logger LOGGER = LogManager.getLogger(VexLoader.class);

    /**
     * <p>
     * Processes a RobotEvents.com link to be able to get an events SKU,
     * the season for that event, the name of the event, and a team list
     * for the event.
     * </p>
     *
     * <p>
     * Either gets season for the current tournament(tied to the RobotEvents link), or
     * allows the user to specify their own season. If season is empty, it will used the
     * season tied to the RobotEvents link. If not empty, it will use the season specified.
     * </p>
     * <p>
     * <b>Note:</b> Team lists can only be generated <i>4 weeks</i> before the start date
     * of the tournament. Trying to do so will cause the program to stop. This is because
     * of restrictions in the VexDB database.
     * </p>
     *
     * @param s the URL of the robot events link
     * @param season Any valid season within the VexDB query list.(can also be "" to get current season)
     * @throws JSONException for when JSON API encounters error
     * @throws IOException for when an I/O error occurs
     */
    public static VexEvent loadEvent(String s, String season) throws JSONException, IOException {
        String eventName;

        //Create URL from link
        URL link = new URL(s);

        String sku = extractSKU(link);

        //Get JSON data from API
        JSONObject eventJson = Team.TeamBuilder
                .readJsonFromUrl("https://api.vexdb.io/v1/get_events?sku=" + sku)
                .getJSONArray("result")
                .getJSONObject(0);
        //Set event season
        season = season.equals("") ? eventJson.getString("season") : season ;
        //Set event name
        eventName = eventJson.getString("name");
        //Print event name
        System.out.printf("Event Name: %s%n", eventName);
        //Print out event code
        System.out.printf("Event Code: %s\n", sku);
        //Print season for stats
        System.out.printf("Season: %s\n", season);
        //Print out venue name
        System.out.printf("Venue: %s\n", eventJson.getString("loc_venue"));
        //Print out address
        System.out.printf("Address: %s\n", eventJson.getString("loc_address1"));
        //Print out city/state
        System.out.printf("\t %s, %s %s\n", eventJson.getString("loc_city"), eventJson.getString("loc_region"),
                eventJson.getString("loc_postcode"));
        //Print out county
        System.out.printf("Country: %s\n", eventJson.getString("loc_country"));
        //Set event date(only grab start day, ignore time)
        //Format: YYYY-MM-DD
        String eventDate = eventJson.getString("start").split("T")[0];
        //Check date to see if it first 4-week restriction
        checkDate(eventDate);
        //Build JSON array from SKU
        JSONArray result = Team.TeamBuilder
                .readJsonFromUrl("https://api.vexdb.io/v1/get_teams?sku=" + sku)
                .getJSONArray("result");
        //Initialize team list
        String[] teamNames = new String[result.length()];
        //Fill the team list
        for(int i = 0; i < result.length(); i++) {
            teamNames[i] = result.getJSONObject(i).getString("number");
        }
        //Print out estimated runtime
        //TODO: Fix me. Although the time spend logging in does add to this
        double estimatedRuntime = (2.0 + (0.3 * teamNames.length) + 0.6 + 9.0 + 3.0);
        System.out.printf("Estimated Runtime(Broken) - (%.2f) seconds\n", estimatedRuntime);
        //Print break
        System.out.println("-----------------------------------------------------------");

        List<Team> teamList = loadTeams(teamNames, season);

        return new VexEvent(season, eventName, eventDate, teamList, sku);
    }

  	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									PROCESSING METHODS									//
	//																						//
	------------------------------------------------------------------------------------------
	*/

    private static String extractSKU(URL link) {
        //Get file path of url
        String[] filePath = link.getPath().split("/");
        //Get and set event code
        return filePath[filePath.length - 1].replace(".html", "");
    }

    /**
     * Checks the current date and compares it to the event date(more specifically,
     * it compares it to the date exactly <u>28 days(4 weeks)</u> before the event date).
     * <p>
     * If the current date isn't within <u>4 weeks(28 days)</u> of the event date(or the event hasn't already happened), then
     * the program will log it as an error and exit with an error code.
     * </p>
     * <p>
     * If the current date is within <u>4 weeks</u> of the event date(or the event has already happened), then
     * the program will continue.
     * </p>
     */
    private static void checkDate(String eventDate){
        //Print break
        System.out.println("-----------------------------------------------------------");
        //New Calendar instance
        Calendar c = Calendar.getInstance();
        //Set leniency to true, so program could subtract 4 weeks(28 days)
        //from event date, without the calendar object throwing an exception
        c.setLenient(true);
        //Set to start of current day
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        //Put into date object
        Date today = c.getTime();
        //Get event date in an array of strings(Format: YYYY-MM-DD)
        String[] eventTimeInfoStr = eventDate.split("-");
        //Initialize and convert string array to int array
        int[] eventTimeInfo = new int[3];
        //Loop through array to fill with integers
        for(int i = 0; i < eventTimeInfo.length; i++) {
            eventTimeInfo[i] = Integer.parseInt(eventTimeInfoStr[i]);
        }
        //Set calendar to event date
        c.set(eventTimeInfo[0], eventTimeInfo[1], eventTimeInfo[2]);
        //Get the actual date
        Date eventDateActual = c.getTime();
        //Set event date to exactly 4 weeks(28 days) before its date,
        //since that is what the program is checking for
        c.set(eventTimeInfo[0], (eventTimeInfo[1] - 1), (eventTimeInfo[2] - 28));
        //Check date with this specified date
        Date dateSpecified = c.getTime();
        //If current Date is greater than 4 weeks before the event
        if(today.before(dateSpecified)) { //Date restriction not met
            //Get difference of dates in milliseconds
            long difInMs = dateSpecified.getTime() - today.getTime();
            //Convert milliseconds to days
            int dayDifference = (int) TimeUnit.MILLISECONDS.toDays(difInMs);
            //Create a DateFormat object to get desired format for date
            DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
            //Print out dates
            System.out.printf("Today:%s\n", df.format(today));
            System.out.printf("Actual:%s\n", df.format(eventDateActual));
            System.out.printf("Specified:%s\n", df.format(dateSpecified));
            //Log the issue
            LOGGER.error(String.format("Requirement not met. Wait (%d) days.", dayDifference));
            //Print out messages
            System.out.println("DATE CHECK:FALSE");
            System.err.println("CANNOT GET DATA FROM API UNTIL 4-WEEK RESTRICTION MET");
            System.err.printf("WAIT (%d) DAYS%nEXITING PROGRAM(1)%n", dayDifference);
            //Stop program
            System.exit(1);
        } else { //Date restriction met
            //Format date Strings
            DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
            System.out.printf("Today's Date: %s%n", df.format(today));
            System.out.printf("Event's Date(Actual): %s%n", df.format(eventDateActual));
            System.out.printf("Event's Date(4 Weeks Prior): %s%n", df.format(dateSpecified));
            System.out.println("DATE CHECK:TRUE");
            //Print break
            System.out.println("-----------------------------------------------------------");
            //Program continues running
        }
    }

    public static List<Team> loadTeams(String[] teamNames, String season) throws IOException {
        List<Team> teamList = new ArrayList<>(teamNames.length);
        //Loop through team list
        for(int i = 0; i < teamNames.length; i++) {
            //Time how long each loop takes
            long sTime = System.currentTimeMillis();
            //Grab team name
            String n = teamNames[i];
            //Parse team and calculate data
            Team t = new Team.TeamBuilder(n, season)
                    .setTeamData()
                    .setEventData()
                    .setRankingData()
                    .setSeasonData()
                    .setSkillsData()
                    .build();

            teamList.add(t);

            //Time taken
            long timeTaken = System.currentTimeMillis() - sTime;
            //Print-out
            System.out.print(String.format("\t%-10s(%dms)\n", n, timeTaken).replace(" ", "."));
        }

        return teamList;
    }
}
