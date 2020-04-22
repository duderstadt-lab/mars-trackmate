/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.trackmate;

 import fiji.plugin.trackmate.Logger;
 import fiji.plugin.trackmate.Model;
 import fiji.plugin.trackmate.Settings;
 import fiji.plugin.trackmate.Spot;
 import fiji.plugin.trackmate.TrackMate;
 import fiji.plugin.trackmate.gui.TrackMateGUIController;
 import fiji.plugin.trackmate.gui.TrackMateWizard;
 import fiji.plugin.trackmate.io.IOUtils;
 import fiji.plugin.trackmate.util.TMUtils;

 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.Set;
 import java.util.TreeSet;

 import javax.swing.ImageIcon;
 import org.scijava.plugin.Plugin;

 /**
  * TrackMateAction to export tracks to a mars MoleculeArchive.
  * @author Karl Duderstadt
  */
 public class GoToMarsAction extends AbstractTMAction {

	  private static final String CONTENT_KEY 	= "Tracks";
  	private static final String DATE_ATT 		= "generationDateTime";
  	private static final String PHYSUNIT_ATT 	= "spaceUnits";
  	private static final String FRAMEINTERVAL_ATT 	= "frameInterval";
  	private static final String FRAMEINTERVALUNIT_ATT 	= "timeUnits";
  	private static final String FROM_ATT 		= "from";
  	private static final String NTRACKS_ATT		= "nTracks";
  	private static final String NSPOTS_ATT		= "nSpots";


  	private static final String TRACK_KEY = "particle";
  	private static final String SPOT_KEY = "detection";
  	private static final String X_ATT = "x";
  	private static final String Y_ATT = "y";
  	private static final String Z_ATT = "z";
  	private static final String T_ATT = "t";

 	private final TrackMateGUIController controller;

 	public GoToMarsAction( final TrackMateGUIController controller )
 	{
 		this.controller = controller;
 	}
 	@Override
 	public void execute(final TrackMate trackmate) {

 		logger.log("Exporting tracks to Mars MoleculeArchive.\n");
 		final Model model = trackmate.getModel();
 		final int ntracks = model.getTrackModel().nTracks(true);
 		if (ntracks == 0) {
 			logger.log("No visible track found. Aborting.\n");
 			return;
 		}

 		logger.log("  building archive.\n");
 		final Element root = marshall(model, trackmate.getSettings(), logger);

 		File folder;
 		try {
 			folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
 		} catch (final NullPointerException npe) {
 			folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
 		}

 		File file;
 		try {
 			String filename = trackmate.getSettings().imageFileName;
 			final int dot = filename.indexOf(".");
 			filename = dot < 0 ? filename : filename.substring(0, dot);
 			file = new File(folder.getPath() + File.separator + filename +"_Tracks.xml");
 		} catch (final NullPointerException npe) {
 			file = new File(folder.getPath() + File.separator + "Tracks.xml");
 		}
 		file = IOUtils.askForFileForSaving(file, controller.getGUI(), logger);
 		if (null == file) {
 			return;
 		}

 		logger.log("  Writing to file.\n");
 		final Document document = new Document(root);
 		final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
 		try {
 			outputter.output(document, new FileOutputStream(file));
 		} catch (final FileNotFoundException e) {
 			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
 		} catch (final IOException e) {
 			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
 		}
 		logger.log("Done.\n");
 	}

 	private static Element marshall(final Model model, final Settings settings, final Logger logger) {
 		logger.setStatus("Marshalling...");
 		final Element content = new Element(CONTENT_KEY);

 		content.setAttribute(NTRACKS_ATT, ""+model.getTrackModel().nTracks(true));
 		content.setAttribute(PHYSUNIT_ATT, model.getSpaceUnits());
 		content.setAttribute(FRAMEINTERVAL_ATT, ""+settings.dt);
 		content.setAttribute(FRAMEINTERVALUNIT_ATT, ""+model.getTimeUnits());
 		content.setAttribute(DATE_ATT, TMUtils.getCurrentTimeString());
 		content.setAttribute(FROM_ATT, TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION);

 		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
 		int i = 0;
 		for (final Integer trackID : trackIDs) {

 			final Set<Spot> track = model.getTrackModel().trackSpots(trackID);

 			final Element trackElement = new Element(TRACK_KEY);
 			trackElement.setAttribute(NSPOTS_ATT, ""+track.size());

 			// Sort them by time
 			final TreeSet<Spot> sortedTrack = new TreeSet<>(Spot.timeComparator);
 			sortedTrack.addAll(track);

 			for (final Spot spot : sortedTrack) {
 				final int frame = spot.getFeature(Spot.FRAME).intValue();
 				final double x = spot.getFeature(Spot.POSITION_X);
 				final double y = spot.getFeature(Spot.POSITION_Y);
 				final double z = spot.getFeature(Spot.POSITION_Z);

 				final Element spotElement = new Element(SPOT_KEY);
 				spotElement.setAttribute(T_ATT, ""+frame);
 				spotElement.setAttribute(X_ATT, ""+x);
 				spotElement.setAttribute(Y_ATT, ""+y);
 				spotElement.setAttribute(Z_ATT, ""+z);
 				trackElement.addContent(spotElement);
 			}
 			content.addContent(trackElement);
 			logger.setProgress(i++ / (0d + model.getTrackModel().nTracks(true)));
 		}

 		logger.setStatus("");
 		logger.setProgress(1);
 		return content;
 	}

 }
