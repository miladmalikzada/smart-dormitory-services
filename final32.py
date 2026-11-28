import pathlib, subprocess, sys
R = pathlib.Path('.')
P = lambda s: R / s
JS = P('src/main/resources/web/app.js')
HTML = P('src/main/resources/web/index.html')
if 'async function dialog(' in JS.read_text():
    sys.exit('The dialogs are already in place - nothing to do.')
def git(*a): subprocess.run(['git', *a], check=True)
def commit(msg, *paths):
    git('add', *[str(p) for p in paths]); git('commit', '-q', '-m', msg); print('committed:', msg)
def sub(path, old, new, count=1):
    t = path.read_text()
    assert t.count(old) == count, f'{path}: expected {count} match(es) for: {old[:70]!r}, found {t.count(old)}'
    path.write_text(t.replace(old, new))

# 1. username pattern
sub(HTML, 'pattern="[A-Za-z0-9][A-Za-z0-9._-]{2,31}"', 'pattern="[A-Za-z0-9][A-Za-z0-9._\\-]{2,31}"')
commit('fix: username pattern is valid for Chrome (escape the dash); no console error on sign-in', HTML)

# 2. dialog helpers
sub(JS, '/* ---------- small builders ---------- */', '''/* ---------- dialogs in the site's own style (instead of the browser's confirm and prompt) ---------- */
// Resolves with the form values when the user presses the main button, or null on Cancel, Esc or a click outside.
function dialog(title, bodyHtml, okLabel, okClass = 'primary') {
  return new Promise(resolve => {
    let answer = null;
    openModal(title, `<form id="f-dialog"><div class="modal-body">${bodyHtml}</div>
      <div class="modal-foot"><button type="button" data-action="close-modal">Cancel</button>
      <button class="${okClass}">${okLabel}</button></div></form>`);
    $('f-dialog').addEventListener('submit', e => { e.preventDefault(); answer = Object.fromEntries(new FormData(e.target).entries()); closeModal(); });
    modal.addEventListener('close', () => resolve(answer), { once: true });
    const first = $('f-dialog').querySelector('input'); if (first) first.focus();
  });
}
const confirmBox = async (title, textHtml, okLabel, okClass = 'danger') =>
  (await dialog(title, `<p>${textHtml}</p>`, okLabel, okClass)) !== null;

/* ---------- small builders ---------- */''')
commit('feat(ui): dialog() and confirmBox() helpers in the site style, answers via a Promise', JS)

# 3. delete confirmations, per resource
sub(JS, "  'delete-student': d => confirm('Delete student ' + d.id + '?') && run(() => api('DELETE', '/students/' + d.id), 'Student deleted'),",
"  'delete-student': async d => { if (await confirmBox('Delete student', 'Delete student <b>' + esc(d.id) + '</b>? This cannot be undone.', 'Delete')) run(() => api('DELETE', '/students/' + d.id), 'Student deleted'); },")
commit('feat(ui): deleting a student asks in a dialog, not a browser box', JS)
sub(JS, "  'delete-room': d => confirm('Delete room ' + d.id + '?') && run(() => api('DELETE', '/rooms/' + d.id), 'Room deleted'),",
"  'delete-room': async d => { if (await confirmBox('Delete room', 'Delete room <b>' + esc(d.id) + '</b>? Only empty rooms without repairs can be deleted.', 'Delete')) run(() => api('DELETE', '/rooms/' + d.id), 'Room deleted'); },")
commit('feat(ui): deleting a room asks in a dialog and says when it is allowed', JS)
sub(JS, "  'delete-ticket': d => confirm('Delete repair ' + d.id + '?') && run(() => api('DELETE', '/tickets/' + d.id), 'Repair deleted'),",
"  'delete-ticket': async d => { if (await confirmBox('Delete repair', 'Delete repair <b>' + esc(d.id) + '</b>?', 'Delete')) run(() => api('DELETE', '/tickets/' + d.id), 'Repair deleted'); },")
commit('feat(ui): deleting a repair asks in a dialog', JS)
sub(JS, "  'delete-visitor': d => confirm('Delete this visitor record?') && run(() => api('DELETE', '/visitors/' + d.id), 'Record deleted'),",
"  'delete-visitor': async d => { if (await confirmBox('Delete visitor record', 'Delete this visitor record?', 'Delete')) run(() => api('DELETE', '/visitors/' + d.id), 'Record deleted'); },")
commit('feat(ui): deleting a visitor record asks in a dialog', JS)
sub(JS, "  'delete-ann': d => confirm('Delete this announcement?') && run(() => api('DELETE', '/announcements/' + d.id), 'Announcement deleted')",
"  'delete-ann': async d => { if (await confirmBox('Delete announcement', 'Delete this announcement?', 'Delete')) run(() => api('DELETE', '/announcements/' + d.id), 'Announcement deleted'); }")
commit('feat(ui): deleting an announcement asks in a dialog', JS)

# 4. account confirmations
sub(JS, "  'reject-account': d => confirm('Decline or switch off ' + d.id + '? They will be signed out.') && run(() => api('POST', '/accounts/' + encodeURIComponent(d.id) + '/reject'), 'Account switched off'),",
"  'reject-account': async d => { if (await confirmBox('Decline or switch off', 'Decline or switch off <b>' + esc(d.id) + '</b>? They will be signed out.', 'Switch off')) run(() => api('POST', '/accounts/' + encodeURIComponent(d.id) + '/reject'), 'Account switched off'); },")
commit('feat(ui): switching an account off asks in a dialog', JS)
sub(JS, "    if (!confirm('Create a new temporary password for ' + d.id + '? Their old password stops working and they are signed out.')) return;",
"    if (!await confirmBox('Reset password', 'Create a new temporary password for <b>' + esc(d.id) + '</b>? Their old password stops working and they are signed out.', 'Create password', 'primary')) return;")
commit('feat(ui): resetting a password asks in a dialog', JS)

# 5. text questions
sub(JS, "  'assign': d => { const t = prompt('Technician name'); if (t) run(() => api('PUT', '/tickets/' + d.id, { action: 'assign', technician: t }), 'Repair assigned'); },",
"""  'assign': async d => {
    const a = await dialog('Assign repair ' + esc(d.id), field('Technician name', 'technician', '', 'required maxlength="60"'), 'Assign');
    if (a && a.technician.trim()) run(() => api('PUT', '/tickets/' + d.id, { action: 'assign', technician: a.technician.trim() }), 'Repair assigned');
  },""")
commit('feat(ui): assigning a technician uses a dialog with a required field', JS)
sub(JS, """  'edit-room': d => { const c = prompt('New capacity', d.cap); if (c === null) return;
    run(() => api('PUT', '/rooms/' + d.id, { capacity: Number(c) }), 'Room updated'); },""",
"""  'edit-room': async d => {
    const a = await dialog('Beds in room ' + esc(d.id), field('Number of beds', 'capacity', d.cap, 'type="number" min="1" max="20" required'), 'Save');
    if (a) run(() => api('PUT', '/rooms/' + d.id, { capacity: Number(a.capacity) }), 'Room updated');
  },""")
commit('feat(ui): changing the beds of a room uses a number field in a dialog', JS)
sub(JS, """  'edit-ann': d => { const t = prompt('Title', d.title); if (t === null) return; const b = prompt('Text', d.body || ''); if (b === null) return;
    run(() => api('PUT', '/announcements/' + d.id, { title: t, body: b }), 'Announcement updated'); },""",
"""  'edit-ann': async d => {
    const a = await dialog('Edit announcement', field('Title', 'title', d.title, 'required maxlength="120"') + field('Text', 'body', d.body || '', 'maxlength="2000"'), 'Save');
    if (a) run(() => api('PUT', '/announcements/' + d.id, { title: a.title, body: a.body }), 'Announcement updated');
  },""")
commit('feat(ui): editing an announcement uses one dialog for title and text', JS)

# 6. guard test
P('src/test/java/com/dorm/web/WebAssetsTest.java').write_text('''package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guards the website files: no browser dialogs, no inline code (the CSP forbids it), valid input patterns. */
class WebAssetsTest {
    private static String read(String name) throws IOException {
        try (var in = WebAssetsTest.class.getResourceAsStream("/web/" + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void noBrowserDialogsInTheScript() throws IOException {
        String js = read("app.js");
        for (String banned : new String[] {"prompt(", "confirm(", "alert("}) {
            Matcher m = Pattern.compile("(?<![A-Za-z.])" + Pattern.quote(banned)).matcher(js);
            assertFalse(m.find(), banned + " would open a plain browser box; use dialog() or confirmBox()");
        }
    }

    @Test void noInlineScriptsOrStylesInThePage() throws IOException {
        String html = read("index.html");
        assertFalse(Pattern.compile("<script(?![^>]*\\\\bsrc=)[^>]*>").matcher(html).find(), "inline <script>");
        assertFalse(html.contains("<style"), "inline <style>");
        assertFalse(Pattern.compile("\\\\sstyle=").matcher(html).find(), "style attribute");
        assertFalse(Pattern.compile("\\\\son[a-z]+=").matcher(html).find(), "inline event handler");
    }

    @Test void patternAttributesAreValidForTheBrowser() throws IOException {
        Matcher m = Pattern.compile("pattern=\\"([^\\"]*)\\"").matcher(read("index.html"));
        int found = 0;
        while (m.find()) {
            found++;
            String p = m.group(1);
            assertDoesNotThrow(() -> Pattern.compile(p), p);
            // Chrome compiles patterns with the "v" flag, where a dash at the end of a class must be escaped
            assertFalse(p.matches(".*[^\\\\\\\\]-\\\\].*"), "unescaped dash before ] in " + p);
        }
        assertTrue(found >= 1);
    }
}
''')
commit('test: WebAssetsTest guards against browser dialogs, inline code and broken patterns', P('src/test/java/com/dorm/web/WebAssetsTest.java'))

# 7. release notes
CL = P('CHANGELOG.md')
sub(CL, '## 1.4.0 - passwords', '''## 1.4.1 - UI polish
* Confirmations (delete, switch off, reset password) and questions (assign technician, change beds, edit announcement) now use dialogs in the site's own style, not the browser's `confirm` and `prompt` boxes. Cancel and Esc do nothing.
* Fix: the username field's `pattern` was invalid for Chrome (the dash must be escaped), which logged a console error and skipped the client-side check.
* New `WebAssetsTest` fails when a browser dialog, inline script or style, or a broken pattern is added again.

## 1.4.0 - passwords''')
commit('docs: changelog for 1.4.1', CL)
sub(P('pom.xml'), '<version>1.4.0</version>', '<version>1.4.1</version>')
commit('build: project version 1.4.1, same as the changelog', P('pom.xml'))
