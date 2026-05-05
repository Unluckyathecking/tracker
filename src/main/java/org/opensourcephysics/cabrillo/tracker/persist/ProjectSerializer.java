package org.opensourcephysics.cabrillo.tracker.persist;

import org.opensourcephysics.cabrillo.tracker.project.TrackerProject;

public interface ProjectSerializer {
    void serialize(TrackerProject project, java.io.Writer writer) throws java.io.IOException;
    
    TrackerProject deserialize(java.io.Reader reader) throws java.io.IOException;
}
