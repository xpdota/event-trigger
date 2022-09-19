document.addEventListener("DOMContentLoaded", function() {
    function adjustCheckboxAvailability(event) {
        // event.target is the path/url input
        // parent is the first common parent for root level elements in config.jelly
        // from parent we can target child elements via querySelector to not harm other elements on the page
        const parent = event.target.parentElement.parentElement.parentElement;

        const classpathEntryPath = parent.querySelector(".secure-groovy-script__classpath-entry-path");
        const classpathApproveCheckbox = parent.querySelector(".secure-groovy-script__classpath-approve");
        if (!classpathApproveCheckbox) {
            return;
        }

        const classpathEntryOldPath = parent.querySelector(".secure-groovy-script__classpath-entry-old-path");
        const classpathEntryPathEdited = classpathEntryPath.value !== classpathEntryOldPath.value;

        if (classpathEntryPathEdited) {
            classpathApproveCheckbox.setAttribute("disabled", "true");
        } else {
            classpathApproveCheckbox.removeAttribute("disabled");
        }
    }

    const classpaths = document.querySelectorAll(".secure-groovy-script__classpath-entry-path");
    classpaths.forEach(function(classpath) {
        classpath.addEventListener("blur", adjustCheckboxAvailability);
    })
});