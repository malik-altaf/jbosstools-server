/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.xpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.util.internal.ProgressMonitorUtil;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Messages;
import org.eclipse.wst.server.core.internal.ProgressUtil;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.publishers.PublishUtil;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
/**
 * Utility class with an assortment of useful file methods.
 * <p>
 * This class provides all its functionality through static members.
 * It is not intended to be subclassed or instantiated.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 */
public final class PublishCopyUtil {
	public interface IPublishCopyCallbackHandler {
		/**
 		 * Copy the file (mf) to a file related to the relative path (path). 
		 * 
		 * For example if this path is "/someFolder/file.txt" you may want to 
		 * copy the file to /home/someone/deployfolder/project.war/someFolder/file.txt
		 * 
		 * @param mf
		 * @param path
		 * @param monitor
		 * @return a list of error status objects. 
		 * @throws CoreException
		 */
		public IStatus[] copyFile(IModuleFile mf, IPath path, IProgressMonitor monitor) throws CoreException;
		
		
		/**
		 * Delete a directory for this path relative to where the module belongs.
		 * For example if this path is "/someFolder" you may want to 
		 * delete the folder /home/someone/deployfolder/project.war/someFolder
		 * 
		 * @param dir
		 * @param monitor
		 * @return a list of error status objects. 
		 */
		public IStatus[] deleteResource(IPath path, IProgressMonitor monitor) throws CoreException ;
		
		/**
		 * Make a directory for this path relative to where the module belongs.
		 * For example if this path is "/someFolder" you may want to 
		 * make the folder /home/someone/deployfolder/project.war/someFolder
		 * 
		 * @param dir
		 * @param monitor
		 * @return a list of error status objects. 
		 */
		public IStatus[] makeDirectoryIfRequired(IPath dir, IProgressMonitor monitor) throws CoreException;
	}
	
	public static class LocalCopyCallback implements IPublishCopyCallbackHandler {

		private static final int BUFFER = 65536;
		private static byte[] buf = new byte[BUFFER];
		private static final File tempDir = ServerPlugin.getInstance().getStateLocation().toFile();
		private static final String TEMPFILE_PREFIX = "tmp"; //$NON-NLS-1$

		private boolean shouldRestartModule = false;
		
		private IServer server;
		private IPath deployRootFolder;
		private IPath tmpDeployRootFolder;
		public LocalCopyCallback(IServer server, IPath deployFolder, IPath temporaryFolder ) {
			this.server = server;
			this.deployRootFolder = deployFolder;
			this.tmpDeployRootFolder = temporaryFolder;
		}
		
		public boolean shouldRestartModule() {
			return shouldRestartModule;
		}
		
		private void checkRestartModule(File file) {
			if( file.getName().toLowerCase().endsWith(".jar")) //$NON-NLS-1$
				shouldRestartModule = true;
		}
		
		public IStatus[] copyFile(IModuleFile mf, IPath relativePath, IProgressMonitor monitor) throws CoreException {
			File file = PublishUtil.getFile(mf);
			checkRestartModule(file);
			if( file != null ) {
				InputStream in = null;
				try {
					in = new FileInputStream(file);
				} catch (IOException e) {
					return new IStatus[] {new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, 
							NLS.bind(Messages.errorReading, file.getAbsolutePath()), e)};
				}
				IStatus ret = copyFile(in, deployRootFolder.append(relativePath), file.lastModified(), mf);
				if( ret != null && ret.isOK())
					return new IStatus[] { ret };
			} // else silently ignore I guess
			return new IStatus[]{};
		}

		/**
		 * Copy a file from a to b. Closes the input stream after use.
		 *
		 * @param in java.io.InputStream
		 * @param to java.lang.String
		 * @return a status
		 */
		private IStatus copyFile(InputStream in, String to) {
			OutputStream out = null;
			
			try {
				out = new FileOutputStream(to);
				
				int avail = in.read(buf);
				while (avail > 0) {
					out.write(buf, 0, avail);
					avail = in.read(buf);
				}
				return Status.OK_STATUS;
			} catch (Exception e) {
				//Trace.trace(Trace.SEVERE, "Error copying file", e);
				return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCopyingFile, new String[] {to, e.getLocalizedMessage()}), e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (Exception ex) {
					// ignore
				}
				try {
					if (out != null)
						out.close();
				} catch (Exception ex) {
					// ignore
				}
			}
		}
		
		/**
		 * Copy a file from a to b. Closes the input stream after use.
		 * 
		 * @param in an input stream
		 * @param to a path to copy to. the directory must already exist. This must be an absolute path
		 * @param ts timestamp
		 * @throws CoreException if anything goes wrong
		 */
		private IStatus copyFile(InputStream in, IPath to, long ts, IModuleFile mf) throws CoreException {
			OutputStream out = null;
			
			File tempFile = null;
			try {
				File file = to.toFile();
				
				// Change from original PublishUtil, will require 
				tempFile = File.createTempFile(TEMPFILE_PREFIX, "." + to.getFileExtension(), getTempFolder()); //$NON-NLS-1$
				
				out = new FileOutputStream(tempFile);
				
				int avail = in.read(buf);
				while (avail > 0) {
					out.write(buf, 0, avail);
					avail = in.read(buf);
				}
				
				out.close();
				out = null;
				
				moveTempFile(tempFile, file);
				
				if (ts != IResource.NULL_STAMP && ts != 0)
					file.setLastModified(ts);
			} catch (CoreException e) {
				throw e;
			} catch (Exception e) {
				IPath path = mf.getModuleRelativePath().append(mf.getName());
				return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCopyingFile, path.toOSString(), e.getLocalizedMessage()), null);
			} finally {
				if (tempFile != null && tempFile.exists())
					tempFile.deleteOnExit();
				try {
					if (in != null)
						in.close();
				} catch (Exception ex) {
					// ignore
				}
				try {
					if (out != null)
						out.close();
				} catch (Exception ex) {
					// ignore
				}
			}
			return null;
		}
		
		/**
		 * Utility method to move a temp file into position by deleting the original and
		 * swapping in a new copy.
		 *  
		 * @param tempFile
		 * @param file
		 * @throws CoreException
		 */
		private void moveTempFile(File tempFile, File file) throws CoreException {
			if (file.exists()) {
				if (!safeDelete(file, 2)) {
					// attempt to rewrite an existing file with the tempFile contents if
					// the existing file can't be deleted to permit the move
					try {
						InputStream in = new FileInputStream(tempFile);
						IStatus status = copyFile(in, file.getPath());
						if (!status.isOK()) {
							MultiStatus status2 = new MultiStatus(ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, file.toString()), null);
							status2.add(status);
							throw new CoreException(status2);
						}
						return;
					} catch (FileNotFoundException e) {
						// shouldn't occur
					} finally {
						tempFile.delete();
					}
					/*if (!safeDelete(file, 8)) {
						tempFile.delete();
						throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, file.toString()), null));
					}*/
				}
			}
			if (!safeRename(tempFile, file, 10))
				throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, 
						NLS.bind(org.jboss.ide.eclipse.as.core.Messages.PublishRenameFailure, 
								tempFile.toString(), file.getAbsolutePath()), null));
		}
		
		/**
		 * Safe rename. Will try multiple times before giving up.
		 * 
		 * @param from
		 * @param to
		 * @param retrys number of times to retry
		 * @return <code>true</code> if it succeeds, <code>false</code> otherwise
		 */
		private boolean safeRename(File from, File to, int retrys) {
			boolean renamedSafely = false;
			// make sure parent dir exists
			File dir = to.getParentFile();
			if (dir != null && !dir.exists())
				dir.mkdirs();
			
			int count = 0;
			while (count < retrys && !renamedSafely) {
				if (from.renameTo(to))
					renamedSafely = true;
				
				count++;
				// delay if we are going to try again
				if (count < retrys) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						// ignore
					}
				}
			}
			return renamedSafely;
		}
		
		protected File getTempFolder() {
			File f = null;
			if( tmpDeployRootFolder != null ) {
				f = tmpDeployRootFolder.toFile();
			} else if( server != null ){
				String path = ServerConverter.getDeployableServer(server).getTempDeployFolder();
				f = new File(path);
			} else {
				return tempDir;
			}
			if( !f.exists() )
				f.mkdirs();
			return f;
		}
		
		public IStatus[] deleteResource(IPath resource, IProgressMonitor monitor) {
			resource = deployRootFolder.append(resource);
			if( resource.toFile().isDirectory())
				return deleteDirectory(resource.toFile(), monitor);
			if( !resource.toFile().delete()) {
				IStatus s = new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, resource.toFile().getAbsolutePath()), null);
				return new IStatus[]{s};
			}
			return new IStatus[] {};
		}

		/**
		 * Utility method to recursively delete a directory.
		 *
		 * @param dir a directory
		 * @param monitor a progress monitor, or <code>null</code> if progress
		 *    reporting and cancellation are not desired
		 * @return a possibly-empty array of error and warning status
		 */
		private IStatus[] deleteDirectory(File dir, IProgressMonitor monitor) {
			if (!dir.exists() || !dir.isDirectory())
				return new IStatus[] { new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorNotADirectory, dir.getAbsolutePath()), null) };
			
			List status = new ArrayList(2);
			
			try {
				File[] files = dir.listFiles();
				int size = files.length;
				monitor = ProgressUtil.getMonitorFor(monitor);
				monitor.beginTask(NLS.bind(Messages.deletingTask, new String[] { dir.getAbsolutePath() }), size * 10);
				
				// cycle through files
				boolean deleteCurrent = true;
				for (int i = 0; i < size; i++) {
					File current = files[i];
					if (current.isFile()) {
						if (!current.delete()) {
							status.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, files[i].getAbsolutePath()), null));
							deleteCurrent = false;
						}
						monitor.worked(10);
					} else if (current.isDirectory()) {
						monitor.subTask(NLS.bind(Messages.deletingTask, new String[] {current.getAbsolutePath()}));
						IStatus[] stat = deleteDirectory(current, ProgressUtil.getSubMonitorFor(monitor, 10));
						if (stat != null && stat.length > 0) {
							deleteCurrent = false;
							addArrayToList(status, stat);
						}
					}
				}
				if (deleteCurrent && !dir.delete())
					status.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, dir.getAbsolutePath()), null));
				monitor.done();
			} catch (Exception e) {
				//Trace.trace(Trace.SEVERE, "Error deleting directory " + dir.getAbsolutePath(), e);
				status.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null));
			}
			
			IStatus[] stat = new IStatus[status.size()];
			status.toArray(stat);
			return stat;
		}

		/**
		 * Safe delete. Tries to delete multiple times before giving up.
		 * 
		 * @param f
		 * @return <code>true</code> if it succeeds, <code>false</code> otherwise
		 */
		private  boolean safeDelete(File f, int retrys) {
			int count = 0;
			while (count < retrys) {
				if (!f.exists())
					return true;
				
				f.delete();
				
				if (!f.exists())
					return true;
				
				count++;
				// delay if we are going to try again
				if (count < retrys) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						// ignore
					}
				}
			}
			return false;
		}
		
		public IStatus[] makeDirectoryIfRequired(IPath relativeDir, IProgressMonitor monitor) {
			deployRootFolder.append(relativeDir).toFile().mkdirs();
			return new IStatus[] {Status.OK_STATUS};
		}
		
	}

	private static final IStatus[] EMPTY_STATUS = new IStatus[0];
	private IPublishCopyCallbackHandler handler;
	public PublishCopyUtil(IPublishCopyCallbackHandler handler) {
		this.handler = handler;
	}

	protected IStatus[] canceledStatus() {
		return new IStatus[]{
				new Status(IStatus.CANCEL, JBossServerCorePlugin.PLUGIN_ID, "Publish Canceled") //$NON-NLS-1$
			}; // todo
	}


	/**
	 * Handle a delta publish.
	 * 
	 * @param delta a module resource delta
	 * @param path the path to publish to
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @return a possibly-empty array of error and warning status
	 */
	public IStatus[] publishDelta(IModuleResourceDelta[] delta, IProgressMonitor monitor) throws CoreException {
		if (delta == null)
			return EMPTY_STATUS;
		
		monitor = ProgressUtil.getMonitorFor(monitor);
		
		List status = new ArrayList(2);
		int size2 = delta.length;
		for (int i = 0; i < size2; i++) {
			IStatus[] stat = publishDelta(delta[i], new Path("/"), monitor); //$NON-NLS-1$
			addArrayToList(status, stat);
		}
		
		IStatus[] stat = new IStatus[status.size()];
		status.toArray(stat);
		return stat;
	}

	/**
	 * Handle a delta publish.
	 * 
	 * @param delta a module resource delta
	 * @param path the path to publish to
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @return a possibly-empty array of error and warning status
	 */
	public IStatus[] publishDelta(IModuleResourceDelta delta, IPath path, IProgressMonitor monitor) throws CoreException {
		List status = new ArrayList(2);
		if( monitor.isCanceled())
			return canceledStatus();

		IModuleResource resource = delta.getModuleResource();
		int kind2 = delta.getKind();
		
		if (resource instanceof IModuleFile) {
			IModuleFile file = (IModuleFile) resource;
			if (kind2 == IModuleResourceDelta.REMOVED) {
				IPath path2 = path.append(file.getModuleRelativePath()).append(file.getName());
				handler.deleteResource(path2, monitor);
			}
			else {
				IPath path2 = path.append(file.getModuleRelativePath()).append(file.getName());
				handler.makeDirectoryIfRequired(path2.removeLastSegments(1), monitor);
				handler.copyFile(file, path2, monitor);
			}
			IStatus[] stat = new IStatus[status.size()];
			status.toArray(stat);
			return stat;
		}
		
		if (kind2 == IModuleResourceDelta.ADDED) {
			IPath path2 = path.append(resource.getModuleRelativePath()).append(resource.getName());
			IStatus[] s = handler.makeDirectoryIfRequired(path2, monitor);
			if( s.length > 0 && !s[0].isOK()) {
				return s;
			}
		}
		
		IModuleResourceDelta[] childDeltas = delta.getAffectedChildren();
		int size = childDeltas.length;
		for (int i = 0; i < size; i++) {
			if( monitor.isCanceled())
				return canceledStatus();
			IStatus[] stat = publishDelta(childDeltas[i], path, monitor);
			addArrayToList(status, stat);
		}
		
		if (kind2 == IModuleResourceDelta.REMOVED) {
			IPath path2 = path.append(resource.getModuleRelativePath()).append(resource.getName());
			IStatus[] stat = handler.deleteResource(path2, monitor);
			if( stat.length > 0 && !stat[0].isOK()) {
				status.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDeleting, path2), null));
			}
		}
		
		IStatus[] stat = new IStatus[status.size()];
		status.toArray(stat);
		return stat;
	}


	/**
	 * Publish the given module resources to the given path.
	 * 
	 * @param resources an array of module resources
	 * @param path a path to publish to
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @return a possibly-empty array of error and warning status
	 */
	public IStatus[] publishFull(IModuleResource[] resources, IProgressMonitor monitor) throws CoreException  {
		monitor.beginTask("Publishing " + resources.length + " resources", 100 * (resources.length) + 100); //$NON-NLS-1$ //$NON-NLS-2$
		handler.makeDirectoryIfRequired(new Path("/"), ProgressMonitorUtil.submon(monitor, 100)); //$NON-NLS-1$
		if( monitor.isCanceled())
			return canceledStatus();
		IStatus[] results = publishFull(resources, new Path("/"), ProgressMonitorUtil.submon(monitor, 100*resources.length)); //$NON-NLS-1$
		monitor.done();
		return results;
	}
	
	public IStatus[] publishFull(IModuleResource[] resources, IPath relative, IProgressMonitor monitor) throws CoreException {
		if (resources == null)
			return EMPTY_STATUS;
		
		monitor = ProgressUtil.getMonitorFor(monitor);
		monitor.beginTask("Publishing " + resources.length + " resources", 100 * (resources.length)); //$NON-NLS-1$ //$NON-NLS-2$
		List status = new ArrayList(2);
		int size = resources.length;
		for (int i = 0; i < size; i++) {
			if( monitor.isCanceled())
				return canceledStatus();
			IStatus[] stat = copy(resources[i], relative, ProgressMonitorUtil.submon(monitor, 100)); 
			addArrayToList(status, stat);
		}
		monitor.done();
		IStatus[] stat = new IStatus[status.size()];
		status.toArray(stat);
		return stat;
	}

	private IStatus[] copy(IModuleResource resource, IPath path, IProgressMonitor monitor) throws CoreException {
		String name = resource.getName();
		//Trace.trace(Trace.PUBLISHING, "Copying: " + name + " to " + path.toString());
		List status = new ArrayList(2);
		if (resource instanceof IModuleFolder) {
			IModuleFolder folder = (IModuleFolder) resource;
			IModuleResource[] children = folder.members();
			if( children.length == 0 )
				handler.makeDirectoryIfRequired(folder.getModuleRelativePath().append(folder.getName()), monitor);
			else {
				IStatus[] stat = publishFull(children, path, monitor);
				addArrayToList(status, stat);
			}
		} else {
			IModuleFile mf = (IModuleFile) resource;
			path = path.append(mf.getModuleRelativePath()).append(name);
			IStatus[] stats = handler.makeDirectoryIfRequired(path.removeLastSegments(1), new NullProgressMonitor());
			if( stats != null && stats.length > 0 && !stats[0].isOK())
				addArrayToList(status, stats);

			addArrayToList(status, handler.copyFile(mf, path, monitor));
		}
		IStatus[] stat = new IStatus[status.size()];
		status.toArray(stat);
		return stat;
	}

	public static void addArrayToList(List list, IStatus[] a) {
		if (list == null || a == null || a.length == 0)
			return;
		
		int size = a.length;
		for (int i = 0; i < size; i++)
			list.add(a[i]);
	}
}