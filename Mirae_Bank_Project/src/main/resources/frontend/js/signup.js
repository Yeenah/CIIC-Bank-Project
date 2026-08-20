const signupForm =
    document.getElementById("signupForm");

const signupError =
    document.getElementById("signupError");

const signupSuccess =
    document.getElementById("signupSuccess");

const signupButton =
    document.getElementById("signupButton");

signupForm.addEventListener(
    "submit",
    async function (event) {

        event.preventDefault();

        // -------------------------------------------------
        // Clear previous messages
        // -------------------------------------------------

        signupError.style.display = "none";
        signupSuccess.style.display = "none";

        // -------------------------------------------------
        // Get values
        // -------------------------------------------------

        const fullName =
            document.getElementById("fullName")
                .value
                .trim();

        const username =
            document.getElementById("username")
                .value
                .trim();

        const email =
            document.getElementById("email")
                .value
                .trim();

        const password =
            document.getElementById("password")
                .value;

        const confirmPassword =
            document.getElementById("confirmPassword")
                .value;

        // -------------------------------------------------
        // Validate required fields
        // -------------------------------------------------

        if (
            !fullName ||
            !username ||
            !email ||
            !password ||
            !confirmPassword
        ) {

            showError(
                "Please complete all required fields."
            );

            return;
        }

        // -------------------------------------------------
        // Validate password length
        // -------------------------------------------------

        if (password.length < 6) {

            showError(
                "Password must be at least 6 characters."
            );

            return;
        }

        // -------------------------------------------------
        // Confirm password
        // -------------------------------------------------

        if (password !== confirmPassword) {

            showError(
                "Passwords do not match."
            );

            return;
        }

        // -------------------------------------------------
        // Disable button
        // -------------------------------------------------

        signupButton.disabled = true;

        signupButton.textContent =
            "Creating Profile...";

        try {

            // =================================================
            // SEND TO JAVA BACKEND
            // =================================================

            const response =
                await fetch(
                    "/api/auth/signup",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body: JSON.stringify({

                            fullName:
                            fullName,

                            username:
                            username,

                            email:
                            email,

                            password:
                            password
                        })
                    }
                );

            // =================================================
            // READ RESPONSE
            // =================================================

            let data;

            try {

                data =
                    await response.json();

            } catch (jsonError) {

                console.error(
                    "Invalid JSON response:",
                    jsonError
                );

                showError(
                    "The server returned an invalid response."
                );

                return;
            }

            // =================================================
            // ERROR RESPONSE
            // =================================================

            if (!response.ok) {

                showError(
                    data.message ||
                    "Unable to create your profile."
                );

                return;
            }

            // =================================================
            // SUCCESS
            // =================================================

            const accountNumber =
                data.accountNumber;

            if (accountNumber) {

                showSuccess(
                    "Profile created successfully! " +
                    "Your account number is " +
                    accountNumber +
                    ". Redirecting to sign in..."
                );

            } else {

                showSuccess(
                    "Profile created successfully! " +
                    "Redirecting to sign in..."
                );
            }

            // -------------------------------------------------
            // Redirect to login
            // -------------------------------------------------

            setTimeout(
                function () {

                    window.location.href =
                        "/html/signin.html";

                },
                2000
            );

        } catch (error) {

            console.error(
                "Signup error:",
                error
            );

            showError(
                "Unable to connect to the server."
            );

        } finally {

            signupButton.disabled = false;

            signupButton.textContent =
                "Create Profile";
        }
    }
);

function showError(message) {

    signupError.textContent =
        message;

    signupError.style.display =
        "block";

    signupSuccess.style.display =
        "none";
}

function showSuccess(message) {

    signupSuccess.textContent =
        message;

    signupSuccess.style.display =
        "block";

    signupError.style.display =
        "none";
}
