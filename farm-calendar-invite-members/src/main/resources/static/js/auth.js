"use strict";
function farmCsrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !header || !token.content || !header.content) {
        throw new Error("ページを再読み込みしてから操作してください。");
    }
    return { [header.content]: token.content };
}

function protectUsernameOnlyField(input) {
    const clearEmailAutofill = () => {
        const value = (input.value || "").trim();
        if (value.includes("@")) {
            input.value = "";
            input.setAttribute("data-email-autofill-cleared", "true");
            return true;
        }
        return false;
    };

    input.addEventListener("input", clearEmailAutofill);
    input.addEventListener("change", clearEmailAutofill);
    input.addEventListener("focus", () => setTimeout(clearEmailAutofill, 0));

    [0, 100, 250, 500, 1000, 1500, 2500, 4000].forEach(delay => {
        setTimeout(clearEmailAutofill, delay);
    });

    window.addEventListener("pageshow", () => setTimeout(clearEmailAutofill, 0));
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
            if (usernameField && (usernameField.value || '').includes('@')) {
                event.preventDefault();
                usernameField.value = '';
                usernameField.focus();
                alert('ユーザー名欄にはGmailアドレスではなく、登録したユーザー名を入力してください。');
                return;
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
