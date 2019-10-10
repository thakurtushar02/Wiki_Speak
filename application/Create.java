package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Represents the Create tab of the WikiSpeak application
 * 
 *@author Jacinta, Lynette, Tushar
 */
public class Create {
	private Button searchButton;
	private TextField search = new TextField();
	private Label create = new Label();
	private HBox searchBar;
	private VBox contents;
	private Label message = new Label();
	private Tab _tab;
	private String _term;
	private View _view;
	private String _name;
	private Popup _popup;
	private File _file;
	private ImageManager _imMan;
	private TabPane _tabPane;
	private Main _main;
	private Slider slider = new Slider();
	private ObservableList<String> listLines = FXCollections.observableArrayList();
	private int numberOfAudioFiles = 0;
	private int numberOfPictures;
	private ProgressBar pbSaveCombine = new ProgressBar();
	private ProgressBar pbSearch = new ProgressBar();
	private final String EMPTY = "Empty";
	private final String VALID = "Valid";
	private final String DUPLICATE = "Duplicate";

	public Create(Tab tab, Popup popup) {
		_tab = tab;
		_popup = popup;
		_imMan = new ImageManager();
	}

	public void setView(View view) {
		_view = view;
	}

	/**
	 * Sets the contents of the Create tab
	 * @param main
	 */
	public void setContents(Main main) {
		if (_main == null) {
			_main = main;
		}
		create.setText("Enter term to search for: ");
		create.setFont(new Font("Arial", 20));

		searchButton = new Button("Search ↳");
		searchButton.disableProperty().bind(search.textProperty().isEmpty());
		pbSearch.setVisible(false);
		searchBar = new HBox(create, search, searchButton, pbSearch);
		searchBar.setSpacing(15);

		message.setFont(new Font("Arial", 16));

		search.setOnKeyPressed(arg0 -> {if (arg0.getCode().equals(KeyCode.ENTER)) searchButton.fire();});

		searchButton.setOnAction(e -> searchTerm(search.getText()));

		contents = new VBox(searchBar, message);
		contents.setPadding(new Insets(15,10,10,15));
		_tab.setContent(contents);
	}

	/**
	 * Retrieves the wiki search of the supplied term and writes it to a file
	 * @param term	the search term given by user
	 */
	public void searchTerm(String term) {
		pbSearch.setVisible(true);
		
		// Term searched using wikit, written to a file and reformatted onto separate lines
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {
				_file = new File ("text.txt");
				ProcessBuilder builder = new ProcessBuilder("wikit", term);
				try {
					// Search and write to file
					Process process = builder.start();
					BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

					PrintWriter out = new PrintWriter(new FileWriter(_file));

					int exitStatus = process.waitFor();
					
					// If search process executes without problems, reformat file contents so that each sentence 
					// is on its own line
					if (exitStatus == 0) {
						String line;
						while ((line = stdout.readLine()) != null) {
							out.println(line);
						}

						out.close();

						String[] cmd = {"sed", "-i", "s/[.] /&\\n/g", _file.toString()};
						ProcessBuilder editFile = new ProcessBuilder(cmd);
						Process edit = editFile.start();

						BufferedReader stdout2 = new BufferedReader(new InputStreamReader(edit.getInputStream()));
						BufferedReader stderr2 = new BufferedReader(new InputStreamReader(edit.getErrorStream()));

						int exitStatus2 = edit.waitFor();

						if (exitStatus2 == 0) {
							String line2;
							while ((line2 = stdout2.readLine()) != null) {
								System.out.println(line2);
							}
						} else {
							String line2;
							while ((line2 = stderr2.readLine()) != null) {
								System.err.println(line2);
							}
						}

					} else {
						String line;
						while ((line = stderr.readLine()) != null) {
							System.err.println(line);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Platform.runLater(new Runnable(){
					// Progress bar is hidden and GUI is updated by reading the text file 
					@Override public void run() {
						pbSearch.setVisible(false);
						try(BufferedReader fileReader = new BufferedReader(new FileReader(_file.toString()))){
							String line = fileReader.readLine();
							// Display contents if there are results, otherwise prompt user to search again
							if(line.contains("not found :^(")) {
								message.setText("Search term is invalid, please try again with another search term.");
								setContents(_main);
							} else {
								message.setText("");
								_term = term;
								deleteFiles();
								displayLines(term);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Displays the wiki search results into the TextArea
	 * @param reply	the search term
	 */
	public void displayLines(String reply) {
		ListView<String> list = new ListView<String>(); // List displaying audio files

		list.setItems(listLines);

		HBox views= new HBox();
		TextArea textArea = new TextArea();
		textArea.setEditable(true);
		textArea.setWrapText(true);
		textArea.setFont(new Font("Arial", 14));

		// Populate TextArea with text file contents
		BufferedReader fileContent;
		try {
			fileContent = new BufferedReader(new FileReader(_file));
			String line;
			while ((line = fileContent.readLine()) != null) {
				textArea.appendText(line + "\n");
			}
			fileContent.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		textArea.setText(textArea.getText().substring(2));

		Label lblList = new Label("Saved audio");
		lblList.setFont(new Font("Arial", 20));

		Text info = new Text("Move audio files ↑ or ↓ to get desired order.\n\n"
				+ "The creation will be created with audio\nfiles in the order "
				+ "they are below.\n\nDouble click to play audio file.");
		info.setFont(new Font("Arial", 14));
		VBox text = new VBox(searchBar, textArea);
		text.setSpacing(10);

		VBox.setVgrow(textArea, Priority.ALWAYS);

		VBox listView = new VBox(lblList, info, list);

		listView.setAlignment(Pos.CENTER_LEFT);
		listView.setSpacing(10);

		views.getChildren().addAll(text, listView);
		views.setSpacing(10);

		// combo box to select voice
		ObservableList<String> voices = FXCollections.observableArrayList("Default", "Espeak");
		final ComboBox<String> combobox = new ComboBox<String>(voices);
		combobox.setValue("Default");
		
		// buttons
		Label lblVoice = new Label("Voice: ");
		lblVoice.setFont(new Font("Arial", 20));
		
		Button butPlay = new Button(" Play ►");
		BooleanBinding playSaveBinding = textArea.selectedTextProperty().isEmpty();
		butPlay.disableProperty().bind(playSaveBinding);
		Button butSave = new Button(" Save ✔");
		butSave.disableProperty().bind(playSaveBinding);
		
		Button butUp = new Button("Move ↑");
		BooleanBinding upDownBinding = Bindings.size(listLines).lessThan(2).or(list.getSelectionModel().selectedItemProperty().isNull());
		butUp.disableProperty().bind(upDownBinding);
		Button butDown = new Button("Move ↓");
		butDown.disableProperty().bind(upDownBinding);
		
		Button butDelete = new Button("Delete ✘");
		butDelete.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		Button butCombine = new Button("Combine ↳");
		final Pane spacer = new Pane();
		spacer.setMinSize(10, 1);

		// slider to select number of pictures
		slider.setMin(1);
		slider.setMax(10);
		slider.setValue(1);
		slider.setMajorTickUnit(1f);
		slider.isSnapToTicks();
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);

		Label photos = new Label("Choose Number of Pictures");
		photos.setFont(new Font("Arial", 20));

		slider.valueProperty().addListener((obs, oldval, newVal) -> slider.setValue(newVal.intValue()));

		HBox lineOptions = new HBox(lblVoice, combobox, butPlay, butSave, spacer, butUp, butDown, butDelete);
		lineOptions.setSpacing(15);
		lineOptions.setAlignment(Pos.BOTTOM_CENTER);

		TextField nameField = new TextField();
		nameField.setPromptText("Enter name of creation");
		
		BooleanBinding combBinding = Bindings.size(listLines).isEqualTo(0).or(nameField.textProperty().isEmpty());
		butCombine.disableProperty().bind(combBinding);
		
		// Does not allow characters to be typed into text field
		nameField.textProperty().addListener((observable, oldValue, newValue) -> {
			String[] badCharacters = {"/", "?", "%", "*", ":", "|", "\"", "<", ">", "\0",
					"\\", "(", ")", "$", "@", "!", "#", "^", "&", "+"};
			for (String s: badCharacters) {
				if (newValue.contains(s)) {
					nameField.setText(oldValue);
				}
			}
		});

		final Pane spacer2 = new Pane();
		spacer2.setMinSize(10, 1);

		HBox.setHgrow(text, Priority.ALWAYS);
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox.setHgrow(spacer2, Priority.ALWAYS);
		VBox.setVgrow(textArea, Priority.ALWAYS);
		pbSaveCombine.setVisible(false);
		HBox nameLayout = new HBox(10, photos, pbSaveCombine, spacer2, nameField, butCombine);
		nameLayout.setAlignment(Pos.BOTTOM_CENTER);

		VBox layout = new VBox(views, lineOptions, nameLayout, slider);
		layout.setPadding(new Insets(10));
		layout.setSpacing(10);

		_tab.setContent(layout);
		_tabPane.requestLayout();

		butPlay.setOnAction(e -> {
			String selectedText = textArea.getSelectedText();
			// Display pop-up if number of highlighted words > 30
			if (selectedText.split(" ").length > 30) {
				_popup.tooManyWordsHighlighted();
			} else {
				Task<Void> task = new Task<Void>() {
					// Preview text according to user's selected voice
					@Override
					protected Void call() throws Exception {				
						String voice;
						String selection = combobox.getSelectionModel().getSelectedItem();
						if ( selection.equals("Default")) {
							voice = "festival --tts";
						} else {
							voice = "espeak";
						}
						String command = "echo \"" + textArea.getSelectedText() + " \" | " + voice ;
						ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
						try {
							Process p = pb.start();
							BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							int exitStatus = p.waitFor();

							if (exitStatus != 0) {
								String line2;
								while ((line2 = stderr.readLine()) != null) {
									System.err.println(line2);
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}
				};
				new Thread(task).start();
			}
		});
		
		// Save selected text as an audio file  
		butSave.setOnAction(e -> {
			String selectedText = textArea.getSelectedText();
			try {
				String fileName = _file.getName();
				FileWriter fw = new FileWriter(fileName, false);
				fw.write("");
				fw.close();
				fw = new FileWriter(fileName, true);
				fw.write(selectedText);
				addCreation(combobox.getSelectionModel().getSelectedItem());
				fw.close();
			} catch (IOException ioex) {
				ioex.getMessage();
			}
		});

		butUp.setOnAction(e -> {
			int i = list.getSelectionModel().getSelectedIndex();
			if (i > 0) {
				String temp = list.getSelectionModel().getSelectedItem();
				list.getItems().remove(i);
				list.getItems().add(i-1, temp);
				list.getSelectionModel().select(i-1);
			}
		});

		butDown.setOnAction(e -> {
			int i = list.getSelectionModel().getSelectedIndex();
			if (i < list.getItems().size()-1) {
				String temp = list.getSelectionModel().getSelectedItem();
				list.getItems().remove(i);
				list.getItems().add(i+1, temp);
				list.getSelectionModel().select(i+1);
			}
		});

		butDelete.setOnAction(e -> {
			if (list.getSelectionModel().getSelectedItem() != null) {
				list.getItems().remove(list.getSelectionModel().getSelectedIndex());
			}
		});

		nameField.setOnKeyPressed(arg0 -> {if (arg0.getCode().equals(KeyCode.ENTER)) butCombine.fire();});
		
		butCombine.setOnAction(e -> {
			String name = nameField.getText();
			String validity = checkName(name);
			_name = name;
			if (validity.equals(EMPTY)) {
				nameField.setPromptText("Nothing entered.");
				butCombine.requestFocus();
			} else if (validity.equals(VALID)) {
				nameField.setPromptText("");
				combineAudioFiles(); // Site of creation creation
			} else if (validity.equals(DUPLICATE)) {
				nameField.clear();
				nameField.setPromptText("");
				butCombine.requestFocus();
				_popup.showStage(_name, "Creation name already exists.\nWould you like to rename or overwrite?", "Rename", "Overwrite", false);
			}
		});

		// Plays the selected audio file on double-click
		list.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					String audio = list.getSelectionModel().getSelectedItem();

					String cmd = "aplay AudioFiles/" + audio +".wav";
					ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
					try {
						pb.start();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

		});

	}

	/**
	 * Checks the validity of the creation name
	 * @param reply	name supplied by user
	 * @return DUPLICATE if the creation already exists
	 * 		   EMPTY if the field is empty
	 * 		   otherwise VALID
	 */
	public String checkName(String reply) {
		File file = new File("./Creations/" + reply + ".mp4");
		if(file.exists()) {
			return DUPLICATE;
		} else if (reply.isEmpty()) {
			return EMPTY;
		} else {
			return VALID;
		}	
	}

	/**
	 * Creates and saves an audiofile into the ListvIew
	 * @param voice	voice selected by user
	 */
	public void addCreation(String voice) {
		pbSaveCombine.setVisible(true);
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {

				String cmd = "mkdir -p AudioFiles";
				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);

				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				numberOfAudioFiles++;
				String nameOfFile = "AudioFile" + numberOfAudioFiles;

				if (voice.equals("Default")) {
					cmd = "cat " + _file.toString() + " | text2wave -o \"./AudioFiles/" + nameOfFile + ".wav\"";
				} else {
					cmd = "espeak -f " + _file.toString() + " --stdout > \"./AudioFiles/" + nameOfFile + ".wav\"";
				}
				builder = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Platform.runLater(new Runnable(){
					@Override public void run() {
						_view.setContents();
						_popup.showFeedback(nameOfFile, false);
						listLines.add(nameOfFile);
						pbSaveCombine.setVisible(false);
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Deletes a specified file
	 * @param name name of the file to be deleted
	 */
	public void removeCreation(String name) {
		File file = new File(name);
		file.delete();
	}

	public void storeTabs(TabPane tabPane) {
		_tabPane = tabPane;
	}

	/**
	 * Creates creation by combining audio files, downloading images, creating video, and merging them all into 
	 * one mp4 video file
	 */
	public void combineAudioFiles() {
		pbSaveCombine.setVisible(true);
		numberOfPictures = (int)slider.getValue();
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				getPics(numberOfPictures, _term);
				String cmd;
				
				// Combine list of audio files into in one if there are multiple, otherwise rename the one audio file
				if (listLines.size() == 1) {
					cmd = "mv ./AudioFiles/AudioFile1.wav ./AudioFiles/"+ "temp" + ".wav";
				} else {
					cmd = "ffmpeg";
					for (String s: listLines) {
						cmd += " -i \"./AudioFiles/" + s + ".wav\"";
					}

					cmd += " -filter_complex \"";
					for (int i = 0; i < listLines.size(); i++) {
						cmd += "[" + i + ":0]";
					}
					cmd += "concat=n=" + listLines.size() + ":v=0:a=1[out]\" -map \"[out]\" ./AudioFiles/" + "temp" + ".wav &>/dev/null";
				}

				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				// Create video with images and text, combine with audio, and remove intermediary output files
				cmd = "cat \"" + _term + "\"??.jpg | ffmpeg -f image2pipe -framerate $((" + numberOfPictures + "))/"
						+ "$(soxi -D \'./AudioFiles/" + "temp" + ".wav\') -i - -c:v libx264 -pix_fmt yuv420p -vf \""
						+ "scale=w=1280:h=720:force_original_aspect_ratio=1,pad=1280:720:(ow-iw)/2:(oh-ih)/2\""
						+ " -r 25 -y visual.mp4 ; rm \"" + _term + "\"??.jpg ; ffmpeg -i visual.mp4 -vf "
						+ "\"drawtext=fontsize=50:fontcolor=white:x=(w-text_w)/2:y=(h-text_h)"
						+ "/2:borderw=5:text=\'" + _term + "\'\" out.mp4 ; ffmpeg -i out.mp4 -i"
						+ " \'./AudioFiles/" + "temp" + ".wav\' -c:v copy -c:a aac -strict experimental"
						+ " -y \'./Creations/" + _name + ".mp4\' &>/dev/null ; rm visual.mp4 ; rm out.mp4";

				ProcessBuilder builderr = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process vidProcess = builderr.start();
					vidProcess.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						_view.setContents();
						_main.refreshGUI(null);
						_popup.showFeedback(_name, false);
						pbSaveCombine.setVisible(false);

					}
				});
				deleteFiles();

				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Deletes supporting files from working directory
	 */
	public void deleteFiles() {
		listLines = FXCollections.observableArrayList();
		numberOfAudioFiles = 0;
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				String cmd = "if [ -d AudioFiles ]; then rm -r AudioFiles; fi; if [ -e text.txt ]; then rm -f text.txt; fi";
				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}

		};
		new Thread(task).start();
	}

	/**
	 * Downloads a number of images (of the search term) from Flickr
	 * @param input	the number of images
	 * @param reply	the search term
	 */
	public void getPics(int input, String reply) {
		numberOfPictures = input;
		_imMan.getImages(input, reply);

	}
}


