'use strict';
/* Smart Dormitory Services - frontend. Talks to the REST API with fetch and a Bearer token. */
const $ = id => document.getElementById(id);
const view = $('view');
let current = 'dashboard';
let me = null;          // { username, role }
let token = null;
let onlyInside = false;
let studentFilter = { text: '', faculty: '' };

try { token = sessionStorage.getItem('token'); } catch (e) { /* storage not available */ }

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const when = t => t ? t.replace('T', ' ').slice(0, 16) : '';
const initials = (a, b) => ((a || '?')[0] + ((b || '')[0] || '')).toUpperCase();
const isAdmin = () => me && me.role === 'ADMIN';
const isStaff = () => me && (me.role === 'ADMIN' || me.role === 'STAFF');

const ICONS = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5"/><circle cx="17.5" cy="9" r="2.5"/><path d="M17.5 14.5c2.4.2 3.7 1.7 4 4.5"/>',
  bed: '<path d="M3 18V7M3 14h18v4M21 14v-2.5A2.5 2.5 0 0 0 18.5 9H11v5"/><circle cx="7" cy="11" r="1.8"/>',
  wrench: '<path d="M14.5 6.5a4 4 0 0 0 5 5l-9.2 9.2a2.1 2.1 0 0 1-3-3z"/><path d="M14.5 6.5l3-3a4 4 0 0 0-3 3z"/>',
  door: '<path d="M6 21V4h9v17M3 21h18"/><circle cx="12" cy="13" r=".8"/><path d="M15 6l4 1v14"/>',
  bell: '<path d="M6 17V11a6 6 0 0 1 12 0v6l2 2H4z"/><path d="M10 21a2 2 0 0 0 4 0"/>',
  key: '<circle cx="8" cy="15" r="4"/><path d="M11 12l9-9M16 7l3 3M14 9l2 2"/>',
  lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>'
};
const icon = n => `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONS[n] || ''}</svg>`;
document.querySelectorAll('[data-icon]').forEach(el => { el.innerHTML = icon(el.dataset.icon); });

/* ---------- API ---------- */
async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const res = await fetch('/api' + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  let data = {};
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (res.status === 401 && path !== '/login') { signOutLocally(); throw new Error('Please sign in again'); }
  if (!res.ok) throw new Error(data.error || ('Request failed (' + res.status + ')'));
  return data;
}

let toastTimer;
function toast(msg, bad) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, 3600);
}

async function run(fn, okMsg) {
  try { await fn(); if (okMsg) toast(okMsg); await show(current); }
  catch (e) { toast(e.message, true); }
}

/* ---------- modal ---------- */
const modal = $('modal');
function openModal(title, bodyHtml, footHtml) {
  $('modal-body').innerHTML = `<div class="modal-head"><h2>${title}</h2><button type="button" class="small" data-action="close-modal" aria-label="Close">&times;</button></div>${bodyHtml}${footHtml || ''}`;
  if (!modal.open) modal.showModal();
}
const closeModal = () => { if (modal.open) modal.close(); };
modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });

/* ---------- small builders ---------- */
const table = (heads, rows) => rows.length
  ? `<div class="table-wrap"><table><thead><tr>${heads.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`
  : '<div class="empty">Nothing here yet.</div>';
const btn = (label, action, id, extra = '', cls = '') =>
  `<button type="button" class="small ${cls}" data-action="${action}" data-id="${esc(id)}" ${extra}>${label}</button>`;
const fullName = s => esc(s.name || ((s.firstName || '') + ' ' + (s.lastName || '')));
const person = s => `<div class="cell-person"><span class="avatar">${esc(initials(s.firstName, s.lastName))}</span>
  <div><b>${fullName(s)}</b><small>${esc(s.id)}${s.faculty ? ' &middot; ' + esc(s.faculty) : ''}</small></div></div>`;
const locked = msg => `<div class="locked">${icon('lock').replace('<svg', '<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"')} ${msg}</div>`;

/* ---------- pages ---------- */
const TITLES = { dashboard: 'Dashboard', students: 'Students', rooms: 'Rooms', tickets: 'Repairs', visitors: 'Visitors', announcements: 'Announcements', accounts: 'Accounts' };
const SUBTITLES = {
  dashboard: 'A quick look at the whole dormitory.',
  students: 'Records, rooms and certificates for every resident.',
  rooms: 'Beds, occupants and free space.',
  tickets: 'Report a problem and follow it until it is fixed.',
  visitors: 'Who is inside the building right now.',
  announcements: 'News for all residents, sent to every channel.',
  accounts: 'Approve staff and administrator requests.'
};
const ART = { dashboard: 'dashboard', students: 'students', rooms: 'rooms', tickets: 'repairs', visitors: 'visitors', announcements: 'announcements', accounts: 'accounts' };
const greeting = () => { const h = new Date().getHours(); return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'; };
const ROLE_NOTE = {
  ADMIN: 'You have full access: students, rooms, repairs and accounts.',
  STAFF: 'You can handle repairs, visitors and look up students (identity numbers are masked).',
  STUDENT: 'You can see the student directory, request a room and report problems.'
};

const views = {
  async dashboard() {
    const [s, events] = await Promise.all([api('GET', '/summary'), api('GET', '/events')]);
    let n = 0;
    const stat = (key, label, ic) => `<div class="stat c${++n}"><span class="ico">${icon(ic)}</span><b>${s[key]}</b><span>${label}</span></div>`;
    const quick = isAdmin() ? btn('+ Add student', 'add-student', '', '', 'light') + btn('Review accounts', 'goto', 'accounts', '', 'ghost-light')
      : btn('Report a repair', 'goto', 'tickets', '', 'light') + btn('See students', 'goto', 'students', '', 'ghost-light');
    return `<section class="hero"><div><span class="eyebrow">${greeting()}</span><h2>Welcome, ${esc(me.username)}</h2>
        <p>${ROLE_NOTE[me.role] || ''}</p><div class="row-actions left">${quick}</div></div></section>
      <div class="cards">${stat('students', 'Students', 'users')}${stat('rooms', 'Rooms', 'bed')}${stat('freeBeds', 'Free beds', 'bed')}
      ${stat('openTickets', 'Open repairs', 'wrench')}${stat('visitorsInside', 'Visitors inside', 'door')}${stat('announcements', 'Announcements', 'bell')}</div>
      <div class="card"><h3>Recent activity</h3>${events.length
        ? `<div class="activity">${events.map(e => `<div><span class="tag">${esc(e.type.replace(/_/g, ' '))}</span><span>${esc(e.message)}</span></div>`).join('')}</div>`
        : '<div class="empty">No activity yet. Actions such as assigning rooms and posting announcements appear here.</div>'}</div>`;
  },

  async students() {
    const [students, rooms] = await Promise.all([api('GET', '/students'), api('GET', '/rooms')]);
    const roomOf = id => (rooms.find(r => r.occupants.includes(id)) || {}).id;
    const faculties = [...new Set(students.map(s => s.faculty).filter(Boolean))].sort();
    const q = studentFilter.text.toLowerCase();
    const shown = students.filter(s => (!studentFilter.faculty || s.faculty === studentFilter.faculty)
      && (!q || [s.name, s.id, s.iin, s.email, s.phone].some(v => (v || '').toLowerCase().includes(q))));
    const showIds = isStaff();
    const rows = shown.map(s => `<tr>
      <td>${person(s)}</td>
      ${showIds ? `<td class="mono">${esc(s.iin || '-')}</td><td>${esc(s.phone || '-')}</td>` : ''}
      <td>${s.courseYear ? 'Year ' + s.courseYear : '-'}</td>
      <td>${roomOf(s.id) ? `<span class="badge good">${esc(roomOf(s.id))}</span>` : '<span class="badge">No room</span>'}</td>
      <td><div class="row-actions">${showIds ? btn('View', 'view-student', s.id) : ''}
        ${showIds && roomOf(s.id) ? btn('Certificate', 'certificate', s.id) : ''}
        ${roomOf(s.id) ? btn('Check out', 'checkout', s.id, `data-room="${esc(roomOf(s.id))}"`) : btn('Request room', 'request-room', s.id)}
        ${isAdmin() ? btn('Edit', 'edit-student', s.id) + btn('Delete', 'delete-student', s.id, '', 'danger') : ''}</div></td></tr>`);
    const note = me.role === 'ADMIN' ? '' : me.role === 'STAFF'
      ? locked('Staff view: identity numbers are masked. Only an administrator can see full numbers or change student records.')
      : locked('Student view: you can see the directory (name, faculty, year). Personal documents are private.');
    return `${note}<div class="card">
      <div class="toolbar"><div class="left">
        <input class="search" id="student-search" placeholder="Search name, ID, IIN, email..." value="${esc(studentFilter.text)}">
        <select id="student-faculty"><option value="">All faculties</option>${faculties.map(f => `<option ${f === studentFilter.faculty ? 'selected' : ''}>${esc(f)}</option>`).join('')}</select>
        <span class="muted">${shown.length} of ${students.length}</span></div>
        <div class="right">${isStaff() ? '<button type="button" data-action="export-students">Export CSV</button>' : ''}${isAdmin() ? '<button class="primary" data-action="add-student" type="button">+ Add student</button>' : ''}</div></div>
      ${table(['Student', ...(showIds ? ['IIN', 'Phone'] : []), 'Course', 'Room', ''], rows)}</div>`;
  },

  async rooms() {
    const [rooms, students] = await Promise.all([api('GET', '/rooms'), api('GET', '/students')]);
    const name = id => (students.find(s => s.id === id) || {}).name || id;
    const fill = r => { const p = r.capacity ? (r.capacity - r.freeBeds) / r.capacity : 0; return p >= 1 ? 'w100' : p >= .75 ? 'w75' : p >= .5 ? 'w50' : p > 0 ? 'w25' : 'w0'; };
    const rows = rooms.map(r => `<tr><td><b>${esc(r.id)}</b></td>
      <td><div class="bar"><i class="${fill(r)}"></i></div><small class="muted">${r.capacity - r.freeBeds} / ${r.capacity} beds used</small></td>
      <td>${r.occupants.map(o => `<span class="chip">${esc(name(o))}<button type="button" data-action="checkout" data-id="${esc(o)}" data-room="${esc(r.id)}" title="Check out">&times;</button></span>`).join('') || '<span class="muted">empty</span>'}</td>
      <td><div class="row-actions">${isAdmin() ? btn('Capacity', 'edit-room', r.id, `data-cap="${r.capacity}"`) + btn('Delete', 'delete-room', r.id, '', 'danger') : ''}</div></td></tr>`);
    const addRoom = isAdmin() ? `<div class="card"><h3>Add a room</h3><form class="inline-form" id="f-room">
        <label>Room number<input name="id" required placeholder="R301"></label>
        <label>Number of beds (capacity)<input name="capacity" type="number" min="1" max="20" value="2" required></label>
        <button class="primary">Add room</button>
        <button type="button" data-action="undo">Undo last room action</button></form>
        <p class="hint">You can change the number of beds later with the Capacity button on a room.</p></div>`
      : locked('Only an administrator can add rooms or change how many beds a room has.');
    return `${addRoom}
      <div class="card">${table(['Room', 'Occupancy', 'Residents', ''], rows)}</div>`;
  },

  async tickets() {
    const [tickets, rooms] = await Promise.all([api('GET', '/tickets'), api('GET', '/rooms')]);
    const next = t => !isStaff() ? '' : t.status === 'OPEN' ? btn('Assign', 'assign', t.id)
      : t.status === 'ASSIGNED' ? btn('Start', 'start', t.id)
      : t.status === 'IN_PROGRESS' ? btn('Resolve', 'resolve', t.id) : '';
    const rows = tickets.map(t => `<tr><td>${esc(t.id)}</td><td><b>${esc(t.roomId)}</b></td><td>${esc(t.description)}</td>
      <td class="${t.priority}">${t.priority}</td><td><span class="badge ${t.status}">${t.status.replace('_', ' ')}</span></td>
      <td>${esc(t.technician || '-')}</td><td><div class="row-actions">${next(t)}${isStaff() ? btn('Delete', 'delete-ticket', t.id, '', 'danger') : ''}</div></td></tr>`);
    return `${isStaff() ? '' : locked('Anyone can report a problem. Only staff and administrators can assign, start, resolve or delete repairs.')}
      <div class="card"><h3>Report a problem</h3><form class="inline-form" id="f-ticket">
        <label>Room<select name="roomId" required>${rooms.map(r => `<option>${esc(r.id)}</option>`).join('')}</select></label>
        <label>Problem<input name="description" required></label>
        <label>Priority<select name="priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option><option>URGENT</option></select></label>
        <button class="primary">Report problem</button></form></div>
      <div class="card">${table(['ID', 'Room', 'Problem', 'Priority', 'Status', 'Technician', ''], rows)}</div>`;
  },

  async visitors() {
    const [list, students] = await Promise.all([api('GET', onlyInside ? '/visitors/inside' : '/visitors'), api('GET', '/students')]);
    const name = id => (students.find(s => s.id === id) || {}).name || id;
    const rows = list.map(v => `<tr><td>${esc(v.id)}</td><td><b>${esc(v.visitorName)}</b></td><td>${esc(name(v.hostStudentId))}</td>
      <td>${when(v.arrivedAt)}</td><td>${v.inside ? '<span class="badge warn">inside</span>' : when(v.departedAt)}</td>
      <td><div class="row-actions">${v.inside ? btn('Mark departed', 'depart', v.id) : ''}${btn('Delete', 'delete-visitor', v.id, '', 'danger')}</div></td></tr>`);
    return `<div class="card"><h3>Record an arrival</h3><form class="inline-form" id="f-visitor">
        <label>Visitor name<input name="visitorName" required></label>
        <label>Visiting<select name="hostStudentId" required>${students.map(s => `<option value="${esc(s.id)}">${fullName(s)} (${esc(s.id)})</option>`).join('')}</select></label>
        <button class="primary">Record arrival</button></form></div>
      <div class="card"><div class="toolbar"><div class="left"><label><span><input type="checkbox" id="only-inside" ${onlyInside ? 'checked' : ''}> Show only visitors inside</span></label></div></div>
      ${table(['ID', 'Visitor', 'Host', 'Arrived', 'Left', ''], rows)}</div>`;
  },

  async announcements() {
    const list = await api('GET', '/announcements');
    return `<div class="card"><h3>Post an announcement</h3><form class="inline-form" id="f-ann">
        <label>Title<input name="title" required></label><label>Text<input name="body"></label>
        <button class="primary">Post announcement</button></form></div>
      <div class="card">${list.length ? list.slice().reverse().map(a => `<div class="ann"><h3>${esc(a.title)}</h3>
        <div>${esc(a.body)}</div><p class="muted">${when(a.postedAt)} &middot; ${esc(a.id)}</p>
        <div class="row-actions">${btn('Edit', 'edit-ann', a.id, `data-title="${esc(a.title)}" data-body="${esc(a.body)}"`)}${btn('Delete', 'delete-ann', a.id, '', 'danger')}</div></div>`).join('')
        : '<div class="empty">No announcements yet.</div>'}</div>`;
  },
  async accounts() {
    if (!isAdmin()) return locked('Only an administrator can manage accounts.');
    const list = await api('GET', '/accounts');
    updatePending(list);
    const waiting = list.filter(a => a.status === 'PENDING');
    const status = a => `<span class="badge ${a.status === 'ACTIVE' ? 'good' : a.status === 'PENDING' ? 'warn' : 'bad'}">${a.status === 'ACTIVE' ? 'Active' : a.status === 'PENDING' ? 'Waiting' : 'Declined'}</span>`;
    const rows = list.map(a => `<tr><td><b>${esc(a.username)}</b></td><td><span class="role-pill">${esc(a.role)}</span></td><td>${status(a)}</td><td>${esc(when(a.createdAt))}</td>
      <td><div class="row-actions">${a.status !== 'ACTIVE' ? btn('Approve', 'approve-account', a.username, '', 'primary') : ''}
        ${a.username !== me.username && a.status !== 'PENDING' ? btn('Reset password', 'reset-password', a.username) : ''}
        ${a.status !== 'REJECTED' && a.username !== me.username ? btn(a.status === 'PENDING' ? 'Decline' : 'Switch off', 'reject-account', a.username, '', 'danger') : ''}</div></td></tr>`);
    return `${waiting.length ? `<div class="locked">${waiting.length} request${waiting.length > 1 ? 's are' : ' is'} waiting for your approval.</div>` : ''}
      <div class="card"><h3>Accounts</h3>${table(['Username', 'Type', 'Status', 'Created', ''], rows)}
      <p class="hint">Students can sign up on their own. Staff and administrator accounts can sign in only after you approve them. If someone forgot their password, press <b>Reset password</b> and give them the temporary one.</p></div>`;
  }
};

function updatePending(list) {
  const n = list.filter(a => a.status === 'PENDING').length;
  $('pending-count').textContent = n;
  $('pending-count').hidden = n === 0;
}

async function show(name) {
  current = name;
  $('page-title').textContent = TITLES[name];
  $('page-sub').textContent = SUBTITLES[name] || '';
  $('page-art').src = 'img/' + (ART[name] || 'dashboard') + '.svg';
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b.dataset.view === name));
  try { view.innerHTML = await views[name](); }
  catch (e) { if (me) view.innerHTML = '<div class="card">Could not load data: ' + esc(e.message) + '</div>'; }
  const search = $('student-search');
  if (search && document.activeElement === document.body && studentFilter.text) { search.focus(); search.setSelectionRange(search.value.length, search.value.length); }
}

/* ---------- student form (add / edit / view) ---------- */
const field = (label, name, v, attrs = '') => `<label>${label}<input name="${name}" value="${esc(v)}" ${attrs}></label>`;
function passwordForm() {
  const owner = me.username === 'admin'
    ? '<p class="muted">The owner account takes its password from the .env file when the server starts, so a new password here is replaced by the .env value after a restart.</p>' : '';
  return `<form id="f-password"><div class="modal-body">${owner}
    ${field('Current password', 'oldPassword', '', 'type="password" required autocomplete="current-password"')}
    ${field('New password (8 characters or more)', 'newPassword', '', 'type="password" required minlength="8" maxlength="128" autocomplete="new-password"')}
    ${field('Repeat the new password', 'confirmPassword', '', 'type="password" required minlength="8" maxlength="128" autocomplete="new-password"')}
    </div><div class="modal-foot"><span class="form-error" id="password-error"></span>
      <button type="button" data-action="close-modal">Cancel</button><button class="primary">Change password</button></div></form>`;
}

function studentForm(s) {
  const edit = !!s;
  s = s || {};
  const sel = (name, v, opts) => `<select name="${name}">${opts.map(o => `<option value="${o[0]}" ${o[0] === (v || '') ? 'selected' : ''}>${o[1]}</option>`).join('')}</select>`;
  return `<form id="f-student" data-id="${esc(s.id || '')}"><div class="modal-body">
    <fieldset><legend>Personal</legend><div class="grid2">
      ${field('First name *', 'firstName', s.firstName, 'required')}${field('Last name *', 'lastName', s.lastName, 'required')}
      ${field('Date of birth *', 'dateOfBirth', s.dateOfBirth, 'type="date" required')}
      <label>Gender${sel('gender', s.gender, [['', 'Not specified'], ['MALE', 'Male'], ['FEMALE', 'Female']])}</label>
      ${field('Nationality', 'nationality', s.nationality, 'placeholder="Kazakhstani"')}
    </div></fieldset>
    <fieldset><legend>Identity documents <span class="secure">${icon('lock').replace('<svg', '<svg width="12" height="12"')} encrypted in the database</span></legend><div class="grid2">
      ${field('IIN (12 digits) *', 'iin', s.iin, 'required inputmode="numeric" maxlength="12" pattern="\\d{12}" placeholder="040517550123"')}
      ${field('National ID number', 'nationalId', s.nationalId, 'maxlength="20" placeholder="046123456"')}
      ${field('Passport number', 'passportNumber', s.passportNumber, 'maxlength="20" placeholder="N12345678"')}
    </div></fieldset>
    <fieldset><legend>Contact</legend><div class="grid2">
      ${field('Phone', 'phone', s.phone, 'type="tel" placeholder="+7 701 000 0000"')}${field('Email', 'email', s.email, 'type="email"')}
      <label class="span2">Home address<input name="address" value="${esc(s.address)}"></label>
    </div></fieldset>
    <fieldset><legend>Study</legend><div class="grid2">
      ${field('Faculty', 'faculty', s.faculty, 'placeholder="Information Systems"')}
      <label>Course year${sel('courseYear', s.courseYear == null ? '' : String(s.courseYear), [['', '-'], ...[1, 2, 3, 4, 5, 6].map(n => [String(n), 'Year ' + n])])}</label>
    </div></fieldset>
    ${edit ? '' : `<fieldset><legend>Accommodation</legend>
      <label class="check"><input type="checkbox" name="assignRoom" checked> Give this student a room automatically (the room with the most free beds)</label>
    </fieldset>`}
    <fieldset><legend>Emergency contact</legend><div class="grid2">
      ${field('Contact name', 'emergencyName', s.emergencyName)}${field('Contact phone', 'emergencyPhone', s.emergencyPhone, 'type="tel"')}
    </div></fieldset></div>
    <div class="modal-foot"><span class="form-error" id="student-error"></span>
      <button type="button" data-action="close-modal">Cancel</button><button class="primary">${edit ? 'Save changes' : 'Add student'}</button></div></form>`;
}

function studentDetail(s, roomId) {
  const d = (k, v) => `<dt>${k}</dt><dd>${v ? esc(v) : '<span class="muted">-</span>'}</dd>`;
  const masked = s.iin && s.iin.startsWith('*');
  return `<div class="modal-body"><div class="profile-head"><span class="avatar">${esc(initials(s.firstName, s.lastName))}</span>
    <div><h3>${fullName(s)}</h3><span class="muted">${esc(s.id)} &middot; ${roomId ? 'Room ' + esc(roomId) : 'No room'}</span></div></div>
    ${masked ? locked('Identity numbers are masked for your role.') : ''}
    <dl class="detail">${d('Date of birth', s.dateOfBirth)}${d('Gender', s.gender)}${d('Nationality', s.nationality)}${d('IIN', s.iin)}
    ${d('National ID', s.nationalId)}${d('Passport', s.passportNumber)}${d('Phone', s.phone)}${d('Email', s.email)}
    ${d('Faculty', s.faculty)}${d('Course year', s.courseYear)}${d('Address', s.address)}
    ${d('Emergency contact', [s.emergencyName, s.emergencyPhone].filter(Boolean).join(' - '))}</dl></div>
    <div class="modal-foot"><button type="button" data-action="close-modal">Close</button></div>`;
}

/* ---------- downloads (certificates, CSV export) ---------- */
function download(doc) {
  const url = URL.createObjectURL(new Blob([doc.content], { type: doc.contentType }));
  const a = document.createElement('a');
  a.href = url; a.download = doc.filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/* ---------- actions ---------- */
const actions = {
  'export-students': async () => { try { download(await api('GET', '/export/students')); toast('Students exported'); } catch (e) { toast(e.message, true); } },
  'certificate': d => openModal('Accommodation certificate',
    `<div class="modal-body"><p>Choose a format for <b>${esc(d.id)}</b>. The certificate lists the room and issue date; it never contains identity numbers.</p>
     <div class="row-actions">${['text', 'html', 'csv'].map(f => btn('Download ' + f.toUpperCase(), 'get-certificate', d.id, `data-format="${f}"`, 'primary')).join('')}</div></div>
     <div class="modal-foot"><button type="button" data-action="close-modal">Close</button></div>`),
  'get-certificate': async d => { try { download(await api('GET', `/certificates/${d.id}/${d.format}`)); toast('Certificate downloaded'); } catch (e) { toast(e.message, true); } },
  'close-modal': () => closeModal(),
  'change-password': () => openModal('Change password', passwordForm()),
  'goto': d => show(d.id),
  'approve-account': d => run(() => api('POST', '/accounts/' + encodeURIComponent(d.id) + '/approve'), 'Account approved'),
  'reject-account': d => confirm('Decline or switch off ' + d.id + '? They will be signed out.') && run(() => api('POST', '/accounts/' + encodeURIComponent(d.id) + '/reject'), 'Account switched off'),
  'reset-password': async d => {
    if (!confirm('Create a new temporary password for ' + d.id + '? Their old password stops working and they are signed out.')) return;
    try {
      const r = await api('POST', '/accounts/' + encodeURIComponent(d.id) + '/reset-password');
      openModal('Temporary password', `<div class="modal-body"><p>Give this password to <b>${esc(r.username)}</b>. It is shown only once. They should change it after signing in.</p>
        <p><code class="temp-password">${esc(r.temporaryPassword)}</code></p></div>
        <div class="modal-foot"><button class="primary" data-action="close-modal">Done</button></div>`);
    } catch (e) { toast(e.message, true); }
  },
  'show-signin': () => authTab('signin'),
  'show-signup': () => authTab('signup'),
  'add-student': () => openModal('Add student', studentForm(null)),
  'edit-student': async d => { try { openModal('Edit student', studentForm(await api('GET', '/students/' + d.id))); } catch (e) { toast(e.message, true); } },
  'view-student': async d => {
    try {
      const [s, rooms] = await Promise.all([api('GET', '/students/' + d.id), api('GET', '/rooms')]);
      openModal('Student profile', studentDetail(s, (rooms.find(r => r.occupants.includes(d.id)) || {}).id));
    } catch (e) { toast(e.message, true); }
  },
  'delete-student': d => confirm('Delete student ' + d.id + '?') && run(() => api('DELETE', '/students/' + d.id), 'Student deleted'),
  'request-room': d => run(() => api('POST', '/room-requests', { studentId: d.id }), 'Room assigned'),
  'checkout': d => run(() => api('DELETE', `/rooms/${d.room}/occupants/${d.id}`), 'Checked out'),
  'undo': () => run(() => api('POST', '/room-requests/undo'), 'Last room action undone'),
  'edit-room': d => { const c = prompt('New capacity', d.cap); if (c === null) return;
    run(() => api('PUT', '/rooms/' + d.id, { capacity: Number(c) }), 'Room updated'); },
  'delete-room': d => confirm('Delete room ' + d.id + '?') && run(() => api('DELETE', '/rooms/' + d.id), 'Room deleted'),
  'assign': d => { const t = prompt('Technician name'); if (t) run(() => api('PUT', '/tickets/' + d.id, { action: 'assign', technician: t }), 'Repair assigned'); },
  'start': d => run(() => api('PUT', '/tickets/' + d.id, { action: 'start' }), 'Work started'),
  'resolve': d => run(() => api('PUT', '/tickets/' + d.id, { action: 'resolve' }), 'Repair resolved'),
  'delete-ticket': d => confirm('Delete repair ' + d.id + '?') && run(() => api('DELETE', '/tickets/' + d.id), 'Repair deleted'),
  'depart': d => run(() => api('PUT', '/visitors/' + d.id), 'Departure recorded'),
  'delete-visitor': d => confirm('Delete this visitor record?') && run(() => api('DELETE', '/visitors/' + d.id), 'Record deleted'),
  'edit-ann': d => { const t = prompt('Title', d.title); if (t === null) return; const b = prompt('Text', d.body || ''); if (b === null) return;
    run(() => api('PUT', '/announcements/' + d.id, { title: t, body: b }), 'Announcement updated'); },
  'delete-ann': d => confirm('Delete this announcement?') && run(() => api('DELETE', '/announcements/' + d.id), 'Announcement deleted')
};

document.addEventListener('click', e => {
  const b = e.target.closest('[data-action]');
  if (b && actions[b.dataset.action]) actions[b.dataset.action](b.dataset);
});

view.addEventListener('change', e => {
  if (e.target.id === 'only-inside') { onlyInside = e.target.checked; show('visitors'); }
  if (e.target.id === 'student-faculty') { studentFilter.faculty = e.target.value; show('students'); }
});
view.addEventListener('input', e => {
  if (e.target.id === 'student-search') { studentFilter.text = e.target.value; show('students'); }
});

const form = f => Object.fromEntries(new FormData(f).entries());
const creators = {
  'f-room': f => run(() => api('POST', '/rooms', { id: form(f).id, capacity: Number(form(f).capacity) }), 'Room added'),
  'f-ticket': f => run(() => api('POST', '/tickets', form(f)), 'Problem reported'),
  'f-visitor': f => run(() => api('POST', '/visitors', form(f)), 'Arrival recorded'),
  'f-ann': f => run(() => api('POST', '/announcements', form(f)), 'Announcement posted')
};

document.addEventListener('submit', async e => {
  e.preventDefault();
  const f = e.target;
  // f.id would return the <input name="id"> of the room form, so read the attribute instead
  const fid = f.getAttribute('id');
  if (fid === 'f-student') {
    const data = form(f);
    if (data.courseYear === '') delete data.courseYear;
    try {
      let msg = 'Student updated';
      if (f.dataset.id) await api('PUT', '/students/' + f.dataset.id, data);
      else {
        const r = await api('POST', '/students', data);
        msg = r.roomId ? `Student added and placed in room ${r.roomId}` : r.roomMessage ? 'Student added. ' + r.roomMessage : 'Student added';
      }
      closeModal();
      toast(msg);
      await show('students');
    } catch (err) { $('student-error').textContent = err.message; }
  } else if (fid === 'f-password') {
    const data = form(f);
    if (data.newPassword !== data.confirmPassword) { $('password-error').textContent = 'The two new passwords do not match'; return; }
    try {
      await api('POST', '/me/password', { oldPassword: data.oldPassword, newPassword: data.newPassword });
      closeModal();
      toast('Password changed. Your other sessions were signed out.');
    } catch (err) { $('password-error').textContent = err.message; }
  } else if (fid === 'login-form') {
    await login(f);
  } else if (fid === 'signup-form') {
    await signUp(f);
  } else if (creators[fid]) {
    creators[fid](f);
  }
});

$('nav').addEventListener('click', e => {
  const b = e.target.closest('button');
  if (b && b.dataset.view) show(b.dataset.view);
});

/* ---------- sign in / out ---------- */
function showApp() {
  $('login-screen').hidden = true;
  $('app').hidden = false;
  $('who-name').textContent = me.username;
  $('who-role').textContent = me.role;
  $('who-avatar').textContent = me.username[0].toUpperCase();
  $('nav-accounts').hidden = !isAdmin();
  if (isAdmin()) api('GET', '/accounts').then(updatePending).catch(() => {});
  if (current === 'accounts' && !isAdmin()) current = 'dashboard';
  $('today').textContent = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  show(current);
}

function signOutLocally() {
  token = null; me = null;
  try { sessionStorage.removeItem('token'); } catch (e) { /* ignore */ }
  closeModal();
  view.innerHTML = '';   // never leave the previous user's data in the page
  $('modal-body').innerHTML = '';
  $('app').hidden = true;
  $('login-screen').hidden = false;
  authTab('signin');
}

function authTab(which) {
  const up = which === 'signup';
  $('login-form').hidden = up;
  $('signup-form').hidden = !up;
  $('tab-signin').classList.toggle('active', !up);
  $('tab-signup').classList.toggle('active', up);
  $('login-error').textContent = '';
  $('signup-error').textContent = '';
  $('signup-ok').textContent = '';
}

$('signup-role').addEventListener('change', e => {
  $('signup-hint').textContent = e.target.value === 'STUDENT'
    ? 'Student accounts are ready immediately.'
    : 'Staff and administrator accounts must be approved by the administrator before you can sign in.';
});

async function signUp(f) {
  $('signup-error').textContent = '';
  $('signup-ok').textContent = '';
  const data = form(f);
  if (data.password !== data.confirm) { $('signup-error').textContent = 'The two passwords are not the same'; return; }
  delete data.confirm;
  $('signup-btn').disabled = true;
  try {
    const r = await api('POST', '/signup', data);
    f.reset();
    if (r.status === 'ACTIVE') {
      authTab('signin');
      $('login-user').value = r.username;
      $('login-error').textContent = '';
      toast(r.message);
    } else {
      $('signup-ok').textContent = r.message;
    }
  } catch (err) { $('signup-error').textContent = err.message; }
  $('signup-btn').disabled = false;
}

async function login(f) {
  $('login-error').textContent = '';
  $('login-btn').disabled = true;
  try {
    const r = await api('POST', '/login', form(f));
    token = r.token; me = { username: r.username, role: r.role };
    try { sessionStorage.setItem('token', token); } catch (e) { /* ignore */ }
    f.reset();
    showApp();
  } catch (err) { $('login-error').textContent = err.message; }
  $('login-btn').disabled = false;
}

$('logout').addEventListener('click', async () => {
  try { await api('POST', '/logout'); } catch (e) { /* already signed out */ }
  signOutLocally();
});

(async function start() {
  if (token) {
    try { const r = await api('GET', '/me'); me = { username: r.username, role: r.role }; showApp(); return; }
    catch (e) { /* token expired */ }
  }
  signOutLocally();
})();
