/**
 * Copyright (c) 2010 Ben Fry and Casey Reas
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.ui.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import processing.plugin.core.builder.SketchProject;
import processing.plugin.ui.ProcessingPlugin;

public class RunSketchAsAppletShortcut implements ILaunchShortcut {

	protected ILaunchConfiguration createConfiguration(IProject project) {
		if (project == null) return null;
		SketchProject sketch = SketchProject.forProject(project);
		ILaunchConfiguration config = null;
		try{
			ILaunchConfigurationType configType = getConfigurationType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, project.getName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, sketch.getMainType());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_WIDTH, sketch.getWidth());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_HEIGHT, sketch.getHeight());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_NAME, "Processing Sketch");
			wc.setAttribute("wasLastBuildSuccessful", sketch.wasLastBuildSuccessful());
			wc.setMappedResources(new IResource[] { sketch.getJavaProject().getUnderlyingResource() });
//			config =wc.doSave();
			config = wc; // this prevents a run config from being saved and sticking around.
		} catch (CoreException ce) {
			ProcessingPlugin.logError(ce);
		}
		return config;
	}

	protected ILaunchConfigurationType getConfigurationType() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLET);
	}
	
	public void launch(IProject proj, String mode) {
		try{
			if (proj.hasNature("processing.plugin.core.sketchNature"))
				launch(createConfiguration(proj), mode);
			else 
				ProcessingPlugin.logInfo("Sketch could not be launched. "
						+ "The selected project does not have the required Sketch nature.");
		} catch (CoreException e) {
			ProcessingPlugin.logError("Launch aborted. CoreException error occured while accessing the project.", e);
		}
	}

	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof IResource) {
				IProject proj = ((IResource)element).getProject();
				if (proj.isAccessible()) {
					launch(proj, mode);
				} else {
					ProcessingPlugin.logInfo("Sketch could not be launched. " +
					"Project was not accessible.");
				}
//			if (element instanceof JavaProject) {
//				/* The Java perspective hijacks Processing sketches and reports them through the selection
//				 * interface as JavaProjects instead of IProjects or IResources or SketchProjects, all of
//				 * which they happen to be, so I'm stuck accessing restricted code. Great. 
//				 */
//				IProject project = ((JavaProject)element).getProject();
//				if(project.isAccessible()) {
//					launch( project, mode);
//				} else {
//					ProcessingLog.logInfo("Sketch could not be launched. " +
//					"Project was not accessible. Try launching from the Processing Perspective");
//				}
//			}
			}else {
//				System.out.println(element.getClass().getName());
				ProcessingPlugin.logInfo("Sketch could not be launched. " +
						"The launcher could not find a Sketch project associated with the provided selection." + 
						"Try relaunching from the Processing Perspective, or launching a specific *.pde file.");
			}
		}
	}

	
	public void launch(IEditorPart editor, String mode) {
		IFile file = ResourceUtil.getFile(editor.getEditorInput());
		
		if(file == null) {
			ProcessingPlugin.logInfo("Launch aborted. Editor contents are not part of a Sketch Project in the workspace");
			return;
		}
		
		IProject proj = file.getProject();
		try{
			if (!proj.hasNature("processing.plugin.core.sketchNature")) {
				ProcessingPlugin.logInfo("Sketch could not be launched. The editor contains a file that is not part of a project with a Sketch nature.");
				return;
			}
			launch(createConfiguration(proj), mode);
		} catch (CoreException e) {
			ProcessingPlugin.logError("Launch aborted.", e);
		}
	}

	private void launch(ILaunchConfiguration config, String mode) {
		if (config == null) return;
		DebugUITools.launch(config, mode);
	}

	
}
