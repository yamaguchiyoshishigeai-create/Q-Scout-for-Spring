(function () {
    var form = document.getElementById("analyzeForm");
    if (!form) {
        return;
    }

    var fileInput = document.getElementById("projectZip");
    var fileField = document.querySelector(".custom-file-field");
    var fileButton = document.querySelector(".custom-file-button");
    var fileName = document.getElementById("projectZipFileName");
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

    function applyCustomFileStyles() {
        if (fileField) {
            fileField.style.display = "flex";
            fileField.style.alignItems = "stretch";
            fileField.style.flex = "1 1 auto";
            fileField.style.minWidth = "0";
            fileField.style.width = "100%";
            fileField.style.border = "1px solid #c9d7d8";
            fileField.style.borderRadius = "12px";
            fileField.style.background = "#fff";
            fileField.style.overflow = "hidden";
        }
        if (fileInput) {
            fileInput.style.position = "absolute";
            fileInput.style.left = "-10000px";
            fileInput.style.top = "auto";
            fileInput.style.width = "1px";
            fileInput.style.height = "1px";
            fileInput.style.opacity = "0";
        }
        if (fileButton) {
            fileButton.style.display = "inline-flex";
            fileButton.style.alignItems = "center";
            fileButton.style.justifyContent = "center";
            fileButton.style.flex = "0 0 auto";
            fileButton.style.margin = "0";
            fileButton.style.padding = "0 18px";
            fileButton.style.borderRight = "1px solid #c9d7d8";
            fileButton.style.background = "#f6faf9";
            fileButton.style.color = "var(--ink)";
            fileButton.style.fontWeight = "700";
            fileButton.style.cursor = "pointer";
            fileButton.style.whiteSpace = "nowrap";
        }
        if (fileName) {
            fileName.style.display = "block";
            fileName.style.flex = "1 1 auto";
            fileName.style.minWidth = "0";
            fileName.style.padding = "14px";
            fileName.style.color = "var(--muted)";
            fileName.style.overflow = "hidden";
            fileName.style.textOverflow = "ellipsis";
            fileName.style.whiteSpace = "nowrap";
        }
    }

    function getEmptyFileLabel() {
        if (fileField && fileField.dataset.emptyLabel) {
            return fileField.dataset.emptyLabel;
        }
        return "";
    }

    function getSelectedFile(source) {
        if (source && source.files && source.files.length > 0) {
            return source.files[0];
        }
        if (fileInput && fileInput.files && fileInput.files.length > 0) {
            return fileInput.files[0];
        }
        return null;
    }

    function updateSelectedFileName(source) {
        if (!fileName) {
            return;
        }
        var file = getSelectedFile(source);
        fileName.textContent = file ? file.name : getEmptyFileLabel();
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
        updateSelectedFileName(fileInput);
    }

    fileInput.addEventListener("change", function (event) {
        var source = event.target;
        var file = getSelectedFile(source);
        if (isTooLarge(file)) {
            handleTooLargeFile();
            return;
        }
        updateSelectedFileName(source);
    });

    form.addEventListener("submit", function (event) {
        var file = getSelectedFile(fileInput);
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

    applyCustomFileStyles();
    updateSelectedFileName(fileInput);

    if (modal.dataset.open === "true") {
        showUploadError(
            modal.dataset.serverTitle || form.dataset.uploadTooLargeTitle,
            modal.dataset.serverBody || form.dataset.uploadTooLargeBody,
            modal.dataset.serverRetry || form.dataset.uploadTooLargeRetry
        );
    }
}());
