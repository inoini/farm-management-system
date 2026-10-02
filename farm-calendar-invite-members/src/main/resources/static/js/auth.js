"use strict";
function farmCsrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !header || !token.content || !header.content) {
        throw new Error("ページを再読み込みしてから操作してください。");
    }
    return { [header.content]: token.content };
}

function restoreUsernameIfEmailAutofill(input) {
    const value = (input.value || "").trim();

    if (!value.includes("@")) {
        if (value) input.dataset.lastUsername = value;
        return false;
    }

    // Chrome / Edge password managers can replace the username field with a
    // saved Gmail address after the user has already typed a valid username.
    // Restore the last manually entered non-email username instead of merely
    // clearing the field.
    const previousUsername = (input.dataset.lastUsername || "").trim();
    input.value = previousUsername;
    input.setAttribute("data-email-autofill-cleared", "true");
    return true;
}

function protectUsernameOnlyField(input) {
    const check = () => restoreUsernameIfEmailAutofill(input);

    input.addEventListener("input", check);
    input.addEventListener("change", check);
    input.addEventListener("focus", () => setTimeout(check, 0));
    input.addEventListener("blur", () => setTimeout(check, 0));

    // Password managers sometimes apply autofill without firing input/change.
    // Re-check for several seconds after the login page is opened.
    [0, 100, 250, 500, 1000, 1500, 2500, 4000, 6000, 8000, 12000].forEach(delay => {
        setTimeout(check, delay);
    });

    const passwordField = input.form?.querySelector('input[type="password"]');
    if (passwordField) {
        passwordField.addEventListener("focus", () => setTimeout(check, 0));
        passwordField.addEventListener("input", () => setTimeout(check, 0));
        passwordField.addEventListener("change", () => setTimeout(check, 0));
    }

    window.addEventListener("pageshow", () => setTimeout(check, 0));
}

// A restored browser-history page must re-check its session after logout.
window.addEventListener("pageshow", event => {
    if (event.persisted) window.location.reload();
});

async function refreshFormCsrf(form) {
    const response = await fetch('/auth/csrf', {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) throw new Error('ログイン画面を更新してください。');
    const data = await response.json();
    if (!data || !data.parameterName || !data.token) {
        throw new Error('セキュリティ情報を取得できませんでした。');
    }
    let input = form.querySelector('input[name="' + CSS.escape(data.parameterName) + '"]');
    if (!input) {
        input = document.createElement('input');
        input.type = 'hidden';
        input.name = data.parameterName;
        form.appendChild(input);
    }
    input.value = data.token;
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('input[data-username-only]').forEach(protectUsernameOnlyField);

    document.querySelectorAll('form[data-refresh-csrf]').forEach(form => {
        let submitting = false;
        form.addEventListener('submit', async event => {
            if (submitting) return;

            const usernameField = form.querySelector('input[data-username-only]');
            if (usernameField) {
                restoreUsernameIfEmailAutofill(usernameField);
                const username = (usernameField.value || '').trim();
                if (!username || username.includes('@')) {
                    event.preventDefault();
                    usernameField.focus();
                    alert('ユーザー名欄にはGmailアドレスではなく、登録したユーザー名を入力してください。');
                    return;
                }
            }

            event.preventDefault();
            const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');
            if (submitButton) submitButton.disabled = true;
            try {
                await refreshFormCsrf(form);
                submitting = true;
                form.submit();
            } catch (error) {
                console.error(error);
                alert('ログイン画面の有効期限を更新できませんでした。ページを再読み込みして、もう一度お試しください。');
                if (submitButton) submitButton.disabled = false;
            }
        });
    });
});
