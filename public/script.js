const AUTH_API = "/api/auth";
const API_BASE = "/api/complaints";
const AUTHORITY_API = "/api/authority";
const TOKEN_KEY = "sessionToken";

let currentUser = null;

const authSection = document.getElementById("authSection");
const appMain = document.getElementById("appMain");
const authorityMain = document.getElementById("authorityMain");
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const loginMessage = document.getElementById("loginMessage");
const registerMessage = document.getElementById("registerMessage");
const tabLogin = document.getElementById("tabLogin");
const tabRegister = document.getElementById("tabRegister");
const logoutBtn = document.getElementById("logoutBtn");
const userBadge = document.getElementById("userBadge");
const serverStatus = document.getElementById("serverStatus");

const form = document.getElementById("complaintForm");
const formMessage = document.getElementById("formMessage");
const complaintList = document.getElementById("complaintList");
const escalatedList = document.getElementById("escalatedList");
const submitBtn = document.getElementById("submitBtn");
const submitSection = document.getElementById("submitSection");
const escalatedSection = document.getElementById("escalatedSection");
const complaintsTitle = document.getElementById("complaintsTitle");
const filedAsName = document.getElementById("filedAsName");

const authorityProfile = document.getElementById("authorityProfile");
const authorityComplaintList = document.getElementById("authorityComplaintList");
const authorityRefreshBtn = document.getElementById("authorityRefreshBtn");

const statTotal = document.getElementById("statTotal");
const statOpen = document.getElementById("statOpen");
const statEscalated = document.getElementById("statEscalated");
const statResolved = document.getElementById("statResolved");

function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token);
  else sessionStorage.removeItem(TOKEN_KEY);
}

function authHeaders(extra = {}) {
  const headers = { ...extra };
  const token = getToken();
  if (token) headers["Authorization"] = "Bearer " + token;
  return headers;
}

function showAuthView() {
  authSection.classList.remove("hidden");
  appMain.classList.add("hidden");
  authorityMain.classList.add("hidden");
  userBadge.classList.add("hidden");
  logoutBtn.classList.add("hidden");
  currentUser = null;
}

function showStudentOrAdminView(user) {
  currentUser = user;
  authSection.classList.add("hidden");
  appMain.classList.remove("hidden");
  authorityMain.classList.add("hidden");
  userBadge.classList.remove("hidden");
  logoutBtn.classList.remove("hidden");

  const roleLabel = user.role === "ADMIN" ? "Admin" : "Student";
  userBadge.textContent = `${user.fullName} (${roleLabel})`;
  userBadge.className = "user-badge " + (user.role === "ADMIN" ? "admin" : "student");

  const isAdmin = user.role === "ADMIN";
  submitSection.classList.toggle("hidden", isAdmin);
  escalatedSection.classList.toggle("hidden", !isAdmin);
  complaintsTitle.textContent = isAdmin ? "All Complaints" : "My Complaints";

  if (!isAdmin) {
    filedAsName.textContent = user.fullName;
  }

  loadComplaints();
}

function showAuthorityView(user) {
  currentUser = user;
  authSection.classList.add("hidden");
  appMain.classList.add("hidden");
  authorityMain.classList.remove("hidden");
  userBadge.classList.remove("hidden");
  logoutBtn.classList.remove("hidden");

  const designation = user.designation || "Authority";
  userBadge.textContent = `${user.fullName} (${designation})`;
  userBadge.className = "user-badge authority";

  loadAuthorityDashboard();
}

function showAppView(user) {
  if (user.role === "AUTHORITY") {
    showAuthorityView(user);
  } else {
    showStudentOrAdminView(user);
  }
}

tabLogin.addEventListener("click", () => {
  tabLogin.classList.add("active");
  tabRegister.classList.remove("active");
  loginForm.classList.remove("hidden");
  registerForm.classList.add("hidden");
});

tabRegister.addEventListener("click", () => {
  tabRegister.classList.add("active");
  tabLogin.classList.remove("active");
  registerForm.classList.remove("hidden");
  loginForm.classList.add("hidden");
});

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  loginMessage.textContent = "";
  loginMessage.className = "msg";

  const payload = {
    username: document.getElementById("loginUsername").value.trim(),
    password: document.getElementById("loginPassword").value
  };

  try {
    const res = await fetch(AUTH_API + "/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Login failed");

    setToken(data.token);
    showAppView(data);
    loginForm.reset();
  } catch (err) {
    loginMessage.textContent = err.message;
    loginMessage.className = "msg error";
  }
});

registerForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  registerMessage.textContent = "";
  registerMessage.className = "msg";

  const payload = {
    fullName: document.getElementById("regFullName").value.trim(),
    username: document.getElementById("regUsername").value.trim(),
    password: document.getElementById("regPassword").value,
    email: document.getElementById("regEmail").value.trim(),
    phone: document.getElementById("regPhone").value.trim()
  };

  try {
    const res = await fetch(AUTH_API + "/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Registration failed");

    setToken(data.token);
    showAppView(data);
    registerForm.reset();
  } catch (err) {
    registerMessage.textContent = err.message;
    registerMessage.className = "msg error";
  }
});

logoutBtn.addEventListener("click", async () => {
  try {
    await fetch(AUTH_API + "/logout", {
      method: "POST",
      headers: authHeaders()
    });
  } catch (_) { /* ignore */ }
  setToken(null);
  showAuthView();
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  submitBtn.disabled = true;
  submitBtn.textContent = "Submitting...";
  formMessage.textContent = "";

  const payload = {
    category: document.getElementById("category").value,
    description: document.getElementById("description").value.trim()
  };

  try {
    const res = await fetch(API_BASE, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Failed to submit complaint");

    formMessage.textContent = `Submitted! Priority: ${data.priorityTier} (score ${data.priorityScore}) — deadline ${data.deadline}`;
    formMessage.className = "msg success";
    form.reset();
    loadComplaints();
  } catch (err) {
    formMessage.textContent = "Error: " + err.message;
    formMessage.className = "msg error";
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Submit Complaint";
  }
});

document.getElementById("refreshBtn").addEventListener("click", loadComplaints);
authorityRefreshBtn.addEventListener("click", loadAuthorityDashboard);

async function loadAuthorityDashboard() {
  if (!getToken()) return;

  authorityProfile.innerHTML = `<p class="loading">Loading profile...</p>`;
  authorityComplaintList.innerHTML = `<p class="loading">Loading complaints...</p>`;

  try {
    const profileRes = await fetch(AUTHORITY_API + "/profile", { headers: authHeaders() });
    if (profileRes.status === 401) {
      setToken(null);
      showAuthView();
      return;
    }
    if (!profileRes.ok) throw new Error("Could not load authority profile");

    const profile = await profileRes.json();
    authorityProfile.innerHTML = `
      <p><strong>${escapeHtml(profile.fullName)}</strong></p>
      <p class="profile-meta">${escapeHtml(profile.designation)} · Level ${profile.authorityLevel ?? "—"}</p>
      <p class="profile-meta">Scope: ${escapeHtml(profile.categoryLabel)}</p>
    `;

    const listRes = await fetch(AUTHORITY_API + "/complaints", { headers: authHeaders() });
    if (!listRes.ok) throw new Error("Could not load assigned complaints");

    const complaints = await listRes.json();
    renderAuthorityList(complaints);

    serverStatus.textContent = "Connected";
    serverStatus.className = "status-pill online";
  } catch (err) {
    authorityComplaintList.innerHTML = `<p class="empty-msg">Could not load dashboard — is the server running?</p>`;
    serverStatus.textContent = "Offline";
    serverStatus.className = "status-pill offline";
  }
}

function renderAuthorityList(complaints) {
  if (!complaints.length) {
    authorityComplaintList.innerHTML = `<p class="empty-msg">No complaints currently assigned to you.</p>`;
    return;
  }

  authorityComplaintList.innerHTML = complaints.map(c => {
    const contactHtml = buildStudentContactHtml(c);
    return `
    <div class="complaint-card ${c.priorityTier}">
      <div class="complaint-top">
        <span>#${c.id} — ${escapeHtml(c.studentName)} · ${escapeHtml(c.categoryLabel || formatCategory(c.category))}</span>
        <span class="tag-row">
          <span class="tag ${c.priorityTier}">${c.priorityTier}</span>
          <span class="status-tag ${c.status}">${c.status}${c.escalationLevel > 0 ? " · Lv " + c.escalationLevel : ""}</span>
        </span>
      </div>
      <p class="complaint-desc">${escapeHtml(c.description)}</p>
      <div class="complaint-meta">
        <span>Category: ${escapeHtml(c.categoryLabel || formatCategory(c.category))}</span>
        <span>Level 0 authority: ${escapeHtml(c.level0Authority || "—")}</span>
        <span>Submitted: ${c.submittedDate}</span>
        <span>Deadline: ${c.deadline}</span>
      </div>
      <div class="authority-actions">
        <button type="button" class="call-btn" onclick="toggleStudentContact(${c.id})">Call / Contact</button>
        <button type="button" class="resolve-btn" onclick="authorityResolveComplaint(${c.id})">Mark Resolved</button>
      </div>
      <div id="contact-${c.id}" class="student-contact hidden">${contactHtml}</div>
    </div>
  `;
  }).join("");
}

function buildStudentContactHtml(c) {
  const parts = [`<p><strong>${escapeHtml(c.studentName)}</strong></p>`];
  if (c.studentPhone) {
    parts.push(`<p>Phone: <a href="tel:${escapeAttr(c.studentPhone)}">${escapeHtml(c.studentPhone)}</a></p>`);
  }
  if (c.studentEmail) {
    parts.push(`<p>Email: <a href="mailto:${escapeAttr(c.studentEmail)}">${escapeHtml(c.studentEmail)}</a></p>`);
  }
  if (!c.studentPhone && !c.studentEmail) {
    parts.push(`<p class="contact-hint">No phone or email on file for this student. Use campus records if needed.</p>`);
  }
  return parts.join("");
}

function toggleStudentContact(id) {
  const el = document.getElementById("contact-" + id);
  if (el) el.classList.toggle("hidden");
}

async function authorityResolveComplaint(id) {
  try {
    const res = await fetch(`${AUTHORITY_API}/complaints/${id}/resolve`, {
      method: "POST",
      headers: authHeaders()
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "Failed to resolve");
    loadAuthorityDashboard();
  } catch (err) {
    alert(err.message || "Could not mark complaint as resolved.");
  }
}

async function loadComplaints() {
  if (!getToken() || (currentUser && currentUser.role === "AUTHORITY")) return;

  complaintList.innerHTML = `<p class="loading">Loading complaints...</p>`;
  if (currentUser && currentUser.role === "ADMIN") {
    escalatedList.innerHTML = `<p class="loading">Loading escalated complaints...</p>`;
  }

  try {
    const allRes = await fetch(API_BASE, { headers: authHeaders() });
    if (allRes.status === 401) {
      setToken(null);
      showAuthView();
      return;
    }
    if (!allRes.ok) throw new Error("Could not load complaints");

    const all = await allRes.json();
    let escalated = [];

    if (currentUser && currentUser.role === "ADMIN") {
      const escalatedRes = await fetch(API_BASE + "/escalated", { headers: authHeaders() });
      if (escalatedRes.ok) {
        escalated = await escalatedRes.json();
      }
      renderList(escalated, escalatedList, true);
    }

    updateStats(all);
    const showResolve = currentUser && currentUser.role === "ADMIN";
    renderList(all, complaintList, showResolve);

    serverStatus.textContent = "Connected";
    serverStatus.className = "status-pill online";
  } catch (err) {
    complaintList.innerHTML = `<p class="empty-msg">Could not load complaints — is the server running?</p>`;
    serverStatus.textContent = "Offline";
    serverStatus.className = "status-pill offline";
  }
}

function updateStats(complaints) {
  statTotal.textContent = complaints.length;
  statOpen.textContent = complaints.filter(c => c.status === "OPEN").length;
  statEscalated.textContent = complaints.filter(c => c.status === "ESCALATED").length;
  statResolved.textContent = complaints.filter(c => c.status === "RESOLVED").length;
}

function renderList(complaints, container, showResolveBtn) {
  if (!complaints.length) {
    container.innerHTML = `<p class="empty-msg">Nothing here yet.</p>`;
    return;
  }

  container.innerHTML = complaints.map(c => `
    <div class="complaint-card ${c.priorityTier}">
      <div class="complaint-top">
        <span>#${c.id} — ${escapeHtml(c.studentName)} (${escapeHtml(formatCategory(c.category))})</span>
        <span class="tag-row">
          <span class="tag ${c.priorityTier}">${c.priorityTier}</span>
          <span class="status-tag ${c.status}">${c.status}${c.escalationLevel > 0 ? " · Lv " + c.escalationLevel : ""}</span>
        </span>
      </div>
      <p class="complaint-desc">${escapeHtml(c.description)}</p>
      <div class="complaint-meta">
        <span>Submitted: ${c.submittedDate}</span>
        <span>Deadline: ${c.deadline}</span>
        <span>Score: ${c.priorityScore}</span>
      </div>
      ${showResolveBtn && c.status !== "RESOLVED"
        ? `<button class="resolve-btn" onclick="resolveComplaint(${c.id})">Mark Resolved</button>`
        : ""}
    </div>
  `).join("");
}

async function resolveComplaint(id) {
  try {
    const res = await fetch(`${API_BASE}/${id}/resolve`, {
      method: "POST",
      headers: authHeaders()
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "Failed to resolve");
    loadComplaints();
  } catch (err) {
    alert(err.message || "Could not mark complaint as resolved.");
  }
}

function formatCategory(cat) {
  const labels = {
    mess: "Mess / Food",
    hostel: "Hostel",
    faculty: "Faculty",
    electrical: "Electrical",
    water: "Water",
    harassment: "Harassment / Ragging",
    other: "Other"
  };
  return labels[cat] || cat;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function escapeAttr(str) {
  return String(str ?? "").replace(/"/g, "&quot;");
}

async function bootstrap() {
  showAuthView();
  const token = getToken();
  if (!token) {
    serverStatus.textContent = "Login required";
    serverStatus.className = "status-pill offline";
    return;
  }

  try {
    const res = await fetch(AUTH_API + "/me", { headers: authHeaders() });
    if (!res.ok) {
      setToken(null);
      serverStatus.textContent = "Login required";
      return;
    }
    const user = await res.json();
    showAppView(user);
  } catch (_) {
    setToken(null);
    serverStatus.textContent = "Offline";
  }
}

window.toggleStudentContact = toggleStudentContact;
window.authorityResolveComplaint = authorityResolveComplaint;
window.resolveComplaint = resolveComplaint;

bootstrap();
setInterval(() => {
  if (getToken() && currentUser) {
    if (currentUser.role === "AUTHORITY") loadAuthorityDashboard();
    else loadComplaints();
  }
}, 30000);
