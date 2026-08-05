

document.addEventListener('DOMContentLoaded', function () {
    const loanSelect = document.getElementById('loanId');
    const amountInput = document.getElementById('amount');
    const hint = document.getElementById('remainingHint');
    if (!loanSelect || !amountInput || !hint) return;

    function sync() {
        const selected = loanSelect.options[loanSelect.selectedIndex];
        const remaining = selected ? selected.getAttribute('data-remaining') : null;
        if (remaining) {
            hint.textContent = 'Remaining balance: ৳ ' + Number(remaining).toLocaleString('en-US', { minimumFractionDigits: 2 });
            amountInput.max = remaining;
        } else {
            hint.textContent = '';
            amountInput.removeAttribute('max');
        }
    }

    loanSelect.addEventListener('change', sync);
    sync();
});
