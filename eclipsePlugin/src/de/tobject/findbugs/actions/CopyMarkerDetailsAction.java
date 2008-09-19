/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.actions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.view.explorer.BugPatternGroup;

public class CopyMarkerDetailsAction implements IObjectActionDelegate {

	private ISelection selection;

	public CopyMarkerDetailsAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection newSelection) {
		this.selection = newSelection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// noop
	}

	public void run(IAction action) {
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
			return;
		}
		Set<IMarker> markers = getMarkers();
		String content = getContent(markers);
		copyToClipboard(content);
	}

	private void copyToClipboard(String content) {
		Clipboard cb = null;
		try {
			cb = new Clipboard(Display.getDefault());
			cb.setContents(new String[] { content }, new TextTransfer[] { TextTransfer
					.getInstance() });
		} finally {
			if (cb != null) {
				cb.dispose();
			}
		}
	}

	private String getContent(Set<IMarker> markers) {
		StringBuilder fullText = new StringBuilder();
		for (IMarker marker : markers) {
			try {
				StringBuilder line = new StringBuilder();

				IResource resource = marker.getResource();
				if (resource != null) {
					IPath location = resource.getLocation();
					if(location != null){
						line.append(location.toPortableString());
					} else {
						line.append(resource.getFullPath());
					}
				}
				Integer lineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
				line.append(":").append(lineNumber);
				String message = (String) marker.getAttribute(IMarker.MESSAGE);
				line.append(" ").append(message);

				line.append(System.getProperty("line.separator", "\n"));
				fullText.append(line.toString());
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
					"Exception while parsing content of FindBugs markers.");
			}
		}
		return fullText.toString();
	}

	private Set<IMarker> getMarkers() {
		Set<IMarker> markers = new HashSet<IMarker>();
		try {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			for (Iterator<?> iter = sSelection.iterator(); iter.hasNext();) {
				Object next = iter.next();
				if(next instanceof IMarker){
					IMarker marker = (IMarker) next;
					if (!marker.isSubtypeOf(FindBugsMarker.NAME)) {
						continue;
					}
					markers.add(marker);
				} else if (next instanceof IAdaptable){
					IAdaptable adapter = (IAdaptable) next;
					IMarker marker = (IMarker) adapter.getAdapter(IMarker.class);
					if (marker == null || !marker.isSubtypeOf(FindBugsMarker.NAME)) {
						continue;
					}
					markers.add(marker);
				} else if (next instanceof BugPatternGroup){
					BugPatternGroup group = (BugPatternGroup) next;
					IMarker[] children = group.getChildren();
					for (IMarker marker : children) {
						markers.add(marker);
					}
				}
			}
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Exception while parsing content of FindBugs markers.");
		}
		return markers;
	}

}