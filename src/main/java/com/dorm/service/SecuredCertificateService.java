package com.dorm.service;

import com.dorm.domain.Role;

import java.util.List;

/** Protection Proxy: only STAFF and ADMIN may issue certificates. */
public class SecuredCertificateService implements CertificateOperations {
    private final CertificateOperations target;
    private final SessionContext session;

    public SecuredCertificateService(CertificateOperations target, SessionContext session) {
        this.target = target;
        this.session = session;
    }

    @Override public IssuedDocument issue(String studentId, String formatId) {
        if (session.role() == Role.STUDENT) throw new AccessDeniedException("Only STAFF or ADMIN may issue certificates");
        return target.issue(studentId, formatId);
    }

    @Override public List<String> formats() { return target.formats(); }
}
