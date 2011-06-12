//========================================================================
//$Id: RuntimeDependencyResolver.java 397 2006-03-23 18:44:41Z janb $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================


package org.mortbay.jetty.plugin;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.mortbay.jetty.plugin.util.PluginLog;

/**
 * RuntimeDependencyResolver
 * 
 * This class is able to pull down a remote pom, find all of it's
 * dependencies and transitively resolve them.
 * 
 *
 */
public class RuntimeDependencyResolver 
{
    private ArtifactFactory artifactFactory;
    private ArtifactResolver artifactResolver;
    private ArtifactMetadataSource metadataSource;
    private ArtifactRepository localRepository;
    private List remoteRepositories;
    
    
    /**
     * RuntimeResolutionListener
     * 
     * Just for debug printing of transitive resolution steps
     *
     */
    class RuntimeResolutionListener implements ResolutionListener
    {
        public void testArtifact(Artifact arg0) { PluginLog.getLog().debug ("TESTING ARTIFACT "+arg0);}      
        public void startProcessChildren(Artifact arg0) {PluginLog.getLog().debug("STARTING CHILDREN "+arg0);}              
        public void endProcessChildren(Artifact arg0) {PluginLog.getLog().debug("ENDING CHILDREN "+arg0);}
        public void includeArtifact(Artifact arg0) {PluginLog.getLog().debug("INCLUDE ARTIFACT "+arg0);}
        public void omitForNearer(Artifact arg0, Artifact arg1) {PluginLog.getLog().debug("OMITTING "+arg0+" for NEARER "+arg1);}               
        public void updateScope(Artifact arg0, String arg1) {PluginLog.getLog().debug("UPDATE of SCOPE "+arg0+ "="+arg1);}              
        public void manageArtifact(Artifact arg0, Artifact arg1) {PluginLog.getLog().debug("MANAGE ARTIFACT "+arg0+" and "+arg1); }         
        public void omitForCycle(Artifact arg0) {PluginLog.getLog().debug("OMIT FOR CYCLE "+arg0);}         
        public void updateScopeCurrentPom(Artifact arg0, String arg1) {PluginLog.getLog().debug("UPDATE SCOPE CURRENT POM "+arg0+"="+arg1);}
        public void selectVersionFromRange(Artifact arg0) {PluginLog.getLog().debug("SELECT VERSION FROM RANGE "+arg0);}
        public void restrictRange(Artifact arg0, Artifact arg1, VersionRange arg2) {PluginLog.getLog().debug("RESTRICT RANGE "+arg0+" "+arg1+" range="+arg2);}
        
    }
    
    
    public RuntimeDependencyResolver (ArtifactFactory artifactFactory, ArtifactResolver artifactResolver, 
            ArtifactMetadataSource metadataSource, ArtifactRepository localRepository, List remoteRepositories)
    {
        this.artifactFactory = artifactFactory;
        this.artifactResolver = artifactResolver;
        this.metadataSource = metadataSource;
        this.localRepository = localRepository;
        this.remoteRepositories = new ArrayList(remoteRepositories);
    }
    
    
    /**
     * Download (if necessary) a pom, and load it as a MavenProject, transitively resolving any
     * dependencies therein.
     * 
     * @param projectBuilder
     * @param groupId
     * @param artifactId
     * @param versionId
     * @return a Set of Artifacts representing the transitively resolved dependencies.
     * 
     * @throws MalformedURLException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     */
    public Set transitivelyResolvePomDependencies (MavenProjectBuilder projectBuilder, String groupId, String artifactId, String versionId, boolean resolveProjectArtifact) 
    throws MalformedURLException, ProjectBuildingException, InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        
        Artifact pomArtifact = getPomArtifact(groupId, artifactId, versionId);
        MavenProject project = loadPomAsProject(projectBuilder, pomArtifact);
        List dependencies = project.getDependencies();
        
        
        Set dependencyArtifacts = MavenMetadataSource.createArtifacts( artifactFactory, dependencies, null, null, null );
        dependencyArtifacts.add(project.getArtifact());
        
        List listeners = Collections.EMPTY_LIST;
        
        if (PluginLog.getLog().isDebugEnabled())
        {
            listeners = new ArrayList();
            listeners.add(new RuntimeResolutionListener());
        }
        
        ArtifactResolutionResult result = artifactResolver.resolveTransitively(dependencyArtifacts, pomArtifact, 
                Collections.EMPTY_MAP, localRepository, remoteRepositories, metadataSource, null, listeners);
        
        Set artifacts = result.getArtifacts();
        
        if (PluginLog.getLog().isDebugEnabled())
        {
            PluginLog.getLog().debug("RESOLVED "+artifacts.size()+" ARTIFACTS");
            Iterator itor = artifacts.iterator();
            while (itor.hasNext())
            {
                Artifact a = (Artifact)itor.next();
                PluginLog.getLog().debug(a.getFile().toURL().toString());
            }
        }
        return artifacts;
    }

    
    
    public MavenProject loadPomAsProject (MavenProjectBuilder projectBuilder, Artifact pomArtifact) 
    throws ProjectBuildingException
    {
        return projectBuilder.buildFromRepository(pomArtifact, remoteRepositories,localRepository);
    }

    
    public Artifact getArtifact (String groupId, String artifactId, String versionId, String type)
    {
        return this.artifactFactory.createBuildArtifact(groupId, artifactId, versionId, type);
    }
    
    
    public Artifact getPomArtifact (String groupId, String artifactId, String versionId)
    {
        return this.artifactFactory.createBuildArtifact(groupId, artifactId, versionId, "pom");
    }
    
    public void removeDependency (Set artifacts, String groupId, String artifactId, String versionId, String type)
    {
        if ((artifacts == null) || artifacts.isEmpty())
            return;
        
        Iterator itor = artifacts.iterator();
        while (itor.hasNext())
        {
            Artifact a = (Artifact)itor.next();
            if (a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId) && a.getType().equals(type))
            {
                //remove if the versions match, or there was no version specified
                if (versionId == null)
                    itor.remove();
                else if (a.getVersion().equals(versionId))
                    itor.remove();      
            }
        }
    }
    
    public void addDependency (Set artifacts, String groupId, String artifactId, String versionId, String type) 
    throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Artifact a = getArtifact(groupId, artifactId, versionId, type);
        artifactResolver.resolve(a, remoteRepositories, localRepository);
        artifacts.add(a);
    }

}
