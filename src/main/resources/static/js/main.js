

document.addEventListener('DOMContentLoaded', function () {
    initTabScopedAuth();
    initSidebarToggle();
    initNavigationLoader();
    initToasts();
    initButtonRipple();
    initGlobalSubmitGuard();
    initLiveNotifications();
});

const BK_TAB_TOKEN_KEY = 'bakirKhata.tabAuthToken';
const BK_TAB_INSTANCE_KEY = 'bakirKhata.tabInstance';


function initTabScopedAuth() {
    const params = new URLSearchParams(window.location.search);
    if (
        params.get('logout') === 'true' ||
        params.get('expired') === 'true'
    ) {
        sessionStorage.removeItem(BK_TAB_TOKEN_KEY);
    }
    let instance = window.name && window.name.startsWith('BK_TAB_') ? window.name : null;
    if (!instance) {
        instance = 'BK_TAB_' + randomId();
        window.name = instance;
    }
    const previousInstance = sessionStorage.getItem(BK_TAB_INSTANCE_KEY);
    if (previousInstance && previousInstance !== instance) {
        sessionStorage.removeItem(BK_TAB_TOKEN_KEY);
    }
    sessionStorage.setItem(BK_TAB_INSTANCE_KEY, instance);

    const tokenFromUrl = params.get('__tab');
    if (tokenFromUrl) sessionStorage.setItem(BK_TAB_TOKEN_KEY, tokenFromUrl);
    const token = sessionStorage.getItem(BK_TAB_TOKEN_KEY);
    if (!token) return;

    document.querySelectorAll('a[href]').forEach(function (link) {
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('http://') || href.startsWith('https://') || href.startsWith('mailto:') || href.startsWith('javascript:')) return;
        link.setAttribute('href', appendTabToken(href, token));
    });

    document.querySelectorAll('form').forEach(function (form) {
        const action = form.getAttribute('action') || '';
        if (action === '/login' || action.endsWith('/login')) return;
        let hidden = form.querySelector('input[name="__tab"]');
        if (!hidden) {
            hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = '__tab';
            form.appendChild(hidden);
        }
        hidden.value = token;
    });
}

function currentTabAuthToken() {
    return sessionStorage.getItem(BK_TAB_TOKEN_KEY);
}

function appendTabToken(url, token) {
    if (!token || url.includes('__tab=')) return url;
    const hashIndex = url.indexOf('#');
    const hash = hashIndex >= 0 ? url.substring(hashIndex) : '';
    const base = hashIndex >= 0 ? url.substring(0, hashIndex) : url;
    return base + (base.includes('?') ? '&' : '?') + '__tab=' + encodeURIComponent(token) + hash;
}

function randomId() {
    if (window.crypto && crypto.getRandomValues) {
        const bytes = new Uint8Array(16);
        crypto.getRandomValues(bytes);
        return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
    }
    return Date.now().toString(36) + Math.random().toString(36).slice(2);
}

/** Indeterminate page-transition indicator for slower server-rendered navigation. */
function initNavigationLoader() {
    let loader = document.querySelector('.page-loading-indicator');
    if (!loader) {
        loader = document.createElement('div');
        loader.className = 'page-loading-indicator';
        loader.setAttribute('aria-hidden', 'true');
        document.body.appendChild(loader);
    }

    function show() { loader.classList.add('page-loading-indicator--visible'); }
    function hide() { loader.classList.remove('page-loading-indicator--visible'); }

    document.addEventListener('click', function (event) {
        if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const link = event.target.closest && event.target.closest('a[href]');
        if (!link || link.target === '_blank' || link.hasAttribute('download')) return;
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:') || href.startsWith('mailto:')) return;
        try {
            const url = new URL(link.href, window.location.href);
            if (url.origin === window.location.origin) show();
        } catch (_) { }
    });
    window.addEventListener('pageshow', hide);
    window.BakirKhataPageLoader = { show: show, hide: hide };
}

/** Mobile/tablet: hamburger opens the sidebar as an overlay drawer. */
function initSidebarToggle() {
    const hamburger = document.getElementById('hamburgerBtn');
    const sidebar = document.getElementById('sidebar');
    if (!hamburger || !sidebar) return;

    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    function openSidebar() {
        sidebar.classList.add('sidebar--open');
        overlay.classList.add('sidebar-overlay--open');
    }
    function closeSidebar() {
        sidebar.classList.remove('sidebar--open');
        overlay.classList.remove('sidebar-overlay--open');
    }

    hamburger.addEventListener('click', function () {
        sidebar.classList.contains('sidebar--open') ? closeSidebar() : openSidebar();
    });
    overlay.addEventListener('click', closeSidebar);
}

/** Auto-hides flash-message toasts after a few seconds. */
function initToasts() {
    document.querySelectorAll('.toast[data-autohide="true"]').forEach(function (toast) {
        setTimeout(function () {
            toast.classList.add('toast--hide');
            setTimeout(function () { toast.remove(); }, 250);
        }, 4000);
    });
}

/** Small material-style ripple feedback on primary/secondary/danger buttons. */
function initButtonRipple() {
    document.querySelectorAll('.btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            const rect = btn.getBoundingClientRect();
            const ripple = document.createElement('span');
            ripple.className = 'ripple';
            const size = Math.max(rect.width, rect.height);
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = (e.clientX - rect.left - size / 2) + 'px';
            ripple.style.top = (e.clientY - rect.top - size / 2) + 'px';
            btn.appendChild(ripple);
            setTimeout(function () { ripple.remove(); }, 500);
        });
    });
}

/** Prevents duplicate form submissions application-wide, including forms inserted by live popups. */
function initGlobalSubmitGuard() {
    document.addEventListener('submit', function (event) {
        if (event.defaultPrevented) return;
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        if (form.checkValidity && !form.checkValidity()) return;
        if (form.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        form.dataset.submitting = 'true';
        if (window.BakirKhataPageLoader) window.BakirKhataPageLoader.show();
        const button = form.querySelector('button[type="submit"], button:not([type]), input[type="submit"]');
        if (button && typeof showButtonLoading === 'function') showButtonLoading(button);
    });
}

/** Shows a loading spinner on a submit button so slow requests give feedback. */
function showButtonLoading(button, label) {
    if (!button) return;
    button.disabled = true;
    button.dataset.originalHtml = button.innerHTML;
    button.innerHTML = '<span class="spinner"></span> ' + (label || 'Saving...');
}

/** Live server-sent notifications. Cash-payment requests appear without a page reload. */
function initLiveNotifications() {
    if (!window.EventSource) return;
    const token = currentTabAuthToken();
    if (!token || window.location.pathname === '/login' || window.location.pathname === '/register') return;
    const source = new EventSource('/notifications/stream?__tab=' + encodeURIComponent(token));
    window.addEventListener('pagehide', function () {
        source.close();
    }, { once: true });
    source.addEventListener('notification', function (event) {
        const parts = String(event.data || '').split('|');
        const type = parts[0] || 'NOTIFICATION';
        const message = parts[1] || 'You have a new notification.';
        const transactionId = parts[2] || '';
        const toast = document.createElement('div');
        toast.className = 'toast toast--success live-toast';
        const reviewUrl = appendTabToken('/notifications', token);
        toast.innerHTML = '<strong>' + escapeHtml(type.replaceAll('_', ' ')) + '</strong><div>' + escapeHtml(message) + '</div>' +
            (transactionId ? '<a href="' + reviewUrl + '" class="link-muted">Open notifications</a>' : '');
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        container.appendChild(toast);
        setTimeout(function () { toast.classList.add('toast--hide'); setTimeout(function () { toast.remove(); }, 250); }, 8000);

        if (type === 'CASH_PAYMENT_REQUEST' && /^\d+$/.test(transactionId)) {
            showCashPaymentConfirmation(transactionId, message, token);
        }
    });
}

function showCashPaymentConfirmation(transactionId, message, token) {
    const existing = document.querySelector('.cash-confirm-overlay[data-transaction-id="' + transactionId + '"]');
    if (existing) return;

    const overlay = document.createElement('div');
    overlay.className = 'cash-confirm-overlay';
    overlay.dataset.transactionId = transactionId;

    const dialog = document.createElement('div');
    dialog.className = 'cash-confirm-dialog';
    dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    const titleId = 'cashConfirmTitle-' + transactionId;
    dialog.setAttribute('aria-labelledby', titleId);

    const icon = document.createElement('div');
    icon.className = 'cash-confirm-dialog__icon';
    icon.innerHTML = '<i class="bi bi-cash-coin"></i>';

    const title = document.createElement('h3');
    title.id = titleId;
    title.textContent = 'Cash payment confirmation';

    const body = document.createElement('p');
    body.className = 'cash-confirm-dialog__message';
    body.textContent = message;

    const actions = document.createElement('div');
    actions.className = 'cash-confirm-dialog__actions';
    actions.appendChild(buildTransactionDecisionForm(transactionId, 'reject', 'Reject', 'btn btn--danger', token));
    actions.appendChild(buildTransactionDecisionForm(transactionId, 'accept', 'Accept payment', 'btn btn--primary', token));

    dialog.append(icon, title, body, actions);
    overlay.appendChild(dialog);
    document.body.appendChild(overlay);
}

function buildTransactionDecisionForm(transactionId, decision, label, className, token) {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/transactions/' + transactionId + '/' + decision;

    if (token) {
        const tab = document.createElement('input');
        tab.type = 'hidden';
        tab.name = '__tab';
        tab.value = token;
        form.appendChild(tab);
    }

    const csrf = document.querySelector('meta[name="_csrf"]');
    const csrfParameter = document.querySelector('meta[name="_csrf_parameter"]');
    if (csrf && csrfParameter) {
        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = csrfParameter.content;
        hidden.value = csrf.content;
        form.appendChild(hidden);
    }

    const button = document.createElement('button');
    button.type = 'submit';
    button.className = className;
    button.textContent = label;
    form.appendChild(button);
    return form;
}

function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value;
    return div.innerHTML;
}
