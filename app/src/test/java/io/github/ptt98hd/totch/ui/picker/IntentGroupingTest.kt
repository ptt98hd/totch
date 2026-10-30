package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.IntentEntry
import io.github.ptt98hd.totch.core.model.IntentTarget
import org.junit.Test
import kotlin.test.assertEquals

class IntentGroupingTest {
    private val entries = listOf(
        IntentEntry("org.zed", "zebra", "org.zed.Main", "org.zed.OPEN", IntentTarget.ACTIVITY),
        IntentEntry("com.t", "Tasker", "com.t.Receiver", "com.t.action.RUN", IntentTarget.BROADCAST),
        IntentEntry("com.t", "Tasker", "com.t.Edit", "com.t.action.EDIT", IntentTarget.ACTIVITY),
        IntentEntry("com.t", "Tasker", "com.t.Other", "net.x.action.EDIT", IntentTarget.ACTIVITY),
        IntentEntry("com.a", "alarm", "com.a.Ring", "com.a.RING", IntentTarget.BROADCAST),
    )

    @Test fun `IN-6 grouped per app, sorted by app label then short action, action and class`() {
        val groups = groupIntents(entries, "")
        assertEquals(listOf("alarm", "Tasker", "zebra"), groups.map { it.appLabel })
        assertEquals(
            listOf("com.t.action.EDIT", "net.x.action.EDIT", "com.t.action.RUN"),
            groups[1].intents.map { it.intentAction },
        )
    }

    @Test fun `IN-6 query matches action or app label`() {
        assertEquals(listOf("com.t.action.RUN"), groupIntents(entries, " run ").single().intents.map { it.intentAction })
        assertEquals(3, groupIntents(entries, "TASK").single().intents.size)
        assertEquals(emptyList(), groupIntents(entries, "zzz"))
    }
}
