// Import Firebase modules from CDN
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getAuth, signInWithEmailAndPassword, signOut, onAuthStateChanged, createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { getFirestore, doc, getDoc, setDoc, collection, onSnapshot, updateDoc, deleteDoc, query, orderBy } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

// ── FIREBASE CONFIGURATION ────────────────────────────────
const firebaseConfig = {
    apiKey: "AIzaSyB2uyp8emimXOyRNXfuv5AJys1mLSPa9xs",
    authDomain: "homeserve-72880.firebaseapp.com",
    projectId: "homeserve-72880",
    storageBucket: "homeserve-72880.firebasestorage.app",
    messagingSenderId: "35163826001",
    appId: "1:35163826001:web:28bd955eaea1dcf4af1570"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// Global State
let unsubscribeBookings = null;
let unsubscribeProviders = null;
let unsubscribeCustomers = null;
let unsubscribeCategories = null;
let unsubscribeSupport = null;

let bookingsChart = null;
let revenueChart = null;

// ── DOM ELEMENTS ──────────────────────────────────────────
const loginContainer = document.getElementById("login-container");
const appContainer = document.getElementById("app-container");
const loginForm = document.getElementById("login-form");
const loginEmail = document.getElementById("login-email");
const loginPassword = document.getElementById("login-password");
const btnLogin = document.getElementById("btn-login");
const loginBtnText = document.getElementById("login-btn-text");
const loginSpinner = document.getElementById("login-spinner");
const loginError = document.getElementById("login-error");
const errorMessage = document.getElementById("error-message");

const adminDisplayName = document.getElementById("admin-display-name");
const adminDisplayEmail = document.getElementById("admin-display-email");
const btnLogout = document.getElementById("btn-logout");

const tabTitle = document.getElementById("tab-title");
const tabSubtitle = document.getElementById("tab-subtitle");

// ── AUTHENTICATION LIFECYCLE ──────────────────────────────
onAuthStateChanged(auth, async (user) => {
    if (user) {
        // Show loading state
        showLoginLoading(true);
        try {
            // Verify if user is registered in the `/admins` collection
            const adminDocRef = doc(db, "admins", user.uid);
            let adminSnap = await getDoc(adminDocRef);

            // Bootstrapping: If email is admin@homeserve.com, register them in admins collection automatically
            const specialAdminNames = {
                "abdullah@homeserve.com": "Abdullah",
                "moazam@homeserve.com": "Moazam",
                "farhan@homeserve.com": "Farhan",
                "admin@homeserve.com": "Primary Administrator"
            };

            if (!adminSnap.exists() && specialAdminNames[user.email]) {
                await setDoc(adminDocRef, {
                    uid: user.uid,
                    email: user.email,
                    name: specialAdminNames[user.email],
                    createdAt: new Date()
                });
                adminSnap = await getDoc(adminDocRef);
            }

            if (adminSnap.exists()) {
                const adminData = adminSnap.data();
                adminDisplayName.innerText = adminData.name || "Admin User";
                adminDisplayEmail.innerText = adminData.email || user.email;

                // Toggle screens
                loginContainer.classList.remove("active");
                appContainer.classList.add("active");

                // Load Real-time listeners
                startRealtimeSync();
            } else {
                throw new Error("Unauthorized access. Account is not registered as an administrator.");
            }
        } catch (error) {
            console.error(error);
            showAuthError(error.message || "Failed to verify admin status.");
            await signOut(auth);
        } finally {
            showLoginLoading(false);
        }
    } else {
        // Clean up listeners
        stopRealtimeSync();

        // Show login
        appContainer.classList.remove("active");
        loginContainer.classList.add("active");
        showLoginLoading(false);
    }
});

// Sign In Trigger
loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    loginError.classList.add("hidden");
    showLoginLoading(true);

    let email = loginEmail.value.trim().toLowerCase();
    if (email === "abdullah") email = "abdullah@homeserve.com";
    if (email === "moazam") email = "moazam@homeserve.com";
    if (email === "farhan") email = "farhan@homeserve.com";

    const password = loginPassword.value;

    const isSpecialAdmin = ["abdullah@homeserve.com", "moazam@homeserve.com", "farhan@homeserve.com"].includes(email);
    const isAdminDefault = email === "admin@homeserve.com";

    // Firebase Auth enforces a hard minimum limit of 6 characters for email passwords.
    // If they enter "pucit" (5 characters), we append "_admin" internally to satisfy this constraint.
    let firebasePassword = password;
    if (isSpecialAdmin && password === "pucit") {
        firebasePassword = "pucit_admin";
    }

    try {
        await signInWithEmailAndPassword(auth, email, firebasePassword);
    } catch (error) {
        console.warn("Sign in error code:", error.code);
        // Handle bootstrapping: If email is admin@homeserve.com or one of the new admins, and auth account doesn't exist, create it
        if ((isAdminDefault || isSpecialAdmin) && (error.code === "auth/user-not-found" || error.code === "auth/invalid-credential" || error.code === "auth/wrong-password")) {
            const requiredPassword = isSpecialAdmin ? "pucit" : "password";
            if (password === requiredPassword) {
                try {
                    // Try to create the user directly using the internally transformed password
                    await createUserWithEmailAndPassword(auth, email, firebasePassword);
                } catch (regError) {
                    console.error("Bootstrapping registration error:", regError);
                    showAuthError("Failed to bootstrap admin account: " + regError.message);
                    showLoginLoading(false);
                }
            } else {
                showAuthError("Incorrect password for admin account.");
                showLoginLoading(false);
            }
        } else {
            showAuthError("Invalid admin credentials: " + error.message);
            showLoginLoading(false);
        }
    }
});

// Logout Trigger
btnLogout.addEventListener("click", async () => {
    if (confirm("Are you sure you want to log out?")) {
        await signOut(auth);
    }
});

// Loading state switches
function showLoginLoading(isLoading) {
    if (isLoading) {
        btnLogin.disabled = true;
        loginBtnText.classList.add("hidden");
        loginSpinner.classList.remove("hidden");
    } else {
        btnLogin.disabled = false;
        loginBtnText.classList.remove("hidden");
        loginSpinner.classList.add("hidden");
    }
}

function showAuthError(msg) {
    errorMessage.innerText = msg;
    loginError.classList.remove("hidden");
}

// ── NAVIGATION CONTROLS ───────────────────────────────────
document.querySelectorAll(".menu-item").forEach(item => {
    item.addEventListener("click", (e) => {
        const tabName = item.getAttribute("data-tab");
        if (tabName) {
            e.preventDefault();
            switchTab(tabName);
        }
    });
});

window.switchTab = function (tabName) {
    // Update active nav items
    document.querySelectorAll(".menu-item").forEach(el => {
        if (el.getAttribute("data-tab") === tabName) {
            el.classList.add("active");
        } else {
            el.classList.remove("active");
        }
    });

    // Update active tabs
    document.querySelectorAll(".tab-pane").forEach(pane => {
        if (pane.id === `tab-${tabName}`) {
            pane.classList.add("active");
        } else {
            pane.classList.remove("active");
        }
    });

    // Update Header Text
    const titles = {
        dashboard: ["Dashboard Overview", "Real-time platform metrics and analytics"],
        bookings: ["Booking Management", "View and monitor all platform service requests"],
        providers: ["Service Providers", "Review credentials, verify and manage professional accounts"],
        customers: ["Customers List", "Manage registered customer profiles and account status"],
        categories: ["Service Categories", "Create and configure available platform categories"],
        support: ["Complaints & Support Tickets", "Handle, respond and resolve customer issues"]
    };

    if (titles[tabName]) {
        tabTitle.innerText = titles[tabName][0];
        tabSubtitle.innerText = titles[tabName][1];
    }
}

// Provider Tab Segment Switcher
document.querySelectorAll(".segment-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        const segment = btn.getAttribute("data-segment");

        // Update active buttons in parent segment row
        btn.parentElement.querySelectorAll(".segment-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");

        // Toggle segments
        const container = btn.closest(".tab-pane");
        container.querySelectorAll(".segment-pane").forEach(pane => {
            if (pane.id === `segment-${segment}`) {
                pane.classList.add("active");
            } else {
                pane.classList.remove("active");
            }
        });
    });
});

// ── REAL-TIME SYNCHRONIZATION ──────────────────────────────
function startRealtimeSync() {
    // 1. Bookings Sync
    const bookingsQuery = query(collection(db, "bookings"), orderBy("createdAt", "desc"));
    unsubscribeBookings = onSnapshot(bookingsQuery, (snapshot) => {
        const bookingsList = [];
        snapshot.forEach(doc => {
            bookingsList.push({ id: doc.id, ...doc.data() });
        });
        updateBookingsDashboard(bookingsList);
    }, (error) => console.error("Bookings sync error:", error));

    // 2. Providers Sync
    unsubscribeProviders = onSnapshot(collection(db, "providers"), (snapshot) => {
        const providersList = [];
        snapshot.forEach(doc => {
            providersList.push({ id: doc.id, ...doc.data() });
        });
        updateProvidersDashboard(providersList);
    }, (error) => console.error("Providers sync error:", error));

    // 3. Customers Sync
    unsubscribeCustomers = onSnapshot(collection(db, "users"), (snapshot) => {
        const customersList = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.role === "user") {
                customersList.push({ id: doc.id, ...data });
            }
        });
        updateCustomersDashboard(customersList);
    }, (error) => console.error("Customers sync error:", error));

    // 4. Categories Sync
    unsubscribeCategories = onSnapshot(collection(db, "categories"), (snapshot) => {
        const categoriesList = [];
        snapshot.forEach(doc => {
            categoriesList.push({ id: doc.id, ...doc.data() });
        });
        updateCategoriesDashboard(categoriesList);
    }, (error) => console.error("Categories sync error:", error));

    // 5. Support Tickets Sync
    unsubscribeSupport = onSnapshot(collection(db, "support_tickets"), (snapshot) => {
        const ticketsList = [];
        snapshot.forEach(doc => {
            ticketsList.push({ id: doc.id, ...doc.data() });
        });
        updateSupportDashboard(ticketsList);
    }, (error) => console.error("Support sync error:", error));
}

function stopRealtimeSync() {
    if (unsubscribeBookings) unsubscribeBookings();
    if (unsubscribeProviders) unsubscribeProviders();
    if (unsubscribeCustomers) unsubscribeCustomers();
    if (unsubscribeCategories) unsubscribeCategories();
    if (unsubscribeSupport) unsubscribeSupport();

    unsubscribeBookings = null;
    unsubscribeProviders = null;
    unsubscribeCustomers = null;
    unsubscribeCategories = null;
    unsubscribeSupport = null;
}

// ── UPDATE UI: BOOKINGS ────────────────────────────────────
function updateBookingsDashboard(bookings) {
    // 1. Calculate Metrics
    const total = bookings.length;
    const completed = bookings.filter(b => b.status === "completed").length;
    const active = bookings.filter(b => b.status === "accepted" || b.status === "in_progress").length;
    const cancelled = bookings.filter(b => b.status === "cancelled").length;
    const totalRevenue = bookings.filter(b => b.status === "completed").reduce((sum, b) => sum + (b.totalAmount || 0), 0);

    // Update domestic metrics
    document.getElementById("metric-total-bookings").innerText = total;
    document.getElementById("metric-completed-bookings").innerText = completed;
    document.getElementById("metric-active-bookings").innerText = active;
    document.getElementById("metric-cancelled-bookings").innerText = cancelled;
    document.getElementById("metric-total-revenue").innerText = `Rs. ${totalRevenue.toLocaleString()}`;

    // 2. Render Recent Bookings Table
    const recentList = bookings.slice(0, 5);
    const recentTbody = document.getElementById("recent-bookings-tbody");
    recentTbody.innerHTML = "";

    if (recentList.length === 0) {
        recentTbody.innerHTML = `<tr><td colspan="7" class="text-center py-4">No recent bookings recorded.</td></tr>`;
    } else {
        recentList.forEach(b => {
            const formattedDate = formatFirebaseDate(b.createdAt);
            const statusClass = getStatusBadgeClass(b.status);
            recentTbody.innerHTML += `
                <tr>
                    <td><strong>#${b.id.substring(0, 6)}</strong></td>
                    <td>${b.customerName || "Customer"}</td>
                    <td>${b.serviceName || "General Service"}</td>
                    <td>${b.providerName || '<span class="text-muted">Not assigned</span>'}</td>
                    <td>${formattedDate}</td>
                    <td><span class="status-badge ${statusClass}">${b.status}</span></td>
                    <td><strong>Rs. ${b.totalAmount || 0}</strong></td>
                </tr>
            `;
        });
    }

    // 3. Render All Bookings Table
    renderAllBookingsTable(bookings);

    // 4. Update Charts
    updateAnalyticsCharts(bookings);
}

function renderAllBookingsTable(bookings) {
    const tbody = document.getElementById("all-bookings-tbody");
    const searchVal = document.getElementById("booking-search").value.toLowerCase();
    const statusFilter = document.getElementById("booking-status-filter").value;

    const filtered = bookings.filter(b => {
        const matchesSearch = (b.customerName || "").toLowerCase().includes(searchVal) ||
            (b.serviceName || "").toLowerCase().includes(searchVal) ||
            (b.providerName || "").toLowerCase().includes(searchVal) ||
            b.id.toLowerCase().includes(searchVal);
        const matchesStatus = (statusFilter === "all") || (b.status === statusFilter);
        return matchesSearch && matchesStatus;
    });

    tbody.innerHTML = "";
    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4">No bookings match the filters.</td></tr>`;
        return;
    }

    filtered.forEach(b => {
        const formattedDate = formatFirebaseDate(b.createdAt);
        const statusClass = getStatusBadgeClass(b.status);
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>#${b.id.substring(0, 6)}</strong></td>
            <td>${b.customerName || "Customer"}</td>
            <td>${b.serviceName || "General Service"}</td>
            <td>${b.providerName || '<span class="text-muted">Not assigned</span>'}</td>
            <td><span class="status-badge ${statusClass}">${b.status}</span></td>
            <td>${formattedDate}</td>
            <td><strong>Rs. ${b.totalAmount || 0}</strong></td>
            <td><button class="btn-action btn-view-booking"><i class="fa-solid fa-eye"></i> View</button></td>
        `;

        tr.querySelector(".btn-view-booking").addEventListener("click", () => showBookingDetails(b));
        tbody.appendChild(tr);
    });
}

// Booking Search Filters Binding
document.getElementById("booking-search").addEventListener("input", () => {
    // Run on local state query
    const searchVal = document.getElementById("booking-search").value.toLowerCase();
    // Re-render
});

// ── UPDATE UI: PROVIDERS ───────────────────────────────────
function updateProvidersDashboard(providers) {
    // 1. Pending Approvals Grid
    const pendingList = providers.filter(p => p.approved === false && p.rejected === false);
    const pendingGrid = document.getElementById("pending-providers-grid");
    pendingGrid.innerHTML = "";

    if (pendingList.length === 0) {
        pendingGrid.innerHTML = `<div class="text-center py-4 w-100 glass"><p class="text-muted">No pending provider approvals at the moment.</p></div>`;
    } else {
        pendingList.forEach(p => {
            const avatar = p.profilePhotoUrl ? `<img src="${p.profilePhotoUrl}" referrerpolicy="no-referrer" alt="Avatar">` : `<i class="fa-solid fa-user-tie"></i>`;
            const docBtn = p.documentUrl ? `<button class="cnic-link-btn" data-url="${p.documentUrl}"><i class="fa-solid fa-address-card"></i> View ID Card / CNIC</button>` : `<span class="text-muted">No ID uploaded</span>`;

            const expertiseTags = (p.categoryId || "General Skill")
                .split(",")
                .map(cat => cat.trim())
                .filter(cat => cat.length > 0)
                .map(cat => `<span class="expertise-tag">${cat.charAt(0).toUpperCase() + cat.slice(1)}</span>`)
                .join("");

            const card = document.createElement("div");
            card.className = "vetting-card glass";
            card.innerHTML = `
                <div class="vetting-card-header">
                    <div class="vetting-avatar">${avatar}</div>
                    <div class="vetting-meta">
                        <h4>${p.name || "Provider"}</h4>
                        <div class="vetting-meta-expertise">${expertiseTags}</div>
                    </div>
                </div>
                <div class="vetting-details">
                    <p><i class="fa-solid fa-phone"></i> ${p.phone || "No phone"}</p>
                    <p><i class="fa-solid fa-envelope"></i> ${p.email || "No email"}</p>
                    <p><i class="fa-solid fa-location-dot"></i> ${p.address || "Lahore, PK"}</p>
                    <p><i class="fa-solid fa-id-card"></i> CNIC: ${p.idNumber || "Not entered"}</p>
                </div>
                ${docBtn}
                <div class="vetting-actions">
                    <button class="btn-action btn-approve w-100"><i class="fa-solid fa-check"></i> Approve</button>
                    <button class="btn-action btn-reject w-100"><i class="fa-solid fa-xmark"></i> Reject</button>
                </div>
            `;

            // Bind Approval Click
            card.querySelector(".btn-approve").addEventListener("click", () => approveProviderAccount(p.id));
            card.querySelector(".btn-reject").addEventListener("click", () => rejectProviderAccount(p.id));
            if (p.documentUrl) {
                card.querySelector(".cnic-link-btn").addEventListener("click", () => showCnicModal(p.documentUrl));
            }

            pendingGrid.appendChild(card);
        });
    }

    // 2. All Service Providers Table
    renderAllProvidersTable(providers);
}

function renderAllProvidersTable(providers) {
    const tbody = document.getElementById("all-providers-tbody");
    const searchVal = document.getElementById("provider-search").value.toLowerCase();

    const filtered = providers.filter(p => {
        return (p.name || "").toLowerCase().includes(searchVal) ||
            (p.phone || "").toLowerCase().includes(searchVal) ||
            (p.email || "").toLowerCase().includes(searchVal) ||
            (p.categoryId || "").toLowerCase().includes(searchVal);
    });

    tbody.innerHTML = "";
    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4">No service providers match criteria.</td></tr>`;
        return;
    }

    filtered.forEach(p => {
        const avatar = p.profilePhotoUrl ? `<img src="${p.profilePhotoUrl}" referrerpolicy="no-referrer" alt="Avatar">` : `<i class="fa-solid fa-user-tie"></i>`;

        // Status determination
        let statusText = "Pending";
        let statusClass = "status-pending";
        if (p.approved) { statusText = "Approved"; statusClass = "status-approved"; }
        else if (p.rejected) { statusText = "Rejected/Blocked"; statusClass = "status-rejected"; }

        // Block status switch
        const isBlocked = p.rejected;
        const blockChecked = isBlocked ? "checked" : "";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><div class="avatar-cell">${avatar}</div></td>
            <td><strong>${p.name || "Provider"}</strong></td>
            <td>${p.categoryId || "General Skill"}</td>
            <td>
                <div>${p.email || "No email"}</div>
                <div class="text-muted" style="font-size:12px">${p.phone || "No phone"}</div>
            </td>
            <td>
                <span class="rating-text"><i class="fa-solid fa-star" style="color:var(--warning)"></i> ${p.rating || 4.8}</span>
                <span class="text-muted" style="font-size:11px">(${p.reviewCount || 0} reviews)</span>
            </td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td>
                <div class="vetting-actions" style="margin-top:0">
                    <button class="btn-action btn-approve btn-xs" ${p.approved ? 'disabled' : ''}><i class="fa-solid fa-circle-check"></i> Approve</button>
                </div>
            </td>
            <td>
                <label class="switch">
                    <input type="checkbox" class="provider-block-toggle" ${blockChecked}>
                    <span class="slider"></span>
                </label>
            </td>
        `;

        tr.querySelector(".btn-approve").addEventListener("click", () => approveProviderAccount(p.id));
        tr.querySelector(".provider-block-toggle").addEventListener("change", (e) => toggleProviderBlock(p.id, e.target.checked));

        tbody.appendChild(tr);
    });
}

// ── UPDATE UI: CUSTOMERS ───────────────────────────────────
function updateCustomersDashboard(customers) {
    const tbody = document.getElementById("all-customers-tbody");
    const searchVal = document.getElementById("customer-search").value.toLowerCase();

    const filtered = customers.filter(c => {
        return (c.name || "").toLowerCase().includes(searchVal) ||
            (c.phone || "").toLowerCase().includes(searchVal) ||
            (c.email || "").toLowerCase().includes(searchVal);
    });

    tbody.innerHTML = "";
    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4">No customer profiles match criteria.</td></tr>`;
        return;
    }

    filtered.forEach(c => {
        const formattedDate = formatFirebaseDate(c.createdAt);
        const statusText = c.blocked ? "Blocked" : "Active";
        const statusClass = c.blocked ? "status-rejected" : "status-approved";
        const btnText = c.blocked ? "Unblock" : "Block";
        const btnClass = c.blocked ? "btn-approve" : "btn-reject";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${c.name || "Customer"}</strong></td>
            <td>${c.email || "No email"}</td>
            <td>${c.phone || "No phone"}</td>
            <td>${formattedDate}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td><button class="btn-action ${btnClass} btn-block-toggle">${btnText}</button></td>
        `;

        tr.querySelector(".btn-block-toggle").addEventListener("click", () => toggleCustomerBlock(c.id, !c.blocked));
        tbody.appendChild(tr);
    });
}

// ── UPDATE UI: CATEGORIES ──────────────────────────────────
function updateCategoriesDashboard(categories) {
    const container = document.getElementById("categories-container");
    container.innerHTML = "";

    if (categories.length === 0) {
        container.innerHTML = `<div class="text-center py-4"><p class="text-muted">No service categories registered.</p></div>`;
        return;
    }

    categories.forEach(cat => {
        const statusText = cat.isActive ? "Active" : "Disabled";
        const btnIcon = cat.isActive ? "fa-toggle-on" : "fa-toggle-off";
        const activeClass = cat.isActive ? "color: var(--success)" : "color: var(--text-muted)";

        const card = document.createElement("div");
        card.className = "cat-item-card glass";
        card.innerHTML = `
            <span class="cat-item-emoji">${cat.icon || "🛠️"}</span>
            <h5>${cat.name}</h5>
            <span class="cat-status-badge">${statusText}</span>
            <button class="btn-action btn-toggle-cat" style="margin-top: 8px; border:none; background:transparent; font-size:20px; ${activeClass}">
                <i class="fa-solid ${btnIcon}"></i>
            </button>
        `;

        card.querySelector(".btn-toggle-cat").addEventListener("click", () => toggleCategoryActive(cat.id, !cat.isActive));
        container.appendChild(card);
    });
}

// Category Creation Trigger
document.getElementById("category-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const nameInput = document.getElementById("cat-name");
    const iconInput = document.getElementById("cat-icon");

    const name = nameInput.value.trim();
    const icon = iconInput.value.trim();
    const catId = name.toLowerCase().replace(/[^a-z0-9]/g, "_");

    try {
        await setDoc(doc(db, "categories", catId), {
            categoryId: catId,
            name: name,
            icon: icon,
            isActive: true
        });
        nameInput.value = "";
        iconInput.value = "";
        alert("Category created successfully!");
    } catch (err) {
        console.error("Error creating category:", err);
        alert("Failed to create category: " + err.message);
    }
});

// ── UPDATE UI: SUPPORT TICKETS ──────────────────────────────
function updateSupportDashboard(tickets) {
    // Update menu badge count
    const openCount = tickets.filter(t => t.status === "open").length;
    const badge = document.getElementById("badge-support-count");
    if (openCount > 0) {
        badge.innerText = openCount;
        badge.classList.remove("hidden");
    } else {
        badge.classList.add("hidden");
    }

    const tbody = document.getElementById("support-tickets-tbody");
    tbody.innerHTML = "";

    if (tickets.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4">No support tickets or complaints submitted.</td></tr>`;
        return;
    }

    tickets.forEach(t => {
        const formattedDate = formatFirebaseDate(t.createdAt);
        const statusClass = t.status === "open" ? "status-pending" : "status-completed";
        const btnHtml = t.status === "open" ? `<button class="btn-action btn-approve btn-resolve"><i class="fa-solid fa-check"></i> Resolve</button>` : `<span class="text-muted">Closed</span>`;

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>#${t.id.substring(0, 6)}</strong></td>
            <td>${t.customerName || "User"}</td>
            <td>
                <div>${t.customerPhone || "No Phone"}</div>
            </td>
            <td><strong>${t.subject || "No Subject"}</strong></td>
            <td style="max-width:240px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="${t.message}">${t.message || "No message body"}</td>
            <td>${formattedDate}</td>
            <td><span class="status-badge ${statusClass}">${t.status || "open"}</span></td>
            <td>${btnHtml}</td>
        `;

        if (t.status === "open") {
            tr.querySelector(".btn-resolve").addEventListener("click", () => resolveSupportTicket(t.id));
        }

        tbody.appendChild(tr);
    });
}

// ── DB ACTIONS ─────────────────────────────────────────────
async function approveProviderAccount(providerId) {
    if (confirm("Are you sure you want to approve this provider?")) {
        try {
            await updateDoc(doc(db, "providers", providerId), {
                approved: true,
                rejected: false
            });
        } catch (err) {
            alert("Error approving provider: " + err.message);
        }
    }
}

async function rejectProviderAccount(providerId) {
    if (confirm("Are you sure you want to reject this provider verification request?")) {
        try {
            await updateDoc(doc(db, "providers", providerId), {
                approved: false,
                rejected: true
            });
        } catch (err) {
            alert("Error rejecting provider: " + err.message);
        }
    }
}

async function toggleProviderBlock(providerId, shouldBlock) {
    try {
        await updateDoc(doc(db, "providers", providerId), {
            rejected: shouldBlock,
            approved: !shouldBlock
        });
    } catch (err) {
        alert("Error blocking/unblocking provider: " + err.message);
    }
}

async function toggleCustomerBlock(customerId, shouldBlock) {
    const action = shouldBlock ? "block" : "unblock";
    if (confirm(`Are you sure you want to ${action} this customer?`)) {
        try {
            await updateDoc(doc(db, "users", customerId), {
                blocked: shouldBlock
            });
        } catch (err) {
            alert("Error blocking/unblocking customer: " + err.message);
        }
    }
}

async function toggleCategoryActive(categoryId, shouldActive) {
    try {
        await updateDoc(doc(db, "categories", categoryId), {
            isActive: shouldActive
        });
    } catch (err) {
        alert("Error updating category status: " + err.message);
    }
}

async function resolveSupportTicket(ticketId) {
    if (confirm("Mark this support ticket as resolved?")) {
        try {
            await updateDoc(doc(db, "support_tickets", ticketId), {
                status: "resolved"
            });
        } catch (err) {
            alert("Error resolving ticket: " + err.message);
        }
    }
}

// ── MODAL OVERLAYS VIEW BINDINGS ───────────────────────────
window.showCnicModal = function (url) {
    const modal = document.getElementById("cnic-modal");
    const img = document.getElementById("cnic-modal-image");
    const loader = document.getElementById("cnic-modal-loader");

    img.classList.add("hidden");
    loader.classList.remove("hidden");
    modal.classList.remove("hidden");

    img.src = url;
    img.onload = () => {
        loader.classList.add("hidden");
        img.classList.remove("hidden");
    };
}

window.closeCnicModal = function () {
    document.getElementById("cnic-modal").classList.add("hidden");
}

function showBookingDetails(b) {
    const modal = document.getElementById("booking-modal");
    const body = document.getElementById("booking-modal-body");
    const date = formatFirebaseDate(b.createdAt);

    body.innerHTML = `
        <div class="booking-modal-grid">
            <div class="modal-detail-item">
                <label>Booking ID</label>
                <p>#${b.bookingId}</p>
            </div>
            <div class="modal-detail-item">
                <label>Status</label>
                <p><span class="status-badge ${getStatusBadgeClass(b.status)}">${b.status}</span></p>
            </div>
            
            <div class="booking-modal-divider"></div>

            <div class="modal-detail-item">
                <label>Customer Name</label>
                <p>${b.customerName || "No Name"}</p>
            </div>
            <div class="modal-detail-item">
                <label>Customer Phone</label>
                <p>${b.customerPhone || "No Phone"}</p>
            </div>

            <div class="booking-modal-full modal-detail-item">
                <label>Service Address</label>
                <p>${b.address || "No Address Provided"}</p>
            </div>

            <div class="booking-modal-divider"></div>

            <div class="modal-detail-item">
                <label>Service Category</label>
                <p>${b.categoryId || "General Skill"}</p>
            </div>
            <div class="modal-detail-item">
                <label>Service Subtype</label>
                <p>${b.serviceName || "General Work"}</p>
            </div>
            
            <div class="booking-modal-full modal-detail-item">
                <label>Job Description</label>
                <p style="background:rgba(255,255,255,0.03); padding:10px; border-radius:8px; font-weight:normal">${b.jobDescription || "No custom job description provided."}</p>
            </div>

            <div class="booking-modal-divider"></div>

            <div class="modal-detail-item">
                <label>Assigned Provider</label>
                <p>${b.providerName || '<span class="text-muted">Unassigned</span>'}</p>
            </div>
            <div class="modal-detail-item">
                <label>Provider Phone</label>
                <p>${b.providerPhone || '<span class="text-muted">Unassigned</span>'}</p>
            </div>

            <div class="booking-modal-divider"></div>

            <div class="modal-detail-item">
                <label>Payment Status</label>
                <p><span class="status-badge ${b.paymentStatus === 'paid' ? 'status-approved' : 'status-pending'}">${b.paymentStatus || 'unpaid'}</span></p>
            </div>
            <div class="modal-detail-item">
                <label>Total Price (Bid Quote)</label>
                <p style="color: var(--success); font-size: 16px; font-weight:800">Rs. ${b.totalAmount || 0}</p>
            </div>
        </div>
    `;

    modal.classList.remove("hidden");
}

window.closeBookingModal = function () {
    document.getElementById("booking-modal").classList.add("hidden");
}

// ── CHARTS RENDERING LOGIC ─────────────────────────────────
function updateAnalyticsCharts(bookings) {
    // Calculate booking distribution by status
    const statusCounts = { pending: 0, accepted: 0, in_progress: 0, completed: 0, cancelled: 0 };
    bookings.forEach(b => {
        if (statusCounts[b.status] !== undefined) {
            statusCounts[b.status]++;
        }
    });

    // Calculate revenue mapping over time (grouped by last 7 bookings for demo/trend)
    const revenueTrend = bookings.filter(b => b.status === "completed").slice(0, 10).reverse();
    const revenueLabels = revenueTrend.map((b, i) => `#${b.id.substring(0, 6)}`);
    const revenueData = revenueTrend.map(b => b.totalAmount || 0);

    // Render bookings distribution chart
    const bookingsCtx = document.getElementById("bookingsChart").getContext("2d");
    if (bookingsChart) {
        bookingsChart.destroy();
    }
    bookingsChart = new Chart(bookingsCtx, {
        type: 'doughnut',
        data: {
            labels: ['Pending', 'Accepted', 'In Progress', 'Completed', 'Cancelled'],
            datasets: [{
                data: [statusCounts.pending, statusCounts.accepted, statusCounts.in_progress, statusCounts.completed, statusCounts.cancelled],
                backgroundColor: ['#FBBF24', '#60A5FA', '#C084FC', '#34D399', '#FCA5A5'],
                borderWidth: 1,
                borderColor: 'rgba(255, 255, 255, 0.05)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: { color: '#9CA3AF', font: { family: 'Plus Jakarta Sans' } }
                }
            }
        }
    });

    // Render revenue trend chart
    const revenueCtx = document.getElementById("revenueChart").getContext("2d");
    if (revenueChart) {
        revenueChart.destroy();
    }

    // Fallback if no revenue yet
    const labels = revenueLabels.length > 0 ? revenueLabels : ['No Sales'];
    const dataPoints = revenueData.length > 0 ? revenueData : [0];

    revenueChart = new Chart(revenueCtx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Completed Transaction value (PKR)',
                data: dataPoints,
                backgroundColor: 'rgba(59, 130, 246, 0.45)',
                borderColor: '#3B82F6',
                borderWidth: 2,
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#9CA3AF', font: { family: 'Plus Jakarta Sans' } }
                },
                y: {
                    grid: { color: 'rgba(255,255,255,0.04)' },
                    ticks: { color: '#9CA3AF', font: { family: 'Plus Jakarta Sans' } }
                }
            }
        }
    });
}

// ── HELPER UTILITIES ───────────────────────────────────────
function formatFirebaseDate(timestamp) {
    if (!timestamp) return "N/A";
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp.seconds * 1000);
    return date.toLocaleDateString("en-US", {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getStatusBadgeClass(status) {
    const mapping = {
        pending: "status-pending",
        accepted: "status-accepted",
        in_progress: "status-in_progress",
        completed: "status-completed",
        cancelled: "status-cancelled"
    };
    return mapping[status] || "status-pending";
}

// Seeding Mock Data Event Listener
// document.getElementById("btn-seed").addEventListener("click", async () => {
//     if (confirm("Are you sure you want to seed the database with mock records? This will populate customers, service providers, bookings, categories, and support tickets so you can see live data instantly!")) {
//         try {
//             // Seed Categories
//             const categoriesToSeed = [
//                 { id: "plumber", name: "Plumber", icon: "🔧", isActive: true },
//                 { id: "electrician", name: "Electrician", icon: "⚡", isActive: true },
//                 { id: "cleaning", name: "Cleaning", icon: "✨", isActive: true },
//                 { id: "painting", name: "Painting", icon: "🎨", isActive: true }
//             ];
//             for (const cat of categoriesToSeed) {
//                 await setDoc(doc(db, "categories", cat.id), cat);
//             }

//             // Seed Users (Customers)
//             const usersToSeed = [
//                 { uid: "user_923001112222", name: "Muhammad Ali", phone: "+923001112222", email: "ali@gmail.com", role: "user", address: "DHA Phase 5, Lahore", blocked: false, createdAt: new Date() },
//                 { uid: "user_923214445555", name: "Ayesha Khan", phone: "+923214445555", email: "ayesha@gmail.com", role: "user", address: "Model Town, Lahore", blocked: false, createdAt: new Date() },
//                 { uid: "user_923126667777", name: "Zainab Bibi", phone: "+923126667777", email: "", role: "user", address: "Gulberg III, Lahore", blocked: true, createdAt: new Date() }
//             ];
//             for (const user of usersToSeed) {
//                 await setDoc(doc(db, "users", user.uid), user);
//             }

//             // Seed Providers
//             const providersToSeed = [
//                 { id: "provider_923009998888", name: "Kamran Akmal", phone: "+923009998888", email: "kamran@gmail.com", categoryId: "plumber", rating: 4.9, reviewCount: 15, approved: true, rejected: false, available: true, address: "Johar Town, Lahore" },
//                 { id: "provider_923218887777", name: "Tariq Mahmood", phone: "+923218887777", email: "tariq@gmail.com", categoryId: "electrician", rating: 4.5, reviewCount: 6, approved: false, rejected: false, available: true, address: "Wapda Town, Lahore" },
//                 { id: "provider_923124443333", name: "Bilal Asif", phone: "+923124443333", email: "bilal@gmail.com", categoryId: "cleaning", rating: 4.7, reviewCount: 8, approved: false, rejected: true, available: false, address: "Faisal Town, Lahore" }
//             ];
//             for (const prov of providersToSeed) {
//                 await setDoc(doc(db, "providers", prov.id), {
//                     uid: prov.id,
//                     name: prov.name,
//                     phone: prov.phone,
//                     email: prov.email,
//                     categoryId: prov.categoryId,
//                     rating: prov.rating,
//                     reviewCount: prov.reviewCount,
//                     approved: prov.approved,
//                     rejected: prov.rejected,
//                     available: prov.available,
//                     address: prov.address,
//                     role: "provider"
//                 });
//             }

//             // Seed Bookings
//             const bookingsToSeed = [
//                 { bookingId: "book_001", userId: "user_923001112222", customerName: "Muhammad Ali", customerPhone: "+923001112222", serviceName: "General Plumbing Leak Fix", categoryId: "plumber", totalAmount: 1800, status: "completed", providerId: "provider_923009998888", providerName: "Kamran Akmal", providerPhone: "+923009998888", paymentStatus: "paid", address: "DHA Phase 5, Lahore", createdAt: new Date() },
//                 { bookingId: "book_002", userId: "user_923214445555", customerName: "Ayesha Khan", customerPhone: "+923214445555", serviceName: "Electric Short Circuit Repair", categoryId: "electrician", totalAmount: 2500, status: "in_progress", providerId: "provider_923218887777", providerName: "Tariq Mahmood", providerPhone: "+923218887777", paymentStatus: "unpaid", address: "Model Town, Lahore", createdAt: new Date() },
//                 { bookingId: "book_003", userId: "user_923126667777", customerName: "Zainab Bibi", customerPhone: "+923126667777", serviceName: "House Deep Cleaning", categoryId: "cleaning", totalAmount: 4000, status: "completed", providerId: "provider_923009998888", providerName: "Kamran Akmal", providerPhone: "+923009998888", paymentStatus: "paid", address: "Gulberg III, Lahore", createdAt: new Date() },
//                 { bookingId: "book_004", userId: "user_923001112222", customerName: "Muhammad Ali", customerPhone: "+923001112222", serviceName: "Bathroom Plumbing Renovation", categoryId: "plumber", totalAmount: 3200, status: "pending", paymentStatus: "unpaid", address: "DHA Phase 5, Lahore", createdAt: new Date() }
//             ];
//             for (const b of bookingsToSeed) {
//                 await setDoc(doc(db, "bookings", b.bookingId), b);
//             }

//             // Seed Support Tickets
//             const ticketsToSeed = [
//                 { id: "ticket_001", customerName: "Muhammad Ali", customerPhone: "+923001112222", subject: "Provider was late", message: "The plumber arrived 45 mins late than scheduled time. Please take note.", status: "open", createdAt: new Date() },
//                 { id: "ticket_002", customerName: "Ayesha Khan", customerPhone: "+923214445555", subject: "Payment double charge", message: "I was charged online twice. Please check transaction ID HS83719.", status: "open", createdAt: new Date() }
//             ];
//             for (const t of ticketsToSeed) {
//                 await setDoc(doc(db, "support_tickets", t.id), t);
//             }

//             alert("Database seeded successfully! Your dashboard tables and charts will now load live real-time data.");
//         } catch (err) {
//             console.error("Seeding error:", err);
//             alert("Failed to seed database: " + err.message);
//         }
//     }
// });
