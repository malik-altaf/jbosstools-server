/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.archives.webtools.filesets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.archives.webtools.IntegrationPlugin;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;

public class NewServerFilesetHandler extends UnitedServerListener {
	private static NewServerFilesetHandler instance;
	public static NewServerFilesetHandler getDefault() {
		if( instance == null )
			instance = new NewServerFilesetHandler();
		return instance;
	}
	
	protected NewServerFilesetHandler() {
		IServerType[] types = ServerCore.getServerTypes();
		for( int i = 0; i < types.length; i++ ) {
			IPath fileToWrite = FilesetUtil.DEFAULT_FS_ROOT.append(types[i].getId());
			if( !fileToWrite.toFile().exists()) {
				IPath p = new Path("filesetdata").append(types[i].getId()); //$NON-NLS-1$
				URL url = FileLocator.find(IntegrationPlugin.getDefault().getBundle(), p, null);
				if( url != null ) {
					try {
					    InputStream fis  = url.openStream();
					    Fileset[] sets = FilesetUtil.loadFilesets(fis, null);
					    if( sets != null && sets.length != 0 ) {
							FilesetUtil.saveFilesets(fileToWrite.toFile(), sets);
					    }
					} catch(IOException ioe) {
						IntegrationPlugin.getDefault().getLog().log(
								new Status(IStatus.ERROR, IntegrationPlugin.PLUGIN_ID, ioe.getMessage(), ioe));
					}
				}
			}
		}
	}
	
	public void serverAdded(IServer server) {
		IPath fileToRead = FilesetUtil.DEFAULT_FS_ROOT.append(FilesetUtil.DEFAULT_FS_ALL_SERVERS);
		Fileset[] sets = FilesetUtil.loadFilesets(fileToRead.toFile(), null);
		ArrayList<Fileset> list = new ArrayList<Fileset>();
		list.addAll(Arrays.asList(sets));
		
		String typeId = server.getServerType().getId();
		fileToRead = FilesetUtil.DEFAULT_FS_ROOT.append(typeId);
		sets = FilesetUtil.loadFilesets(fileToRead.toFile(), null);
		list.addAll(Arrays.asList(sets));
		Fileset[] finalSets = (Fileset[]) list.toArray(new Fileset[list.size()]);

		FilesetUtil.saveFilesets(server, finalSets);
	}
}
