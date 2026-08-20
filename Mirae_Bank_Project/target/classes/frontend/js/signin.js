"use strict";

// ------------------------------------------------------------
// API CONFIGURATION
// ------------------------------------------------------------

const API_BASE_URL = window.location.origin;

// Storage key MUST match dashboard.js
const TOKEN_KEY = "authToken";
const USER_KEY = "currentUser";

// ------------------------------------------------------------
// DOM ELEMENTS
// ------------------------------------------------------------

const loginForm = document.getElementById("loginForm");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");

const usernameError = document.getElementById("usernameError");
const passwordError = document.getElementById("passwordError");

const loginMessage = document.getElementById("loginMessage");

const loginButton = document.getElementById("loginButton");
const buttonText = document.getElementById("buttonText");
const buttonLoader = document.getElementById("buttonLoader");

const togglePassword =
    document.getElementById("togglePassword");

const forgotPassword =
    document.getElementById("forgotPassword");

// ------------------------------------------------------------
// PAGE LOAD
// ------------------------------------------------------------

document.addEventListener("DOMContentLoaded", function () {

    console.log("Mirae Bank login page loaded.");

    // Check whether a previous session already exists.
    const existingToken =
        sessionStorage.getItem(TOKEN_KEY);

    console.log(
        "Existing samplebankToken:",
        existingToken
    );

    // Do NOT automatically redirect just because a token exists.
    // This prevents confusing login/dashboard loops.
});

// ------------------------------------------------------------
// PASSWORD SHOW / HIDE
// ------------------------------------------------------------

if (togglePassword) {

    togglePassword.addEventListener(
        "click",
        function () {

            if (passwordInput.type === "password") {

                passwordInput.type = "text";

                togglePassword.textContent =
                    "Hide";

                togglePassword.setAttribute(
                    "aria-label",
                    "Hide password"
                );

            } else {

                passwordInput.type = "password";

                togglePassword.textContent =
                    "Show";

                togglePassword.setAttribute(
                    "aria-label",
                    "Show password"
                );
            }
        }
    );
}

// ------------------------------------------------------------
// FORGOT PASSWORD
// ------------------------------------------------------------

if (forgotPassword) {

    forgotPassword.addEventListener(
        "click",
        function (event) {

            event.preventDefault();

            showMessage(
                "Password recovery is not available.",
                "info"
            );
        }
    );
}

// ------------------------------------------------------------
// INPUT VALIDATION
// ------------------------------------------------------------

function clearErrors() {

    if (usernameError) {
        usernameError.textContent = "";
    }

    if (passwordError) {
        passwordError.textContent = "";
    }

    if (usernameInput) {
        usernameInput.classList.remove("input-error");
    }

    if (passwordInput) {
        passwordInput.classList.remove("input-error");
    }
}

function validateForm() {

    clearErrors();

    let valid = true;

    const username =
        usernameInput.value.trim();

    const password =
        passwordInput.value;

    if (!username) {

        usernameError.textContent =
            "Username is required.";

        usernameInput.classList.add(
            "input-error"
        );

        valid = false;
    }

    if (!password) {

        passwordError.textContent =
            "Password is required.";

        passwordInput.classList.add(
            "input-error"
        );

        valid = false;
    }

    return valid;
}

// ------------------------------------------------------------
// MESSAGE
// ------------------------------------------------------------

function showMessage(message, type) {

    if (!loginMessage) {
        return;
    }

    loginMessage.textContent = message;

    loginMessage.className =
        "message " + type;

    loginMessage.style.display = "block";
}

function clearMessage() {

    if (!loginMessage) {
        return;
    }

    loginMessage.textContent = "";

    loginMessage.className =
        "message";
}

// ------------------------------------------------------------
// LOADING STATE
// ------------------------------------------------------------

function setLoading(loading) {

    if (!loginButton) {
        return;
    }

    loginButton.disabled = loading;

    if (loading) {

        if (buttonText) {
            buttonText.textContent =
                "Signing In...";
        }

        if (buttonLoader) {
            buttonLoader.style.display =
                "inline-block";
        }

    } else {

        if (buttonText) {
            buttonText.textContent =
                "Sign In";
        }

        if (buttonLoader) {
            buttonLoader.style.display =
                "none";
        }
    }
}

// ------------------------------------------------------------
// LOGIN
// ------------------------------------------------------------

if (loginForm) {

    loginForm.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            clearMessage();

            if (!validateForm()) {
                return;
            }

            const username =
                usernameInput.value.trim();

            const password =
                passwordInput.value;

            setLoading(true);

            try {

                console.log(
                    "Sending login request..."
                );

                const response =
                    await fetch(
                        API_BASE_URL +
                        "/api/auth/login",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body: JSON.stringify({
                                username: username,
                                password: password
                            })
                        }
                    );

                console.log(
                    "Login HTTP status:",
                    response.status
                );

                let data;

                try {

                    data =
                        await response.json();

                } catch (jsonError) {

                    throw new Error(
                        "The server returned an invalid response."
                    );
                }

                console.log(
                    "Login response:",
                    data
                );

                // ------------------------------------------------
                // LOGIN FAILED
                // ------------------------------------------------

                if (!response.ok) {

                    throw new Error(
                        data.message ||
                        "Invalid username or password."
                    );
                }

                // ------------------------------------------------
                // CHECK TOKEN
                // ------------------------------------------------

                if (!data.token) {

                    throw new Error(
                        "Login succeeded, but the server did not return a session token."
                    );
                }

                // ------------------------------------------------
                // SAVE SESSION
                // ------------------------------------------------

                sessionStorage.setItem(
                    TOKEN_KEY,
                    data.token
                );

                // Save useful account information.
                const userData = {
                    username:
                        data.username || username,

                    accountNumber:
                        data.accountNumber || "",

                    accountName:
                        data.accountName || "",

                    balance:
                        data.balance || "0"
                };

                sessionStorage.setItem(
                    USER_KEY,
                    JSON.stringify(userData)
                );

                // ------------------------------------------------
                // VERIFY STORAGE
                // ------------------------------------------------

                console.log(
                    "Stored token:",
                    sessionStorage.getItem(
                        TOKEN_KEY
                    )
                );

                console.log(
                    "Stored user:",
                    sessionStorage.getItem(
                        USER_KEY
                    )
                );

                // ------------------------------------------------
                // SUCCESS
                // ------------------------------------------------

                showMessage(
                    "Login successful. Redirecting...",
                    "success"
                );

                // Give the browser a moment to display message.
                setTimeout(
                    function () {

                        window.location.href =
                            "/html/dashboard.html";

                    },
                    300
                );

            } catch (error) {

                console.error(
                    "Login error:",
                    error
                );

                showMessage(
                    error.message ||
                    "Unable to connect to the banking server.",
                    "error"
                );

                // Make sure an invalid login does not leave
                // an old token in storage.
                sessionStorage.removeItem(
                    TOKEN_KEY
                );

                sessionStorage.removeItem(
                    USER_KEY
                );

            } finally {

                setLoading(false);
            }
        }
    );
}

// ------------------------------------------------------------
// ENTER KEY / INPUT CLEANUP
// ------------------------------------------------------------

if (usernameInput) {

    usernameInput.addEventListener(
        "input",
        function () {

            usernameError.textContent = "";

            usernameInput.classList.remove(
                "input-error"
            );

            clearMessage();
        }
    );
}

if (passwordInput) {

    passwordInput.addEventListener(
        "input",
        function () {

            passwordError.textContent = "";

            passwordInput.classList.remove(
                "input-error"
            );

            clearMessage();
        }
    );
}
