// ========================================================================
// Copyright 2006-2007 Sabre Holdings.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;

/**
 * Describes set of files matched by <fileset/> elements in ant configuration
 * file. It is used to group application classes, libraries, and scannedTargets
 * elements.
 * 
 * @author Jakub Pawlowicz
 */
public class FileMatchingConfiguration
{

    private List directoryScanners;

    public FileMatchingConfiguration()
    {
        this.directoryScanners = new ArrayList();
    }

    /**
     * @param directoryScanner new directory scanner retrieved from the
     *            <fileset/> element.
     */
    public void addDirectoryScanner(DirectoryScanner directoryScanner)
    {
        this.directoryScanners.add(directoryScanner);
    }

    /**
     * @return a list of base directories denoted by a list of directory
     *         scanners.
     */
    public List getBaseDirectories()
    {
        List baseDirs = new ArrayList();
        Iterator scanners = directoryScanners.iterator();
        while (scanners.hasNext())
        {
            DirectoryScanner scanner = (DirectoryScanner) scanners.next();
            baseDirs.add(scanner.getBasedir());
        }

        return baseDirs;
    }

    /**
     * Checks if passed file is scanned by any of the directory scanners.
     * 
     * @param pathToFile a fully qualified path to tested file.
     * @return true if so, false otherwise.
     */
    public boolean isIncluded(String pathToFile)
    {
        Iterator scanners = directoryScanners.iterator();
        while (scanners.hasNext())
        {
            DirectoryScanner scanner = (DirectoryScanner) scanners.next();
            scanner.scan();
            String[] includedFiles = scanner.getIncludedFiles();

            for (int i = 0; i < includedFiles.length; i++)
            {
                File includedFile = new File(scanner.getBasedir(), includedFiles[i]);
                if (pathToFile.equalsIgnoreCase(includedFile.getAbsolutePath()))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
