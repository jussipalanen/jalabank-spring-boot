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
