/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.gui;

import games.stendhal.client.GameObjects;
import games.stendhal.client.GameScreen;
import games.stendhal.client.SpriteStore;
import games.stendhal.client.Sprite;
import games.stendhal.client.StaticGameLayers;
import games.stendhal.client.StendhalClient;
import games.stendhal.client.StendhalUI;
import games.stendhal.client.stendhal;
import games.stendhal.client.entity.Inspector;
import games.stendhal.client.entity.User;
import games.stendhal.client.gui.styled.Style;
import games.stendhal.client.gui.styled.WoodStyle;
import games.stendhal.client.gui.styled.swing.StyledJButton;
import games.stendhal.client.gui.wt.GroundContainer;
import games.stendhal.client.gui.wt.InternalManagedDialog;
import games.stendhal.client.gui.wt.SettingsPanel;
import games.stendhal.client.gui.wt.core.WtBaseframe;
import games.stendhal.client.gui.wt.core.WtClickListener;
import games.stendhal.client.gui.wt.core.WtMessageBox;
import games.stendhal.client.gui.wt.core.WtPanel;
import games.stendhal.client.gui.wt.core.WtWindowManager;
import games.stendhal.client.sound.SoundSystem;
import games.stendhal.client.update.ClientGameConfiguration;

import games.stendhal.common.CollisionDetection;
import games.stendhal.common.Direction;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import marauroa.common.Log4J;
import marauroa.common.game.AttributeNotFoundException;
import marauroa.common.game.RPObject;

import org.apache.log4j.Logger;

/** The main class that create the screen and starts the arianne client. */
public class j2DClient extends StendhalUI {

	private static final long serialVersionUID = 3356310866399084117L;

	/** width of the game screen (without the chat line) */
	public static int SCREEN_WIDTH;

	/** height of the game screen (without the chat line) */
	public static int SCREEN_HEIGHT;

	static {
		String[] dim = stendhal.SCREEN_SIZE.split("x");
		SCREEN_WIDTH = Integer.parseInt(dim[0]);
		SCREEN_HEIGHT = Integer.parseInt(dim[1]);
	}

	/** the logger instance. */
	private static final Logger logger = Log4J.getLogger(j2DClient.class);

	/**
	 * The man window frame.
	 */
	private JFrame	frame;

	private GameScreen screen;

	private Canvas canvas;

	private JLayeredPane	pane;

	private KTextEdit gameLog;

	private boolean gameRunning;

	/** NOTE: It sounds bad to see here a GUI component. Try other way. */
	private JTextField playerChatText;

// Not currently used (maybe later?)
//	private FXLayer fx;

	private long lastKeyRelease;

	private long lastKeyEventsCleanUpStart;

	private int[] veryFastKeyEvents = new int[4]; // at leat one more than

	/** a nicer way of handling the keyboard */
	private Map<Integer, Object> pressed;

	private boolean ctrlDown;

	private boolean shiftDown;

	private boolean altDown;

	/** the main frame */
	private WtBaseframe baseframe;

	/** this is the ground */
	private GroundContainer ground;

	/** settings panel */
	private SettingsPanel settings;

	/** the dialog "really quit?" */
	private WtPanel quitDialog;

	private Component	xquitDialog;

	private Sprite offlineIcon;

	private boolean offline;

	private int blinkOffline;

	private static final boolean newCode =
			(System.getProperty("stendhal.newgui") != null);

	private boolean fixkeyboardHandlinginX() {
		logger.debug("OS: " + System.getProperty("os.name"));
		try {
			// NOTE: X does handle input in a different way of the rest of the world.
			// This fixs the problem.
			Runtime.getRuntime().exec("xset r off");
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					try {
						Runtime.getRuntime().exec("xset r on");
					} catch (Exception e) {
						logger.error(e);
					}
				}
			});
			return true;
		} catch (Exception e) {
			logger.error("Error setting keyboard handling", e);
		}

		return false;
	}

	public j2DClient(StendhalClient client) {
		super(client);

		setDefault(this);

		pressed = new HashMap<Integer, Object>();


		frame = new JFrame();

		frame.setTitle(ClientGameConfiguration.get("GAME_NAME") + " " + stendhal.VERSION
		        + " - a multiplayer online game using Arianne");


		// create a frame to contain our game

		URL url = SpriteStore.get().getResourceURL(ClientGameConfiguration.get("GAME_ICON"));
		frame.setIconImage(new ImageIcon(url).getImage());


		// When the user tries to close the window, don't close immediately,
		// but show a confirmation dialog. 
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);


		Container content = frame.getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));


		/*
		 * Get hold the content of the frame and set up the resolution
		 * of the game
		 */
		pane = new JLayeredPane();
		pane.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
		content.add(pane);


		/*
		 * Wrap canvas in panel that can has setPreferredSize()
		 */
		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
		pane.add(panel, JLayeredPane.DEFAULT_LAYER);


		/*
		 * Setup our rendering canvas
		 */
		canvas = new Canvas();
		canvas.setBounds(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		canvas.setIgnoreRepaint(true);
		panel.add(canvas);


		/*
		 * Chat input field
		 */
		playerChatText = new JTextField("");

		StendhalChatLineListener chatListener = new StendhalChatLineListener(client, playerChatText);
		playerChatText.addActionListener(chatListener);
		playerChatText.addKeyListener(chatListener);

		content.add(playerChatText);


		/*
		 * Always redirect focus to chat field
		 */
		canvas.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				playerChatText.requestFocus();
			}

			public void focusLost(FocusEvent e) {
			}
		});


		/*
		 * Handle focus assertion and window closing
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent ev) {
				playerChatText.requestFocus();
			}


			@Override
			public void windowActivated(WindowEvent ev) {
				playerChatText.requestFocus();
			}


			@Override
			public void windowGainedFocus(WindowEvent ev) {
				playerChatText.requestFocus();
			}


			@Override
			public void windowClosing(WindowEvent e) {
				requestQuit();
			}
		});


		/*
		 * Quit dialog
		 */
		if(newCode) {
			xquitDialog = buildQuitDialog();
			xquitDialog.setVisible(false);

			pane.add(xquitDialog, JLayeredPane.MODAL_LAYER);
		}


		/*
		 * Game log
		 */
		gameLog = new KTextEdit();
		gameLog.setPreferredSize(new Dimension(SCREEN_WIDTH, 200));


		if(System.getProperty("stendhal.onewindow") != null) {
			content.add(gameLog);
			frame.pack();
		} else {
			/*
			 * In own window
			 */
			final JDialog dialog = new JDialog(frame, "Game chat and events log");

			content = dialog.getContentPane();
			content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
			content.add(gameLog);

			dialog.addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent e) {
					playerChatText.requestFocus();
				}

				public void focusLost(FocusEvent e) {
				}
			});

			dialog.pack();


			/*
			 * Move tracker
			 */
			frame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					Rectangle bounds = frame.getBounds();

					dialog.setLocation(
				        	bounds.x,
						bounds.y + bounds.height);

					dialog.setVisible(true);
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					Rectangle bounds = frame.getBounds();

					dialog.setLocation(
				        	bounds.x,
						bounds.y + bounds.height);
				}
			});
		}


		// workaround for key auto repeat on X11 (linux)
		// First we try the JNI solution. In case this fails, we do this:
		// In the default case xset -r is execute on program start and xset r on exit
		// As this will affect all applications you can write keys.x=magic to use
		// a method called MagicKeyListener. Caution: This does not work on all pcs
		// and creates create stress on the network and server in case it does not work.
		KeyListener keyListener = new GameKeyHandler();
		if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
			if (!X11KeyConfig.getResult()) {
				boolean useXSet = WtWindowManager.getInstance().getProperty("keys.x", "xset").equals("xset");
				if (useXSet) {
					if (!fixkeyboardHandlinginX()) {
						keyListener = new MagicKeyListener(keyListener);
					}
				} else {
					keyListener = new MagicKeyListener(keyListener);
				}
			}
		}


		// add a key input system (defined below) to our canvas so we can
		// respond to key pressed
		playerChatText.addKeyListener(keyListener);
		canvas.addKeyListener(keyListener);

		// Display a warning message in case the screen size was adjusted
		// This is a temporary solution until this issue is fixed server side.
		// I hope that it discourages its use without the risks of unabdateable
		// clients being distributed
		if (!stendhal.SCREEN_SIZE.equals("640x480")) {
			addEventLine("Using window size cheat: " + stendhal.SCREEN_SIZE, Color.RED);
		}



		frame.setLocation(new Point(20, 20));

		// finally make the window visible
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);


		/*
		 * create the buffering strategy which will allow AWT
		 * to manage our accelerated graphics
		 */
		BufferStrategy strategy;
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();

		screen = new GameScreen(strategy, SCREEN_WIDTH, SCREEN_HEIGHT);
		screen.setComponent(canvas);

		GameScreen.setDefaultScreen(screen);
		client.setScreen(screen);

// Not currently used (maybe later?)
//		fx = new FXLayer(SCREEN_WIDTH, SCREEN_HEIGHT);


		offlineIcon = SpriteStore.get().getSprite("data/gui/offline.png");

		// create the baseframe
		baseframe = new WtBaseframe(screen);
		// register native event handler
		canvas.addMouseListener(baseframe);
		canvas.addMouseMotionListener(baseframe);
		// create ground
		ground = new GroundContainer(this);

		baseframe.addChild(ground);
		// the settings panel creates all other
		settings = new SettingsPanel(this, ground);
		ground.addChild(settings);

		// set some default window positions
		WtWindowManager windowManager = WtWindowManager.getInstance();
		windowManager.setDefaultProperties("corpse", false, 0, 190);
		windowManager.setDefaultProperties("chest", false, 100, 190);


		// Start the main game loop, note: this method will not
		// return until the game has finished running. Hence we are
		// using the actual main thread to run the game.
		gameLoop();

		logger.debug("Exit");
		System.exit(0);
	} // constructor


	/**
	 * Add a native in-window dialog to the screen.
	 *
	 * @param	comp		The component to add.
	 */
	public void addDialog(Component comp) {
		pane.add(comp, JLayeredPane.PALETTE_LAYER);
	}


	/**
	 * Build the in-window quit dialog [panel].
	 *
	 *
	 */
	protected Component buildQuitDialog() {
		InternalManagedDialog	imd;
		Style	style;
		JPanel	panel;
		JButton	b;


		panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(null);
		panel.setPreferredSize(new Dimension(150, 75));

		style = WoodStyle.getInstance();

		b = new StyledJButton(style);
		b.setText("Yes");
		b.setBounds(30, 25, 40, 25);
		b.addActionListener(new QuitConfirmCB());

		panel.add(b);


		b = new StyledJButton(style);
		b.setText("No");
		b.setBounds(80, 25, 40, 25);
		b.addActionListener(new QuitCancelCB());

		panel.add(b);


		imd = new InternalManagedDialog("quit", "Quit");
		imd.setContent(panel);

		return imd.getDialog();
	}


	public void gameLoop() {

		final int frameLength = (int) (1000.0 / stendhal.FPS_LIMIT);

		int fps = 0;

		StaticGameLayers staticLayers = client.getStaticGameLayers();
		GameObjects gameObjects = client.getGameObjects();

		long oldTime = System.nanoTime();

		// Clear the first screen
		screen.clear();

		screen.place(-100, -100);

		SoundSystem.playSound("welcome", 100);

		// keep looping until the game ends
		long refreshTime = System.currentTimeMillis();
		long lastMessageHandle = refreshTime;

		gameRunning = true;

		while (gameRunning) {
			fps++;
			// figure out what time it is right after the screen flip then
			// later we can figure out how long we have been doing redrawing
			// / networking, then we know how long we need to sleep to make
			// the next flip happen at the right time

			screen.nextFrame();
			long now = System.currentTimeMillis();
			long delta = now - refreshTime;
			refreshTime = now;

			logger.debug("Move objects");
			gameObjects.move(delta);

			if (frame.getState() != Frame.ICONIFIED) {
				logger.debug("Draw screen");
				draw();
				rotateKeyEventCounters();
			}

			// TODO: only draw it if it is required to save cpu time
			// fx.draw(screen.expose());

			logger.debug("Query network");
			if (client.loop(0)) {
				lastMessageHandle = System.currentTimeMillis();
			}

			logger.debug("Move screen");
			moveScreen( staticLayers);

			if (System.nanoTime() - oldTime > 1000000000) {
				oldTime = System.nanoTime();
				logger.debug("FPS: " + Integer.toString(fps));
				long freeMemory = Runtime.getRuntime().freeMemory() / 1024;
				long totalMemory = Runtime.getRuntime().totalMemory() / 1024;

				logger.debug("Total/Used memory: " + totalMemory + "/" + (totalMemory - freeMemory));

				fps = 0;
			}

			// Shows a offline icon if no messages are recieved in 10 seconds.
			if ((refreshTime - lastMessageHandle > 120000) || !client.getConnectionState()) {
				setOffline(true);
			} else {
				setOffline(false);
			}

			logger.debug("Start sleeping");
			// we know how long we want per screen refresh (40ms) then
			// we add the refresh time and subtract the current time
			// leaving us with the amount we still need to sleep.
			long wait = frameLength + refreshTime - System.currentTimeMillis();

			if (wait > 0) {
				if (wait > 100) {
					logger.info("Waiting " + wait + " ms");
					wait = 100;
				}

				try {
					Thread.sleep(wait);
				} catch (Exception e) {
				}
				;
			}

			logger.debug("End sleeping");
		}

		logger.info("Request logout");
		client.logout();
		SoundSystem.get().exit();
	}

	private void moveScreen(StaticGameLayers gameLayers) {
		try {
			if (User.isNull()) {
				return;
			}

			double x = User.get().getX();
			double y = User.get().getY();

			double screenx = screen.getX();
			double screeny = screen.getY();
			double screenw = screen.getWidth();
			double screenh = screen.getHeight();
			double sdx = screen.getdx();
			double sdy = screen.getdy();

			double dsx = screenx + screenw / 2;
			double dsy = screeny + screenh / 2;

			if (dsx - x < -2) {
				sdx += 0.6;
			} else if ((dsx - x > -0.5) && (dsx - x < 0.5)) {
				sdx /= 1.6;
			} else if (dsx - x > 2) {
				sdx -= 0.6;
			}

			if (dsy - y < -2) {
				sdy += 0.6;
			} else if ((dsy - y > -0.5) && (dsy - y < 0.5)) {
				sdy /= 1.6;
			} else if (dsy - y > 2) {
				sdy -= 0.6;
			}

			screen.move(sdx, sdy);
		} catch (AttributeNotFoundException e) {
			// Logger.thrown("j2DClient::moveScreen","X",e);
		}
	}


	protected Direction keyCodeToDirection(int keyCode) {
		switch (keyCode) {
			case KeyEvent.VK_LEFT:
				return Direction.LEFT;

			case KeyEvent.VK_RIGHT:
				return Direction.RIGHT;

			case KeyEvent.VK_UP:
				return Direction.UP;

			case KeyEvent.VK_DOWN:
				return Direction.DOWN;

			default:
				return null;
		}
	}


	protected void onKeyPressed(KeyEvent e) {
		if (e.isShiftDown()) {
			/*
			 * We are going to use shift to move to previous/next line of text
			 * with arrows so we just ignore the keys if shift is pressed.
			 */
			return;
		}

		switch (e.getKeyCode()) {
			case KeyEvent.VK_L:
				if(e.isControlDown()) {
					/*
					 * Ctrl+L
					 * Make game log visible
					 */
					SwingUtilities.getRoot(gameLog).setVisible(true);
				}

				break;

			case KeyEvent.VK_R:
				if(e.isControlDown()) {
					/*
					 * Ctrl+R
					 * Remove text bubbles
					 */
					client.clearTextBubbles();
				}

				break;

			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				/*
				 * Ctrl means face, otherwise move
				 */
				client.addDirection(
					keyCodeToDirection(e.getKeyCode()),
					e.isControlDown());

				break;
		}
	}


	protected void onKeyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				/*
				 * Ctrl means face, otherwise move
				 */
				client.removeDirection(
					keyCodeToDirection(e.getKeyCode()),
					e.isControlDown());
		}
	}


	protected void quitCancelCB() {
		xquitDialog.setVisible(false);
	}


	protected void quitConfirmCB() {
		shutdown();
	}


	/**
	 * Rotates the veryFastKeyEvents array
	 */
	private void rotateKeyEventCounters() {
		if (lastKeyEventsCleanUpStart + 300 < System.currentTimeMillis()) {
			lastKeyEventsCleanUpStart = System.currentTimeMillis();

			for (int i = veryFastKeyEvents.length - 1; i > 0; i--) {
				veryFastKeyEvents[i - 1] = veryFastKeyEvents[i];
			}
			veryFastKeyEvents[veryFastKeyEvents.length - 1] = 0;
		}
	}


	/**
	 * Save the current keyboard modifier (i.e. Alt/Ctrl/Shift) state.
	 *
	 * @param	ev		The keyboard event.
	 */
	protected void updateModifiers(KeyEvent ev) {
		altDown = ev.isAltDown();
		ctrlDown = ev.isControlDown();
		shiftDown = ev.isShiftDown();
	}


	/**
	 * Shutdown the client. Save state and tell the main loop to stop.
	 */
	protected void shutdown() {
		Log4J.startMethod(logger, "shutdown");
		gameRunning = false;

		// try to save the window configuration
		WtWindowManager.getInstance().save();
		Log4J.finishMethod(logger, "shutdown");
	}


	/*
	 * Draw the screen.
	 */
	protected void draw() {
		/*
		 * Draw the GameLayers from bootom to top, relies on exact
		 * naming of the layers
		 */
		StaticGameLayers gameLayers = client.getStaticGameLayers();
		String set = gameLayers.getRPZoneLayerSet();

		GameObjects gameObjects = client.getGameObjects();

		gameLayers.draw(screen, set + "_0_floor");
		gameLayers.draw(screen, set + "_1_terrain");
		gameLayers.draw(screen, set + "_2_object");
		gameObjects.draw(screen);
		gameLayers.draw(screen, set + "_3_roof");
		gameLayers.draw(screen, set + "_4_roof_add");
		gameObjects.drawHPbar(screen);
		gameObjects.drawText(screen);


		// create the map if there is none yet
		if (gameLayers.changedArea()) {
			CollisionDetection cd = gameLayers.getCollisionDetection();
			if (cd != null) {
				gameLayers.resetChangedArea();
				settings.updateMinimap(cd, screen.expose().getDeviceConfiguration(), gameLayers.getArea());
			}
		}

	//	RPObject player = client.getPlayer();
		settings.setPlayer(User.get());

		baseframe.draw(screen.expose());

		if (offline && (blinkOffline > 0)) {
			offlineIcon.draw(screen.expose(), 560, 420);
		}

		if (blinkOffline < -10) {
			blinkOffline = 20;
		} else {
			blinkOffline--;
		}
	}


	//
	// <StendhalGUI>
	//

	/**
	 * Determine if the Alt key is held down.
	 *
	 * @return	Returns <code>true</code> if down.
	 */
	@Override
	public boolean isAltDown() {
		return altDown;
	}

	/**
	 * Determine if the <Ctrl> key is held down.
	 *
	 * @return	Returns <code>true</code> if down.
	 */
	@Override
	public boolean isCtrlDown() {
		return ctrlDown;
	}

	/**
	 * Determine if the <Shift> key is held down.
	 *
	 * @return	Returns <code>true</code> if down.
	 */
	@Override
	public boolean isShiftDown() {
		return shiftDown;
	}

	/**
	 * Sets the context menu. It is closed automatically one the user
	 * clicks outside of it.
	 */
	public void setContextMenu(JPopupMenu contextMenu) {
		baseframe.setContextMenu(contextMenu);
	}


	//
	// StendhalUI
	//

	/**
	 * Add an event line.
	 *
	 */
	@Override
	public void addEventLine(String text) {
		gameLog.addLine(text);
	}


	/**
	 * Add an event line.
	 *
	 */
	@Override
	public void addEventLine(String header, String text) {
		gameLog.addLine(header, text);
	}


	/**
	 * Add an event line.
	 *
	 */
	@Override
	public void addEventLine(String text, Color color) {
		gameLog.addLine(text, color);
	}


	/**
	 * Add an event line.
	 *
	 */
	@Override
	public void addEventLine(String header, String text, Color color) {
		gameLog.addLine(header, text, color);
	}


	/**
	 * Initiate outfit selection by the user.
	 */
	public void chooseOutfit() {
		int	outfit;


		RPObject player = client.getPlayer();

		if(player.has("outfit_org")) {
			outfit = player.getInt("outfit_org");
		} else {
			outfit = player.getInt("outfit");
		}

		// Should really keep only one instance of this around
		OutfitDialog dialog = new OutfitDialog(frame, "Set outfit", outfit, 23, 16, 11, 24);
		dialog.setVisible(true);
	}


	/**
	 * Get the current game screen height.
	 *
	 * @return	The height.
	 */
	@Override
	public int getHeight()
	{
		return SCREEN_HEIGHT;
	}


	/**
	 * Get the entity inspector.
	 *
	 * @return	The inspector.
	 */
	@Override
	public Inspector getInspector() {
		return ground;
	}


	/**
	 * Get the game screen.
	 *
	 * @return	The game screen.
	 */
	@Override
	public GameScreen getScreen() {
		return screen;
	}


	/**
	 * Get the current game screen width.
	 *
	 * @return	The width.
	 */
	@Override
	public int getWidth() {
		return SCREEN_WIDTH;
	}


	/**
	 * Request quit confirmation from the user.
	 * This stops all player actions and shows a dialog in which the
	 * player can confirm that they really wants to quit the program.
	 * If so it flags the client for termination.
	 */
	@Override
	public void requestQuit() {
		/*
		 * Stop the player
		 */
		client.stop();

		if(newCode) {
			Dimension psize = xquitDialog.getPreferredSize();

			xquitDialog.setBounds(
				(getWidth() - psize.width) / 2,
				(getHeight() - psize.height) / 2,
				psize.width,
				psize.height);

			xquitDialog.validate();
			xquitDialog.setVisible(true);

		} else {
		// quit messagebox already showing?
		if (quitDialog == null) {
			// no, so show it
			quitDialog = new WtMessageBox("quit", 220, 220, 200, "Quit Stendhal?",
			        WtMessageBox.ButtonCombination.YES_NO);
			quitDialog.registerClickListener(new WtClickListener() {

				public void onClick(String name, Point point) {
					quitDialog = null; // remove field as the messagebox is
					// closed now
					if (name.equals(WtMessageBox.ButtonEnum.YES.getName())) {
						// Yes-Button clicked...logut and quit.
						shutdown();
					}
				};
			});
			baseframe.addChild(quitDialog);
		}
		}
	}


	/**
	 * Set the input chat line text.
	 *
	 * @param	text		The text.
	 */
	@Override
	public void setChatLine(String text) {
		playerChatText.setText(text);
	}


	/**
	 * Set the offline indication state.
	 *
	 * @param	offline		<code>true</code> if offline.
	 */
	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	//
	//

	protected class GameKeyHandler implements KeyListener {
		public void keyPressed(KeyEvent e) {
			// detect X11 auto repeat still beeing active
			if ((lastKeyRelease > 0) && (lastKeyRelease + 1 >= e.getWhen())) {
				veryFastKeyEvents[veryFastKeyEvents.length - 1]++;
				if ((veryFastKeyEvents[0] > 2) && (veryFastKeyEvents[1] > 2) && (veryFastKeyEvents[2] > 2)) {
					addEventLine("Detecting serious bug in keyboard handling.", Color.RED);

					addEventLine(
				                "Try executing xset -r in a terminal windows. Please write a bug report at http://sourceforge.net/tracker/?group_id=1111&atid=101111 including the name and version of your operating system and distribution",
				                Color.BLACK);
				}
			}

			updateModifiers(e);

			if (!pressed.containsKey(Integer.valueOf(e.getKeyCode()))) {
				onKeyPressed(e);
				pressed.put(Integer.valueOf(e.getKeyCode()), null);
			}
		}


		public void keyReleased(KeyEvent e) {
			lastKeyRelease = e.getWhen();

			updateModifiers(e);
			onKeyReleased(e);
			pressed.remove(Integer.valueOf(e.getKeyCode()));
		}


		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == 27) {
				// Escape
				requestQuit();
			}
		}
	}

	//
	//

	protected class QuitCancelCB implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			quitCancelCB();
		}
	}


	protected class QuitConfirmCB implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			quitConfirmCB();
		}
	}


	//
	//

	public static void main(String args[]) {
		if (args.length > 0) {
			int i = 0;
			String port = null;
			String username = null;
			String password = null;
			String host = null;

			while (i != args.length) {
				if (args[i].equals("-u")) {
					username = args[i + 1];
				} else if (args[i].equals("-p")) {
					password = args[i + 1];
				} else if (args[i].equals("-h")) {
					host = args[i + 1];
				} else if (args[i].equals("-port")) {
					port = args[i + 1];
				}
				i++;
			}

			if ((username != null) && (password != null) && (host != null) && (port != null)) {
				StendhalClient client = StendhalClient.get();
				try {
					client.connect(host, Integer.parseInt(port), true);
					client.login(username, password);

					new j2DClient(client);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				return;
			}
		}

		System.out.println("Stendhal j2DClient\n");
		System.out.println("  games.stendhal.j2DClient -u username -p pass -h host -c character\n");
		System.out.println("Required parameters");
		System.out.println("* -h\tHost that is running Marauroa server");
		System.out.println("* -port\tport of the Marauroa server (try 32160)");
		System.out.println("* -u\tUsername to log into Marauroa server");
		System.out.println("* -p\tPassword to log into Marauroa server");
	}
}
