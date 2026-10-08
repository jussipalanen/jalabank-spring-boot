// Small enhancements for the Jalabank pages. Everything also works without JavaScript.
document.addEventListener("DOMContentLoaded", () => {
    // Ask before destructive actions: <button data-confirm="Are you sure?">
    document.querySelectorAll("[data-confirm]").forEach((element) => {
        element.addEventListener("click", (event) => {
            if (!window.confirm(element.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Open a link in a small pop-up window: <a href="..." target="name" data-popup="width=460,height=640">.
    // If the browser blocks the pop-up, the link opens normally in a new tab.
    document.querySelectorAll("a[data-popup]").forEach((link) => {
        link.addEventListener("click", (event) => {
            const popup = window.open(link.href, link.target || "_blank", `popup,${link.dataset.popup}`);
            if (popup) {
                popup.focus();
                event.preventDefault();
            }
        });
    });

    // Copy a value to the clipboard: <button data-copy="123456">
    document.querySelectorAll("button[data-copy]").forEach((button) => {
        const label = button.textContent;
        button.addEventListener("click", async () => {
            try {
                await navigator.clipboard.writeText(button.dataset.copy);
            } catch {
                // Older browsers, or a page that is not https or localhost
                const field = document.createElement("textarea");
                field.value = button.dataset.copy;
                document.body.append(field);
                field.select();
                document.execCommand("copy");
                field.remove();
            }
            button.textContent = "Copied";
            setTimeout(() => (button.textContent = label), 1500);
            const status = document.getElementById("copy-status");
            if (status) {
                status.textContent = `Copied ${button.dataset.copy}. Close this window and paste it into the code field.`;
            }
        });
    });

    // Keep only digits in a code field, so a pasted "123 456" still fits: <input data-digits="6">.
    // When the user comes back from the authenticator window, the field is ready for pasting.
    document.querySelectorAll("input[data-digits]").forEach((input) => {
        const length = Number(input.dataset.digits);
        input.addEventListener("input", () => {
            const digits = input.value.replace(/\D/g, "").slice(0, length);
            if (digits !== input.value) {
                input.value = digits;
            }
        });
        window.addEventListener("focus", () => {
            if (!input.value) {
                input.focus();
            }
        });
    });

    // Close a pop-up window: <button data-close-window>
    document.querySelectorAll("[data-close-window]").forEach((button) => {
        button.addEventListener("click", () => window.close());
    });

    // Submit a filter form as soon as a select changes: <select data-autosubmit>
    document.querySelectorAll("select[data-autosubmit]").forEach((select) => {
        select.addEventListener("change", () => select.form.submit());
    });

    // Row selection for deleting several transactions
    const selectAll = document.getElementById("select-all");
    const rowChecks = Array.from(document.querySelectorAll("input.row-select"));
    const deleteSelected = document.getElementById("delete-selected");
    const selectedCount = document.getElementById("selected-count");

    const update = () => {
        const checked = rowChecks.filter((check) => check.checked).length;
        rowChecks.forEach((check) => check.closest("tr").classList.toggle("selected", check.checked));
        if (selectAll) {
            selectAll.checked = checked > 0 && checked === rowChecks.length;
            selectAll.indeterminate = checked > 0 && checked < rowChecks.length;
        }
        if (deleteSelected) {
            deleteSelected.disabled = checked === 0;
            deleteSelected.dataset.confirm = `Delete ${checked} selected transaction${checked === 1 ? "" : "s"}?`;
        }
        if (selectedCount) {
            selectedCount.textContent = checked;
        }
    };

    if (selectAll) {
        selectAll.addEventListener("change", () => {
            rowChecks.forEach((check) => (check.checked = selectAll.checked));
            update();
        });
    }
    rowChecks.forEach((check) => check.addEventListener("change", update));
    update();
});
