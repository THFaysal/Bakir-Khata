

document.addEventListener('DOMContentLoaded', function () {
    const buttons = document.querySelectorAll('[data-login-mode]');
    const identifier = document.getElementById('identifier');
    const credential = document.getElementById('credential');
    const identifierLabel = document.getElementById('identifierLabel');
    const credentialLabel = document.getElementById('credentialLabel');
    const identifierIcon = document.getElementById('identifierIcon');
    if (!buttons.length || !identifier || !credential) return;

    const modes = {
        email: {
            identifierLabel: 'Email address',
            identifierType: 'email',
            identifierIcon: 'bi-envelope',
            credentialLabel: 'Password',
            credentialMinLength: 6,
            credentialInputMode: null
        },
        mobile: {
            identifierLabel: 'Mobile number',
            identifierType: 'tel',
            identifierIcon: 'bi-phone',
            credentialLabel: 'PIN',
            credentialMinLength: 4,
            credentialInputMode: 'numeric'
        }
    };

    function applyMode(mode) {
        const cfg = modes[mode];
        identifier.type = cfg.identifierType;
        identifierLabel.textContent = cfg.identifierLabel;
        if (identifierIcon) {
            identifierIcon.className = 'bi ' + cfg.identifierIcon + ' form-group__icon';
        }
        credentialLabel.textContent = cfg.credentialLabel;
        credential.minLength = cfg.credentialMinLength;
        if (cfg.credentialInputMode) {
            credential.setAttribute('inputmode', cfg.credentialInputMode);
            credential.setAttribute('pattern', '[0-9]*');
            credential.setAttribute('maxlength', '6');
        } else {
            credential.removeAttribute('inputmode');
            credential.removeAttribute('pattern');
            credential.removeAttribute('maxlength');
        }
        identifier.value = '';
        credential.value = '';
        identifier.focus();

        buttons.forEach(function (btn) {
            btn.classList.toggle('login-toggle__btn--active', btn.getAttribute('data-login-mode') === mode);
        });
    }

    buttons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            applyMode(btn.getAttribute('data-login-mode'));
        });
    });
});
