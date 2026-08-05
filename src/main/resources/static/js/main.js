

document.addEventListener('DOMContentLoaded', function () {
    initSidebarToggle();
    initToasts();
    initButtonRipple();
});

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

/** Shows a loading spinner on a submit button so slow requests give feedback. */
function showButtonLoading(button, label) {
    if (!button) return;
    button.disabled = true;
    button.dataset.originalHtml = button.innerHTML;
    button.innerHTML = '<span class="spinner"></span> ' + (label || 'Saving...');
}
