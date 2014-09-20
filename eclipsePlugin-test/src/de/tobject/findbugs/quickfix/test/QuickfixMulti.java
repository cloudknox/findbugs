/*
 * Contributions to FindBugs
 * Copyright (C) 2014, Kevin Lubick
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
package de.tobject.findbugs.quickfix.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.test.AbstractQuickfixTest;
import de.tobject.findbugs.test.TestScenario;

import edu.umd.cs.findbugs.BugPattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *  Tests the ability to fix multiple bugs at once from the Problems view
 *
 * @author Kevin Lubick
 *
 */
public class QuickfixMulti extends AbstractQuickfixTest {

    @Override
    protected TestScenario getTestScenario() {
          return TestScenario.MULTIQUICKFIX;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.MULTIQUICKFIX);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        //tearDownTestProject();
    }

    protected void doTestMultiQuickfixResolution(IProject project, String expectedPattern) throws CoreException, IOException {
        // Run FindBugs on the entire project
        work(createFindBugsWorker(), project);

        // Assert the expected markers are present
        IMarker[] markers = MarkerUtil.getAllMarkers(project);
        assertPresentBugPattern(expectedPattern, markers);
        assertTrue(markers.length > 0);
        //assertEquals(expectedBugsToFix, markers.length);

        // Assert all markers have resolution
        assertAllMarkersHaveResolutions(markers);

        markers = filterMarkers(markers, expectedPattern);

        // Apply resolution to each marker
        applyMultiResolutionToAllMarkers(markers);



        // TODO Assert output file
        //assertEqualFiles(getExpectedOutputFile(classFileName), getInputCompilationUnit(classFileName));
        //assertEquals(0, getInputFileMarkers(classFileName).length);
    }

    private void applyMultiResolutionToAllMarkers(IMarker[] markers) {
        IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(markers[0]);
        if (resolutions[0] instanceof WorkbenchMarkerResolution) {
            //this represents one of the bugs a user would click on in the problems menu
            WorkbenchMarkerResolution resolutionFromProblemsMenu = ((WorkbenchMarkerResolution)resolutions[0]);

            //in theory, we should have filtered all the bugs of the passed in type
            //So, findOtherMarkers should return them all
            assertEquals(markers.length - 1, resolutionFromProblemsMenu.findOtherMarkers(markers).length);

            resolutionFromProblemsMenu.run(markers, null);
        } else {
            fail("Should have been a WorkBenchMarkerResolution: "+resolutions[0]);
        }

    }

    //Filters out markers so that only markers with the expected pattern are in the array
    private IMarker[] filterMarkers(IMarker[] markers, String bugPatternType) {
        ArrayList<IMarker> filteredMarkers = new ArrayList<>();

        for (int i = 0; i < markers.length; i++) {
            BugPattern pattern = MarkerUtil.findBugPatternForMarker(markers[i]);
            if (pattern.getType().equals(bugPatternType)) {
                filteredMarkers.add(markers[i]);
            }
        }

        return filteredMarkers.toArray(new IMarker[filteredMarkers.size()]);
    }

}
