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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;

import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.ContextCommand;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DoubleColumn;
import org.scijava.module.ModuleService;

import de.mpg.biochem.mars.molecule.SdmmImageMetadata;
import de.mpg.biochem.mars.molecule.SingleMolecule;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;

 /**
  * TrackMateAction to export tracks to a mars MoleculeArchive.
  * @author Karl Duderstadt
  */
 public class GoToMarsAction extends AbstractTMAction {

  	private static final String PHYSUNIT_ATT 	= "spaceUnits";
  	private static final String FRAMEINTERVAL_ATT 	= "frameInterval";
  	private static final String FRAMEINTERVALUNIT_ATT 	= "timeUnits";
  	private static final String FROM_ATT 		= "from";
  	private static final String NTRACKS_ATT		= "nTracks";
  	private static final String NSPOTS_ATT		= "nSpots";


  	//private static final String TRACK_KEY = "particle";
  	//private static final String SPOT_KEY = "detection";
  	private static final String X_ATT = "x";
  	private static final String Y_ATT = "y";
  	private static final String Z_ATT = "z";
  	//private static final String T_ATT = "t";

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
 		
 		logger.setStatus("building archive...");
 		
 		SingleMoleculeArchive archive = new SingleMoleculeArchive("FromTrackMate.yama");
 		
 		SdmmImageMetadata marsMetadata = new SdmmImageMetadata(trackmate.getSettings().imp, "unknown", "NULL", null);
 		archive.putMetadata(marsMetadata);
 		
 		//Build log
		LogBuilder builder = new LogBuilder();
		
		String log = LogBuilder.buildTitleBlock("Import from TrackMate");
		
		builder.addParameter(NTRACKS_ATT, ""+model.getTrackModel().nTracks(true));
		builder.addParameter(PHYSUNIT_ATT, model.getSpaceUnits());
		builder.addParameter(FRAMEINTERVAL_ATT, ""+trackmate.getSettings().dt);
		builder.addParameter(FRAMEINTERVALUNIT_ATT, ""+model.getTimeUnits());
		builder.addParameter(FROM_ATT, TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION);
 		
 		log += builder.buildParameterList();

 		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
 		int i = 0;
 		for (final Integer trackID : trackIDs) {
 			SingleMolecule molecule = new SingleMolecule(MarsMath.getUUID58());
 			molecule.setMetadataUID(marsMetadata.getUID());
 			
 			final Set<Spot> track = model.getTrackModel().trackSpots(trackID);

 			molecule.setParameter("TrackID", trackID);
 			molecule.setParameter(NSPOTS_ATT, track.size());

 			// Sort them by time
 			final TreeSet<Spot> sortedTrack = new TreeSet<>(Spot.timeComparator);
 			sortedTrack.addAll(track);
 			
 			MarsTable table = new MarsTable("DataTable");
 			DoubleColumn frameColumn = new DoubleColumn("frame");
 			DoubleColumn xColumn = new DoubleColumn(X_ATT);
 			DoubleColumn yColumn = new DoubleColumn(Y_ATT);
 			DoubleColumn zColumn = new DoubleColumn(Z_ATT);

 			for (final Spot spot : sortedTrack) {
 				final double frame = spot.getFeature(Spot.FRAME).intValue();
 				final double x = spot.getFeature(Spot.POSITION_X);
 				final double y = spot.getFeature(Spot.POSITION_Y);
 				final double z = spot.getFeature(Spot.POSITION_Z);

 				frameColumn.add(frame);
 				xColumn.add(x);
 				yColumn.add(y);
 				zColumn.add(z);
 			}
 			
 			table.add(frameColumn);
 			table.add(xColumn);
 			table.add(yColumn);
 			table.add(zColumn);
 			
 			molecule.setDataTable(table);
 			
 			archive.put(molecule);
 			logger.setProgress(i++ / (0d + model.getTrackModel().nTracks(true)));
 		}

 		logger.setStatus("");
 		logger.setProgress(1);
 		
 		archive.naturalOrderSortMoleculeIndex();
 		
 		archive.logln(log);
 		archive.logln(LogBuilder.endBlock(true));
 		
 		//I guess this can be accessed in TMUtils now with TMUtils.getContext() but this doesn't seem to be in the default version yet...
 		Context context = ( Context ) IJ.runPlugIn( "org.scijava.Context", "" );
 		
 		final Future<CommandModule> future =
		context.getService(CommandService.class).run(SpitOutMoleculeArchive.class, true, "input", archive);
	    // wait for the execution thread to complete
	    final Module module = ((ModuleService) context.getService(ModuleService.class)).waitFor(future);

 		logger.log("Done.\n");
 	}
 	
 	@Plugin(type = Command.class, name = "SpitOutMoleculeArchive")
	public static class SpitOutMoleculeArchive extends ContextCommand {

		@Parameter
		private SingleMoleculeArchive input;

		@Parameter(type = ItemIO.OUTPUT)
		private SingleMoleculeArchive output;

		@Override
		public void run() {
			output = input;
		}
	}
 }
