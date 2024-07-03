package cs1302.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MusixMatchApi {

	/**@author Shivam Mishra
	 * Represents an MusixMatch API document.
	 */
	public static class Track {
		int track_id;
		String track_name;
		int track_rating;
		int commontrack_id;
		int artist_id;
		String artist_name;

		public String toString() {
			return track_id + ":" + track_name + ":" + track_rating + ":" + artist_id + ":" + artist_name;
		}
	} // 

	/**
	 * Represents a response from the musixmatch Search API. This is used by Gson to
	 * create an object from the JSON response body.
	 */
	class TrackResponseApi {
		MessageTrack message;
	}

	/**
	 * Represents a response from the musixmatch Search API. This is used by Gson to
	 * create an object from the JSON response body.
	 */
	class LyricsResponseApi {
		MessageLyrics message;
	}

	// Class representing the top-level 'message'
	class MessageTrack {
		Header header;
		Body body;
	}

	// Class representing the top-level 'message'
	class MessageLyrics {
		Header header;
		BodyLyrics body;
	}

	// Class representing the 'header'
	class Header {
		int status_code;
		double execute_time;
		int available;
	}

	// Class representing the 'body'
	class Body {
		List<TrackItem> track_list; // This holds a list of track items
	}

	// Class representing the 'body'
	class BodyLyrics {
		Lyrics lyrics;; // This holds a list of track items
	}

	// Class representing each Lyrics
	class Lyrics {
		String lyrics_body;
	}

	// Class representing each 'track_item' in 'track_list'
	class TrackItem {
		Track track;
	}

	private static final String TRACK_SEARCH_ENDPOINT = "https://api.musixmatch.com/ws/1.1/track.search?";
	private static final String TRACK_GET_ENDPOINT = "https://api.musixmatch.com/ws/1.1/track.get?";
	private static final String ARTIST_SEARCH_ENDPOINT = "http://api.musixmatch.com/ws/1.1/artist.search?";
	private static final String LYRICS_ENDPOINT = "https://api.musixmatch.com/ws/1.1/matcher.lyrics.get?";

	/**
	 * this method track name and artist name as input and uses it to query on musix
	 * match track search API
	 * 
	 * @param track
	 * @param artist
	 * @return HttpResponse<String>
	 */
	HttpResponse<String> getTrackApiResponse(String track, String artist) {

		System.out.println("Going to search on musix : " + track + "::" + artist);
		String q_track = URLEncoder.encode(track, StandardCharsets.UTF_8); // encoding searched text
		String q_artist = URLEncoder.encode(artist, StandardCharsets.UTF_8); // encoding limit value
		String sort = URLEncoder.encode("desc", StandardCharsets.UTF_8);
		String query = String.format("q_track=%s&q_artist=%s&s_track_rating=%s&page_size=1&page=1&apikey=%s", q_track,
				q_artist, sort, getKey()); // creating a
		// query for API
		HttpResponse<String> response;
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TRACK_SEARCH_ENDPOINT + query)).build();
			System.out.println(request.toString());
			response = ApiApp.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			// System.out.println(response.body());
		} catch (Exception e) {
			System.out.println("Inside exception of musix api request");
			response = null;
			System.out.println(e.toString());
			ApiApp.showAlert("Error", "Error", e.toString());
		}

		return response;
	}

	/**
	 * this method accpets track name and artists name as it is from musixmatch
	 * track and provides the lyrics
	 * 
	 * @param track
	 * @param artist
	 * @return HttpResponse<String>
	 */
	HttpResponse<String> getLyricsApiResponse(String track, String artist) {

		System.out.println("Going to search lyrics on musix : " + track + "::" + artist);
		String q_track = URLEncoder.encode(track, StandardCharsets.UTF_8); // encoding searched text
		String q_artist = URLEncoder.encode(artist, StandardCharsets.UTF_8); // encoding limit value
		String sort = URLEncoder.encode("desc", StandardCharsets.UTF_8);
		String query = String.format("q_track=%s&q_artist=%s&apikey=%s", q_track, q_artist, getKey()); // creating a
																										// query for API
		HttpResponse<String> response;
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LYRICS_ENDPOINT + query)).build();
			System.out.println(request.toString());
			response = ApiApp.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			// System.out.println(response.body());
		} catch (Exception e) {
			System.out.println("Inside exception of musix api request");
			response = null;
			System.out.println(e.toString());
			ApiApp.showAlert("Error", "Error", e.toString());
		}

		return response;
	}

	/**
	 * This method accepts the HttpResponse<String> response from musixmatch track
	 * search API and extracts ratings from it using GSON
	 * 
	 * @param response
	 * @return Track
	 */
	Track getRating(HttpResponse<String> response) {
		System.out.println("Inside getRating method now..");
		try {
			TrackResponseApi res = ApiApp.GSON.fromJson(response.body(), TrackResponseApi.class);
			System.out.println("Size ------>" + res.message.body.track_list.size());
			System.out.println("====> " + response.body().toString());
			if (res.message.body.track_list.size() == 0)
				return null;
			else
				return res.message.body.track_list.get(0).track;
		} catch (Exception e) {
			System.out.println("Could not find the track");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * This method accepts the HttpResponse<String> response from musixmatch lyrics
	 * search API and extracts lyrics from it using GSON
	 * 
	 * @param response
	 * @return String (lyrics)
	 */
	String getLyrics(HttpResponse<String> response) {
		System.out.println("Inside getLyrics method now..");
		try {
			LyricsResponseApi res = ApiApp.GSON.fromJson(response.body(), LyricsResponseApi.class);
			if (res.message.header.status_code != 200) {
				return "Not Available";
			} else {
				return res.message.body.lyrics.lyrics_body;
			}
		} catch (Exception e) {
			System.out.println("Could not find the lyrics");
			e.printStackTrace();
			return "Not Available";
		}

	}

	/**
	 * this method reads the config.properties file and extracts the musixmatch api
	 * key.
	 * 
	 * @return apikey
	 */
	private String getKey() {

		String configPath = "resources/config.properties";
		try (FileInputStream configFileStream = new FileInputStream(configPath)) {
			Properties config = new Properties();
			config.load(configFileStream);
			// config.list(System.out); // list all using standard out
			String musixApiKey = config.getProperty("musixapi.key"); // get musixAPi.key
			return musixApiKey;
		} catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
			ApiApp.showAlert("Error", "Error", ioe.toString());
			return "";
		} // try

	}
}
