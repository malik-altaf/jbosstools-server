/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.internal.extendedproperties;

import static org.jboss.ide.eclipse.as.core.util.IJBossRuntimeResourceConstants.AS7_DEPLOYMENTS;
import static org.jboss.ide.eclipse.as.core.util.IJBossRuntimeResourceConstants.AS7_STANDALONE;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.launching.environments.EnvironmentsManager;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.jboss.ide.eclipse.as.core.Messages;
import org.jboss.ide.eclipse.as.core.resolvers.ConfigNameResolver;
import org.jboss.ide.eclipse.as.core.server.IDefaultLaunchArguments;
import org.jboss.ide.eclipse.as.core.server.IDeploymentScannerModifier;
import org.jboss.ide.eclipse.as.core.server.IServerModuleStateVerifier;
import org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7ModuleStateVerifier;
import org.jboss.ide.eclipse.as.core.server.internal.v7.LocalJBoss7DeploymentScannerAdditions;
import org.jboss.ide.eclipse.as.core.server.internal.v7.LocalJBoss7ServerRuntime;
import org.jboss.ide.eclipse.as.core.util.IJBossRuntimeResourceConstants;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.ide.eclipse.as.management.core.IJBoss7ManagerService;
import org.jboss.ide.eclipse.as.management.core.IJBossManagerServiceProvider;
import org.jboss.ide.eclipse.as.management.core.JBoss7ManagerUtil;

public class JBossAS7ExtendedProperties extends JBossExtendedProperties implements IJBossManagerServiceProvider {
	public JBossAS7ExtendedProperties(IAdaptable obj) {
		super(obj);
	}
	public String getNewXPathDefaultRootFolder() {
		return ""; //$NON-NLS-1$
	}

	public String getNewFilesetDefaultRootFolder() {
		return IJBossRuntimeResourceConstants.AS7_STANDALONE + "/" + IJBossRuntimeResourceConstants.CONFIGURATION; //$NON-NLS-1$
	}
	
	public String getNewClasspathFilesetDefaultRootFolder() {
		return IJBossRuntimeResourceConstants.AS7_MODULES + "/org"; //$NON-NLS-1$
	}

	public boolean runtimeSupportsExposingManagement() {
		return true;
	}
	
	public int getJMXProviderType() {
		return JMX_DEFAULT_PROVIDER;
	}

	public boolean runtimeSupportsBindingToAllInterfaces() {
		String version = getServerBeanLoader().getFullServerVersion();
		if( version == null )
			return true;
		if( version.startsWith("7.0.1") || version.startsWith("7.0.0"))  //$NON-NLS-1$//$NON-NLS-2$
			return false;
		return true;
	}
	public int getMultipleDeployFolderSupport() {
		return DEPLOYMENT_SCANNER_AS7_MANAGEMENT_SUPPORT;
	}

	public String getVerifyStructureErrorMessage() throws CoreException {
		if( server.getRuntime() == null ) 
			return NLS.bind(Messages.ServerMissingRuntime, server.getName());
		if( !server.getRuntime().getLocation().toFile().exists())
			return NLS.bind(Messages.RuntimeFolderDoesNotExist, server.getRuntime().getLocation().toOSString());
		IRuntime rt = server.getRuntime();
		LocalJBoss7ServerRuntime rt2 = (LocalJBoss7ServerRuntime)rt.loadAdapter(LocalJBoss7ServerRuntime.class, null);
		String cfile = rt2.getConfigurationFile();
		IPath cFilePath = rt.getLocation().append(IJBossRuntimeResourceConstants.AS7_STANDALONE)
				.append(IJBossRuntimeResourceConstants.CONFIGURATION).append(cfile);
		if( !cFilePath.toFile().exists())
			return NLS.bind(Messages.JBossAS7ConfigurationFileDoesNotExist, cFilePath.toOSString());
		return null;
	}

	public boolean canVerifyRemoteModuleState() {
		return true;
	}
	
	public IServerModuleStateVerifier getModuleStateVerifier() {
		return new JBoss7ModuleStateVerifier();
	}
	public IDefaultLaunchArguments getDefaultLaunchArguments() {
		if( server != null)
			return new JBoss70DefaultLaunchArguments(server);
		return new JBoss70DefaultLaunchArguments(runtime);
	}

	public IDeploymentScannerModifier getDeploymentScannerModifier() {
		return new LocalJBoss7DeploymentScannerAdditions();
	}

	
	public String getJBossAdminScript() {
		return IJBossRuntimeResourceConstants.AS_70_MANAGEMENT_SCRIPT;
	}
	
	public int getFileStructure() {
		return FILE_STRUCTURE_CONFIG_DEPLOYMENTS;
	}
	public IExecutionEnvironment getDefaultExecutionEnvironment() {
		return EnvironmentsManager.getDefault().getEnvironment("JavaSE-1.6"); //$NON-NLS-1$
	}
	
	/**
	 * @since 2.5
	 */
	public IJBoss7ManagerService getManagerService() {
		return JBoss7ManagerUtil.getManagerService(getManagerServiceId());
	}
	
	/**
	 * @since 2.5
	 */
	public String getManagerServiceId() {
		return IJBoss7ManagerService.AS_VERSION_710_Beta;
	}
	
	/**
	 * Returns the full path of a local server's server/{config}/deploy folder
	 */
	@Override
	public String getServerDeployLocation() {
		if( runtime == null )
			return null;
		IPath p = runtime.getLocation().append(AS7_STANDALONE).append(AS7_DEPLOYMENTS);
		return ServerUtil.makeGlobal(runtime, p).toString();
	}
}
