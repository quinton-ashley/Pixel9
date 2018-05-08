package com.qashto.pixel9;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.qashto.guint.QAUtil;
import com.hamoid.VideoExport;
import oscP5.*;

import processing.core.*;

public class Pixel9 extends PApplet {
	public static void main(String[] args) {
		PApplet.main("com.qashto.pixel9.Pixel9");
	}

	public void settings() {
//		fullScreen();
		//use 1280 for a 13 inch MacBook, use 1920 for HD, use 4096 for UHD.
		//use 800 for a 13 inch MacBook, use 1080 for HD, use 2160 for UHD.
		size(280, 800, P2D);
		pixelDensity(2);
	}

	boolean recording = false;
	int sessionNum = 1;
	int framesSaved = 0;
	String videoName = "Pixels9_" + sessionNum;
	VideoExport vidEx;
	byte responseType = 0;
	boolean commandPressed = false;
	boolean reset = false;
	boolean userPause = false;
	boolean userSaveFrame = false;
	boolean userSaveFrames = false;
	String homeDir = System.getProperty("user.home");
	java.io.File framesDir = new java.io.File(homeDir + "/Pictures/Pixels9/");
	String vidDir = homeDir + "/temp/Pixels9/";
	OscP5 oscar;

	int lineWidth = 200;
	double speed = 10;
	double jitter = .1;
	int rateRandom = 100;
	int rateTrail = 4;
	int ogRate = 5;
	int drawRate = ogRate;
	int interlaceX = 999;
	int interlaceY = 10;
	int[] m = new int[6];

	Console console = new Console(15, color(255), 13);

	public void setup() {
		noStroke();
		background(0);
		frameRate(60);
		if (recording) {
			vidEx = new VideoExport(this, String.format("%s%s.mp4", vidDir, videoName));
			vidEx.setFrameRate(60);
			vidEx.dontSaveDebugInfo();
		}
		surface.setTitle("Pixel9 by Quinton Ashley");
		console.visible = false;
		
		oscar = new OscP5(this, 6005);
		oscar.plug(this, "setSpeed", "/speed");
		oscar.plug(this, "setLineWidth", "/lineWidth");
		//		strokeWeight(2);
	}
	
	public void setSpeed(float speed) {
		console.log("speed:" + speed);
		this.speed = speed;
	}
	
	public void setLineWidth(float lineWidth) {
		console.log("width:" + lineWidth);
		this.lineWidth = (int) lineWidth;
	}

	public void draw() {
		loadPixels();
		m[0] = (int)(noise(frameCount/100) * 255);
		m[1] = (int)(noise(frameCount/200) * 255) + 150;
		m[2] = (int)(noise(frameCount/400) * 255);
		m[3] = (int)(noise(frameCount/25) * 255) + 150;
		m[4] = (int)(noise(frameCount/800) * 255);
		m[5] = (int)(noise(frameCount/1600) * 255) + 150;

		int x, y, r, g, b, i;
		int c = 0;
		c = QAUtil.replaceByte(c, 3, 255);
		drawRate = ogRate + (int)(Math.random() * rateRandom);
		for (int q = 0; q < rateTrail; q++, drawRate++) {
			for (i = (int) (frameCount % (interlaceX * (Math.random() * jitter + 1))) ; i < pixels.length; i += drawRate) {
				x = (i % pixelWidth);
				y = (int) (Math.ceil(i / pixelWidth));
				//			r = (( (x + y + frameCount*speed) % lineWidth < lineWidth/2)?255-frameCount % 200:150+frameCount % 108);
				//			g = (( (x + frameCount*speed) % lineWidth < lineWidth/2)?255-frameCount % 230:150-frameCount % 46);
				//			b = (( (x + frameCount*speed) % lineWidth >= lineWidth/2)?255-frameCount % 40:150+frameCount % 92);
				r = (( (x * x * 0.00001 + y * y * lineWidth * 0.000001 + frameCount*speed) % lineWidth < lineWidth/2)?m[0]:m[1]);
				//			g = (( (x * x * 0.001 + y * y * lineWidth * 0.000001 + frameCount*speed) % lineWidth < lineWidth/2)?m[2]:m[3]);
				b = (( (x + y * y * y * 0.00001+ frameCount*speed) % lineWidth >= lineWidth/2)?m[4]:m[4]);
				c = QAUtil.replaceByte(c, 2, (int) (Math.random() * r));
				c = QAUtil.replaceByte(c, 1, 100);
				c = QAUtil.replaceByte(c, 0, b);
				pixels[i] = c;
			}	
		}
		//		drawRate = ogRate + (int)(Math.random() * rateRandom);
		//		for (i = (int) (frameCount % (interlaceX * (Math.random() * jitter + 1))) ; i < pixels.length; i += drawRate) {
		//			pixels[i] = 0;
		//		}
		updatePixels();

		if (console.visible) {
			console.update();
		}
		if (userSaveFrame && (frameCount % 5 == 0)) {
			saveFrame(String.format(framesDir+"/%d.%d.tif", 
					sessionNum, framesSaved));
			framesSaved++;
			userSaveFrame = false;
			console.log("one frame saved!");
			console.log("to save frames continuously,");
			console.log("enter a capital S.");
		}
		if (recording) {
			vidEx.saveFrame();
		}
	}

	public void keyPressed() {
		response(key);
	}

	public void response(char key) {
		if (responseType == 0) {
			if (key == CODED) {
				if (keyCode == LEFT) {
					//userAdjust(false);
				} else if (keyCode == RIGHT) {
					//userAdjust(true);
				} else if (keyCode == 157) { // 157 is the command key's value
					commandPressed = true;
				}
			} else if (!commandPressed) {
				switch (key) {
				case 'c': //toggle the console's visibility
					console.visible = !console.visible;
					break;
				case 'e':
					console.print("your input: ");
					responseType = 1;
					break;
				case 'q': exit(); break;
				case 'k':
					userPause = !userPause;
					if (userPause) {
						noLoop();
					} else {
						loop();
					}
					break;
				case 'K':
					console.printf("%f\n", frameRate);
					break;
				case 's':
					if (userSaveFrames) {
						console.println("Frames saved!");
					}
					userSaveFrames = false;
					userSaveFrame = true;
					console.println("one frame saved!");
					console.println("to save frames continuously enter a capital S.");
					break;
				case 'S': userSaveFrames = !userSaveFrames;
				if (userSaveFrames) {
					console.println("WARNING: saving frames!");
					console.println("to stop saving you must type s again!");
				} else {
					console.println("frames saved!");
				}
				break;

				default:
					console.printf("%c is not a valid key\n", key);
					break;
				}
			} else if (responseType == 1){
				if (key == CODED) {
					if (keyCode == 157) { // 157 is the command key's value
						commandPressed = true;
					}
				} else if (key == ENTER) {
					// println the input to display it on the console permanently
					console.println(console.input);
					// call your method that takes the input
					// TODO:
					// reset the input string
					console.input = "";
					responseType = 0;
				} else if (key == 8) { // 8 is the delete key's value
					console.delete();
				} else if (!commandPressed) {
					console.input += key;
				} else if (key == 'v') {
					String text = console.getTextFromClipboard();
					console.input += text;
				}
			}
		}
	}

	public void keyReleased() {
		if (key == CODED) {
			if (keyCode == 157) { // 157 is the command key's value
				commandPressed = false;
			}
		}
	}

	/** Console is a class for Processing that allows console text and user input 
	 * to be displayed both in your Processing App and PDE/IDE console.
	 * 
	 * @author quintonashley5
	 * 
	 * Sample code: constructor

	Console console = new Console(15, color(255), 13);

	 * Sample code: toggle visibility in draw

	public void draw() {
		if (console.visible) {
	console.update();
	}
}

	 * Sample code: console input using keyPressed

	public void keyPressed() {
		if (key == CODED) {
			if (keyCode == 157) { // 157 is the command key's value
					commandPressed = true;
				}
			} else if (key == ENTER) {
				// println the input to display it on the console permanently
				console.println(console.input);
				// call your method that takes the input
				yourMethod(console.input);
				// reset the input string
				console.input = "";
			} else if (key == 8) { // 8 is the delete key's value
				console.delete();
			} else if (!commandPressed) {
			console.input += key;
		} else if (key == 'v') {
			String text = console.getTextFromClipboard();
			console.input += text;
		}
	}

	 */
	public class Console {
		// These variables can be edited at any time.
		boolean visible = true;
		int textColor;
		int textPadding;
		int textSize;
		int lines;
		String input = "";

		// Don't touch these variables unless you know what you're doing!
		private ByteArrayOutputStream baos = new ByteArrayOutputStream(16);
		private PrintStream stream = new PrintStream(baos);
		private String[] text;

		/** Text padding default is textSize/5
		 * @param textSize the size of the console text in the Processing window
		 * @param textColor use one of processing's color() methods to input textColor easily
		 * @param lines the number of lines of text that will be displayed in the Processing window
		 */
		public Console(int textSize, int textColor, int lines) {
			this(textColor, textSize/5, textSize, lines);
		}

		/** Constructor method for a Console object
		 * @param textColor use one of processing's color() methods to input textColor easily
		 * @param textPadding the spacing between each line of text and the distance of the sides from the top left corner
		 * @param textSize the size of the console text in the Processing window
		 * @param lines the number of lines of text that will be displayed in the Processing window
		 */
		public Console(int textColor, int textPadding, int textSize, int lines) {
			this.textColor = textColor;
			this.textSize = textSize;
			this.textPadding = textPadding;
			this.lines = lines;
		}

		/** Functions the same as String.format(String format, Object... args)
		 * @param format
		 * @param args
		 */
		public void printf(String format, Object... args) {
			try {
				stream.printf(format, args);
				System.out.printf(format, args);
			} catch (Exception e) {
				println("error: printf format exception");
			}
		}

		/** Functions the same as String.println(String string)
		 * @param string
		 */
		public void println(String string) {
			stream.println(string);
			System.out.println(string);
		}

		public void log(String string) {
			this.println(string);
		}

		/** Functions the same as String.print(String string)
		 * @param string
		 */
		public void print(String string) {
			stream.print(string);
			System.out.print(string);
		}

		/**
		 * Deletes one character from the input string, if the input string has a character to delete.
		 * Call in the keyPressed() Processing method when key == 8
		 */
		public void delete() {
			if (input.length() > 0) {
				input = input.substring(0, input.length()-1);
			}
		}

		/**
		 * When called in the draw() method, update() will display the console text in the Processing window.
		 * You can use a method like this to toggle the console's visibility:
		if (console.visible) {
		console.update();
		}
		 */
		public void update() {
			text = split(baos.toString(), "\n");
			fill(textColor);
			textSize(textSize);
			int h = 1;
			// the on screen console will only show the most recent logs
			for (int i = (text.length-lines >= 0) ? text.length-lines+1 : 0; i < text.length; i++, h++) {
				if (i < text.length-1) {
					text(text[i].substring(0, (text[i].length() < 80) ? text[i].length(): 80),
							textPadding, h*textSize+h*textPadding);
				} else {
					text(text[i].substring(0, (text[i].length() < 80) ? text[i].length(): 80) + input,
							textPadding, h*textSize+h*textPadding);
				}
			}
		}

		/** Get's text from the clipboard, will print an error if the clipboard does not contain text.
		 * @return
		 */
		public String getTextFromClipboard() {
			return (String) getFromClipboard(DataFlavor.stringFlavor);
		}

		/** This method is adapted from the processing forum method at this link:
		 * https://forum.processing.org/one/topic/pasting-text-from-the-clipboard-into-processing.html
		 * @param flavor
		 * @return
		 */
		private Object getFromClipboard(DataFlavor flavor) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents = clipboard.getContents(null);
			Object obj = null;
			if (contents != null && contents.isDataFlavorSupported(flavor)) {
				try {
					obj = contents.getTransferData(flavor);
				}
				catch (UnsupportedFlavorException exu) { // Unlikely but we must catch it
					println("Unsupported flavor: " + exu);
				}
				catch (java.io.IOException exi) {
					println("Unavailable data: " + exi);
				}
			}
			return obj;
		}

		/** This method copies String text to the clipboard
		 * @param text the String to copy to the clipboard
		 */
		public void copyToClipboard(String text) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(text);
			clipboard.setContents(selection, selection);
		}

		/**
		 * This method must be called before Processing's exit() method!
		 */
		public void close() {
			println("bye!");
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stream.close();
		}
	}

	public void exit() {
		console.close();
		super.exit();
	}
}
