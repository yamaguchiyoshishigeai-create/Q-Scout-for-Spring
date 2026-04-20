(function () {
    var form = document.getElementById("analyzeForm");
    if (!form) {
        return;
    }

    var fileInput = document.getElementById("projectZip");
    var button = document.getElementById("submitButton");
    var running = document.getElementById("running");
    var modal = document.getElementById("uploadErrorModal");
    var modalTitle = document.getElementById("uploadErrorTitle");
    var modalBody = document.getElementById("uploadErrorBody");
    var modalRetry = document.getElementById("uploadErrorRetry");
    var modalClose = document.getElementById("uploadErrorClose");
    var maxUploadBytes = Number(form.dataset.maxUploadBytes || 0);
    var helpers = window.qScoutApp || {};

    function setRunning(visible) {
        if (helpers.setVisible) {
            helpers.setVisible(running, visible, "block");
            return;
        }
        running.style.display = visible ? "block" : "none";
    }

    function hideUploadError() {
        modal.classList.remove("is-visible");
    }

    function showUploadError(title, body, retry) {
        modalTitle.textContent = title;
        modalBody.textContent = body;
        modalRetry.textContent = retry;
        modal.classList.add("is-visible");
        button.disabled = false;
        setRunning(false);
    }

    function isTooLarge(file) {
        return Boolean(file) && maxUploadBytes > 0 && file.size > maxUploadBytes;
    }

    function handleTooLargeFile() {
        showUploadError(
            form.dataset.uploadTooLargeTitle,
            form.dataset.uploadTooLargeBody,
            form.dataset.uploadTooLargeRetry
        );
        fileInput.value = "";
    }

    fileInput.addEventListener("change", function () {
        var file = fileInput.files[0];
        if (isTooLarge(file)) {
            handleTooLargeFile();
        }
    });

    form.addEventListener("submit", function (event) {
        var file = fileInput.files[0];
        if (isTooLarge(file)) {
            event.preventDefault();
            handleTooLargeFile();
            return;
        }
        button.disabled = true;
        setRunning(true);
    });

    modalClose.addEventListener("click", hideUploadError);

    if (helpers.bindOverlayClose) {
        helpers.bindOverlayClose(modal, hideUploadError);
    }

    if (helpers.bindEscapeClose) {
        helpers.bindEscapeClose(hideUploadError);
    }

    if (modal.dataset.open === "true") {
        showUploadError(
            modal.dataset.serverTitle || form.dataset.uploadTooLargeTitle,
            modal.dataset.serverBody || form.dataset.uploadTooLargeBody,
            modal.dataset.serverRetry || form.dataset.uploadTooLargeRetry
        );
    }
}());
