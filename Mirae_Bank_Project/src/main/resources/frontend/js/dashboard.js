"use strict";

const API_BASE = window.location.origin;

const TOKEN_KEY = "authToken";
const USER_KEY = "currentUser";

let currentAccount = null;
let currentTransactions = [];

function getToken() {

    return sessionStorage.getItem(TOKEN_KEY);

}

function requireLogin() {

    const token = getToken();

    console.log("Dashboard token:", token);

    if (!token || token.trim() === "") {

        console.warn(
            "No authentication token found."
        );

        window.location.replace("/html/signin.html");

        return false;
    }

    return true;
}

async function apiRequest(endpoint, options = {}) {

    const token = getToken();

    if (!token) {

        window.location.replace("/html/signin.html");

        throw new Error(
            "Authentication token missing."
        );
    }

    const headers = {

        "Content-Type": "application/json",

        "Authorization":
            "Bearer " + token
    };

    const response = await fetch(
        API_BASE + endpoint,
        {
            ...options,

            headers: {
                ...headers,
                ...(options.headers || {})
            }
        }
    );

    // --------------------------------------------------------
    // Unauthorized
    // --------------------------------------------------------

    if (response.status === 401) {

        console.warn(
            "Session expired or token is invalid."
        );

        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);

        window.location.replace("/html/signin.html");

        throw new Error(
            "Your session has expired."
        );
    }

    // --------------------------------------------------------
    // Read response
    // --------------------------------------------------------

    let data;

    const contentType =
        response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {

        try {

            data = await response.json();

        } catch (error) {

            throw new Error(
                "Invalid JSON response from server."
            );
        }

    } else {

        data = await response.text();

    }

    // --------------------------------------------------------
    // API ERROR
    // --------------------------------------------------------

    if (!response.ok) {

        let message = "Request failed.";

        if (typeof data === "object" && data !== null) {

            message =
                data.message ||
                data.error ||
                message;

        } else if (data) {

            message = data;

        }

        throw new Error(message);
    }

    return data;
}

function setupNavigation() {

    const buttons =
        document.querySelectorAll(
            "[data-section]"
        );

    buttons.forEach(button => {

        button.addEventListener(
            "click",
            function() {

                const section =
                    button.dataset.section;

                if (!section) {
                    return;
                }

                showSection(section);

            }
        );

    });

    // --------------------------------------------------------
    // Mobile menu
    // --------------------------------------------------------

    const menuToggle =
        document.getElementById(
            "menuToggle"
        );

    const sidebar =
        document.getElementById(
            "sidebar"
        );

    if (menuToggle && sidebar) {

        menuToggle.addEventListener(
            "click",
            function() {

                sidebar.classList.toggle(
                    "open"
                );

            }
        );

    }

}

function showSection(sectionName) {

    const sections =
        document.querySelectorAll(
            ".page-section"
        );

    sections.forEach(section => {

        section.classList.remove(
            "active"
        );

    });

    const selectedSection =
        document.getElementById(
            sectionName
        );

    if (selectedSection) {

        selectedSection.classList.add(
            "active"
        );

    }

    // --------------------------------------------------------
    // Navigation active state
    // --------------------------------------------------------

    const navItems =
        document.querySelectorAll(
            ".nav-item"
        );

    navItems.forEach(item => {

        item.classList.remove(
            "active"
        );

        if (
            item.dataset.section ===
            sectionName
        ) {

            item.classList.add(
                "active"
            );

        }

    });

    // --------------------------------------------------------
    // Close mobile sidebar
    // --------------------------------------------------------

    const sidebar =
        document.getElementById(
            "sidebar"
        );

    if (sidebar) {

        sidebar.classList.remove(
            "open"
        );

    }

    // --------------------------------------------------------
    // Refresh when opening relevant pages
    // --------------------------------------------------------

    if (
        sectionName === "accounts" ||
        sectionName === "history" ||
        sectionName === "statement"
    ) {

        refreshDashboard();

    }

    if (sectionName === "listAccounts") {

        loadAccounts();

    }

}

async function loadAccount() {

    try {

        const account =
            await apiRequest(
                "/api/account",
                {
                    method: "GET"
                }
            );

        console.log(
            "Account:",
            account
        );

        currentAccount = account;

        updateAccountUI(
            account
        );

        return account;

    } catch (error) {

        console.error(
            "Unable to load account:",
            error
        );

        showError(
            error.message
        );

        throw error;
    }
}

function updateAccountUI(account) {

    if (!account) {
        return;
    }

    const accountName =
        account.accountName ||
        account.name ||
        "Customer";

    const accountNumber =
        account.accountNumber ||
        account.accountNo ||
        "-";

    const balance =
        account.balance ?? 0;

    // --------------------------------------------------------
    // Profile
    // --------------------------------------------------------

    setText(
        "profileName",
        accountName
    );

    setText(
        "welcomeName",
        getFirstName(accountName)
    );

    setText(
        "profileAvatar",
        getInitials(accountName)
    );

    // --------------------------------------------------------
    // Main balance
    // --------------------------------------------------------

    setBalance(
        "totalBalance",
        balance
    );

    setBalance(
        "savingsBalance",
        balance
    );

    // --------------------------------------------------------
    // Account number
    // --------------------------------------------------------

    setText(
        "checkingBalance",
        accountNumber
    );

    setText(
        "primaryAccountNumber",
        "Account: " + accountNumber
    );

    // --------------------------------------------------------
    // Dashboard summary
    // --------------------------------------------------------

    setText(
        "summaryAccountName",
        accountName
    );

    setText(
        "summaryAccountNumber",
        accountNumber
    );

    setBalance(
        "summaryBalance",
        balance
    );

    // --------------------------------------------------------
    // Accounts page
    // --------------------------------------------------------

    setText(
        "accountPageNumber",
        accountNumber
    );

    setText(
        "accountPageName",
        accountName
    );

    setBalance(
        "accountPageBalance",
        balance
    );

    setText(
        "accountInfoNumber",
        accountNumber
    );

    setText(
        "accountInfoName",
        accountName
    );

    setBalance(
        "accountInfoBalance",
        balance
    );

    // --------------------------------------------------------
    // Banking forms
    // --------------------------------------------------------

    setText(
        "depositAccount",
        accountNumber
    );

    setText(
        "withdrawAccount",
        accountNumber
    );

    setText(
        "transferFromAccount",
        accountNumber
    );

    // --------------------------------------------------------
    // Statement
    // --------------------------------------------------------

    setText(
        "statementName",
        accountName
    );

    setText(
        "statementAccount",
        accountNumber
    );

    setBalance(
        "statementBalance",
        balance
    );

    // --------------------------------------------------------
    // Settings
    // --------------------------------------------------------

    setText(
        "settingsAccountName",
        accountName
    );

    setText(
        "settingsAccountNumber",
        accountNumber
    );

    setBalance(
        "settingsBalance",
        balance
    );

}

async function loadAccounts() {

    try {

        const accounts =
            await apiRequest(
                "/api/accounts",
                {
                    method: "GET"
                }
            );

        renderAccounts(accounts);

        return accounts;

    } catch (error) {

        console.error(
            "Unable to load accounts:",
            error
        );

        showError(
            error.message
        );

        throw error;

    }

}

function renderAccounts(accounts) {

    const table =
        document.getElementById(
            "accountListTable"
        );

    const total =
        document.getElementById(
            "totalAccounts"
        );

    if (!table) {
        return;
    }

    table.innerHTML = "";

    if (!Array.isArray(accounts) || accounts.length === 0) {

        table.innerHTML = `

                <tr>
                    <td colspan="3" style="text-align:center;">
                        No accounts found.
                    </td>
                </tr>

            `;

        if (total) {
            total.textContent = "0";
        }

        return;
    }

    accounts.forEach(account => {

        const row =
            document.createElement(
                "tr"
            );

        row.innerHTML = `

                <td>
                    ${escapeHtml(account.accountNumber || "-")}
                </td>

                <td>
                    ${escapeHtml(account.accountName || "-")}
                </td>

                <td>
                    ${escapeHtml(formatMoney(account.balance))}
                </td>

            `;

        table.appendChild(row);

    });

    if (total) {
        total.textContent = accounts.length;
    }

}

async function loadTransactions() {

    try {

        const transactions =
            await apiRequest(
                "/api/transactions",
                {
                    method: "GET"
                }
            );

        console.log(
            "Transactions:",
            transactions
        );

        currentTransactions =
            Array.isArray(transactions)
                ? transactions
                : [];

        renderRecentTransactions(
            currentTransactions
        );

        renderHistory(
            currentTransactions
        );

        renderStatement(
            currentTransactions
        );

        return currentTransactions;

    } catch (error) {

        console.error(
            "Unable to load transactions:",
            error
        );

        showError(
            error.message
        );

        throw error;
    }
}

function renderRecentTransactions(
    transactions
) {

    const container =
        document.getElementById(
            "recentTransactions"
        );

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (
        !transactions ||
        transactions.length === 0
    ) {


    container.innerHTML = `
        <div class="empty-state">
            <p>No transactions found.</p>
            <small>Your recent banking activity will appear here.</small>
        </div>
    `;

    return;
    }

    const recent =
        transactions.slice(0, 5);

    recent.forEach(transaction => {

        const item =
            document.createElement(
                "div"
            );

        item.className =
            "transaction-item";

        const type =
            transaction.type ||
            "TRANSACTION";

        const amount =
            formatMoney(
                transaction.amount
            );

        const date =
            formatDate(
                transaction.createdAt
            );

        const remarks =
            transaction.remarks ||
            "-";

        item.innerHTML = `

                <div class="transaction-icon">
                    ${getTransactionIcon(type)}
                </div>

                <div class="transaction-info">

                    <strong>
                        ${escapeHtml(type)}
                    </strong>

                    <small>
                        ${escapeHtml(remarks)}
                    </small>

                    <small>
                        ${escapeHtml(date)}
                    </small>

                </div>

                <strong class="transaction-amount">
                    ${escapeHtml(amount)}
                </strong>

            `;

        container.appendChild(
            item
        );

    });

}

function renderHistory(
    transactions
) {

    const tableBody =
        document.getElementById(
            "historyTable"
        );

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = "";

    if (
        !transactions ||
        transactions.length === 0
    ) {

        tableBody.innerHTML = `

                <tr>

                    <td
                        colspan="6"
                        style="text-align:center;"
                    >
                        No transactions found.
                    </td>

                </tr>

            `;

        return;
    }

    transactions.forEach(
        transaction => {

            const row =
                document.createElement(
                    "tr"
                );

            row.innerHTML = `

                    <td>
                        ${escapeHtml(
                transaction.reference || "-"
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                formatDate(
                    transaction.createdAt
                )
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                transaction.type || "-"
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                formatMoney(
                    transaction.amount
                )
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                formatMoney(
                    transaction.balanceAfter
                )
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                transaction.remarks || "-"
            )}
                    </td>

                `;

            tableBody.appendChild(
                row
            );

        }
    );

}

function renderStatement(
    transactions
) {

    const tableBody =
        document.getElementById(
            "statementTable"
        );

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = "";

    const latest =
        transactions.slice(0, 10);

    if (latest.length === 0) {

        tableBody.innerHTML = `

                <tr>

                    <td
                        colspan="5"
                        style="text-align:center;"
                    >
                        No transactions found.
                    </td>

                </tr>

            `;

        return;
    }

    latest.forEach(
        transaction => {

            const row =
                document.createElement(
                    "tr"
                );

            row.innerHTML = `

                    <td>
                        ${escapeHtml(
                formatDate(
                    transaction.createdAt
                )
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                transaction.reference || "-"
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                transaction.remarks || "-"
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                transaction.type || "-"
            )}
                    </td>

                    <td>
                        ${escapeHtml(
                formatMoney(
                    transaction.amount
                )
            )}
                    </td>

                `;

            tableBody.appendChild(
                row
            );

        }
    );

}

async function deposit(amount) {

    const numericAmount =
        Number(amount);

    if (
        !amount ||
        Number.isNaN(numericAmount) ||
        numericAmount <= 0
    ) {

        showToast(
            "Please enter a valid deposit amount.",
            "error"
        );

        return false;
    }

    try {

        showToast(
            "Processing deposit...",
            "info"
        );

        const result =
            await apiRequest(
                "/api/transactions/deposit",
                {
                    method: "POST",

                    body:
                        JSON.stringify({
                            amount:
                                String(amount)
                        })
                }
            );

        console.log(
            "Deposit result:",
            result
        );

        showToast(
            result.message ||
            "Deposit successful.",
            "success"
        );

        await refreshDashboard();

        return true;

    } catch (error) {

        console.error(
            "Deposit error:",
            error
        );

        showToast(
            error.message ||
            "Deposit failed.",
            "error"
        );

        return false;
    }

}

async function withdraw(amount) {

    const numericAmount =
        Number(amount);

    if (
        !amount ||
        Number.isNaN(numericAmount) ||
        numericAmount <= 0
    ) {

        showToast(
            "Please enter a valid withdrawal amount.",
            "error"
        );

        return false;
    }

    try {

        showToast(
            "Processing withdrawal...",
            "info"
        );

        const result =
            await apiRequest(
                "/api/transactions/withdraw",
                {
                    method: "POST",

                    body:
                        JSON.stringify({
                            amount:
                                String(amount)
                        })
                }
            );

        console.log(
            "Withdrawal result:",
            result
        );

        showToast(
            result.message ||
            "Withdrawal successful.",
            "success"
        );

        await refreshDashboard();

        return true;

    } catch (error) {

        console.error(
            "Withdrawal error:",
            error
        );

        showToast(
            error.message ||
            "Withdrawal failed.",
            "error"
        );

        return false;
    }

}

async function transfer(
    receiverAccountNumber,
    amount
) {

    const receiver =
        receiverAccountNumber
            ? receiverAccountNumber.trim()
            : "";

    const numericAmount =
        Number(amount);

    if (!receiver) {

        showToast(
            "Please enter the destination account number.",
            "error"
        );

        return false;
    }

    if (
        !amount ||
        Number.isNaN(numericAmount) ||
        numericAmount <= 0
    ) {

        showToast(
            "Please enter a valid transfer amount.",
            "error"
        );

        return false;
    }

    try {

        showToast(
            "Processing transfer...",
            "info"
        );

        const result =
            await apiRequest(
                "/api/transactions/transfer",
                {
                    method: "POST",

                    body:
                        JSON.stringify({

                            receiverAccountNumber:
                            receiver,

                            amount:
                                String(amount)

                        })
                }
            );

        console.log(
            "Transfer result:",
            result
        );

        showToast(
            result.message ||
            "Transfer successful.",
            "success"
        );

        await refreshDashboard();

        return true;

    } catch (error) {

        console.error(
            "Transfer error:",
            error
        );

        showToast(
            error.message ||
            "Transfer failed.",
            "error"
        );

        return false;
    }

}

function setupForms() {

    // --------------------------------------------------------
    // Deposit
    // --------------------------------------------------------

    const depositForm =
        document.getElementById(
            "depositForm"
        );

    if (depositForm) {

        depositForm.addEventListener(
            "submit",
            async function(event) {

                event.preventDefault();

                const input =
                    document.getElementById(
                        "depositAmount"
                    );

                if (!input) {
                    return;
                }

                const success =
                    await deposit(
                        input.value
                    );

                if (success) {

                    input.value = "";

                }

            }
        );

    }

    // --------------------------------------------------------
    // Withdraw
    // --------------------------------------------------------

    const withdrawForm =
        document.getElementById(
            "withdrawForm"
        );

    if (withdrawForm) {

        withdrawForm.addEventListener(
            "submit",
            async function(event) {

                event.preventDefault();

                const input =
                    document.getElementById(
                        "withdrawAmount"
                    );

                if (!input) {
                    return;
                }

                const success =
                    await withdraw(
                        input.value
                    );

                if (success) {

                    input.value = "";

                }

            }
        );

    }

    // --------------------------------------------------------
    // Transfer
    // --------------------------------------------------------

    const transferForm =
        document.getElementById(
            "transferForm"
        );

    if (transferForm) {

        transferForm.addEventListener(
            "submit",
            async function(event) {

                event.preventDefault();

                const receiver =
                    document.getElementById(
                        "receiverAccountNumber"
                    );

                const amount =
                    document.getElementById(
                        "transferAmount"
                    );

                if (
                    !receiver ||
                    !amount
                ) {

                    return;

                }

                const success =
                    await transfer(
                        receiver.value,
                        amount.value
                    );

                if (success) {

                    receiver.value = "";
                    amount.value = "";

                }

            }
        );

    }

    // --------------------------------------------------------
    // Logout
    // --------------------------------------------------------

    const logoutButton =
        document.getElementById(
            "logoutButton"
        );

    if (logoutButton) {

        logoutButton.addEventListener(
            "click",
            async function(event) {

                event.preventDefault();

                await logout();

            }
        );

    }

}

function setupTransactionSearch() {

    const searchInput =
        document.getElementById(
            "transactionSearch"
        );

    const filter =
        document.getElementById(
            "transactionFilter"
        );

    if (!searchInput && !filter) {
        return;
    }

    function applyFilter() {

        const search =
            searchInput
                ? searchInput.value
                    .trim()
                    .toLowerCase()
                : "";

        const selectedFilter =
            filter
                ? filter.value
                : "all";

        const filtered =
            currentTransactions.filter(
                transaction => {

                    const type =
                        String(
                            transaction.type || ""
                        ).toLowerCase();

                    const text =
                        [

                            transaction.reference,
                            transaction.type,
                            transaction.remarks,
                            transaction.createdAt

                        ]
                            .join(" ")
                            .toLowerCase();

                    const matchesSearch =
                        !search ||
                        text.includes(search);

                    let matchesType = true;

                    if (
                        selectedFilter !==
                        "all"
                    ) {

                        if (
                            selectedFilter ===
                            "deposit"
                        ) {

                            matchesType =
                                type.includes(
                                    "deposit"
                                );

                        }

                        if (
                            selectedFilter ===
                            "withdraw"
                        ) {

                            matchesType =
                                type.includes(
                                    "withdraw"
                                );

                        }

                        if (
                            selectedFilter ===
                            "transfer"
                        ) {

                            matchesType =
                                type.includes(
                                    "transfer"
                                );

                        }

                    }

                    return (
                        matchesSearch &&
                        matchesType
                    );

                }
            );

        renderHistory(
            filtered
        );

    }

    if (searchInput) {

        searchInput.addEventListener(
            "input",
            applyFilter
        );

    }

    if (filter) {

        filter.addEventListener(
            "change",
            applyFilter
        );

    }

}

async function logout() {

    const token =
        getToken();

    try {

        if (token) {

            await fetch(
                API_BASE +
                "/api/auth/logout",
                {
                    method: "POST",

                    headers: {
                        "Authorization":
                            "Bearer " + token
                    }
                }
            );

        }

    } catch (error) {

        console.warn(
            "Logout request failed:",
            error
        );

    } finally {

        sessionStorage.removeItem(
            TOKEN_KEY
        );

        sessionStorage.removeItem(
            USER_KEY
        );

        sessionStorage.removeItem(
            "samplebankUsername"
        );

        sessionStorage.removeItem(
            "samplebankAccountNumber"
        );

        sessionStorage.removeItem(
            "samplebankAccountName"
        );

        sessionStorage.removeItem(
            "samplebankBalance"
        );

        window.location.replace(
            "/html/signin.html"
        );

    }

}

async function refreshDashboard() {

    if (!requireLogin()) {
        return;
    }

    try {

        await Promise.all([
            loadAccount(),
            loadTransactions()
        ]);

    } catch (error) {

        console.error(
            "Dashboard refresh failed:",
            error
        );

    }

}

function updateCurrentDate() {

    const element =
        document.getElementById(
            "currentDate"
        );

    if (!element) {
        return;
    }

    const now =
        new Date();

    element.textContent =
        now.toLocaleDateString(
            "en-PH",
            {
                year: "numeric",
                month: "short",
                day: "numeric"
            }
        );

}

function formatMoney(value) {

    const number =
        Number(value);

    if (Number.isNaN(number)) {

        return "₱0.00";

    }

    return new Intl.NumberFormat(
        "en-PH",
        {
            style: "currency",
            currency: "PHP",
            minimumFractionDigits: 2
        }
    ).format(number);

}

function formatDate(value) {

    if (!value) {
        return "-";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {

        return String(value);

    }

    return date.toLocaleString(
        "en-PH",
        {
            year: "numeric",
            month: "short",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }
    );

}

function setText(
    id,
    value
) {

    const element =
        document.getElementById(
            id
        );

    if (!element) {
        return;
    }

    if (
        element.tagName ===
        "INPUT"
    ) {

        element.value =
            value ?? "";

    } else {

        element.textContent =
            value ?? "-";

    }

}

function setBalance(
    id,
    value
) {

    const element =
        document.getElementById(
            id
        );

    if (!element) {
        return;
    }

    const formatted =
        formatMoney(value);

    if (
        element.tagName ===
        "INPUT"
    ) {

        element.value =
            formatted;

    } else {

        element.textContent =
            formatted;

    }

}

function getFirstName(name) {

    if (!name) {
        return "Customer";
    }

    return String(name)
        .trim()
        .split(/\s+/)[0];

}

function getInitials(name) {

    if (!name) {
        return "CU";
    }

    const parts =
        String(name)
            .trim()
            .split(/\s+/);

    if (parts.length === 1) {

        return parts[0]
            .substring(0, 2)
            .toUpperCase();

    }

    return (
        parts[0][0] +
        parts[parts.length - 1][0]
    ).toUpperCase();

}

function getTransactionIcon(type) {

    const value =
        String(type || "")
            .toUpperCase();

    if (value.includes("DEPOSIT")) {
        return "↑";
    }

    if (value.includes("WITHDRAW")) {
        return "↓";
    }

    if (value.includes("TRANSFER")) {
        return "⇄";
    }

    return "↔";

}

function escapeHtml(value) {

    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

}

function showError(message) {

    const element =
        document.getElementById(
            "errorMessage"
        );

    if (!element) {
        return;
    }

    element.textContent =
        message ||
        "An error occurred.";

    element.style.display =
        "block";

}

function showToast(
    message,
    type = "info"
) {

    const toast =
        document.getElementById(
            "toast"
        );

    if (!toast) {

        alert(message);

        return;

    }

    toast.textContent =
        message;

    toast.className =
        "toast " + type;

    toast.classList.add(
        "show"
    );

    setTimeout(
        function() {

            toast.classList.remove(
                "show"
            );

        },
        3000
    );

}

document.addEventListener(
    "DOMContentLoaded",
    async function() {

        console.log(
            "Dashboard loaded."
        );

        console.log(
            "Current URL:",
            window.location.href
        );

        console.log(
            "Token:",
            getToken()
        );

        // ----------------------------------------------------
        // Require authentication
        // ----------------------------------------------------

        if (!requireLogin()) {
            return;
        }

        // ----------------------------------------------------
        // Setup UI
        // ----------------------------------------------------

        setupNavigation();

        setupForms();

        setupTransactionSearch();

        updateCurrentDate();
        updateCurrentDateTime();

        // ----------------------------------------------------
        // Load REAL backend data
        // ----------------------------------------------------

        await refreshDashboard();

        console.log(
            "Dashboard initialization complete."
        );

    }
);

function updateCurrentDateTime() {

    const now = new Date();

    const dateElement = document.getElementById("currentDate");
    const timeElement = document.getElementById("currentTime");

    if (dateElement) {
        dateElement.textContent = now.toLocaleDateString("en-PH", {
            year: "numeric",
            month: "short",
            day: "numeric"
        });
    }

    if (timeElement) {
        timeElement.textContent = now.toLocaleTimeString("en-PH", {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        });
    }
}

setInterval(updateCurrentDateTime, 1000);
