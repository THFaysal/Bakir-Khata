document.addEventListener('DOMContentLoaded', function () {
    const loanSelect = document.getElementById('loanId');
    const amountInput = document.getElementById('amount');
    const hint = document.getElementById('remainingHint');
    const dateInput = document.getElementById('paymentDate');
    const dateHint = document.getElementById('paymentDateHint');
    if (!loanSelect || !amountInput || !hint) return;

    // Payment date can never be after today, regardless of which loan is selected.
    const todayStr = new Date().toISOString().split('T')[0];

    function sync() {
        const selected = loanSelect.options[loanSelect.selectedIndex];
        const remaining = selected ? selected.getAttribute('data-remaining') : null;
        const borrowDate = selected ? selected.getAttribute('data-borrow-date') : null;

        if (remaining) {
            hint.textContent = 'Remaining balance: ৳ ' + Number(remaining).toLocaleString('en-US', { minimumFractionDigits: 2 });
            amountInput.max = remaining;
        } else {
            hint.textContent = '';
            amountInput.removeAttribute('max');
        }

        if (dateInput) {
            dateInput.max = todayStr;
            if (borrowDate) {
                dateInput.min = borrowDate;
                if (dateHint) {
                    dateHint.textContent = 'Must be between ' + borrowDate + ' and ' + todayStr;
                }
                // If a previously chosen date now falls outside the new loan's valid range, clear it
                // so the user can't accidentally submit a mismatched date.
                if (dateInput.value && (dateInput.value < borrowDate || dateInput.value > todayStr)) {
                    dateInput.value = '';
                }
            } else {
                dateInput.removeAttribute('min');
                if (dateHint) {
                    dateHint.textContent = '';
                }
            }
        }
    }

    loanSelect.addEventListener('change', sync);
    sync();
});