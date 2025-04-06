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
       // --- Backend now provides the full short URL ---
       // data should look like: { alias: "...", originalUrl: "...", shortUrl: "https://d50.in/r/..." }

       const fullShortUrl = data.shortUrl; // Get the full URL from the response
       const originalUrl = data.originalUrl;

       // 1. Set the clickable link href
       shortUrlLink.href = fullShortUrl;

       // 2. Set the displayed text (remove protocol for display)
       shortUrlLink.textContent = fullShortUrl.replace(/^https?:\/\//, '');

       // 3. Show the original long URL
       originalUrlDisplay.textContent = originalUrl;

       // 4. Show result area, hide error area
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