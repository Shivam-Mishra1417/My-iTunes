package cs1302.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ITunesApi {
	
	/**@author Shivam Mishra
	 * Represents a response from the iTunes Search API. This is used by Gson to
	 * create an object from the JSON response body. This class is provided with
	 * project's starter code, and the instance variables are intentionally set
	 * to package private visibility.
	 */
	public class ItunesResponse {
	    int resultCount;
	    ItunesResult[] results;
	} // ItunesResponse
	
	/**
	 * Represents a result in a response from the iTunes Search API. This is
	 * used by Gson to create an object from the JSON response body. This class
	 * is provided with project's starter code, and the instance variables are
	 * intentionally set to package private visibility.
	 * @see <a href="https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/UnderstandingSearchResults.html#//apple_ref/doc/uid/TP40017632-CH8-SW1">Understanding Search Results</a>
	 */
	public class ItunesResult {
	    String wrapperType;
	    String kind;
	    String artworkUrl100;
	    String artistName;
	    String trackName;
	    String country;
	    String currency;
	    String releaseDate;
	    // the rest of the result is intentionally omitted since we don't use it
	} // ItunesResult

	/**
	 * 
	 * This method interacts with the iTunesAPI and returns response based upon request.
	 * 
	 * @param searchText
	 * @param searchType
	 * @return HttpResponse<String>
	 */
	 HttpResponse<String> getApiResponse(String searchText, String searchType) {

		String term = URLEncoder.encode(searchText, StandardCharsets.UTF_8); // encoding searched text
		String limit = URLEncoder.encode(ApiApp.LIMIT, StandardCharsets.UTF_8); // encoding limit value
		String type = URLEncoder.encode(searchType, StandardCharsets.UTF_8); // encoding search type
		String query = String.format("?term=%s&limit=%s&media=%s", term, limit, type); // creating a query for API

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://itunes.apple.com/search" + query))
				.build();
		// System.out.println(request.toString());

		HttpResponse<String> response;
		try {
			response = ApiApp.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			// System.out.println(response.body());
		} catch (Exception e) {
			response = null;
			ApiApp.showAlert("Error", "Error", e.toString());
		}

		return response;
	}


	/**
	 * 
	 * This method takes response from the API as input and using GSON, extracts
	 * distinct track URIs.
	 * 
	 * @param response
	 * @return Set<String>
	 */
	 List<ItunesResult> getImageUriSet(HttpResponse<String> response) {
		System.out.println("Inside getImageUriSet now..");
		//System.out.println(response.body());
		ItunesResponse res = ApiApp.GSON.fromJson(response.body(), ItunesResponse.class);
		Set<String> uniqueTracks = new HashSet<>(); // to create a set of distinct image URIs
		List<ItunesResult> results = new ArrayList<ItunesResult>();
		for (int i = 0; i < res.results.length; i++) {
			if (!uniqueTracks.contains(res.results[i].trackName)) {
				uniqueTracks.add(res.results[i].trackName);
				results.add(res.results[i]);
			}
		}

		System.out.println("The total distinct track URIs are : " + uniqueTracks.size());
		return results;
	}



	
}
