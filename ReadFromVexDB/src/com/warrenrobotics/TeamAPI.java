package com.warrenrobotics;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.SQLAdminScopes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.*;

/**
 * Interacts with Google Sheets using the Google Sheets API v4 to automatically take 
 * team names and assign certain statistics to them.
 * 
 * This class is responsible for all interactions with Google Sheets.
 * 
 * @author Robert Engle | WHS Robotics | Team 90241B
 * @version 1.1
 * @since 2018-02-21
 *
 */
public class TeamAPI {
	//Instance variables
	private String spreadsheetId; 
	private String range;
	private String valueRenderOption;
	private String dateTimeRenderOption;
	private String refreshToken;
	private String accessToken;
	private String scope;
	private ValueRange response;
	public String[] teamList;
	
	/**
	 * Constructs a TeamAPI object to interpret data from a Google Sheets using the Google Sheets API v4
	 * 
	 * @param spreadsheetId the id of the spreadsheet(commonly found in the link of the spreadsheet)
	 * @param valueRenderOption how values are represented in output. Default is FORMATTED_VALUE
	 * @param accessToken the oauth2 access token obtained from the OAuth 2.0 Playground
	 * @param scope the scope of data allowed access for the end user. Since we are planning on editing and reading spreadsheets, 
	 * 		  we will use https://www.googleapis.com/auth/spreadsheets
	 */
	public TeamAPI(String spreadsheetId, String refreshToken, String scope) throws IOException, GeneralSecurityException, InterruptedException{
		this.spreadsheetId = spreadsheetId;
		this.range = "Sheet1";
		this.valueRenderOption = "FORMATTED_VALUE";
		this.dateTimeRenderOption = "SERIAL_NUMBER";
		this.refreshToken = refreshToken;
		this.scope = scope;
		//Assign access token
		setAccessToken();
		//Create sheet service and request data using it
		Sheets sheetsService = createSheetsService();
		//Execute a sheets.get request
		executeGetRequest(sheetsService);
		//Process into team list
		processResponseIntoTeamList();
		executeWriteRequest(sheetsService);
		
	}
	
	/**
	 * Creates a sheets service that can be used to make a request for data
	 * 
	 * @return a Sheets object that can be used to grab data
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private Sheets createSheetsService() throws IOException, GeneralSecurityException {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
	    GoogleCredential credential = new GoogleCredential().setAccessToken(this.accessToken);
	    
	    return new Sheets.Builder(httpTransport, jsonFactory, credential)
	        .setApplicationName(this.scope)
	        .build();
	}
	
	/**
	 * Retrieves an access token using the refresh token
	 * 
	 * @return an access token that can be used to create a proper credential
	 */
	public void setAccessToken() throws IOException, GeneralSecurityException{
		//Initialize OAuth Credential strings
		String clientId;
		String clientSecret;
		/*
		 * Note to users who plan to use this:
		 * 
		 * Create a txt file named "OAuth2Credentials.txt" (or anything you like and change the variable "filepath" below)
		 * in the current working directory(or change "filepath" below)
		 * 
		 * Inside the text file, on the first line put only the OAuth2 client ID,
		 * and on the second line put only the OAuth2 client secret.
		 */
		String filePath = "src\\com\\warrenrobotics\\OAuth2Credentials.txt";
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		//Set client id to first line
		clientId = br.readLine();
		//Set client secret to second line
		clientSecret = br.readLine();
		//Don't leak resources
		br.close();
		//Create a token response using refresh token and oauth credentials
		TokenResponse response = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), 
				this.refreshToken, clientId, clientSecret).execute();
		//Set the access token as the response
		this.accessToken = response.getAccessToken();   
	}
	
	/**
	 * Processes the response into an array of strings containing team names(IE: ["90241A", "90241B"])
	 * 
	 * @return the team names as an array of strings
	 */
	private void processResponseIntoTeamList() {
		//Build string from ValueRange
		String responseStr = this.response.toString();
		//Build a json object from the string
		JSONObject json = new JSONObject(responseStr);
		//Build a json array from the "values" section
		JSONArray values = json.getJSONArray("values");
		//Make an array of strings for team names. Is length - 1 since the first iteration of values doesn't contain a team
		String[] teams = new String[values.length() - 1];
		//Start at index 1 to ignore top of row(which shows "Team")
		for(int i = 1; i < values.length(); i++) {
			//Must input at i - 1 since array starts at 0 still
			teams[i - 1] = values.getJSONArray(i).getString(0);
		}
		//Set the class team list
		this.teamList = teams;
	}
	
	private void buildValues(String[] arr, Team t) {
		/*
		 * For each time, write in this specific order:
		 * 
		 * avgOPR, avgDPR, avgCCWM, avgAP, avgSP, avgTSRP, vratingRank, vrating, avgRank, avgSkills_robot,
		 * avgSkills_auton, avgSkills_combined, avgMaxScore, totalEvents
		*/
		arr[0] = Double.toString(t.getAvgOPR());
		arr[1] = Double.toString(t.getAvgDPR());
		arr[2] = Double.toString(t.getAvgCCWM());
		arr[3] = Integer.toString(t.getAvgAP());
		arr[4] = Integer.toString(t.getAvgSP());
		arr[5] = Integer.toString(t.getAvgTRSP());
		arr[6] = Integer.toString(t.getvrating_rank());
		arr[7] = Double.toString(t.getvrating());
		arr[8] = Integer.toString(t.getAvgRank());
		arr[9] = Integer.toString(t.getAvgSkillsScore_robot());
		arr[10] = Integer.toString(t.getAvgSkillsScore_auton());
		arr[11] = Integer.toString(t.getAvgSkillsScore_combined());
		arr[12] = Integer.toString(t.getAvgMaxScore());
		arr[13] = Integer.toString(t.getNumEvents());
	}
	
	public void executeGetRequest(Sheets sheetsService) throws IOException{
		//Setup a request for getting spreadsheet data
		Sheets.Spreadsheets.Values.Get request =
		    sheetsService.spreadsheets().values().get(this.spreadsheetId, this.range);
		    request.setValueRenderOption(this.valueRenderOption);
		    request.setDateTimeRenderOption(this.dateTimeRenderOption);
		//Get a response as a ValueRange(which can converted to JSON Objects)
		this.response = request.execute();
	}
	
	public void executeWriteRequest(Sheets sheetsService) throws IOException, InterruptedException{
		//Write data for a team into spreadsheet
		/*
		 * For each time, write in this specific order:
		 * 
		 * avgOPR, avgDPR, avgCCWM, avgAP, avgSP, avgTSRP, vratingRank, vrating, avgRank, avgSkills_robot,
		 * avgSkills_auton, 
		*/
		for(int i = 0; i < teamList.length; i++) {
			//Grab team name
			String s = teamList[i];
			//Parse into team object
			Team t = TeamBuilder.parseTeam(s);
			//Initialize array for inputting data
			String[] valuesArr = new String[14];
			//Build array with proper data
			buildValues(valuesArr, t);
			//Configure body for input
			List<List<Object>> values = Arrays.asList(Arrays.asList(valuesArr));
			//Configure body as a ValueRange object
			ValueRange body = new ValueRange().setValues(values);
			//Configure range as Sheet1!F#:S# where # is a number based on the current team-(i+2)
			String range = "Sheet1!F" + (i + 2) + ":S" + (i+2);
			//Send write request and receive response
			UpdateValuesResponse result = 
					sheetsService.spreadsheets().values().update(this.spreadsheetId, range, body)
					.setValueInputOption("USER_ENTERED")
					.execute();
			//Print out success message
			System.out.println("#" + (i + 2) + " STATS UPDATED: " + t.name + " (" + result.getUpdatedCells() + " cells updated)");
			//Wait 1 second(fix for quota)
			Thread.sleep(1000);
		}
		System.out.println("SUCCESSFUL WRITE - " + teamList.length + " TEAMS ACQUIRED");
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									   GETTER METHODS								    //
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Returns the ValueRange response as a String
	 * 
	 * @return a string of the request response
	 */
	public String getResponse() { return this.response.toString(); }
	/**
	 * Retrieves the current access token provided by setAccessToken()
	 * 
	 * @return an access token allowing access to a sheet
	 */
	public String getAccessToken() { return this.accessToken; }
	
	/**
	 * Retrieves the current team list provided by proccessResponseIntoTeamList()
	 * @return
	 */
	public String[] getTeamList() { return this.teamList; }
}