'use strict';
const view = document.getElementById('view');
const roleSel = document.getElementById('role');
let current = 'dashboard';
let role = 'STUDENT';
try { role = localStorage.getItem('role') || 'STUDENT'; } catch (e) { /* storage not available */ }
roleSel.value = role;

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const when = t => t ? t.replace('T', ' ').slice(0, 16) : '';

async function api(method, path, body) {
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Role': role },
    body: body ? JSON.stringify(body) : undefined
  });
  let data = {};
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (!res.ok) throw new Error(data.error || ('Request failed (' + res.status + ')'));
  return data;
}

let toastTimer;
function toast(msg, bad) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, 3200);
}

async function run(fn, okMsg) {
  try { await fn(); if (okMsg) toast(okMsg); await show(current); }
  catch (e) { toast(e.message, true); }
}

const table = (heads, rows) => rows.length
  ? `<table><thead><tr>${heads.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table>`
  : '<div class="empty">Nothing here yet.</div>';
const btn = (label, action, id, extra = '', cls = '') =>
  `<button class="small ${cls}" data-action="${action}" data-id="${esc(id)}" ${extra}>${label}</button>`;

const views = {
  async dashboard() {
    const [s, events] = await Promise.all([api('GET', '/summary'), api('GET', '/events')]);
    const stat = (n, label) => `<div class="stat"><b>${s[n]}</b>${label}</div>`;
    return `<h2>Dashboard</h2>
      <div class="cards">${stat('students', 'Students')}${stat('rooms', 'Rooms')}${stat('freeBeds', 'Free beds')}
      ${stat('openTickets', 'Open repairs')}${stat('visitorsInside', 'Visitors inside')}${stat('announcements', 'Announcements')}</div>
      <div class="card"><h3>Recent activity</h3>${events.length
        ? events.map(e => `<div><span class="muted">${esc(e.type)}</span> &nbsp;${esc(e.message)}</div>`).join('')
        : '<div class="empty">No activity yet.</div>'}</div>`;
  },

  async students() {
    const [students, rooms] = await Promise.all([api('GET', '/students'), api('GET', '/rooms')]);
    const roomOf = id => (rooms.find(r => r.occupants.includes(id)) || {}).id;
    const rows = students.map(s => `<tr><td>${esc(s.id)}</td><td>${esc(s.name)}</td><td>${esc(s.phone)}</td>
      <td>${esc(roomOf(s.id) || '-')}</td><td>
      ${roomOf(s.id) ? btn('Check out', 'checkout', s.id, `data-room="${esc(roomOf(s.id))}"`) : btn('Request room', 'request-room', s.id)}
      ${btn('Edit', 'edit-student', s.id, `data-name="${esc(s.name)}" data-phone="${esc(s.phone)}"`)}
      ${btn('Delete', 'delete-student', s.id, '', 'danger')}</td></tr>`);
    return `<h2>Students</h2>
      <div class="card"><form class="inline" id="f-student">
        <label>Name<input name="name" required></label><label>Phone<input name="phone"></label>
        <button class="primary">Add student</button></form></div>
      <div class="card">${table(['ID', 'Name', 'Phone', 'Room', 'Actions'], rows)}</div>`;
  },

  async rooms() {
    const [rooms, students] = await Promise.all([api('GET', '/rooms'), api('GET', '/students')]);
    const name = id => (students.find(s => s.id === id) || {}).name || id;
    const rows = rooms.map(r => `<tr><td>${esc(r.id)}</td><td>${r.capacity}</td><td>${r.freeBeds}</td>
      <td>${r.occupants.map(o => `<span class="chip">${esc(name(o))}<button data-action="checkout" data-id="${esc(o)}" data-room="${esc(r.id)}" title="Check out">&times;</button></span>`).join('') || '<span class="muted">empty</span>'}</td>
      <td>${btn('Change capacity', 'edit-room', r.id, `data-cap="${r.capacity}"`)}${btn('Delete', 'delete-room', r.id, '', 'danger')}</td></tr>`);
    return `<h2>Rooms</h2>
      <div class="card"><form class="inline" id="f-room">
        <label>Room number<input name="id" required placeholder="R301"></label>
        <label>Capacity<input name="capacity" type="number" min="1" value="2" required></label>
        <button class="primary">Add room</button>
        <button type="button" class="small" data-action="undo">Undo last room action</button></form></div>
      <div class="card">${table(['Room', 'Capacity', 'Free beds', 'Residents', 'Actions'], rows)}</div>`;
  },

  async tickets() {
    const [tickets, rooms] = await Promise.all([api('GET', '/tickets'), api('GET', '/rooms')]);
    const next = t => t.status === 'OPEN' ? btn('Assign', 'assign', t.id)
      : t.status === 'ASSIGNED' ? btn('Start', 'start', t.id)
      : t.status === 'IN_PROGRESS' ? btn('Resolve', 'resolve', t.id) : '';
    const rows = tickets.map(t => `<tr><td>${esc(t.id)}</td><td>${esc(t.roomId)}</td><td>${esc(t.description)}</td>
      <td class="${t.priority}">${t.priority}</td><td><span class="badge ${t.status}">${t.status.replace('_', ' ')}</span></td>
      <td>${esc(t.technician || '-')}</td><td>${next(t)}${btn('Delete', 'delete-ticket', t.id, '', 'danger')}</td></tr>`);
    return `<h2>Repairs</h2>
      <p class="muted">Anyone can report a problem. Only STAFF or ADMIN can assign, start, resolve or delete (switch the role at the top).</p>
      <div class="card"><form class="inline" id="f-ticket">
        <label>Room<select name="roomId" required>${rooms.map(r => `<option>${esc(r.id)}</option>`).join('')}</select></label>
        <label>Problem<input name="description" required size="32"></label>
        <label>Priority<select name="priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option><option>URGENT</option></select></label>
        <button class="primary">Report problem</button></form></div>
      <div class="card">${table(['ID', 'Room', 'Problem', 'Priority', 'Status', 'Technician', 'Actions'], rows)}</div>`;
  },

  async visitors() {
    const [list, students] = await Promise.all([api('GET', window.onlyInside ? '/visitors/inside' : '/visitors'), api('GET', '/students')]);
    const name = id => (students.find(s => s.id === id) || {}).name || id;
    const rows = list.map(v => `<tr><td>${esc(v.id)}</td><td>${esc(v.visitorName)}</td><td>${esc(name(v.hostStudentId))}</td>
      <td>${when(v.arrivedAt)}</td><td>${v.inside ? '<b>inside</b>' : when(v.departedAt)}</td>
      <td>${v.inside ? btn('Mark departed', 'depart', v.id) : ''}${btn('Delete', 'delete-visitor', v.id, '', 'danger')}</td></tr>`);
    return `<h2>Visitors</h2>
      <div class="card"><form class="inline" id="f-visitor">
        <label>Visitor name<input name="visitorName" required></label>
        <label>Visiting<select name="hostStudentId" required>${students.map(s => `<option value="${esc(s.id)}">${esc(s.name)} (${esc(s.id)})</option>`).join('')}</select></label>
        <button class="primary">Record arrival</button>
        <label style="flex-direction:row;align-items:center;gap:6px"><input type="checkbox" id="only-inside" ${window.onlyInside ? 'checked' : ''}> only visitors inside</label></form></div>
      <div class="card">${table(['ID', 'Visitor', 'Host', 'Arrived', 'Left', 'Actions'], rows)}</div>`;
  },

  async announcements() {
    const list = await api('GET', '/announcements');
    return `<h2>Announcements</h2>
      <div class="card"><form class="inline" id="f-ann">
        <label>Title<input name="title" required></label><label>Text<input name="body" size="40"></label>
        <button class="primary">Post announcement</button></form></div>
      <div class="card">${list.length ? list.slice().reverse().map(a => `<div class="ann"><h3>${esc(a.title)}</h3>
        <div>${esc(a.body)}</div><div class="muted">${when(a.postedAt)} &middot; ${esc(a.id)}</div>
        ${btn('Edit', 'edit-ann', a.id, `data-title="${esc(a.title)}" data-body="${esc(a.body)}"`)}${btn('Delete', 'delete-ann', a.id, '', 'danger')}</div>`).join('')
        : '<div class="empty">No announcements yet.</div>'}</div>`;
  }
};

async function show(name) {
  current = name;
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b.dataset.view === name));
  try { view.innerHTML = await views[name](); }
  catch (e) { view.innerHTML = '<div class="card">Could not load data: ' + esc(e.message) + '</div>'; }
}

const form = f => Object.fromEntries(new FormData(f).entries());

const actions = {
  'request-room': d => run(() => api('POST', '/room-requests', { studentId: d.id }), 'Room assigned'),
  'checkout': d => run(() => api('DELETE', `/rooms/${d.room}/occupants/${d.id}`), 'Checked out'),
  'undo': () => run(() => api('POST', '/room-requests/undo'), 'Last room action undone'),
  'edit-student': d => { const n = prompt('Name', d.name); if (n === null) return; const p = prompt('Phone', d.phone || ''); if (p === null) return;
    run(() => api('PUT', '/students/' + d.id, { name: n, phone: p }), 'Student updated'); },
  'delete-student': d => confirm('Delete student ' + d.id + '?') && run(() => api('DELETE', '/students/' + d.id), 'Student deleted'),
  'edit-room': d => { const c = prompt('New capacity', d.cap); if (c === null) return;
    run(() => api('PUT', '/rooms/' + d.id, { capacity: Number(c) }), 'Room updated'); },
  'delete-room': d => confirm('Delete room ' + d.id + '?') && run(() => api('DELETE', '/rooms/' + d.id), 'Room deleted'),
  'assign': d => { const t = prompt('Technician name'); if (t) run(() => api('PUT', '/tickets/' + d.id, { action: 'assign', technician: t }), 'Ticket assigned'); },
  'start': d => run(() => api('PUT', '/tickets/' + d.id, { action: 'start' }), 'Work started'),
  'resolve': d => run(() => api('PUT', '/tickets/' + d.id, { action: 'resolve' }), 'Ticket resolved'),
  'delete-ticket': d => confirm('Delete ticket ' + d.id + '?') && run(() => api('DELETE', '/tickets/' + d.id), 'Ticket deleted'),
  'depart': d => run(() => api('PUT', '/visitors/' + d.id), 'Departure recorded'),
  'delete-visitor': d => confirm('Delete this visitor record?') && run(() => api('DELETE', '/visitors/' + d.id), 'Record deleted'),
  'edit-ann': d => { const t = prompt('Title', d.title); if (t === null) return; const b = prompt('Text', d.body || ''); if (b === null) return;
    run(() => api('PUT', '/announcements/' + d.id, { title: t, body: b }), 'Announcement updated'); },
  'delete-ann': d => confirm('Delete this announcement?') && run(() => api('DELETE', '/announcements/' + d.id), 'Announcement deleted')
};

view.addEventListener('click', e => {
  const b = e.target.closest('[data-action]');
  if (b && actions[b.dataset.action]) actions[b.dataset.action](b.dataset);
});

view.addEventListener('change', e => {
  if (e.target.id === 'only-inside') { window.onlyInside = e.target.checked; show('visitors'); }
});

const creators = {
  'f-student': f => run(() => api('POST', '/students', form(f)), 'Student added'),
  'f-room': f => run(() => api('POST', '/rooms', { id: form(f).id, capacity: Number(form(f).capacity) }), 'Room added'),
  'f-ticket': f => run(() => api('POST', '/tickets', form(f)), 'Problem reported'),
  'f-visitor': f => run(() => api('POST', '/visitors', form(f)), 'Arrival recorded'),
  'f-ann': f => run(() => api('POST', '/announcements', form(f)), 'Announcement posted')
};

view.addEventListener('submit', e => {
  e.preventDefault();
  if (creators[e.target.id]) creators[e.target.id](e.target);
});

document.getElementById('nav').addEventListener('click', e => {
  if (e.target.dataset.view) show(e.target.dataset.view);
});

roleSel.addEventListener('change', () => {
  role = roleSel.value;
  try { localStorage.setItem('role', role); } catch (e) { /* ignore */ }
  toast('Now acting as ' + role);
  show(current);
});

show('dashboard');
