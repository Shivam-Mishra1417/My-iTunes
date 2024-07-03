package cs1302.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cs1302.api.ITunesApi.ItunesResult;
import cs1302.api.MusixMatchApi.Track;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Shivam Mishra
 * My application is a personal Itunes where you can lookup songs with their rating and lyrics from 
 * variuos global artists. iTunes API is used to fetch the tracks based on user query and for each 
 * track rating and lyrics is fetched from MusixMatch API by giving track name and artist name as input.
 * 
 */
public class ApiApp extends Application {

	/** HTTP client. */
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
			.followRedirects(HttpClient.Redirect.NORMAL) // always redirects, except from HTTPS to HTTP
			.build(); // builds and returns a HttpClient object

	/** Google {@code Gson} object for parsing JSON-formatted strings. */
	public static Gson GSON = new GsonBuilder().setPrettyPrinting() // enable nice output when printing
			.create(); // builds and returns a Gson object

	private Stage stage;
	private Scene scene;
	private VBox root;
	private HBox searchBar;
	private Label searchLabel;
	private TextField queryTermField;
	private static Button getImages;
	private TextFlow messageBar;
	private ScrollPane mainContent;
	private VBox itemScreen;
	private HBox footer;
	private ProgressBar progressBar;
	private HBox copyright;
	private Label copyrightTextiTunes;
	private Label copyrightTextMusixMatch;
	private static final String DEFAULT_STRING = "dua lipa";
	private static final int IMAGE_COUNT = 50;
	public static final String LIMIT = "50"; // limit on API to restrict response size
	private List<HBox> images = new ArrayList<HBox>(); // List to store the tracks after downloading from URIs
	private List<ItunesResult> results = new ArrayList<ItunesResult>();

	/**
	 * Constructs a {@code GalleryApp} object}.
	 */
	public ApiApp() {
		this.stage = null;
		this.scene = null;
		this.root = new VBox(5);
		this.searchBar = new HBox(5);
		this.searchLabel = new Label("Search:");
		this.queryTermField = new TextField(DEFAULT_STRING);
		this.getImages = new Button("Get Tracks");
		this.messageBar = new TextFlow();
		this.itemScreen = new VBox(5);
		this.mainContent = new ScrollPane(itemScreen);
		this.footer = new HBox(5);
		this.progressBar = new ProgressBar();
		this.copyright = new HBox(5);
		this.copyrightTextiTunes = new Label("Tracks provided by iTunes Search API.");
		this.copyrightTextMusixMatch = new Label("//Tracks ratings and lyrics provided by musixmatch API.");
	} // GalleryApp

	/** {@inheritDoc} */
	@Override
	public void init() {
		System.out.println("init() called");
		HBox.setHgrow(this.queryTermField, Priority.ALWAYS);
		this.searchLabel.setStyle("-fx-font-size: 15px;");
		this.progressBar.setProgress(0);
		this.progressBar.setMinWidth(620);
		this.progressBar.setMinHeight(15);
		this.searchBar.getChildren().addAll( this.searchLabel, this.queryTermField,
				 this.getImages);
		this.messageBar.getChildren()
				.add(new Text("Type in a term, then click the Get Tracks button."));
		this.copyright.getChildren().addAll(this.copyrightTextiTunes,this.copyrightTextMusixMatch);
		this.footer.getChildren().addAll(this.progressBar);
		this.root.getChildren().addAll(this.searchBar, this.messageBar, this.mainContent, this.footer,this.copyright);
		// actions
		this.getImages.setOnAction(
				event -> this.loadContent(this.queryTermField.getText(), "music"));

	} // init

	/** {@inheritDoc} */
	@Override
	public void start(Stage stage) {
		this.stage = stage;
		this.scene = new Scene(this.root, 640, 560);
		this.stage.setOnCloseRequest(event -> Platform.exit());
		this.stage.setTitle("My iTunes");
		Image icon = new Image("file:resources/icon.png");
		this.stage.getIcons().add(icon);// added icon to the application
		this.stage.setScene(this.scene);
		this.stage.sizeToScene();
		this.stage.show();
		Platform.runLater(() -> this.stage.setResizable(false));
		Platform.runLater(() -> this.queryTermField.requestFocus());
		this.defaultContent();
	} // start

	/** {@inheritDoc} */
	@Override
	public void stop() {
		System.out.println("stop() called");
	} // stop

	/**
	 * This method is called when get Tracks button is clicked. It takes the
	 * searched text  as input and then download the data from
	 * iTunes API and update tracks data in the application.
	 * 
	 * For each tracks it gets the rating and lyrics from musixmatch API.
	 * 
	 * @param searchText
	 * @param searchType
	 */
	private void loadContent(String searchText, String searchType) {
		this.onClickGetImages(); // screen changes when getImages button clicked
		ITunesApi ituneApi = new ITunesApi();
		HttpResponse<String> iTunesResponse = ituneApi.getApiResponse(searchText, searchType); // get the response from iTunes API
		System.out.println(iTunesResponse.request().toString());
		results = ituneApi.getImageUriSet(iTunesResponse);// get the distinct image URIs from response
		
		Task<Void> task = new Task<Void>() { // Start the downloading task
			HttpResponse<String> musixmatchTrackApiResponse=null;
			HttpResponse<String> musixmatchLyricsApiResponse = null;
			protected Void call() {
				try {
					if (results.size() < 5) {
						showAlert("Error", "Error", "URL: " + iTunesResponse.request().toString() + "\n" + "Exception: "
								+ results.size() + " distinct results found, but 5 or more are needed.");
						System.out.println("Going to cancel the task");
						cancel(); // cancel the downloading task if less than 5 distinct URIs available
					} else {
						images.clear(); // clear the existing images from list
						Iterator<ItunesResult> iterator = results.iterator();
						int ct = 1;
						MusixMatchApi mma = new MusixMatchApi();
						while (iterator.hasNext()) {
							System.out.println("\n\n\n");
							ItunesResult iTune = iterator.next();
							String imageUrl = iTune.artworkUrl100;
							String track_name = iTune.trackName;
							String artist = iTune.artistName;
							String date = formatDate(iTune.releaseDate);							
							HBox content = new HBox(5);
							VBox detailBox = new VBox(5);
							VBox lyricsBox = new VBox(5);
							HBox.setHgrow(content, Priority.ALWAYS);
							Text trackName = new Text("Track :" + track_name);
							Text artistName = new Text("Artist :" + artist);
							Text releaseDate = new Text("Released : " + date);
							detailBox.setAlignment(Pos.CENTER_LEFT);
							lyricsBox.setAlignment(Pos.CENTER_RIGHT);
							trackName.setFont(Font.font("Helvetica", FontWeight.BOLD, 20));
							artistName.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 20));
							releaseDate.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 20));
							musixmatchTrackApiResponse = mma.getTrackApiResponse(track_name,artist);
							Track track = mma.getRating(musixmatchTrackApiResponse);
							Text ratingText = new Text("Rating  :N/A");;
							//Text lyricsText =  new Text("Not Available");
							
							if(track!=null)
							{
								 ratingText = new Text("Rating  :"+track.track_rating+"/100");
							}
							ratingText.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 20));
							detailBox.setMaxWidth(400);
							trackName.setWrappingWidth(330); // Maximum width of 150 pixels
							artistName.setWrappingWidth(330);
							releaseDate.setWrappingWidth(330);
							
							// Create a button on the main window
					        Button openPopupButton = new Button("See Lyrics");
					        
					        if(track==null) {
					        	openPopupButton.setDisable(true);
					        }
					        // Event handler for the button click
					        openPopupButton.setOnAction(event -> {
					            // Create a new stage for the popup
					        	String lyrics1 = "Not Available";
					        	try {
					        	musixmatchLyricsApiResponse = mma.getLyricsApiResponse(track.track_name,track.artist_name);
								lyrics1 = mma.getLyrics(musixmatchLyricsApiResponse);					           
					        	}
					        	catch(Exception e) {
					        		lyrics1 = "Not Available";
					        	}
					        	Stage popupStage = new Stage();
					            // Set the modality to make this a separate, independent window
					            popupStage.initModality(Modality.APPLICATION_MODAL);
					            popupStage.setTitle(track_name);
					            Label paragraph = new Label(lyrics1);

					            VBox vbox = new VBox(10); // 10px spacing between elements					            
					            ScrollPane scrollLyrics = new ScrollPane(paragraph);
					            vbox.getChildren().add(scrollLyrics);
					            
					            Scene popupScene = new Scene(vbox, 520, 460); 
					            popupStage.setScene(popupScene);

					            // Show the popup window
					            popupStage.show();
					        });

							ImageView img = new ImageView(new Image(imageUrl));
							 img.setFitWidth(128);
							 img.setFitHeight(150);
							 detailBox.getChildren().addAll(trackName, artistName, releaseDate);
							 lyricsBox.getChildren().addAll(ratingText,openPopupButton);
							content.getChildren().addAll(img, detailBox,lyricsBox);
							images.add(content);
							updateProgress(ct, results.size());
							ct++;
						}
						Platform.runLater(() -> {
							System.out.println("Called runLater to add images in screen");
							itemScreen.getChildren().clear();
							for (int j = 0; j < Math.min(IMAGE_COUNT, images.size()); j++) {
								System.out.println("Adding images - " + j);
								itemScreen.getChildren().add(images.get(j));
							}
							getImages.setDisable(false);
							messageBar.getChildren().clear();
							messageBar.getChildren().add(new Text("Here is the list of tracks..."));
							
						});
					}
					System.out.println("at the end of try");
				} catch (Exception e) {
					showAlert("Error", "Error", e.toString());
				}
				return null;
			}
		};

		progressBar.progressProperty().bind(task.progressProperty());
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();

		// Unbind progress bar and set its value to 1.0 when task is cancelled
		task.setOnCancelled(event -> {
			progressBar.progressProperty().unbind();
			progressBar.setProgress(1.0);
		});

	}

	/**
	 * This method is reponsible to generate the initial screen when application is
	 * loaded using default.png provided
	 */
	private void defaultContent() {
		// mainContent.getChildren().clear();
		itemScreen.getChildren().clear();
		//for (int i = 0; i < IMAGE_COUNT; i++) {
			ImageView imageView = new ImageView(new Image("file:resources/readme-banner.png"));
			imageView.setFitWidth(640); // Set image width
			imageView.setFitHeight(460); // Set image height
			this.itemScreen.getChildren().add(imageView);
		//}
	}

	/**
	 * This method is reponsible to update screen elements when getTracks button is
	 * clicked. When a user click the "Get Tracks" button : (i) the Get Tracks
	 * buttons should be disabled (ii) Instruction should be replaced with the
	 * "Getting Tracks..." 
	 */
	private void onClickGetImages() {

		this.getImages.setDisable(true); // Disable the get images button
		this.messageBar.getChildren().clear(); // clear the message bar
		this.messageBar.getChildren().add(new Text("Getting tracks...")); // update the message bar
	}

	

	/**
	 * This method is responsible for the appropriate error message on the screen if
	 * any exception occurred in a flow.
	 * 
	 * @param title
	 * @param headerText
	 * @param contentText
	 */
	static void showAlert(String title, String headerText, String contentText) {
		Platform.runLater(() -> {
			getImages.setDisable(false);
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle(title);
			alert.setHeaderText(headerText);
			alert.setContentText(contentText);
			alert.showAndWait();
		});
	}
	
	private String formatDate(String date) {
		//Parse the ISO 8601 date into a ZonedDateTime (with time zone)
		ZonedDateTime zonedDateTime = ZonedDateTime.parse(date);

		// Convert to LocalDate to remove time and timezone information
		LocalDate localDate = zonedDateTime.toLocalDate();

		// Create a formatter to convert into "dd mon yyyy"
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

		// Format the LocalDate using the custom pattern
		String formattedDate = localDate.format(formatter);
		return formattedDate;
		}
} // ApiApp

