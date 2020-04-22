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

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import javax.swing.ImageIcon;
import org.scijava.plugin.Plugin;

@Plugin( type = TrackMateActionFactory.class )
public class GoToMarsActionFactory implements TrackMateActionFactory {
	public static final ImageIcon ICON = new ImageIcon(GoToMarsActionFactory.class.getResource("mars_icon.png"));
 	public static final String NAME = "Go to Mars";

 	public static final String KEY = "EXPORT_TRACKS_TO_MARS";
 	public static final String INFO_TEXT = "<html>" +
 				"Export the tracks in the current model content to a Mars " +
 				"MoleculeArchive and provide the archive as an output. " +
 				"<p> " +
 				"The MoleculeArchive will have one element per track, and each track " +
 				"contains several spot elements. These spots are " +
 				"sorted by frame number, and have 4 numerical attributes: " +
 				"the frame number this spot is in, and its X, Y, Z position in " +
 				"physical units as specified in the image properties. " +
 				"<p>" +
 				"Currently this format <u>cannot</u> handle track merging and " +
 				"splitting properly, and is suited only for non-branching tracks." +
 				"</html>";
 	
	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public TrackMateAction create( final TrackMateGUIController controller )
	{
		return new GoToMarsAction( controller );
	}

	@Override
	public ImageIcon getIcon()
	{
		return ICON;
	}
}
