/*-
 * #%L
 * TrackMate Mars export plugin
 * %%
 * Copyright (C) 2020 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.trackmate;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.gui.components.LogPanel;
import ij.IJ;
import ij.ImagePlus;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.PositiveInteger;

import java.awt.Frame;
import java.util.Collection;
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

import de.mpg.biochem.mars.image.PeakShape;
import de.mpg.biochem.mars.metadata.*;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.SingleMolecule;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;
import de.mpg.biochem.mars.object.MartianObject;
import de.mpg.biochem.mars.object.ObjectArchive;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.LogBuilder;
import de.mpg.biochem.mars.util.MarsMath;

import java.lang.reflect.Field;

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
  	private static final String X_ATT = "X";
  	private static final String Y_ATT = "Y";
  	private static final String Z_ATT = "Z";
  	private static final String T_ATT = "T";
 	
 	@SuppressWarnings("unchecked")
	@Override
	public void execute(TrackMate trackmate, SelectionModel selectionModel, DisplaySettings displaySettings,
			Frame parent) {
 		logger.log("Exporting tracks to Mars MoleculeArchive.\n");
 		final Model model = trackmate.getModel();
 		final FeatureModel fm = model.getFeatureModel();
 		
 		final int ntracks = model.getTrackModel().nTracks(true);
 		if (ntracks == 0) {
 			logger.log("No visible track found. Aborting.\n");
 			return;
 		}
 		
 		final Collection< String > spotFeatures = trackmate.getModel().getFeatureModel().getSpotFeatures();
 		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
 		final boolean spotsHaveShape = (model.getTrackModel().trackSpots(trackIDs.stream().findAny().get()).stream().findAny().get().getRoi() != null) ? true : false; 
 		
 		String archiveType = (spotsHaveShape) ? "ObjectArchive" : "SingleMoleculeArchive";
 		
 		logger.log("  building " + archiveType + ".\n");
 		logger.setStatus("building " + archiveType + "...");
 		
 		//Build log
		LogBuilder builder = new LogBuilder();
		
		String log = LogBuilder.buildTitleBlock("Import from TrackMate");
		
		builder.addParameter("Type", archiveType);
		builder.addParameter(NTRACKS_ATT, ""+model.getTrackModel().nTracks(true));
		builder.addParameter(PHYSUNIT_ATT, model.getSpaceUnits());
		builder.addParameter(FRAMEINTERVAL_ATT, ""+trackmate.getSettings().dt);
		builder.addParameter(FRAMEINTERVALUNIT_ATT, ""+model.getTimeUnits());
		builder.addParameter(FROM_ATT, TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION);
 		
 		log += builder.buildParameterList();
 		
 		@SuppressWarnings("rawtypes")
		MoleculeArchive archive = (spotsHaveShape) ? new ObjectArchive("FromTrackMate.yama") : new SingleMoleculeArchive("FromTrackMate.yama");
 		
 		//trackmate.
 		ImagePlus imp = trackmate.getSettings().imp;
 		int[] dim = imp.getDimensions();
 		
 		MarsOMEMetadata marsMetadata = new MarsOMEMetadata();
 		
 		MarsOMEImage image = new MarsOMEImage();
 		
 		image.setName("Unknown");
 		image.setID("Unknown");
 		
 		if (imp.getOriginalFileInfo() != null && imp.getOriginalFileInfo().directory != null) {
			marsMetadata.setSourceDirectory(imp.getOriginalFileInfo().directory);
			image.setName(imp.getOriginalFileInfo().directory);
			if (imp.getOriginalFileInfo().description != null)
				image.setDescription(imp.getOriginalFileInfo().description);
		}
 		
		image.setImageID(0);
		image.setSizeX(new PositiveInteger(dim[0]));
		image.setSizeY(new PositiveInteger(dim[1]));
		image.setSizeC(new PositiveInteger(dim[2]));
		image.setSizeZ(new PositiveInteger(dim[3]));
		image.setSizeT(new PositiveInteger(dim[4]));
		
		image.setTimeIncrementInSeconds(trackmate.getSettings().dt);
		
 		for (int channelIndex=0; channelIndex < dim[2]; channelIndex++) {
			MarsOMEChannel channel = new MarsOMEChannel();
			image.setChannel(channel, channelIndex);
		}
			
 		image.setDimensionOrder(DimensionOrder.valueOf("XYCZT"));
 		
 		//Loop through all dimensions in nested loops and generate planes for all.
		for (int z=0; z < image.getSizeZ(); z++)
			for (int c=0; c < image.getSizeC(); c++)
				for (int t=0; t < image.getSizeT(); t++) {
					int planeIndex = (int) image.getPlaneIndex(z, c, t);
					MarsOMEPlane plane = new MarsOMEPlane(image, 0, planeIndex, 
							new NonNegativeInteger(z), 
							new NonNegativeInteger(c),
							new NonNegativeInteger(t));
					image.setPlane(plane, planeIndex);
				}
 		
		marsMetadata.setImage(image, 0);
 		archive.putMetadata(marsMetadata);
 		
 		int i = 0;
 		for (final Integer trackID : trackIDs) {
 			Molecule molecule = (spotsHaveShape) ? new MartianObject(MarsMath.getUUID58()) : new SingleMolecule(MarsMath.getUUID58());
 			molecule.setMetadataUID(marsMetadata.getUID());
 			
 			final Set<Spot> track = model.getTrackModel().trackSpots(trackID);

 			molecule.setParameter("TrackID", trackID);
 			molecule.setParameter(NSPOTS_ATT, track.size());

 			// Sort them by time
 			final TreeSet<Spot> sortedTrack = new TreeSet<>(Spot.timeComparator);
 			sortedTrack.addAll(track);
 			
 			MarsTable table = new MarsTable("Table");
 			DoubleColumn tColumn = new DoubleColumn(T_ATT);
 			DoubleColumn xColumn = new DoubleColumn(X_ATT);
 			DoubleColumn yColumn = new DoubleColumn(Y_ATT);
 			DoubleColumn zColumn = new DoubleColumn(Z_ATT);
 			
 			table.add(tColumn);
 			table.add(xColumn);
 			table.add(yColumn);
 			table.add(zColumn);
 			
 			for ( final String feature : spotFeatures ) {
 				if (!feature.equals(Spot.FRAME) 
 						&& !feature.equals(Spot.POSITION_X) 
 						&& !feature.equals(Spot.POSITION_Y) 
 						&& !feature.equals(Spot.POSITION_Z)) {
 					DoubleColumn col = new DoubleColumn(feature);
 					table.add(col);
 				}
 			}

 			int row = 0;
 			for (final Spot spot : sortedTrack) {
 				table.appendRow();
 				
 				//We add these manually to ensure they are the first columns in the table.
 				final int frame = spot.getFeature(Spot.FRAME).intValue();
 				final double x = spot.getFeature(Spot.POSITION_X);
 				final double y = spot.getFeature(Spot.POSITION_Y);
 				final double z = spot.getFeature(Spot.POSITION_Z);

 				table.setValue(T_ATT, row, frame - 1);
 				table.setValue(X_ATT, row, x);
 				table.setValue(Y_ATT, row, y);
 				table.setValue(Z_ATT, row, z);
 				
 				for ( final String feature : spotFeatures ) {
 					if (feature.equals(Spot.FRAME) 
 	 						|| feature.equals(Spot.POSITION_X) 
 	 						|| feature.equals(Spot.POSITION_Y) 
 	 						|| feature.equals(Spot.POSITION_Z))
 								continue;	
 					
 					final Double val = spot.getFeature( feature );
 					if ( null == val ) {
 						table.setValue(feature, row, Double.NaN);
 					} else {
 						if ( fm.getSpotFeatureIsInt().get( feature ).booleanValue() )
 						{
 							table.setValue(feature, row, val.intValue() );
 						}
 						else
 						{
 							table.setValue(feature, row, val.doubleValue() );
 						}
 					}
 				}
 				row++;
 				
 				if (spotsHaveShape) ((MartianObject) molecule).putShape(frame - 1, new PeakShape(spot.getRoi().x, spot.getRoi().y));
 			}
 			
 			molecule.setTable(table);
 			
 			archive.put(molecule);
 			logger.setProgress(i++ / (0d + model.getTrackModel().nTracks(true)));
 		}

 		logger.setStatus("");
 		logger.setProgress(1);
 		
 		archive.logln(log);
 		archive.log(model.getLogger().toString());
 		archive.logln(LogBuilder.endBlock(true));
 		
 		//I guess this can be accessed in TMUtils now with TMUtils.getContext() but this doesn't seem to be in the default version yet...
 		Context context = ( Context ) IJ.runPlugIn( "org.scijava.Context", "" );
 		
 		final Future<CommandModule> future =
		context.getService(CommandService.class).run(SpitOutMoleculeArchive.class, true, "input", archive);
	    // wait for the execution thread to complete
	    final Module module = ((ModuleService) context.getService(ModuleService.class)).waitFor(future);

 		logger.log("Done.\n");
 	}
 	
 	@Plugin(type = Command.class, name = "Export to MoleculeArchive from Trackmate")
	public static class SpitOutMoleculeArchive extends ContextCommand {

		@Parameter
		private MoleculeArchive<?, ?, ?, ?> input;

		@Parameter(type = ItemIO.OUTPUT)
		private MoleculeArchive<?, ?, ?, ?> TrackmateResults;

		@Override
		public void run() {
			TrackmateResults = input;
		}
	}

 }
