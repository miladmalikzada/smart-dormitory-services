package com.dorm.service;

/** A printable page. Every value is escaped, so a name like <script> cannot inject markup. */
public class HtmlCertificateFormat implements CertificateFormat {
    @Override public String id() { return "html"; }
    @Override public String contentType() { return "text/html; charset=utf-8"; }
    @Override public String extension() { return "html"; }

    @Override public String render(AccommodationCertificate c) {
        return "<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><title>Accommodation certificate</title>\n"
                + "<style>body{font-family:Georgia,serif;max-width:640px;margin:60px auto;padding:48px;border:6px double #4f46e5;text-align:center}"
                + "h1{color:#3730a3;letter-spacing:2px}.name{font-size:28px;margin:24px 0}.small{color:#666;margin-top:40px}</style></head>\n"
                + "<body><h1>ACCOMMODATION CERTIFICATE</h1><p>This certifies that</p>\n"
                + "<div class=\"name\"><b>" + esc(c.studentName()) + "</b> (" + esc(c.studentId()) + ")</div>\n"
                + (c.faculty() == null ? "" : "<p>" + esc(c.faculty())
                    + (c.courseYear() == null ? "" : ", year " + c.courseYear()) + "</p>\n")
                + "<p>is a resident of the university dormitory, <b>room " + esc(c.roomId()) + "</b>.</p>\n"
                + "<p class=\"small\">Issued on " + c.issuedOn() + " by Smart Dormitory Services</p></body></html>\n";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
