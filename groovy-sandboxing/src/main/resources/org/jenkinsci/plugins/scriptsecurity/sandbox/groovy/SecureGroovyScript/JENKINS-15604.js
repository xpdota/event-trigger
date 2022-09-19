// https://issues.jenkins-ci.org/browse/JENKINS-15604 workaround:
function cmChange(editor, change) {
    editor.save();
    $$('.validated').each(function (e) {e.onchange();});
}
