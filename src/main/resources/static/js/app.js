(function () {
    function setVisible(element, visible, displayValue) {
        if (!element) {
            return;
        }
        element.style.display = visible ? (displayValue || "block") : "none";
    }

    function bindOverlayClose(overlay, onClose) {
        if (!overlay || typeof onClose !== "function") {
            return;
        }

        overlay.addEventListener("click", function (event) {
            if (event.target === overlay) {
                onClose();
            }
        });
    }

    function bindEscapeClose(onClose) {
        if (typeof onClose !== "function") {
            return;
        }

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                onClose();
            }
        });
    }

    window.qScoutApp = {
        bindEscapeClose: bindEscapeClose,
        bindOverlayClose: bindOverlayClose,
        setVisible: setVisible
    };
}());
