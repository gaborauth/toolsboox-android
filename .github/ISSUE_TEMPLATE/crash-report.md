---
name: Crash report
about: Create a crash report to help us improve
title: ''
labels: ''
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**The stacktrace**
```
    kotlin.UninitializedPropertyAccessException: lateinit property roomId has not been initialized
        at com.toolsboox.plugin.teamdrawer.ui.NoteFragment.onResume(NoteFragment.kt:126)
        at androidx.fragment.app.Fragment.performResume(Fragment.java:3180)
        at androidx.fragment.app.FragmentStateManager.resume(FragmentStateManager.java:606)
        at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:285)
```

**Device details:**
 - Device: [e.g. Note Air]
 - OS: [e.g. 3.2.4]
 - Version [e.g. 1.2.0-01]

**Additional context**
Add any other context about the problem here.
