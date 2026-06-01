package com.diego.odontoflowbackend.audit;

import com.diego.odontoflowbackend.security.SecurityUtils;
import org.hibernate.envers.RevisionListener;

/** Stamps each Envers revision with the currently authenticated user id (NFR01). */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevision revision = (AuditRevision) revisionEntity;
        try {
            revision.setUserId(SecurityUtils.currentUserId().toString());
        } catch (Exception e) {
            revision.setUserId(null); // no authenticated context (e.g. system task)
        }
    }
}
