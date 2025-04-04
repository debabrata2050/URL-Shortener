document.addEventListener('DOMContentLoaded', () => {
    // Get references to DOM elements
    const form = document.getElementById('shorten-form');
    const longUrlInput = document.getElementById('longUrl');
    const customAliasInput = document.getElementById('customAlias');
    const resultArea = document.getElementById('result-area');
    const errorArea = document.getElementById('error-area');
    const shortUrlLink = document.getElementById('shortUrl');
    const errorMessageParagraph = document.getElementById('error-message');
    const submitButton = document.getElementById('submit-button');
    const copyButton = document.getElementById('copy-button');
    const originalUrlDisplay = document.getElementById('original-url-display');

    const API_ENDPOINT = '/api/shorten'; // Backend endpoint for creating links
    const REDIRECT_PREFIX = '/r/';       // Backend prefix for redirection

    // Handle form submission
    form.addEventListener('submit', async (event) => {
        event.preventDefault(); // Prevent default browser submission

        // --- Reset UI ---
        hideMessages();
        submitButton.disabled = true;
        submitButton.textContent = 'Shortening...';

        const longUrl = longUrlInput.value.trim();
        const customAlias = customAliasInput.value.trim();

        // --- Basic Client-Side Validation ---
        if (!longUrl) {
            displayError("Long URL cannot be empty.");
            resetSubmitButton();
            return;
        }
        // Validate custom alias format (letters, numbers, hyphens only)
        if (customAlias && !/^[a-zA-Z0-9-]*$/.test(customAlias)) {
            displayError("Custom alias contains invalid characters. Use only letters, numbers, and hyphens.");
            resetSubmitButton();
            return;
        }

        // --- Prepare Request Body ---
        const requestBody = {
            url: longUrl
        };
        if (customAlias) {
            requestBody.alias = customAlias;
        }

        // --- Make API Call ---
        try {
            const response = await fetch(API_ENDPOINT, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json', // Indicate we expect JSON back
                },
                body: JSON.stringify(requestBody),
            });

            const data = await response.json(); // Try to parse JSON body

            if (response.ok) { // Status 200-299
                displaySuccess(data);
            } else {
                // Handle backend errors (expecting JSON error format)
                let message = `Error ${response.status}: ${data.message || response.statusText || 'An unknown error occurred.'}`;
                if (data.fieldErrors) {
                    const fieldErrorMessages = Object.entries(data.fieldErrors)
                        .map(([field, msg]) => `${field}: ${msg}`)
                        .join('; ');
                    message += ` Details: ${fieldErrorMessages}`;
                }
                displayError(message);
            }

        } catch (error) {
            console.error("Fetch error:", error);
            displayError('Network error: Failed to connect to the server. Please try again.');
        } finally {
            // --- Restore Button ---
            resetSubmitButton();
        }
    });

    // --- Copy to Clipboard ---
    copyButton.addEventListener('click', () => {
        const urlToCopy = shortUrlLink.href;
        if (!urlToCopy || urlToCopy === window.location.href + '#') { // Check if it's the placeholder href
             console.warn("No valid URL to copy.");
             return;
        }

        navigator.clipboard.writeText(urlToCopy).then(() => {
            // Visual feedback on successful copy
            const originalIconHTML = copyButton.innerHTML;
            copyButton.innerHTML = 'Copied!';
            copyButton.classList.add('copy-button-copied'); // Style feedback

            setTimeout(() => {
                copyButton.innerHTML = originalIconHTML; // Restore original icon/text
                copyButton.classList.remove('copy-button-copied');
            }, 1500); // Duration of feedback

        }).catch(err => {
            console.error('Failed to copy URL: ', err);
            alert("Could not copy the link to clipboard. Please try manually."); // Fallback alert
        });
    });


   function displaySuccess(data) {
           // 1. Construct the FULL, clickable URL including the redirect path '/r/'
           //    Example: http://localhost:8080 + /r/ + yourAlias => http://localhost:8080/r/yourAlias
           const fullShortUrl = `${window.location.origin}${REDIRECT_PREFIX}${data.alias}`;

           // 2. Set the 'href' attribute of the link element to the full URL.
           //    This ensures the link works correctly when clicked.
           shortUrlLink.href = fullShortUrl;

           // 3. Create the text to DISPLAY to the user.
           //    Use the same full URL and remove the 'http://' or 'https://' prefix using regex.
           //    Example: http://localhost:8080/r/yourAlias => localhost:8080/r/yourAlias
           const displayUrlText = fullShortUrl.replace(/^https?:\/\//, '');

           // 4. Set the visible text content of the link element to the modified text.
           shortUrlLink.textContent = displayUrlText;

           // 5. Show the original long URL below the short link for reference.
           originalUrlDisplay.textContent = data.url;

           // 6. Show the result area and hide any previous errors.
           resultArea.style.display = 'block';
           errorArea.style.display = 'none';
       }

       function displayError(message) {
           errorMessageParagraph.textContent = message;
           errorArea.style.display = 'block';   // Show error area
           resultArea.style.display = 'none';  // Hide result area
       }

       function hideMessages() {
            errorArea.style.display = 'none';
            resultArea.style.display = 'none';
            errorMessageParagraph.textContent = '';
            shortUrlLink.textContent = ''; // Clear previous text
            shortUrlLink.href = '#'; // Reset link href to placeholder
            originalUrlDisplay.textContent = '';
       }

       function resetSubmitButton() {
           submitButton.disabled = false;
           submitButton.textContent = 'Shorten';
       }

}); // End DOMContentLoaded